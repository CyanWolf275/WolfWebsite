package top.cyanwolf.main.GemMerchant;

import com.google.gson.Gson;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;

import java.util.concurrent.ConcurrentHashMap;

@Controller
@ServerEndpoint("splendor/wss/{id}")
public class GameController {
    private static final ConcurrentHashMap<Long, Session> sessions = new ConcurrentHashMap<Long, Session>();
    private static final Room room = new Room();
    private static final Gson gson = new Gson();

    @OnOpen
    public void open(Session s, @PathParam(value = "id") String id){
        sessions.put(Long.parseLong(id), s);
        if(room.getStatus() == Room.Status.PREPARING)
            room.addPlayer(new Player(s, Long.parseLong(id)));
    }
    @OnMessage
    public void onMessage(String msg){
        JSONObject jsonObject = new JSONObject(msg);
        String action = jsonObject.getString("action");
        if(action.equals("start")){
            if(room.getPlayers().size() > 1){
                Board b = room.start();
                int id = 0;
                for(Player p : room.getPlayers()){
                    p.getSession().getAsyncRemote().sendText(gson.toJson(b));
                    b.id = id++;
                }
            }
        }else{
            int id = jsonObject.getInt("id");
            if(action.equals("take_gems")){
                int[] gemlst = new int[6];
                for(int i=0;i<6;i++){
                    gemlst[i] = jsonObject.getJSONArray("gemlst").getInt(i);
                }
                room.takeGems(gemlst, id);
                Board b = room.getBoard();
                for(Player p : room.getPlayers()){
                    p.getSession().getAsyncRemote().sendText(gson.toJson(b));
                }
            }else if(action.equals("purchase_card")){
                
            }
        }
    }
}
