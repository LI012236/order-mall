package com.example.mall.controller;

import com.example.mall.common.ApiResponse;
import com.example.mall.common.PageResult;
import com.example.mall.dto.ProductRequest;
import com.example.mall.model.Product;
import com.example.mall.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台商品管理（需要 ADMIN 角色，由 AuthInterceptor 统一校验）
 */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResult<Product>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "category", required = false) String category) {

        List<Product> list = productService.listAll(keyword, category, page, size);
        long total = productService.countAll(keyword, category);
        return ApiResponse.ok(PageResult.of(list, total, Math.max(page, 1), Math.min(Math.max(size, 1), 100)));
    }

    @PostMapping
    public ApiResponse<Product> create(@RequestBody ProductRequest req) {
        return ApiResponse.ok(productService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> update(@PathVariable("id") long id, @RequestBody ProductRequest req) {
        return ApiResponse.ok(productService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> offline(@PathVariable("id") long id) {
        productService.offline(id);
        return ApiResponse.ok(Map.of("id", id, "status", 0));
    }
}
