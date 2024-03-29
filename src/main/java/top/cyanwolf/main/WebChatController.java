package top.cyanwolf.main;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@ServerEndpoint(value = "/webchat/websocket/{key}")
public class WebChatController {

    private static final Connection jt;
    private static final PreparedStatement ps1;
    private static final PreparedStatement ps2;
    private static final PreparedStatement ps3;

    static {
        try {
            jt = DriverManager.getConnection("jdbc:mysql://172.17.0.7:3306/Views?useSSL=false&autoreconnect=true&autoreconnectforpools=true&failOverReadOnly=false", "reader", "Reader75846");
            ps1 = jt.prepareStatement("select id,username from Users where tempkey=?");
            ps2 = jt.prepareStatement("insert into `chatmessages`(time,sender,groupid,content,encrypted) values(?,?,?,?,?)");
            ps3 = jt.prepareStatement("select name from all_members where id=?");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ConcurrentHashMap<Long, Session> sessions = new ConcurrentHashMap<Long, Session>();
    private MyUser user;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @OnOpen
    public void open(Session s, @PathParam(value = "key") String key) throws SQLException {
        ps1.setString(1, key);
        ResultSet rs = ps1.executeQuery();
        rs.next();
        user = new MyUser();
        user.setId(rs.getLong(1));
        ps3.setLong(1, user.getId());
        rs = ps3.executeQuery();
        rs.next();
        user.setName(rs.getString(1));
        ps3.clearParameters();
        ps1.clearParameters();
        sessions.put(user.getId(), s);
        for(Session session : sessions.values()){
            session.getAsyncRemote().sendText(new JSONObject(new WebChatMessage(0, getOnlineCount(), user.getId(), "上线 " + user.getName(), false).toMap()).toString());
        }
    }
    @OnMessage
    public void message(String msg) throws SQLException {
        JSONObject jsonObject = new JSONObject(msg);
        String content = jsonObject.getString("content");
        boolean encrypted = jsonObject.getBoolean("encrypted");
        int sessionID = jsonObject.getInt("sessionID");
        ps2.setString(1, SDF.format(new Date()));
        ps2.setLong(2, user.getId());
        ps2.setLong(3, sessionID);
        ps2.setString(4, content);
        ps2.setBoolean(5, encrypted);
        ps2.execute();
        ps2.clearParameters();
        if(sessionID == 1){
            for(Session session : sessions.values()){
                session.getAsyncRemote().sendText(new JSONObject(new WebChatMessage(1, 1, user.getId(), user.getName() + " " + content, false).toMap()).toString());
            }
        }
    }
    @OnClose
    public void close() throws IOException {
        sessions.remove(user.getId()).close();
        for(Session session : sessions.values()){
            session.getAsyncRemote().sendText(new JSONObject(new WebChatMessage(0, getOnlineCount(), user.getId(), "下线 " + user.getName(), false).toMap()).toString());
        }
    }
    @OnError
    public void error(Session session, Throwable e){
        e.printStackTrace();
    }
    private static synchronized int getOnlineCount(){
        return sessions.size();
    }
}
