package com.example.mall.controller;

import com.example.mall.common.ApiResponse;
import com.example.mall.common.BizException;
import com.example.mall.common.IdempotentRetryException;
import com.example.mall.common.PageResult;
import com.example.mall.dto.CreateOrderRequest;
import com.example.mall.model.Order;
import com.example.mall.model.OrderItem;
import com.example.mall.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<Order> create(@RequestAttribute("userId") Long userId,
                                     @RequestBody CreateOrderRequest req) {
        try {
            return ApiResponse.ok(orderService.create(userId, req));
        } catch (IdempotentRetryException e) {
            // 并发下唯一索引拦下重复插入，新事务里已能查到先落库的那一单
            Order exist = orderService.findByRequestId(e.getRequestId());
            if (exist == null) {
                throw new BizException(500, "并发写入冲突");
            }
            return ApiResponse.ok(exist);
        }
    }

    @GetMapping
    public ApiResponse<PageResult<Order>> list(@RequestAttribute("userId") Long userId,
                                               @RequestParam(name = "page", defaultValue = "1") int page,
                                               @RequestParam(name = "size", defaultValue = "10") int size,
                                               @RequestParam(name = "status", required = false) String status) {
        List<Order> list = orderService.page(userId, status, page, size);
        long total = orderService.count(userId, status);
        return ApiResponse.ok(PageResult.of(list, total, Math.max(page, 1), Math.min(Math.max(size, 1), 100)));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<Map<String, Object>> detail(@RequestAttribute("userId") Long userId,
                                                   @PathVariable("orderNo") String orderNo) {
        Order order = orderService.requireOwned(userId, orderNo);
        List<OrderItem> items = orderService.items(order.id());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order", order);
        data.put("items", items);
        return ApiResponse.ok(data);
    }

    /** 模拟支付：真实项目会跳转收银台，这里直接改状态 */
    @PostMapping("/{orderNo}/pay")
    public ApiResponse<Order> pay(@RequestAttribute("userId") Long userId,
                                  @PathVariable("orderNo") String orderNo) {
        return ApiResponse.ok(orderService.pay(userId, orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<Order> cancel(@RequestAttribute("userId") Long userId,
                                     @PathVariable("orderNo") String orderNo) {
        return ApiResponse.ok(orderService.cancel(userId, orderNo));
    }

    @PostMapping("/{orderNo}/confirm")
    public ApiResponse<Order> confirm(@RequestAttribute("userId") Long userId,
                                      @PathVariable("orderNo") String orderNo) {
        return ApiResponse.ok(orderService.confirm(userId, orderNo));
    }
}
