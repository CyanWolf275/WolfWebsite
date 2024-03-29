package top.cyanwolf.main;

import jakarta.websocket.Session;

public class WebChatData {
    public Session session;
    public long sessionID;
    public WebChatData(Session session, long sessionID){
        this.session = session;
        this.sessionID = sessionID;
    }
}
