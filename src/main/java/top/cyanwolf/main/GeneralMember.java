package top.cyanwolf.main;

public class GeneralMember {
    public long id;
    public String name;
    public String jointime;
    public int age;
    public int level;
    public String lastSeen;

    public GeneralMember(long id, String name, String jointime, int age, int level){
        this.id = id;
        this.name = name;
        this.jointime = jointime;
        this.age = age;
        this.level = level;
    }
    public void setLastSeen(String lastSeen){
        this.lastSeen = lastSeen;
    }
    public String getLastSeen(){
        return lastSeen;
    }
}
