package Classes.NetworkCommunication;

import Classes.PlayPanel;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Jonathan Roop on 8/4/2017.
 */
public class Packet implements Serializable{
    private Synchronizer synchronizer;
    private boolean connectionRejected = false;

    public Packet(Synchronizer synchronizer){
        if(synchronizer!=null) this.synchronizer = synchronizer.copyForNetworking();
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
