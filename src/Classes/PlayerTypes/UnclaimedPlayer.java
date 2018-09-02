package Classes.PlayerTypes;

import Classes.*;
import Classes.Character;
import Classes.NetworkCommunication.PlayerData;

/**
 * Created by HydrusBeta on 8/2/2017.
 */
public class UnclaimedPlayer extends Player {

    // The playerData came from the network and contains all of this player's information. Use it to initialize the RemotePlayer.
    public UnclaimedPlayer(){
        this.playerData = new PlayerData("Open Slot",-1);
        this.cannon = new Cannon(playerData);
        this.character = new Character(playerData.getCharacterEnum());
        usernameButton.setText(playerData.getUsername());
    }

    /* Implementing abstract methods from Player class: */

    public void registerToPlayPanel(PlayPanel playPanel){
        // Anything needed here?
    }

    public void rotateCannon(PlayerData playerData){
        // cannon rotation is handled by the MouseEvent handlers, above
    }

    public void shootCannon(PlayerData playerData){
        //ToDo: check for consistency with data obtained from the server
    }

    // Note: this method is irrelevant and should never be called
    public double computeInitialDistance(Orb orb){
        System.err.println("computeInitialDistance() was called on an UnclaimedPlayer instance... This was probably" +
                "not supposed to happen");
        return 0.0;
    }
}
