package com.example.mall.model;

import java.math.BigDecimal;

public record Product(
        Long id,
        String name,
        String category,
        BigDecimal price,
        Integer stock,
        String coverUrl,
        String description,
        Integer status,
        Integer version,
        String createdAt,
        String updatedAt
) {
}
