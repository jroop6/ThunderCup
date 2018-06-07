package Classes.NetworkCommunication;

import java.io.Serializable;

/**
 * The host periodically broadcasts a LatencyPacket to all clients. The clients set the playerID of the packet they
 * receive and send it back immediately. When the host receives the packets, it can retrieve the latency for each player
 * by calling getLatency()
 */
public class LatencyPacket implements Serializable {
    private long playerID;
    private long timeSent;

    public LatencyPacket(){
        this.timeSent = System.nanoTime();
    }

    public long getPlayerID(){
        return playerID;
    }
    public long getLatency(){
        return (System.nanoTime()-timeSent)/2;
    }

    public void setPlayerID(long playerID){
        this.playerID = playerID;
    }
}
