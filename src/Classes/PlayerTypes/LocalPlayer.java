package Classes.PlayerTypes;

import Classes.Cannon;
import Classes.Character;
import Classes.NetworkCommunication.PlayerData;
import Classes.Orb;
import Classes.PlayPanel;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

import java.util.Random;

/**
 * Created by HydrusBeta on 7/26/2017.
 */
public class LocalPlayer extends Player {

    PlayPanel playPanel;

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
        this.playPanel = playPanel;
        /*playPanel.addEventHandler(MouseEvent.MOUSE_MOVED,(event)->{
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
            playPanel.fireCannon(this, newAngle); // The orb needs to be fired from the PlayPanel in order to add the fired orb to the playPanelData.
        });

        // The angle of the cannon should still be controllable while the mouse button is pressed down:
        playPanel.addEventHandler(MouseEvent.MOUSE_DRAGGED,(event)->{
            double mouseRelativeX = event.getX() - cannon.getPosX();
            double mouseRelativeY = event.getY() - cannon.getPosY();
            double newAngle = Math.atan2(mouseRelativeY,mouseRelativeX)*180.0/Math.PI + 90.0;
            playerData.changeCannonAngle(newAngle); // updates model
            cannon.setAngle(newAngle); // updates view
        });*/
    }

    // Points the cannon at a position given in scene coordinates.
    public void pointCannon(double sceneX, double sceneY){
        Point2D localLoc = playPanel.sceneToLocal(sceneX, sceneY);
        double mouseRelativeX = localLoc.getX() - cannon.getPosX();
        double mouseRelativeY = localLoc.getY() - cannon.getPosY();
        double newAngleRad = Math.atan2(mouseRelativeY,mouseRelativeX);
        double newAngleDeg = newAngleRad*180.0/Math.PI + 90.0;
        cannon.setAngle(newAngleDeg); // updates view
        playerData.changeCannonAngle(newAngleDeg); // updates model
    }

    public void fireCannon(){
        Orb firedOrb = changeFireCannon(); // This will update the playerData model
        playPanel.getPlayPanelData().getShootingOrbs().add(firedOrb); // updates playPanelData model
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
