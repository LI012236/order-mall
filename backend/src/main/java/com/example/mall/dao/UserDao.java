package com.example.mall.dao;

import com.example.mall.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class UserDao {

    private static final RowMapper<User> MAPPER = (rs, i) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("salt"),
            rs.getString("nickname"),
            rs.getString("phone"),
            rs.getString("role"),
            rs.getString("created_at")
    );

    private static final String COLUMNS =
            "id, username, password, salt, nickname, phone, role, created_at";

    private final JdbcTemplate jdbc;

    public UserDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public User findByUsername(String username) {
        List<User> list = jdbc.query(
                "SELECT " + COLUMNS + " FROM mall_user WHERE username = ?", MAPPER, username);
        return list.isEmpty() ? null : list.get(0);
    }

    public User findById(long id) {
        List<User> list = jdbc.query(
                "SELECT " + COLUMNS + " FROM mall_user WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long insert(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO mall_user(username, password, salt, nickname, phone, role, created_at)"
                            + " VALUES (?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.username());
            ps.setString(2, user.password());
            ps.setString(3, user.salt());
            ps.setString(4, user.nickname());
            ps.setString(5, user.phone());
            ps.setString(6, user.role());
            ps.setString(7, user.createdAt());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }
}
