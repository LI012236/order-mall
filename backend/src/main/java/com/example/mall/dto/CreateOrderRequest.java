package com.example.mall.dto;

public record CreateOrderRequest(
        String requestId,
        String receiverName,
        String receiverPhone,
        String receiverAddress
) {
}
