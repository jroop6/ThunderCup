package Classes.PlayerTypes;

import Classes.*;
import Classes.Character;
import Classes.Images.CannonImages;
import Classes.Images.CharacterImages;
import Classes.Animation.OrbImages;
import Classes.NetworkCommunication.PlayerData;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static Classes.NetworkCommunication.PlayPanelData.ARRAY_HEIGHT;


/**
 * Think of this as the controller in an MVC pattern. Player has a PlayerData (model) as well as a Cannon, Character,
 * Label, and ComboBox to display that data (view).
 */
public abstract class Player {
    // model:
    protected PlayerData playerData;

    // view:
    protected Button usernameButton;
    protected ComboBox<String> teamChoice = new ComboBox<>();
    protected Label latencyLabel = new Label("Latency: 0 milliseconds");
    protected Cannon cannon;

    // additional component controllers, each with models and views:
    protected Character character;

    // The PlayPanel associated with this player (needed for firing new shootingOrbs):
    PlayPanel playPanel;

    // EventHandlers:
    private EventHandler<MouseEvent> cannonChangeEventHandler;
    private EventHandler<MouseEvent> characterChangeEventHandler;

    // Data needed for constructing the shooter Orbs. These values are determined *once* by the host at the start
    // of the game and are never changed. Host and client maintain their own copies separately.
    private int seed;
    private Random ammunitionGenerator;

    // initialize the username Button, latency Label, ComboBox, and eventHandlers for changing cannon/character:
    public Player(){
        usernameButton = new Button();
        usernameButton.setFont(new Font(48.0));

        latencyLabel.setFont(new Font(36.0));

        teamChoice.getItems().addAll("Team 1", "Team 2", "Team 3", "Team 4", "Team 5", "Team 6", "Team 7", "Team 8", "Team 9", "Team 10");
        teamChoice.setStyle("-fx-font: 36.0px \"Comic Sans\";");

        cannonChangeEventHandler = event -> {
            System.out.println("clicky click on cannon");
            incrementCannonEnum();
        };
        characterChangeEventHandler = event -> {
            System.out.println("clicky click on character");
            incrementCharacterEnum();
        };

    }

    /* Abstract Methods: */
    public abstract double computeInitialDistance(Orb orb);

    /* Concrete methods: */
    public void registerToPlayPanel(PlayPanel playPanel){
        this.playPanel = playPanel;
    }
    public PlayerData getPlayerData(){
        return playerData;
    }
    public ImageView getCannonStaticBackground(){
        return cannon.getStaticBackground();
    }
    public ImageView getCannonStaticForeground(){
        return cannon.getStaticForeground();
    }
    public ImageView getCannonMovingPart(){
        return cannon.getMovingPart();
    }
    public Sprite getCharacterSprite(){
        return character.getSprite();
    }
    public Button getUsernameButton(){
        return usernameButton;
    }
    public ComboBox<String> getTeamChoice(){
        return teamChoice;
    }
    public Label getLatencyLabel(){
        return latencyLabel;
    }

    private void incrementCharacterEnum(){
        CharacterImages nextType = playerData.getCharacterEnum().next();
        if (this instanceof LocalPlayer){
            while(!nextType.isPlayable()){
                nextType = nextType.next();
            }
        }
        else{ // player must be an instance of BotPlayer
            while(nextType.getBotDifficulty()==null){
                nextType = nextType.next();
            }
            changeUsername("fillyBot [" + nextType.getBotDifficulty() +"]");
        }
        playerData.changeCharacter(nextType); // updates model
        character.setCharacterEnum(nextType); // updates view
    }
    private void incrementCannonEnum(){
        CannonImages nextType = playerData.getCannonEnum().next();
        while(!nextType.isSelectable()){
            nextType = nextType.next();
        }
        playerData.changeCannon(nextType); // updates model
        cannon.setCannonEnum(nextType); // updates view
    }

    public void changeResignPlayer(){
        playerData.changeDefeated(true); // updates model
        playerData.changeFrozen(true); // updates model
        playerData.changeCannonDisabled(true); // updates model
        freezePlayer(); // updates view (GameScene)
        // in the MultiplayerSceneSelection, the view is updated in either the deleteRemovedPlayer() or
        // processPacketAsClient() methods, for host and clients respectively.
    }

    public void changeDisableCannon(){
        playerData.changeCannonDisabled(true); // updates model
        // There is no change to the view, other than the fact that the player is no longer able to fire.
    }

    public void freezePlayer(){
        character.freeze(); // updates view (GameScene)
        cannon.freeze();// updates view (GameScene)
    }

    public void changeUsername(String newUsername){
        playerData.changeUsername(newUsername); // updates model
        usernameButton.setText(newUsername); // updates view
    }

    public void changeTeam(int newTeam){
        playerData.changeTeam(newTeam); // updates model
        // View is already updated. This function is called in response to a selection being made in the comboBox, which
        // serves as the view itself.
    }

    // Points the cannon at a position given in scene coordinates.
    public void pointCannon(double sceneX, double sceneY){
        if(playerData.getDefeated()) return;
        Point2D localLoc = playPanel.sceneToLocal(sceneX, sceneY);
        double mouseRelativeX = localLoc.getX() - cannon.getPosX();
        double mouseRelativeY = localLoc.getY() - cannon.getPosY(); // recall that the y-axis points down.
        double newAngleRad = Math.atan2(mouseRelativeY,mouseRelativeX);
        double newAngleDeg = newAngleRad*180.0/Math.PI; // 0 degrees points to the right, 90 degrees points straight up.
        pointCannon(newAngleDeg); // updates model and view.
    }

    // Points the cannon at a given angle, in degrees (0 degrees points to the right)
    public void pointCannon(double angle){
        if(playerData.getDefeated()) return;
        playerData.changeCannonAngle(angle); // updates model
    }

    private void setFireCannon(){
        if(playerData.getDefeated()) return;
        OrbImages newShooterOrbEnum = playPanel.getNextShooterOrbEnum(ammunitionGenerator.nextDouble());
        playerData.setFire(newShooterOrbEnum); // updates Player model
        // View is updated in the PlayPanel repaint() method, which paints the first two ammunitionOrbs on the canvas.
        // Note: The PlayPanel model was already updated via the updatePlayer() method in the PlayPanel class.
    }

    public void changeFireCannon(){
        if(playerData.getCannonDisabled()) return;
        OrbImages newShooterOrbEnum = playPanel.getNextShooterOrbEnum(ammunitionGenerator.nextDouble());
        Orb firedOrb = playerData.changeFire(cannon.getAngle()*(Math.PI/180), newShooterOrbEnum); // updates Player model
        Queue<Orb> firedOrbList = new LinkedList<>();
        firedOrbList.add(firedOrb);
        playPanel.getPlayPanelData().changeAddShootingOrbs(firedOrbList); // updates PlayPanel model
        // View is updated in the PlayPanel repaint() method, which paints the first two ammunitionOrbs on the canvas.
    }

    public void relocateCannon(double x, double y){
        cannon.relocate(x,y,playerData.getCannonAnimationFrame());
    }
    public void relocateCharacter(double x, double y){
        character.relocate(x,y);
    }
    public void setScale(double scaleFactor){
        cannon.setScale(scaleFactor, playerData.getCannonAnimationFrame());
        character.setScale(scaleFactor);
    }

    // Called every frame by the host to update a player's data according to a packet received over the network
    public void updateWithChangers(PlayerData newPlayerData, Map<Long, Long> latencies){
        if(newPlayerData.isFiring()){
            getPlayerData().getFiredOrbs().addAll(newPlayerData.getFiredOrbs()); // updates model
            getPlayerData().changeFiringFlag(true); // marks data as updated
            for(int i=0; i<newPlayerData.getFiredOrbs().size(); i++) setFireCannon(); // updates model
            getPlayerData().changeAmmunitionFlag(true); // marks data as updated
            // note: view will be updated in the PlayPanel's repaint() method.
        }
        if(newPlayerData.isCannonChanged()){
            playerData.changeCannon(newPlayerData.getCannonEnum()); // updates model
            cannon.setCannonEnum(newPlayerData.getCannonEnum()); // updates view //todo: update the view in a different method
        }
        if(newPlayerData.isCharacterChanged()){
            playerData.changeCharacter(newPlayerData.getCharacterEnum()); //updates model
            character.setCharacterEnum(newPlayerData.getCharacterEnum()); //updates view //todo: update the view in a different method
        }
        if(newPlayerData.isDefeatedChanged()){
            playerData.changeDefeated(newPlayerData.getDefeated()); //updates model
            // Note: In the MultiplayerSceneSelection, this player's corresponding PlayerSlot will be removed in either
            // the deleteRemovedPlayer() or processPacketAsClient() methods, for host and clients respectively.
            // In the GameScene, the player should also have been frozen. Updating the frozen state updates the view.
        }
        if(newPlayerData.isFrozenChanged()){
            playerData.changeFrozen(newPlayerData.getFrozen()); // updates model
        }
        if(newPlayerData.isTeamChanged()){
            playerData.changeTeam(newPlayerData.getTeam()); // updates model
            teamChoice.getSelectionModel().select(newPlayerData.getTeam()-1); // updates view //todo: update the view in a different method
        }
        if(newPlayerData.isUsernameChanged()){
            playerData.changeUsername(newPlayerData.getUsername()); // updates model
            usernameButton.setText(newPlayerData.getUsername()); // updates view //todo: update the view in a different method
        }

        playerData.changeCannonAngle(newPlayerData.getCannonAngle()); //updates model

        if(latencies!=null){ // Todo: This line is temporary. latencies should never be null in the final game.
            if(latencies.containsKey(playerData.getPlayerID())){
                playerData.changeLatency(latencies.get(playerData.getPlayerID())); // updates model
            }
            if(playerData.getLatency()<1000000) latencyLabel.setText(String.format("Latency: %d microseconds",playerData.getLatency()/1000L)); // updates view //todo: update the view in a different method
            else latencyLabel.setText(String.format("Latency: %d milliseconds",playerData.getLatency()/1000000L)); // updates view //todo: update the view in a different method
        }
    }


    // Called every frame by a client to update a player's data according to a packet received over the network. The
    // data is *not* updated if the locally-stored flags indicate that the player has manually changed something
    // since the last packet was sent. This is to prevent the host from overwriting the client's change request with
    // old data.
    public void updateWithSetters(PlayerData newPlayerData, boolean isLocalPlayer){
        if(newPlayerData.isFiring() && !isLocalPlayer){
            System.out.println("CLIENT: Another player has fired. Incrementing their ammunitionOrbs");
            for (int i=0; i<newPlayerData.getFiredOrbs().size(); i++) setFireCannon();
            // note: view will be updated in the PlayPanel's repaint() method.
        }
        if(newPlayerData.isCannonChanged() && !playerData.isCannonChanged()){
            playerData.setCannon(newPlayerData.getCannonEnum()); // updates model
            cannon.setCannonEnum(newPlayerData.getCannonEnum()); // updates view //todo: update the view in a different method
        }
        if(newPlayerData.isCharacterChanged() && !playerData.isCharacterChanged()){
            playerData.setCharacter(newPlayerData.getCharacterEnum()); //updates model
            character.setCharacterEnum(newPlayerData.getCharacterEnum()); //updates view //todo: update the view in a different method
        }
        if(newPlayerData.isDefeatedChanged() && !playerData.isDefeatedChanged()){
            playerData.setDefeated(newPlayerData.getDefeated()); //updates model
        }
        if(newPlayerData.isFrozenChanged() && !playerData.isFrozenChanged()){
            playerData.setFrozen(newPlayerData.getFrozen()); // updates model
        }
        if(newPlayerData.isTeamChanged() && !playerData.isTeamChanged()){
            playerData.setTeam(newPlayerData.getTeam()); // updates model
            teamChoice.getSelectionModel().select(newPlayerData.getTeam()-1); // updates view //todo: update the view in a different method
        }
        if(newPlayerData.isUsernameChanged() && !playerData.isUsernameChanged()){
            playerData.setUsername(newPlayerData.getUsername()); // updates model
            usernameButton.setText(newPlayerData.getUsername()); // updates view //todo: update the view in a different method
        }

        // remote players' cannon angles are always updated:
        if(!isLocalPlayer){
            playerData.setCannonAngle(newPlayerData.getCannonAngle()); //updates model
        }

        // players' latencies are always updated:
        playerData.setLatency(newPlayerData.getLatency()); // updates model
        if(newPlayerData.getLatency()<1000000) latencyLabel.setText(String.format("Latency: %d microseconds",newPlayerData.getLatency()/1000L)); // updates view //todo: update the view in a different method
        else latencyLabel.setText(String.format("Latency: %d milliseconds",newPlayerData.getLatency()/1000000L)); // updates view //todo: update the view in a different method

        // check for consistency between this player's ammunitionOrbs and the host's data for ammunitionOrbs. If they
        // are different for too long, then override the client's data with the host's data.
        playerData.checkForConsistency(newPlayerData);

    }

    public void updateView(){
        if(playerData.isFrozenChanged()){
            freezePlayer(); // updates view
        }

        cannon.setAngle(playerData.getCannonAngle(), playerData.getCannonAnimationFrame()); // updates view



    }

    public void makeEnumsChangeable(){
        getCannonMovingPart().addEventHandler(MouseEvent.MOUSE_CLICKED,cannonChangeEventHandler);
        getCharacterSprite().addEventHandler(MouseEvent.MOUSE_CLICKED,characterChangeEventHandler);
    }

    public void makeEnumsNotChangeable(){
        getCannonMovingPart().removeEventHandler(MouseEvent.MOUSE_CLICKED,cannonChangeEventHandler);
        getCharacterSprite().removeEventHandler(MouseEvent.MOUSE_CLICKED,characterChangeEventHandler);
    }

    // Attempt to load Orbs from the file first. If the file doesn't exist or if "RANDOM" is specified or if there are
    // only a few orbs in the file, then add random Orbs to the ammunition until we have 10.
    public void readAmmunitionOrbs(String filename, int seed, int positionIndex){
        this.seed = seed;
        if(ammunitionGenerator==null) ammunitionGenerator = new Random(seed);
        List<Orb> ammunitionOrbs = playerData.getAmmunition();
        ammunitionOrbs.clear();
        if(!filename.substring(0,6).equals("RANDOM")){
            InputStream stream = getClass().getClassLoader().getResourceAsStream(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            try{
                // skip ahead to where the shooter orbs are:
                String line;
                while(!(line = reader.readLine()).equals("***SHOOTER_ORBS***"));

                // skip ahead to the line for this player's positionIndex:
                for(int i=0; i<positionIndex; i++){
                    reader.readLine();
                }

                // Add the Orbs specified in the file to the ammunitionOrbs Queue:
                int nextOrbSymbol;
                int temp = 0;
                while((nextOrbSymbol = reader.read())!=-1 && nextOrbSymbol!='\n' && nextOrbSymbol!='\r'){
                    OrbImages orbEnum = OrbImages.lookupOrbImageBySymbol((char)nextOrbSymbol);
                    if(orbEnum==null){
                        System.err.println("Unparseable character \"" + nextOrbSymbol + "\" in ammunitionOrbs file. Skipping that one...");
                        continue;
                    }
                    ammunitionOrbs.add(new Orb(orbEnum,0,0, Orb.BubbleAnimationType.STATIC)); // Updates model
                    temp++;
                    // Note: view gets updated 24 times per second in the repaint() method of the PlayPanel.
                }
                // System.out.println("read " + temp + "orbs from file");
            } catch(IOException e){
                e.printStackTrace();
            }
        }
        // Add random Orbs to the ammunitionOrbs Queue after that:
        while(ammunitionOrbs.size()<2){
            System.out.println();
            int randomOrdinal = ammunitionGenerator.nextInt(OrbImages.values().length);
            OrbImages orbImage = OrbImages.values()[randomOrdinal];
            ammunitionOrbs.add(new Orb(orbImage,0,0, Orb.BubbleAnimationType.STATIC)); // Updates model
            // Note: view gets updated 24 times per second in the repaint() method of the PlayPanel.
        }
    }
}


