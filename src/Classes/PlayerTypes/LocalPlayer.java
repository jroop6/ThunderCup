package Classes.PlayerTypes;

import Classes.NetworkCommunication.PlayerData;
import Classes.NetworkCommunication.Synchronizer;
import Classes.OrbData;

import java.util.Random;

/**
 * Created by HydrusBeta on 7/26/2017.
 */
public class LocalPlayer extends PlayerData {

    public LocalPlayer(String username, boolean isHost, Synchronizer synchronizer){
        super(username,LocalPlayer.createID(isHost), synchronizer);
    }

    // create a (probably) unique player ID
    private static long createID(boolean isHost){
        long playerID;
        if(isHost) playerID = HOST_ID;
        else{
            do{
                playerID = (new Random()).nextLong();
                if(playerID>0) playerID = -playerID;
                System.out.println("player ID is: " + playerID);
            } while (playerID == HOST_ID || playerID == UNCLAIMED_PLAYER_ID || playerID == GAME_ID);
        }
        return playerID;
    }

    public double computeInitialDistance(OrbData orbData){
        return 0.0;
    }
}
