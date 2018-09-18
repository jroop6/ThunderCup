package Classes.PlayerTypes;

import Classes.NetworkCommunication.PlayerData;
import Classes.NetworkCommunication.Synchronizer;
import Classes.OrbData;

public class RemotePlayer extends PlayerData {

    // The playerData came from the network and contains all of this player's information. Use it to initialize the RemotePlayer.
    public RemotePlayer(String username, long playerID, Synchronizer synchronizer){
        super(username, playerID, synchronizer);
    }

    public double computeInitialDistance(OrbData orbData){
        return (getLatency()/1000000000)* orbData.getOrbColor().getOrbSpeed();
    }
}
