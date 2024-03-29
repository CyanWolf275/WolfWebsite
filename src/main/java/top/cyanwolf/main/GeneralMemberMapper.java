package top.cyanwolf.main;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GeneralMemberMapper implements RowMapper<GeneralMember> {
    @Override
    public GeneralMember mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GeneralMember(rs.getLong("id"), rs.getString("name"), rs.getString("jointime"), rs.getInt("age"), rs.getInt("level"));
    }
}
