package com.example.mall.dto;

/**
 * 修改购物车：两个字段都可以单独传，为 null 表示不改
 */
public record UpdateCartRequest(Integer quantity, Integer checked) {
}
