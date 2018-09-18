package Classes.PlayerTypes;

import Classes.*;
import Classes.CharacterData;
import Classes.NetworkCommunication.PlayerData;
import Classes.NetworkCommunication.Synchronizer;

/**
 * Created by Jonathan Roop on 8/2/2017.
 */
public class UnclaimedPlayer extends PlayerData {

    // The playerData came from the network and contains all of this player's information. Use it to initialize the RemotePlayer.
    public UnclaimedPlayer(Synchronizer synchronizer){
        super("Open Slot",UNCLAIMED_PLAYER_ID,synchronizer);
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
    public double computeInitialDistance(OrbData orbData){
        System.err.println("computeInitialDistance() was called on an UnclaimedPlayer instance... This was probably" +
                "not supposed to happen");
        return 0.0;
    }
}
