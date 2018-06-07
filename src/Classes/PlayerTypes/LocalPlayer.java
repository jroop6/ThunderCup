package Classes.PlayerTypes;

import Classes.Cannon;
import Classes.Character;
import Classes.NetworkCommunication.PlayerData;
import Classes.Orb;
import Classes.PlayPanel;
import javafx.scene.input.MouseEvent;

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


    /* Implementing abstract methods from Player class: */

    public void registerToPlayPanel(PlayPanel playPanel){
        playPanel.addEventHandler(MouseEvent.MOUSE_MOVED,(event)->{
            double mouseRelativeX = event.getX() - cannon.getPosX();
            double mouseRelativeY = event.getY() - cannon.getPosY();
            double newAngle = Math.atan2(mouseRelativeY,mouseRelativeX)*180.0/Math.PI + 90.0;
            playerData.changeCannonAngle(newAngle); // updates model
            cannon.setAngle(newAngle); // updates view
        });

        // Add the ability to fire the cannon:
        playPanel.addEventHandler(MouseEvent.MOUSE_PRESSED, (event) ->{
            double mouseRelativeX = event.getX() - cannon.getPosX();
            double mouseRelativeY = event.getY() - cannon.getPosY();
            double newAngle = Math.atan2(mouseRelativeY,mouseRelativeX);
            System.out.println("cannon fired!: (" + mouseRelativeX + ", " + mouseRelativeY + "). Angle = " + newAngle);
            playPanel.fireCannon(this, newAngle); // The orb needs to be fired from the PlayPanel in order to obtain the Orb's starting position (The LocalPlayer has no idea where it's cannon is located on the PlayPanel)
        });

        // The angle of the cannon should still be controllable while the mouse button is pressed down:
        playPanel.addEventHandler(MouseEvent.MOUSE_DRAGGED,(event)->{
            double mouseRelativeX = event.getX() - cannon.getPosX();
            double mouseRelativeY = event.getY() - cannon.getPosY();
            double newAngle = Math.atan2(mouseRelativeY,mouseRelativeX)*180.0/Math.PI + 90.0;
            playerData.changeCannonAngle(newAngle); // updates model
            cannon.setAngle(newAngle); // updates view
        });
    }

    public void rotateCannon(PlayerData playerData){
        // cannon rotation is handled by the MouseEvent handlers, above. Nothing to do here.
    }

    public void shootCannon(PlayerData playerData){
        //ToDo: check for consistency with data obtained from the server
    }

    public double computeInitialDistance(Orb orb){
        return 0.0;
    }
}
