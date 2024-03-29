package top.cyanwolf.main.GemMerchant;

import java.util.ArrayList;
import java.util.List;

public class Room {
    public enum Status{
        PREPARING,
        INGAME,
        ENDED
    }
    private List<Player> playerList;
    private int id;
    private List<Card> cards1;
    private List<Card> cards2;
    private List<Card> cards3;
    private int[] gems;
    private int currentPlayer;
    private Status status;
    public Room(){
        playerList = new ArrayList<Player>();
        cards1 = new ArrayList<Card>();
        cards2 = new ArrayList<Card>();
        cards3 = new ArrayList<Card>();
        gems = new int[6];
        status = Status.PREPARING;
        for(int i=0;i<6;i++)
            gems[i] = 6;
    }
    public Status getStatus(){
        return status;
    }
    public Board start(){
        currentPlayer = (int)(Math.random() * playerList.size());
        loadCards();
        return getBoard();
    }
    public Board getBoard(){
        Board b = new Board();
        List<Card> lv1 = new ArrayList<Card>();
        List<Card> lv2 = new ArrayList<Card>();
        List<Card> lv3 = new ArrayList<Card>();
        for(int i = 0; i<Math.min(cards1.size(), 4); i++)
            lv1.add(cards1.get(i));
        for(int i = 0; i<Math.min(cards2.size(), 4); i++)
            lv2.add(cards2.get(i));
        for(int i = 0; i<Math.min(cards3.size(), 4); i++)
            lv3.add(cards3.get(i));
        b.cards1 = lv1;
        b.cards2 = lv2;
        b.cards3 = lv3;
        b.gems = gems;
        b.currentPlayer = currentPlayer;
        b.playerList = playerList;
        return b;
    }
    public int getCurrentPlayer(){
        return currentPlayer;
    }
    public void addPlayer(Player p){
        playerList.add(p);
    }
    public List<Player> getPlayers(){
        return playerList;
    }
    public boolean takeGems(int[] gemlst, int player){
        int multiple = -1;
        for(int i=0;i<gemlst.length;i++)
            if(gemlst[i] > 1)
                multiple = i;
        if(multiple > -1){
            gems[multiple] -= gemlst[multiple];
            playerList.get(player).getGemRepo()[multiple] += gemlst[multiple];
        }else{
            for(int i=0;i<gems.length;i++){
                gems[i] -= gemlst[i];
                playerList.get(player).getGemRepo()[i] += gemlst[i];
            }
        }
        currentPlayer = (currentPlayer + 1) % playerList.size();
        return true;
    }
    private void loadCards(){
        //TODO
    }


}
