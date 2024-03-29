package top.cyanwolf.main;

import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ComponentList {
    public ArrayList<ChatUser> list;
    public ComponentList(){
        this.list = new ArrayList<ChatUser>();
    }
}
