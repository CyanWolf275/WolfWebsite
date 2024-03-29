package top.cyanwolf.main;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {
    public long id;
    public String time;
    public int type;
    public long sender;
    public String name;
    public String content;
    public Message(long id, Date time, int type, BigInteger sender, String name, String content){
        this.id = id;
        this.time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
        this.type = type;
        this.sender = sender.longValue();
        this.name = name;
        this.content = content;
    }
}
