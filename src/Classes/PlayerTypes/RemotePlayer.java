package Classes.PlayerTypes;

import Classes.Cannon;
import Classes.Character;
import Classes.NetworkCommunication.PlayerData;
import Classes.Orb;
import Classes.PlayPanel;

/**
 * For the moment, this is a copy of Localplayer, just to serve as a placeholder. Eventually this will need to be changed.
 */
public class RemotePlayer extends Player {

    // The playerData came from the network and contains all of this player's information. Use it to initialize the RemotePlayer.
    public RemotePlayer(PlayerData playerData){
        this.playerData = playerData;
        this.cannon = new Cannon(playerData);
        this.character = new Character(playerData);
        usernameButton.setText(playerData.getUsername());
        teamChoice.getSelectionModel().select(playerData.getTeam()-1);
    }


    /* Implementing abstract methods from Player class: */

    public void registerToPlayPanel(PlayPanel playPanel){
        // Anything needed here?
    }

    public void rotateCannon(PlayerData playerData){
        this.playerData.setCannonAngle(playerData.getCannonAngle()); // updates model
        cannon.setAngle(playerData.getCannonAngle()); // updates view
    }

    public void shootCannon(PlayerData playerData){
        // do nothing for remote players
    }

    public double computeInitialDistance(Orb orb){
        return (playerData.getLatency()/1000000000)*orb.getOrbEnum().getOrbSpeed();
    }
}
