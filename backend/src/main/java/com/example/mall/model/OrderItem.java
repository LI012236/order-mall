package com.example.mall.model;

import java.math.BigDecimal;

/**
 * 订单明细：product_name 与 price 是下单时的快照，
 * 商品后续改名/改价不会影响历史订单。
 */
public record OrderItem(
        Long id,
        Long orderId,
        Long productId,
        String productName,
        BigDecimal price,
        Integer quantity,
        BigDecimal amount
) {
}
