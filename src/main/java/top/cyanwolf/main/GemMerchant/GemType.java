package top.cyanwolf.main.GemMerchant;

public enum GemType {
    RED(0), GREEN(1), BLUE(2), PURPLE(3), WHITE(4), GOLD(5);

    private final int value;

    GemType(int value){
        this.value = value;
    }
    public int getValue(){
        return value;
    }
}
