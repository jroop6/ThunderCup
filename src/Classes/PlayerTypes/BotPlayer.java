package Classes.PlayerTypes;

import Classes.Cannon;
import Classes.Character;
import Classes.Images.CannonImages;
import Classes.Images.CharacterImages;
import Classes.NetworkCommunication.PlayerData;
import Classes.Orb;
import Classes.PlayPanel;

import java.util.Random;

/**
 * Created by HydrusBeta on 8/6/2017.
 */
public class BotPlayer extends Player {

    PlayPanel playPanel;

    //
    public BotPlayer(){
        // create a (probably) unique player ID:
        long playerID;
        do{
            playerID = (new Random()).nextLong();
            System.out.println("player ID is: " + playerID);
        } while (playerID == 0 || playerID == -1); // A non-host player absolutely cannot have an ID of 0 or -1. These are reserved for the host and unclaimed player slots, respectively.

        // initialize model:
        this.playerData = new PlayerData("FillyBot [easy]",playerID);
        playerData.setCannon(CannonImages.BOT_CANNON);
        playerData.setCharacter(CharacterImages.FILLY_BOT);

        // initialize "views":
        this.cannon = new Cannon(playerData);
        this.character = new Character(playerData);
        character.setCharacterEnum(CharacterImages.FILLY_BOT); // character initializes with a default player, so change it to a bot, here.
        usernameButton.setText(playerData.getUsername());
        teamChoice.getSelectionModel().select(playerData.getTeam()-1);

    }


    /* Implementing abstract methods from Player class: */

    public void registerToPlayPanel(PlayPanel playPanel){
        this.playPanel = playPanel;
    }

    private long nextTime = 0;
    public void tick(){
        //Todo: implement this.
        // temporary implementation, just to see if the framework is working:

        if(System.nanoTime() > nextTime){
            double newAngle = 180.0*(new Random().nextDouble()) - 90.0; // degrees
            System.out.println("new angle is " + newAngle);
            playerData.changeCannonAngle(newAngle); // degrees
            cannon.setAngle(newAngle); //updates view

            nextTime = System.nanoTime() + 2000000000;
            playPanel.fireCannon(this, (newAngle - 90.0)*Math.PI/180.0); // radians
        }
    }

    public double computeInitialDistance(Orb orb){
        return 0.0;
    }
}

