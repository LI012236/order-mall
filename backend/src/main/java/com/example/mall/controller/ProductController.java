package com.example.mall.controller;

import com.example.mall.common.ApiResponse;
import com.example.mall.common.PageResult;
import com.example.mall.model.Product;
import com.example.mall.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前台商品接口（无需登录）
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResult<Product>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "category", required = false) String category) {

        List<Product> list = productService.listOnSale(keyword, category, page, size);
        long total = productService.countOnSale(keyword, category);
        return ApiResponse.ok(PageResult.of(list, total, Math.max(page, 1), Math.min(Math.max(size, 1), 100)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> detail(@PathVariable("id") long id) {
        return ApiResponse.ok(productService.getOnSale(id));
    }
}
