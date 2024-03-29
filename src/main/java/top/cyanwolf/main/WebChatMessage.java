package top.cyanwolf.main;

import java.util.HashMap;
import java.util.Map;

public class WebChatMessage {
    public int type;
    public int sessionID;
    public long sender;
    public String content;
    public boolean encrypted;
    public WebChatMessage(int type, int sessionID, long sender, String content, boolean encrypted){
        this.type = type;
        this.sessionID = sessionID;
        this.sender = sender;
        this.content = content;
        this.encrypted = encrypted;
    }
    public Map<String, Object> toMap(){
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("type", type);
        result.put("sessionID", sessionID);
        result.put("sender", sender);
        result.put("content", content);
        result.put("encrypted", encrypted);
        return result;
    }
}
