package com.example.mall.dao;

import com.example.mall.model.CartItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CartDao {

    private static final RowMapper<CartItem> MAPPER = (rs, i) -> new CartItem(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("product_id"),
            rs.getInt("quantity"),
            rs.getInt("checked"),
            rs.getString("product_name"),
            rs.getBigDecimal("price"),
            rs.getInt("stock"),
            rs.getString("cover_url")
    );

    private static final String SELECT_BASE =
            "SELECT c.id, c.user_id, c.product_id, c.quantity, c.checked,"
                    + " p.name AS product_name, p.price, p.stock, p.cover_url"
                    + " FROM cart c JOIN product p ON p.id = c.product_id";

    private final JdbcTemplate jdbc;

    public CartDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CartItem> findByUser(long userId) {
        return jdbc.query(SELECT_BASE + " WHERE c.user_id = ? ORDER BY c.id DESC", MAPPER, userId);
    }

    public List<CartItem> findCheckedByUser(long userId) {
        return jdbc.query(
                SELECT_BASE + " WHERE c.user_id = ? AND c.checked = 1 ORDER BY c.id DESC", MAPPER, userId);
    }

    public CartItem findById(long id) {
        List<CartItem> list = jdbc.query(SELECT_BASE + " WHERE c.id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 加入购物车：依靠唯一索引 uk_user_product 做「存在则累加」
     */
    public void upsert(long userId, long productId, int quantity) {
        jdbc.update(
                "INSERT INTO cart(user_id, product_id, quantity) VALUES (?,?,?)"
                        + " ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)",
                userId, productId, quantity);
    }

    public int updateQuantity(long id, int quantity) {
        return jdbc.update("UPDATE cart SET quantity = ? WHERE id = ?", quantity, id);
    }

    public int updateChecked(long id, int checked) {
        return jdbc.update("UPDATE cart SET checked = ? WHERE id = ?", checked, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM cart WHERE id = ?", id);
    }

    public int deleteByUserAndProducts(long userId, List<Long> productIds) {
        if (productIds.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", productIds.stream().map(x -> "?").toList());
        Object[] args = new Object[productIds.size() + 1];
        args[0] = userId;
        for (int i = 0; i < productIds.size(); i++) {
            args[i + 1] = productIds.get(i);
        }
        return jdbc.update("DELETE FROM cart WHERE user_id = ? AND product_id IN (" + placeholders + ")", args);
    }

    public int clear(long userId) {
        return jdbc.update("DELETE FROM cart WHERE user_id = ?", userId);
    }
}
