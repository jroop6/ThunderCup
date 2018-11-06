package Classes.NetworkCommunication;

import java.io.Serializable;

import static Classes.Player.GAME_ID;

/**
 * Created by Jonathan Roop on 8/4/2017.
 */
public class Packet implements Serializable{
    private Synchronizer synchronizer;
    private boolean connectionRejected = false;
    private int i=0;

    public Packet(Synchronizer synchronizer){
        //if(synchronizer!=null) this.synchronizer = synchronizer.copyForNetworking();
        if(synchronizer!=null) this.synchronizer = synchronizer;
    }

    public void rejectConnection(){
        connectionRejected = true;
    }

    public Synchronizer getSynchronizer(){
        return synchronizer;
    }

    public boolean isConnectionRejected(){
        return connectionRejected;
    }

    public void print(){
        System.out.println("************************");
        System.out.println("* Synchronizer is " + synchronizer);
        System.out.println("************************");
    }
}
