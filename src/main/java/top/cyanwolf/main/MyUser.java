package top.cyanwolf.main;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MyUser implements UserDetails {
    private long id;
    private String username;
    private String password;
    private boolean enabled = true;
    private boolean nonExpired = true;
    private boolean nonLocked = true;
    private boolean credentialsNonExpired = true;
    private String name;
    private boolean gpt4;
    private String secret;
    private String tempkey;
    private ChatUser chatUser;
    private List<String> roles = new ArrayList<String>();
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
        for(String role : roles)
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return nonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return nonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public long getId(){
        return id;
    }
    public String getName(){
        return name;
    }

    public void setId(long id){
        this.id = id;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

    public void setNonExpired(boolean nonExpired){
        this.nonExpired = nonExpired;
    }

    public void setNonLocked(boolean nonLocked){
        this.nonLocked = nonLocked;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired){
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public void setRoles(String role){
        List<String> roles = new ArrayList<String>();
        roles.add(role);
        this.roles = roles;
    }

    public List<String> getRoles(){
        return roles;
    }

    public void setGpt4(boolean gpt4) {
        this.gpt4 = gpt4;
    }

    public boolean getGpt4(){
        return gpt4;
    }
    public String getSecret(){
        return secret;
    }
    public void setSecret(String secret){
        this.secret = secret;
    }
    public String getTempkey(){
        return tempkey;
    }
    public void setTempkey(String tempkey){
        this.tempkey = tempkey;
    }
    public void setChatuser(ChatUser chatUser){
        this.chatUser = chatUser;
    }
    public ChatUser getChatUser(){
        return chatUser;
    }
}
