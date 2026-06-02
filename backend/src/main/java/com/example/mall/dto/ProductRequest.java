package com.example.mall.dto;

import java.math.BigDecimal;

/**
 * 商品新增 / 修改入参。
 * JSON 字段：name、category、price、stock、cover_url、description
 */
public record ProductRequest(
        String name,
        String category,
        BigDecimal price,
        Integer stock,
        String coverUrl,
        String description
) {
}
