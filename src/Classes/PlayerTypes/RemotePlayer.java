package Classes.PlayerTypes;

import Classes.Cannon;
import Classes.Character;
import Classes.NetworkCommunication.PlayerData;
import Classes.Orb;

public class RemotePlayer extends Player {

    // The playerData came from the network and contains all of this player's information. Use it to initialize the RemotePlayer.
    public RemotePlayer(PlayerData playerData){
        this.playerData = playerData;
        this.cannon = new Cannon(playerData);
        this.character = new Character(playerData);
        usernameButton.setText(playerData.getUsername());
        teamChoice.getSelectionModel().select(playerData.getTeam()-1);
    }

    public double computeInitialDistance(Orb orb){
        return (playerData.getLatency()/1000000000)*orb.getOrbEnum().getOrbSpeed();
    }
}
