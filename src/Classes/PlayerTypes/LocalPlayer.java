package Classes.PlayerTypes;

import Classes.Cannon;
import Classes.Character;
import Classes.NetworkCommunication.PlayerData;
import Classes.Orb;

import java.util.Random;

/**
 * Created by HydrusBeta on 7/26/2017.
 */
public class LocalPlayer extends Player {

    public LocalPlayer(String username, boolean isHost){
        // create a (probably) unique player ID:
        long playerID;
        if(isHost) playerID = 0;
        else{
            do{
                playerID = (new Random()).nextLong();
                System.out.println("player ID is: " + playerID);
            } while (playerID == 0 || playerID == -1); // A non-host local player absolutely cannot have an ID of 0 or -1. These are reserved for the host and unclaimed player slots, respectively.
        }

        // initialize playerData
        playerData = new PlayerData(username,playerID);

        // Initialize the "views" (playerData will specify a default character and cannon):
        cannon = new Cannon(playerData);
        character = new Character(playerData);
        usernameButton.setText(playerData.getUsername());
        teamChoice.getSelectionModel().select(playerData.getTeam()-1);
    }

    public double computeInitialDistance(Orb orb){
        return 0.0;
    }
}
