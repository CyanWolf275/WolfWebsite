package top.cyanwolf.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MyUser user = userMapper.loadUserByUserName(username);
        if(Objects.isNull(user)){
            throw new UsernameNotFoundException("User not found");
        }
        user.setRoles(userMapper.getRolesByUsername(username));
        user.setName(userMapper.getName(user.getId()));
        user.setGpt4(userMapper.getGpt4(user.getId()));
        user.setSecret(userMapper.getSecret(user.getId()));
        String key = UUID.randomUUID().toString().replaceAll("-", "");
        userMapper.setKey(user.getId(), key);
        user.setTempkey(key);
        try {
            user.setChatuser(new ChatUser(user.getId(), "C:\\Users\\Administrator\\Desktop\\chat\\", user.getGpt4()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return user;
    }
}
