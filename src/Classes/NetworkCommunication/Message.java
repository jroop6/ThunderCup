package Classes.NetworkCommunication;
import java.io.Serializable;

public class Message implements Serializable, Comparable<Message> {
    String string;
    private long playerID;
    public Message (String string, long playerID){
        this.string = string;
        this.playerID = playerID;
    }
    public Message (Message other){
        this.string = other.string;
        this.playerID = other.playerID;
    }
    public int compareTo(Message other){
        if(playerID==other.playerID && string.equals(other.string)) return 0;
        else return -1;
    }

    public String getString(){
        return string;
    }
    public long getPlayerID(){
        return playerID;
    }
}