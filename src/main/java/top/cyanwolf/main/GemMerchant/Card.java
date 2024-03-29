package top.cyanwolf.main.GemMerchant;

import com.google.gson.Gson;

public class Card {
    private int level;
    private int score;
    private GemType gem;
    private int[] requirements;

    public Card(int level, int score, GemType gem, int[] requirements){
        this.level = level;
        this.score = score;
        this.gem = gem;
        this.requirements = requirements;
    }
    public int[] getRequirements(){
        return requirements;
    }
    public GemType getGem(){
        return gem;
    }
    public String toJson(){
        return new Gson().toJson(this);
    }
}
