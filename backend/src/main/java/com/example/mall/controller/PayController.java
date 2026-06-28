package com.example.mall.controller;

import com.example.mall.common.ApiResponse;
import com.example.mall.dto.PayCallbackRequest;
import com.example.mall.model.Order;
import com.example.mall.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模拟第三方支付回调（无需登录，靠验签保证安全）
 */
@RestController
@RequestMapping("/api/pay")
public class PayController {

    private final OrderService orderService;

    public PayController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/callback")
    public ApiResponse<Map<String, Object>> callback(@RequestBody PayCallbackRequest req) {
        Order order = orderService.handlePayCallback(req.orderNo(), req.tradeNo(), req.sign());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order_no", order.orderNo());
        data.put("status", order.status());
        data.put("paid_at", order.paidAt());
        return ApiResponse.ok(data);
    }
}
