package Classes.PlayerTypes;

import Classes.*;
import Classes.Character;
import Classes.Images.CannonImages;
import Classes.Images.CharacterImages;
import Classes.Images.OrbImages;
import Classes.NetworkCommunication.PlayerData;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.util.*;


/**
 * Think of this as the controller in an MVC model. Player has a PlayerData (model) as well as a Cannon, Character,
 * Label, and ComboBox to display that data (view).
 */
public abstract class Player {
    // model:
    protected PlayerData playerData;

    // view:
    protected Cannon cannon;
    protected Character character;
    protected Button usernameButton;
    protected ComboBox<String> teamChoice = new ComboBox<>();
    protected Label latencyLabel = new Label("Latency: 0 milliseconds");

    // EventHandlers:
    private EventHandler<MouseEvent> cannonChangeEventHandler;
    private EventHandler<MouseEvent> characterChangeEventHandler;

    // Data needed for constructing the shooter Orbs. These values are determined *once* by the host at the start
    // of the game and are never changed. Host and client maintain their own copies separately.
    int seed;
    Random ammunitionGenerator;

    // instance initializer to create the username Button, latency Label, ComboBox, and eventHandlers for changing cannon/character:
    {
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
    public abstract void registerToPlayPanel(PlayPanel playPanel);
    public abstract double computeInitialDistance(Orb orb);

    /* Concrete methods: */

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

    public void incrementCharacterEnum(){
        CharacterImages nextType = playerData.getCharacterEnum().next();
        if (this instanceof LocalPlayer){
            while(!nextType.isPlayable()){
                nextType = nextType.next();
            }
        }
        else{ // player must be an instance of BotPlayer
            while(!nextType.isBot()){
                nextType = nextType.next();
            }
        }
        playerData.changeCharacter(nextType); // updates model
        character.setCharacterEnum(nextType); // updates view
    }
    public void incrementCannonEnum(){
        CannonImages nextType = playerData.getCannonEnum().next();
        while(!nextType.isSelectable()){
            nextType = nextType.next();
        }
        playerData.changeCannon(nextType); // updates model
        cannon.setCannonEnum(nextType); // updates view
    }

    public void resignPlayer(){
        playerData.changeDefeated(true); // updates model
        // in the MultiplayerSceneSelection, the view is updated in either the deleteRemovedPlayer() or
        // processPacketAsClient() methods, for host and clients respectively.
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

    // For now, this only modifies the ammunition orbs. The shooter orbs have already been placed via the updatePlayer() method in the PlayPanel class
    // I might change this later.
    public void setFireCannon(){
        int randomOrdinal = ammunitionGenerator.nextInt(OrbImages.values().length);
        playerData.setFire(randomOrdinal);
    }

    public Orb changeFireCannon(double angle){
        int randomOrdinal = ammunitionGenerator.nextInt(OrbImages.values().length);
        return playerData.changeFire(angle, randomOrdinal); // updates model
    }

    public void relocateCannon(double x, double y){
        cannon.relocate(x,y);
    }
    public void relocateCharacter(double x, double y){
        character.relocate(x,y);
    }
    public void setScale(double scaleFactor){
        cannon.setScale(scaleFactor);
        character.setScale(scaleFactor);
    }

    // Called every frame by the host to update a player's data according to a packet received over the network
    public void updateWithChangers(PlayerData newPlayerData, Map<Long, Long> latencies){
        if(newPlayerData.isFiring()){
            getPlayerData().getFiredOrbs().addAll(newPlayerData.getFiredOrbs()); // updates model
            getPlayerData().changeFiringFlag(true); // marks data as updated
            System.out.println("HOST: player's firing flag has been set to " + getPlayerData().isFiring());
            for(int i=0; i<newPlayerData.getFiredOrbs().size(); i++) setFireCannon(); // updates model
            getPlayerData().changeAmmunitionFlag(true); // marks data as updated
            // note: view will be updated in the PlayPanel's repaint() method.
        }
        if(newPlayerData.isCannonChanged()){
            playerData.changeCannon(newPlayerData.getCannonEnum()); // updates model
            cannon.setCannonEnum(newPlayerData.getCannonEnum()); // updates view
        }
        if(newPlayerData.isCharacterChanged()){
            playerData.changeCharacter(newPlayerData.getCharacterEnum()); //updates model
            character.setCharacterEnum(newPlayerData.getCharacterEnum()); //updates view
        }
        if(newPlayerData.isDefeatedChanged()){
            playerData.changeDefeated(newPlayerData.getDefeated()); //updates model
            character.freeze(); // updates view
            cannon.freeze(); // updates view
            // Note: In the MultiplayerSceneSelection, this player's corresponding PlayerSlot will be removed in either
            // the deleteRemovedPlayer() or processPacketAsClient() methods, for host and clients respectively.
        }
        if(newPlayerData.isTeamChanged()){
            playerData.changeTeam(newPlayerData.getTeam()); // updates model
            teamChoice.getSelectionModel().select(newPlayerData.getTeam()-1); // updates view
        }
        if(newPlayerData.isUsernameChanged()){
            playerData.changeUsername(newPlayerData.getUsername()); // updates model
            usernameButton.setText(newPlayerData.getUsername()); // updates view
        }

        playerData.changeCannonAngle(newPlayerData.getCannonAngle()); //updates model
        cannon.setAngle(newPlayerData.getCannonAngle()); // updates view

        if(latencies!=null){ // Todo: This line is temporary. latencies should never be null in the final game.
            if(latencies.containsKey(playerData.getPlayerID())){
                playerData.changeLatency(latencies.get(playerData.getPlayerID())); // updates model
            }
            if(playerData.getLatency()<1000000) latencyLabel.setText(String.format("Latency: %d microseconds",playerData.getLatency()/1000L)); // updates view
            else latencyLabel.setText(String.format("Latency: %d milliseconds",playerData.getLatency()/1000000L)); // updates view
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
            cannon.setCannonEnum(newPlayerData.getCannonEnum()); // updates view
        }
        if(newPlayerData.isCharacterChanged() && !playerData.isCharacterChanged()){
            playerData.setCharacter(newPlayerData.getCharacterEnum()); //updates model
            character.setCharacterEnum(newPlayerData.getCharacterEnum()); //updates view
        }
        if(newPlayerData.isDefeatedChanged() && !playerData.isDefeatedChanged()){
            playerData.setDefeated(newPlayerData.getDefeated()); //updates model
            character.freeze(); // updates view
            cannon.freeze(); // updates view
        }
        if(newPlayerData.isTeamChanged() && !playerData.isTeamChanged()){
            playerData.setTeam(newPlayerData.getTeam()); // updates model
            teamChoice.getSelectionModel().select(newPlayerData.getTeam()-1); // updates view
        }
        if(newPlayerData.isUsernameChanged() && !playerData.isUsernameChanged()){
            playerData.setUsername(newPlayerData.getUsername()); // updates model
            usernameButton.setText(newPlayerData.getUsername()); // updates view
        }

        // remote players' cannon angles are always updated:
        if(!isLocalPlayer){
            playerData.setCannonAngle(newPlayerData.getCannonAngle()); //updates model
            cannon.setAngle(newPlayerData.getCannonAngle()); // updates view
        }

        // players' latencies are always updated:
        playerData.setLatency(newPlayerData.getLatency()); // updates model
        if(newPlayerData.getLatency()<1000000) latencyLabel.setText(String.format("Latency: %d microseconds",newPlayerData.getLatency()/1000L)); // updates view
        else latencyLabel.setText(String.format("Latency: %d milliseconds",newPlayerData.getLatency()/1000000L)); // updates view

        // check for consistency between this player's ammunitionOrbs and the host's data for ammunitionOrbs. If they
        // are different for too long, then override the client's data with the host's data.
        playerData.checkForConsistency(newPlayerData);

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
    public void readAmmunitionOrbs(String filename, int seed){
        this.seed = seed;
        ammunitionGenerator = new Random(seed);
        List<Orb> ammunitionOrbs = playerData.getAmmunition();
        // Add the Orbs specified in the file to the ammunitionOrbs Queue:
        if(!filename.equals("RANDOM")){
            InputStream ammunitionFile = getClass().getClassLoader().getResourceAsStream(filename);
            Scanner ammunitionScanner = new Scanner(ammunitionFile);
            while(ammunitionScanner.hasNext()){
                char nextOrbSymbol = ammunitionScanner.next().charAt(0);
                OrbImages orbImage = OrbImages.lookupOrbImageBySymbol(nextOrbSymbol);
                ammunitionOrbs.add(new Orb(orbImage,0,0)); // Updates model
                // Note: view gets updated 24 times per second in the repaint() method of the PlaypPanel.
            }
        }
        // Add random Orbs to the ammunitionOrbs Queue after that:
        while(ammunitionOrbs.size()<10){
            System.out.println();
            int randomOrdinal = ammunitionGenerator.nextInt(OrbImages.values().length);
            OrbImages orbImage = OrbImages.values()[randomOrdinal];
            ammunitionOrbs.add(new Orb(orbImage,0,0)); // Updates model
            // Note: view gets updated 24 times per second in the repaint() method of the PlaypPanel.
        }
    }
}


