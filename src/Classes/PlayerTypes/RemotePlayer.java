package Classes.PlayerTypes;

import Classes.Cannon;
import Classes.CharacterData;
import Classes.NetworkCommunication.PlayerData;
import Classes.OrbData;

public class RemotePlayer extends Player {

    // The playerData came from the network and contains all of this player's information. Use it to initialize the RemotePlayer.
    public RemotePlayer(PlayerData playerData){
        this.playerData = playerData;
        this.cannon = new Cannon(playerData);
        this.characterData = new CharacterData(playerData.getCharacterEnum());
        usernameButton.setText(playerData.getUsername());
        teamChoice.getSelectionModel().select(playerData.getTeam()-1);
    }

    public double computeInitialDistance(OrbData orbData){
        return (playerData.getLatency()/1000000000)* orbData.getOrbColor().getOrbSpeed();
    }
}
