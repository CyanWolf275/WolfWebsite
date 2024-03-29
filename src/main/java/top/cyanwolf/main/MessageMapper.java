package top.cyanwolf.main;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MessageMapper implements RowMapper<Message> {
    @Override
    public Message mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Message(rs.getLong("id"), rs.getTimestamp("time"), rs.getInt("type"), rs.getBigDecimal("sender").toBigInteger(), rs.getString("name"), rs.getString("content"));
    }
}
