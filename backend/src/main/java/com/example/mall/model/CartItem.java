package com.example.mall.model;

/**
 * 购物车行（含联表查出的商品信息）
 */
public record CartItem(
        Long id,
        Long userId,
        Long productId,
        Integer quantity,
        Integer checked,
        String productName,
        java.math.BigDecimal price,
        Integer stock,
        String coverUrl
) {
}
