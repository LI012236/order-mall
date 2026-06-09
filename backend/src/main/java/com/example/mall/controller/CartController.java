package com.example.mall.controller;

import com.example.mall.common.ApiResponse;
import com.example.mall.dto.AddCartRequest;
import com.example.mall.dto.CartView;
import com.example.mall.dto.UpdateCartRequest;
import com.example.mall.service.CartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车（需要登录，userId 由 AuthInterceptor 注入）
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestAttribute("userId") Long userId) {
        List<CartView> items = cartService.list(userId);
        int checkedCount = items.stream().filter(x -> x.checked() == 1).mapToInt(CartView::quantity).sum();
        return ApiResponse.ok(Map.of("list", items, "checked_quantity", checkedCount));
    }

    @PostMapping
    public ApiResponse<CartView> add(@RequestAttribute("userId") Long userId,
                                     @RequestBody AddCartRequest req) {
        return ApiResponse.ok(cartService.add(userId, req.productId(), req.quantity()));
    }

    @PutMapping("/{id}")
    public ApiResponse<CartView> update(@RequestAttribute("userId") Long userId,
                                        @PathVariable("id") long id,
                                        @RequestBody UpdateCartRequest req) {
        return ApiResponse.ok(cartService.update(userId, id, req.quantity(), req.checked()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> remove(@RequestAttribute("userId") Long userId,
                                                   @PathVariable("id") long id) {
        cartService.remove(userId, id);
        return ApiResponse.ok(Map.of("deleted", true, "id", id));
    }
}
