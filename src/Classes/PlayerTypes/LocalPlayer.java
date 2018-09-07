package Classes.PlayerTypes;

import Classes.NetworkCommunication.PlayerData;
import Classes.OrbData;

import java.util.Random;

/**
 * Created by HydrusBeta on 7/26/2017.
 */
public class LocalPlayer extends PlayerData {

    public LocalPlayer(String username, boolean isHost){
        super(username,LocalPlayer.createID(isHost));
    }

    // create a (probably) unique player ID
    private static long createID(boolean isHost){
        long playerID;
        if(isHost) playerID = 0;
        else{
            do{
                playerID = (new Random()).nextLong();
                System.out.println("player ID is: " + playerID);
            } while (playerID == 0 || playerID == -1); // A non-host local player absolutely cannot have an ID of 0 or -1. These are reserved for the host and unclaimed player slots, respectively.
        }
        return playerID;
    }

    public double computeInitialDistance(OrbData orbData){
        return 0.0;
    }
}
