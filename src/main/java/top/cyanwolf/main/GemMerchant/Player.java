package top.cyanwolf.main.GemMerchant;

import com.google.gson.Gson;
import jakarta.websocket.Session;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private int score;
    private long id;
    private List<Card> cardList;
    private List<Merchant> merchantList;
    private List<Card> reservedCards;
    private int[] gemRepo;
    private Session session;
    public Player(Session session, long id){
        score = 0;
        cardList = new ArrayList<Card>();
        merchantList = new ArrayList<Merchant>();
        reservedCards = new ArrayList<Card>();
        gemRepo = new int[6];
        this.session = session;
        this.id = id;
    }
    public int getScore(){
        return score;
    }
    public Session getSession(){
        return session;
    }
    public int[] getGemRepo(){
        return gemRepo;
    }
    public void getCard(Card c, int[] currency){
        cardList.add(c);
        for(int i=0;i<currency.length;i++)
            gemRepo[i] -= currency[i];
    }
    public boolean checkCard(Card c){
        int[] requirements = c.getRequirements();
        for(int i=0;i<requirements.length;i++)
            if(getTotalGem(i) < requirements[i])
                return false;
        return true;
    }
    public boolean checkReserved(Card c){
        int[] requirements = c.getRequirements();
        int count = 0;
        for(int i=0;i<requirements.length;i++)
            if(getTotalGem(i) < requirements[i])
                count++;
        return count <= 1;
    }
    public int getTotalGem(int type){
        int count = gemRepo[type];
        for(Card c : cardList)
            if(c.getGem().getValue() == type)
                count++;
        return count;
    }
    public String toJson(){
        return new Gson().toJson(this);
    }
}
