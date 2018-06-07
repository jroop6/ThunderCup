package Classes.NetworkCommunication;
import java.io.Serializable;

public class Message implements Serializable {
    String string;
    long playerID;
    public Message (String string, long playerID){
        this.string = string;
        this.playerID = playerID;
    }
    public String getString(){
        return string;
    }
    public long getPlayerID(){
        return playerID;
    }
}