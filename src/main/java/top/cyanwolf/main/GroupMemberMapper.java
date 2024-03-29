package top.cyanwolf.main;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GroupMemberMapper implements RowMapper<GroupMember> {
    @Override
    public GroupMember mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GroupMember(rs.getString("name"), rs.getInt("count(*)"));
    }
}
