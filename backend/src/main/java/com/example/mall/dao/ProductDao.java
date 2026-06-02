package com.example.mall.dao;

import com.example.mall.model.Product;
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
public class ProductDao {

    private static final RowMapper<Product> MAPPER = (rs, i) -> new Product(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getBigDecimal("price"),
            rs.getInt("stock"),
            rs.getString("cover_url"),
            rs.getString("description"),
            rs.getInt("status"),
            rs.getInt("version"),
            rs.getString("created_at"),
            rs.getString("updated_at")
    );

    private static final String COLUMNS =
            "id, name, category, price, stock, cover_url, description, status, version, created_at, updated_at";

    private final JdbcTemplate jdbc;

    public ProductDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 动态条件查询：keyword 走名称模糊匹配，category 精确匹配，status 为空表示不限
     */
    private String buildWhere(String keyword, String category, Integer status, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND name LIKE ?");
            args.add("%" + keyword.trim() + "%");
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND category = ?");
            args.add(category.trim());
        }
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }
        return where.toString();
    }

    public List<Product> search(String keyword, String category, Integer status, int offset, int size) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM product"
                + buildWhere(keyword, category, status, args)
                + " ORDER BY id DESC LIMIT ? OFFSET ?";
        args.add(size);
        args.add(offset);
        return jdbc.query(sql, MAPPER, args.toArray());
    }

    public long count(String keyword, String category, Integer status) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM product" + buildWhere(keyword, category, status, args);
        Long total = jdbc.queryForObject(sql, Long.class, args.toArray());
        return total == null ? 0 : total;
    }

    public Product findById(long id) {
        List<Product> list = jdbc.query(
                "SELECT " + COLUMNS + " FROM product WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long insert(Product p) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO product(name, category, price, stock, cover_url, description, status)"
                            + " VALUES (?,?,?,?,?,?,1)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.name());
            ps.setString(2, p.category());
            ps.setBigDecimal(3, p.price());
            ps.setInt(4, p.stock());
            ps.setString(5, p.coverUrl());
            ps.setString(6, p.description());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    /** 更新时 version + 1，为后续乐观锁留接口 */
    public int update(long id, Product p) {
        return jdbc.update(
                "UPDATE product SET name=?, category=?, price=?, stock=?, cover_url=?, description=?,"
                        + " version = version + 1 WHERE id = ?",
                p.name(), p.category(), p.price(), p.stock(),
                p.coverUrl(), p.description(), id);
    }

    /** 软下架：不物理删除，历史订单仍可关联 */
    public int offline(long id) {
        return jdbc.update("UPDATE product SET status = 0, version = version + 1 WHERE id = ?", id);
    }

    /** 扣减库存（条件更新，库存不足时影响行数为 0）——M5 下单时使用 */
    public int deductStock(long productId, int quantity) {
        return jdbc.update(
                "UPDATE product SET stock = stock - ?, version = version + 1"
                        + " WHERE id = ? AND stock >= ? AND status = 1",
                quantity, productId, quantity);
    }

    /** 回补库存（取消订单时使用） */
    public int restoreStock(long productId, int quantity) {
        return jdbc.update(
                "UPDATE product SET stock = stock + ?, version = version + 1 WHERE id = ?",
                quantity, productId);
    }
}
