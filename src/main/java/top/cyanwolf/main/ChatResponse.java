package top.cyanwolf.main;

public class ChatResponse {
    String response;
    String time;
    int tokens;
    public ChatResponse(String response, String time, int tokens){
        this.response = response;
        this.time = time;
        this.tokens = tokens;
    }
}
