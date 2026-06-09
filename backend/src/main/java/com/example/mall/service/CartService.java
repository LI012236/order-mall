package com.example.mall.service;

import com.example.mall.common.BizException;
import com.example.mall.dao.CartDao;
import com.example.mall.dao.ProductDao;
import com.example.mall.dto.CartView;
import com.example.mall.model.CartItem;
import com.example.mall.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private static final int MAX_QUANTITY = 99;

    private final CartDao cartDao;
    private final ProductDao productDao;

    public CartService(CartDao cartDao, ProductDao productDao) {
        this.cartDao = cartDao;
        this.productDao = productDao;
    }

    public List<CartView> list(long userId) {
        return cartDao.findByUser(userId).stream().map(CartService::toView).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public CartView add(long userId, Long productId, Integer quantity) {
        if (productId == null) {
            throw new BizException(400, "商品 ID 不能为空");
        }
        if (quantity == null || quantity <= 0 || quantity > MAX_QUANTITY) {
            throw new BizException(400, "购买数量需在 1-" + MAX_QUANTITY + " 之间");
        }

        Product product = productDao.findById(productId);
        if (product == null || product.status() == null || product.status() != 1) {
            throw new BizException(404, "商品不存在或已下架");
        }

        // 已存在的数量 + 本次数量，不能超过库存
        List<CartItem> existing = cartDao.findByUser(userId).stream()
                .filter(item -> item.productId().equals(productId))
                .toList();
        int already = existing.isEmpty() ? 0 : existing.get(0).quantity();
        if (already + quantity > product.stock()) {
            throw new BizException(409, "库存不足，当前库存 " + product.stock());
        }

        cartDao.upsert(userId, productId, quantity);
        CartItem item = cartDao.findByUser(userId).stream()
                .filter(x -> x.productId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new BizException(500, "购物车写入失败"));
        return toView(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public CartView update(long userId, long cartId, Integer quantity, Integer checked) {
        CartItem item = requireOwned(userId, cartId);

        if (quantity != null) {
            if (quantity <= 0 || quantity > MAX_QUANTITY) {
                throw new BizException(400, "购买数量需在 1-" + MAX_QUANTITY + " 之间");
            }
            if (quantity > item.stock()) {
                throw new BizException(409, "库存不足，当前库存 " + item.stock());
            }
            cartDao.updateQuantity(cartId, quantity);
        }
        if (checked != null) {
            if (checked != 0 && checked != 1) {
                throw new BizException(400, "checked 只能是 0 或 1");
            }
            cartDao.updateChecked(cartId, checked);
        }

        return toView(cartDao.findById(cartId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(long userId, long cartId) {
        requireOwned(userId, cartId);
        cartDao.delete(cartId);
    }

    /** 越权校验：只能操作自己的购物车 */
    private CartItem requireOwned(long userId, long cartId) {
        CartItem item = cartDao.findById(cartId);
        if (item == null) {
            throw new BizException(404, "购物车记录不存在");
        }
        if (!item.userId().equals(userId)) {
            throw new BizException(403, "无权操作他人的购物车");
        }
        return item;
    }

    private static CartView toView(CartItem item) {
        BigDecimal amount = item.price().multiply(BigDecimal.valueOf(item.quantity()))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        return new CartView(item.id(), item.productId(), item.productName(), item.price(),
                item.quantity(), item.checked(), item.stock(), item.coverUrl(), amount);
    }
}
