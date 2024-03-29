package top.cyanwolf.main;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MyUserMapper implements RowMapper<MyUser> {

    @Override
    public MyUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        MyUser user = new MyUser();
        user.setId(rs.getLong(1));
        user.setName(rs.getString(2));
        return user;
    }
}
