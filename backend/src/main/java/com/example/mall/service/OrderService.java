package com.example.mall.service;

import com.example.mall.common.BizException;
import com.example.mall.common.IdempotentRetryException;
import com.example.mall.common.PaySignUtil;
import com.example.mall.dao.CartDao;
import com.example.mall.dao.OrderDao;
import com.example.mall.dao.ProductDao;
import com.example.mall.dto.CreateOrderRequest;
import com.example.mall.model.CartItem;
import com.example.mall.model.Order;
import com.example.mall.model.OrderItem;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class OrderService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    private final OrderDao orderDao;
    private final CartDao cartDao;
    private final ProductDao productDao;

    public OrderService(OrderDao orderDao, CartDao cartDao, ProductDao productDao) {
        this.orderDao = orderDao;
        this.cartDao = cartDao;
        this.productDao = productDao;
    }

    /**
     * 下单主流程（一个事务里完成四件事）：
     * 1. 按当前价重算金额（不信任前端）
     * 2. 条件更新扣库存，任一商品失败则整体回滚
     * 3. 写订单 + 订单明细（快照）
     * 4. 清掉已下单的购物车记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Order create(long userId, CreateOrderRequest req) {
        String receiverName = trim(req.receiverName());
        String receiverPhone = trim(req.receiverPhone());
        String receiverAddress = trim(req.receiverAddress());

        if (receiverName.isEmpty() || receiverName.length() > 32) {
            throw new BizException(400, "收货人姓名需为 1-32 个字符");
        }
        if (!PHONE.matcher(receiverPhone).matches()) {
            throw new BizException(400, "手机号格式不正确");
        }
        if (receiverAddress.length() < 5 || receiverAddress.length() > 255) {
            throw new BizException(400, "收货地址需为 5-255 个字符");
        }

        String requestId = trim(req.requestId());
        if (requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        } else if (requestId.length() > 64) {
            throw new BizException(400, "request_id 长度不能超过 64");
        }

        // 幂等快速路径
        Order exist = orderDao.findByRequestId(requestId);
        if (exist != null) {
            if (!exist.userId().equals(userId)) {
                throw new BizException(409, "request_id 已被占用");
            }
            return exist;
        }

        List<CartItem> cartItems = cartDao.findCheckedByUser(userId);
        if (cartItems.isEmpty()) {
            throw new BizException(400, "购物车中没有已勾选的商品");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> itemSnapshots = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();

        for (CartItem cart : cartItems) {
            int rows = productDao.deductStock(cart.productId(), cart.quantity());
            if (rows == 0) {
                throw new BizException(409, "商品「" + cart.productName() + "」库存不足或已下架");
            }
            BigDecimal amount = cart.price()
                    .multiply(BigDecimal.valueOf(cart.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(amount);
            itemSnapshots.add(new OrderItem(null, null, cart.productId(), cart.productName(),
                    cart.price(), cart.quantity(), amount));
            productIds.add(cart.productId());
        }

        Order order = new Order(
                null,
                nextOrderNo(),
                userId,
                total.setScale(2, RoundingMode.HALF_UP),
                "CREATED",
                receiverName,
                receiverPhone,
                receiverAddress,
                requestId,
                LocalDateTime.now().format(FMT),
                null, null, null, null
        );

        Long orderId;
        try {
            orderId = orderDao.insert(order);
        } catch (DuplicateKeyException e) {
            // 并发下另一个线程先落库：抛异常触发回滚，撤销已扣库存
            throw new IdempotentRetryException(requestId);
        }

        Long finalOrderId = orderId;
        List<OrderItem> withOrderId = itemSnapshots.stream()
                .map(i -> new OrderItem(null, finalOrderId, i.productId(), i.productName(),
                        i.price(), i.quantity(), i.amount()))
                .toList();
        orderDao.insertItems(withOrderId);
        cartDao.deleteByUserAndProducts(userId, productIds);

        return order.withId(orderId);
    }

    @Transactional(readOnly = true)
    public List<Order> page(long userId, String status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 1);
        return orderDao.page(userId, status, (safePage - 1) * safeSize, safeSize);
    }

    @Transactional(readOnly = true)
    public long count(long userId, String status) {
        return orderDao.count(userId, status);
    }

    @Transactional(readOnly = true)
    public Order requireOwned(long userId, String orderNo) {
        Order order = orderDao.findByNo(orderNo);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!order.userId().equals(userId)) {
            throw new BizException(403, "无权查看他人订单");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderItem> items(long orderId) {
        return orderDao.findItems(orderId);
    }

    @Transactional(readOnly = true)
    public Order findByRequestId(String requestId) {
        return orderDao.findByRequestId(requestId);
    }

    // ---------------- 状态流转 ----------------

    @Transactional(rollbackFor = Exception.class)
    public Order pay(long userId, String orderNo) {
        Order order = requireOwned(userId, orderNo);
        return transition(order, "CREATED", "PAID", "paid_at", "当前订单状态不可支付");
    }

    /**
     * 取消订单：待支付和已支付都可取消，取消后回补库存。
     * 只允许创建者操作；状态用条件更新控制，避免并发重复回补。
     */
    @Transactional(rollbackFor = Exception.class)
    public Order cancel(long userId, String orderNo) {
        Order order = requireOwned(userId, orderNo);
        if (!"CREATED".equals(order.status()) && !"PAID".equals(order.status())) {
            throw new BizException(409, "当前订单状态不可取消（" + order.status() + "）");
        }

        int rows = orderDao.updateStatus(order.id(), order.status(), "CANCELLED", "cancelled_at");
        if (rows == 0) {
            throw new BizException(409, "订单状态已变更，请刷新后重试");
        }

        for (OrderItem item : orderDao.findItems(order.id())) {
            productDao.restoreStock(item.productId(), item.quantity());
        }
        return orderDao.findByNo(orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order confirm(long userId, String orderNo) {
        Order order = requireOwned(userId, orderNo);
        return transition(order, "SHIPPED", "COMPLETED", "completed_at", "只有已发货订单可以确认收货");
    }

    /** 后台发货：只有 ADMIN 能调用（权限由拦截器保证） */
    @Transactional(rollbackFor = Exception.class)
    public Order ship(String orderNo) {
        Order order = orderDao.findByNo(orderNo);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        return transition(order, "PAID", "SHIPPED", "shipped_at", "只有已支付订单可以发货");
    }

    /** 后台订单列表：userId 传 null 表示查全部 */
    @Transactional(readOnly = true)
    public List<Order> adminPage(String status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 1);
        return orderDao.page(null, status, (safePage - 1) * safeSize, safeSize);
    }

    @Transactional(readOnly = true)
    public long adminCount(String status) {
        return orderDao.count(null, status);
    }

    /**
     * 支付回调：外部系统通知，必须是幂等的。
     * 已支付 → 直接返回成功；已取消 → 拒绝；并发重复回调 → 条件更新只成功一次。
     */
    @Transactional(rollbackFor = Exception.class)
    public Order handlePayCallback(String orderNo, String tradeNo, String sign) {
        if (!PaySignUtil.verify(orderNo, tradeNo, sign)) {
            throw new BizException(400, "签名校验失败");
        }

        Order order = orderDao.findByNo(orderNo);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if ("CANCELLED".equals(order.status())) {
            throw new BizException(409, "订单已取消，不能支付");
        }
        if (!"CREATED".equals(order.status())) {
            return order;   // 已支付/已发货/已完成：幂等返回
        }

        int rows = orderDao.updateStatus(order.id(), "CREATED", "PAID", "paid_at");
        if (rows == 0) {
            Order latest = orderDao.findByNo(orderNo);
            if (latest != null && !"CREATED".equals(latest.status())) {
                return latest;   // 并发回调，已被另一请求处理
            }
            throw new BizException(409, "订单状态异常");
        }
        return orderDao.findByNo(orderNo);
    }

    private Order transition(Order order, String from, String to, String timeColumn, String message) {
        int rows = orderDao.updateStatus(order.id(), from, to, timeColumn);
        if (rows == 0) {
            Order latest = orderDao.findByNo(order.orderNo());
            String current = latest == null ? "UNKNOWN" : latest.status();
            throw new BizException(409, message + "（当前状态 " + current + "）");
        }
        return orderDao.findByNo(order.orderNo());
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private String nextOrderNo() {
        return "SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
