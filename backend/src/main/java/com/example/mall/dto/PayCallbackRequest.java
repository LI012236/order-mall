package com.example.mall.dto;

public record PayCallbackRequest(String orderNo, String tradeNo, String sign) {
}
