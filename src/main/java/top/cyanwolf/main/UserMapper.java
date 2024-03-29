package top.cyanwolf.main;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("select roles from Users where username=#{username}")
    public String getRolesByUsername(String username);
    @Select("select * from Users where username=#{username}")
    public MyUser loadUserByUserName(String username);
    @Select("select name from all_members where id=#{id}")
    public String getName(long id);
    @Select("select gpt4 from Users where id=#{id}")
    public boolean getGpt4(long id);
    @Select("select secret from Users where id=#{id}")
    public String getSecret(long id);
    @Update("update Users set tempkey=#{key} where id=#{id}")
    public void setKey(long id, String key);
}
