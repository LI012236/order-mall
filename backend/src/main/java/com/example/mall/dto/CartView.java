package com.example.mall.dto;

/**
 * 购物车展示对象：比数据库多一个小计 amount
 */
public record CartView(
        Long id,
        Long productId,
        String productName,
        java.math.BigDecimal price,
        Integer quantity,
        Integer checked,
        Integer stock,
        String coverUrl,
        java.math.BigDecimal amount
) {
}
