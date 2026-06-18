package com.example.mall.model;

import java.math.BigDecimal;

public record Order(
        Long id,
        String orderNo,
        Long userId,
        BigDecimal totalAmount,
        String status,
        String receiverName,
        String receiverPhone,
        String receiverAddress,
        String requestId,
        String createdAt,
        String paidAt,
        String shippedAt,
        String completedAt,
        String cancelledAt
) {
    public Order withId(Long newId) {
        return new Order(newId, orderNo, userId, totalAmount, status, receiverName, receiverPhone,
                receiverAddress, requestId, createdAt, paidAt, shippedAt, completedAt, cancelledAt);
    }
}
