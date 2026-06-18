package com.example.mall.dao;

import com.example.mall.model.Order;
import com.example.mall.model.OrderItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderDao {

    private static final RowMapper<Order> ORDER_MAPPER = (rs, i) -> new Order(
            rs.getLong("id"),
            rs.getString("order_no"),
            rs.getLong("user_id"),
            rs.getBigDecimal("total_amount"),
            rs.getString("status"),
            rs.getString("receiver_name"),
            rs.getString("receiver_phone"),
            rs.getString("receiver_address"),
            rs.getString("request_id"),
            rs.getString("created_at"),
            rs.getString("paid_at"),
            rs.getString("shipped_at"),
            rs.getString("completed_at"),
            rs.getString("cancelled_at")
    );

    private static final RowMapper<OrderItem> ITEM_MAPPER = (rs, i) -> new OrderItem(
            rs.getLong("id"),
            rs.getLong("order_id"),
            rs.getLong("product_id"),
            rs.getString("product_name"),
            rs.getBigDecimal("price"),
            rs.getInt("quantity"),
            rs.getBigDecimal("amount")
    );

    private static final String ORDER_COLUMNS =
            "id, order_no, user_id, total_amount, status, receiver_name, receiver_phone,"
                    + " receiver_address, request_id, created_at, paid_at, shipped_at, completed_at, cancelled_at";

    private final JdbcTemplate jdbc;

    public OrderDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long insert(Order o) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO orders(order_no, user_id, total_amount, status, receiver_name,"
                            + " receiver_phone, receiver_address, request_id, created_at)"
                            + " VALUES (?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, o.orderNo());
            ps.setLong(2, o.userId());
            ps.setBigDecimal(3, o.totalAmount());
            ps.setString(4, o.status());
            ps.setString(5, o.receiverName());
            ps.setString(6, o.receiverPhone());
            ps.setString(7, o.receiverAddress());
            ps.setString(8, o.requestId());
            ps.setString(9, o.createdAt());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void insertItems(List<OrderItem> items) {
        List<Object[]> batch = new ArrayList<>();
        for (OrderItem item : items) {
            batch.add(new Object[]{
                    item.orderId(), item.productId(), item.productName(),
                    item.price(), item.quantity(), item.amount()
            });
        }
        jdbc.batchUpdate(
                "INSERT INTO order_item(order_id, product_id, product_name, price, quantity, amount)"
                        + " VALUES (?,?,?,?,?,?)", batch);
    }

    public Order findByNo(String orderNo) {
        List<Order> list = jdbc.query(
                "SELECT " + ORDER_COLUMNS + " FROM orders WHERE order_no = ?", ORDER_MAPPER, orderNo);
        return list.isEmpty() ? null : list.get(0);
    }

    public Order findByRequestId(String requestId) {
        List<Order> list = jdbc.query(
                "SELECT " + ORDER_COLUMNS + " FROM orders WHERE request_id = ?", ORDER_MAPPER, requestId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<OrderItem> findItems(long orderId) {
        return jdbc.query(
                "SELECT id, order_id, product_id, product_name, price, quantity, amount"
                        + " FROM order_item WHERE order_id = ? ORDER BY id", ITEM_MAPPER, orderId);
    }

    private String buildWhere(Long userId, String status, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (userId != null) {
            where.append(" AND user_id = ?");
            args.add(userId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            args.add(status.trim());
        }
        return where.toString();
    }

    public List<Order> page(Long userId, String status, int offset, int size) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT " + ORDER_COLUMNS + " FROM orders"
                + buildWhere(userId, status, args)
                + " ORDER BY id DESC LIMIT ? OFFSET ?";
        args.add(size);
        args.add(offset);
        return jdbc.query(sql, ORDER_MAPPER, args.toArray());
    }

    public long count(Long userId, String status) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM orders" + buildWhere(userId, status, args);
        Long total = jdbc.queryForObject(sql, Long.class, args.toArray());
        return total == null ? 0 : total;
    }

    /** 状态流转：只允许从 fromStatus 变更到 toStatus，返回影响行数 */
    public int updateStatus(long id, String fromStatus, String toStatus, String timeColumn) {
        return jdbc.update(
                "UPDATE orders SET status = ?, " + timeColumn + " = NOW() WHERE id = ? AND status = ?",
                toStatus, id, fromStatus);
    }
}
