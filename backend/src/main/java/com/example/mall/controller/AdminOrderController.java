package com.example.mall.controller;

import com.example.mall.common.ApiResponse;
import com.example.mall.common.PageResult;
import com.example.mall.model.Order;
import com.example.mall.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台订单管理（需要 ADMIN 角色）
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResult<Order>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status) {

        List<Order> list = orderService.adminPage(status, page, size);
        long total = orderService.adminCount(status);
        return ApiResponse.ok(PageResult.of(list, total, Math.max(page, 1), Math.min(Math.max(size, 1), 100)));
    }

    @PutMapping("/{orderNo}/ship")
    public ApiResponse<Order> ship(@PathVariable("orderNo") String orderNo) {
        return ApiResponse.ok(orderService.ship(orderNo));
    }
}
