package Classes;

import Classes.Audio.Music;
import Classes.Audio.SoundEffect;
import Classes.Audio.SoundManager;
import Classes.Images.ButtonImages;
import Classes.Images.StaticBgImages;
import Classes.NetworkCommunication.*;
import Classes.PlayerTypes.*;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.*;

import static javafx.scene.layout.AnchorPane.setBottomAnchor;
import static javafx.scene.layout.AnchorPane.setLeftAnchor;
import static javafx.scene.layout.AnchorPane.setRightAnchor;

/**
 * Created by HydrusBeta on 7/22/2017.
 *
 */
public class GameScene extends Scene {
    static final double GRAVITY = 1000.0; // pixels per second squared
    private final int SEED = 14; // todo: temporary seed value passed to each PlayPanel

    // Fields related to layout:
    private StackPane rootNode;

    // Fields containing data, controllers, or mix of data and JavaFx nodes:
    private Map<Integer, PlayPanel> playPanelMap = new HashMap<>(); // For quick access to a PlayPanel using the team number
    private GameData gameData = new GameData();
    private ChatBox chatBox;
    private LocalPlayer localPlayer;
    private List<Player> players;
    private int numPlayers;

    // Variables related to displaying victory/defeat graphics:
    private boolean victoryPauseStarted = false;
    private boolean victoryDisplayStarted = false;
    private long victoryTime = 0;
    private int victoriousTeam;

    // Variables related to animation and timing:
    private AnimationTimer animationTimer;
    private boolean initializing = true;
    public static final int ANIMATION_FRAME_RATE = 24;
    private long nextAnimationFrameInstance = 0; // Time at which all animations will be updated to the next frame (nanoseconds)

    // Variables related to network communications:
    private ConnectionManager connectionManager;
    private boolean isHost;
    private long nextLatencyTest = 0; // The time at which the next latency probe will be sent out (nanoseconds).
    private long nextSendInstance = 0; // The time at which the next packet will be sent (nanoseconds).
    private final long maxConsecutivePacketsMissed; // If this many packets are missed consecutively from a particular player, alert the user.
    private Map<Long,Integer> missedPacketsCount = new HashMap<Long,Integer>(); // maps playerIDs to the number of misssed packets for that player.

    // for misc debugging:
    private int numberOfPacketsProcessed = 0;
    private int numberOfPacketsRemaining = 0;
    private int MSSCalls = 0;
    private long nextReport = 0; // The time at which miscellaneous debugging info will next be printed (nanoseconds).
    private long[] playPanelTickTime = {0,0,Long.MAX_VALUE,0}; // number of times the tick() method has been called, the cumulative time (nanoseconds) for their executions, minimum execution time, maximum execution time.

    // for pause menu
    private Rectangle pauseOverlay;
    private StackPane pauseMenu;

    // a negative value for puzzleGroupIndex indicates that a RANDOM puzzle with -puzzleGroupIndex rows should be created.
    public GameScene(boolean isHost, ConnectionManager connectionManager, List<Player> players, LocationType locationType, int puzzleGroupIndex){
        super(new StackPane());
        rootNode = (StackPane) getRoot();
        this.isHost = isHost;
        this.connectionManager = connectionManager;
        this.players = players;
        numPlayers = players.size();
        maxConsecutivePacketsMissed = connectionManager.getPacketsSentPerSecond() * 5;

        // Arrange the players into their teams by adding each one to an appropriate List:
        Map<Integer, List<Player>> teams = new HashMap<>();
        for (Player player: players) {
            int team = player.getPlayerData().getTeam();
            if(teams.containsKey(team)){
                teams.get(team).add(player);
            }
            else{
                List<Player> newTeam = new LinkedList<>();
                newTeam.add(player);
                teams.put(team,newTeam);
            }

            // while we're here, check to see if this is the LocalPlayer.
            if(player instanceof LocalPlayer) localPlayer = (LocalPlayer) player;
            missedPacketsCount.put(player.getPlayerData().getPlayerID(),0);
        }

        // Now create one PlayPanel for each team and assign its players:
        for (List<Player> playerList: teams.values()){
            int team = playerList.get(0).getPlayerData().getTeam();
            String puzzleURL;
            if(puzzleGroupIndex<0) puzzleURL = "RANDOM_" + (-puzzleGroupIndex);
            else puzzleURL = String.format("res/data/puzzles/puzzle_%02d_%02d_01",playerList.size(),puzzleGroupIndex);
            System.out.println("puzzle url: " + puzzleURL);
            PlayPanel newPlayPanel = new PlayPanel(team, playerList, LocationType.NIGHTTIME,SEED,puzzleURL);
            playPanelMap.put(team,newPlayPanel);
        }

        // Add the PlayPanels to the A ScrollableView on the Scene:
        ScrollableView<PlayPanel> playPanels = new ScrollableView<>(locationType.getBackground().getImageView(), locationType.getMidground().getImageView(), locationType.getSeparator());
        playPanels.addItems(this.playPanelMap.values());
        rootNode.getChildren().add(playPanels);

        // Add a chat overlay. Put it at the bottom of the screen, with no background:
        chatBox = new ChatBox(gameData, localPlayer.getPlayerData(), 125, false);
        chatBox.addMessage("Use mouse wheel to scroll messages");
        chatBox.addMessage("Press p to pause");
        AnchorPane chatBoxPositioner = new AnchorPane();
        chatBoxPositioner.getChildren().add(chatBox);
        setBottomAnchor(chatBox,0.0);
        setRightAnchor(chatBox,130.0);
        setLeftAnchor(chatBox,130.0);
        chatBoxPositioner.setPickOnBounds(false);
        chatBoxPositioner.setMinWidth(1.0);
        chatBoxPositioner.setMinHeight(1.0);
        rootNode.getChildren().add(chatBoxPositioner);

        // Get the pause menu ready, but don't add it to the scene yet:
        pauseOverlay = createPauseOverlay();
        pauseMenu = createPauseMenu();

        // Add mouse listeners for the local player:
        addEventHandler(MouseEvent.MOUSE_MOVED,(event)->{
            localPlayer.pointCannon(event.getX(), event.getY());
        });
        addEventHandler(MouseEvent.MOUSE_PRESSED, (event) ->{
            localPlayer.pointCannon(event.getX(), event.getY());
            localPlayer.changeFireCannon();
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED,(event)->{
            localPlayer.pointCannon(event.getX(), event.getY());
        });

        // Add keyboard listeners:
        setOnKeyPressed(event -> {
            switch(event.getCode()){
                case ENTER:
                    System.out.println("enter pressed!");
                    chatBox.toggleTextFieldVisibility();
                    break;
                case P:
                    System.out.println("Pause pressed!");
                    gameData.changePause(true); // ToDo: Temporary. This should actually TOGGLE the boolean, not just set it to true.
                    // displayPauseMenu(); // ToDo: This line is temporary. displayPauseMenu should be called in the animationTimer, when gameData is being examined.
                    break;
                case U: // A temporary unpause function. ToDo: remove this.
                    gameData.changePause(false);
                    //removePauseMenu();
            }
        });

        // Add a mouse listener to allow the user to scroll through the chat:
        setOnScroll(event -> {
            int direction = event.getDeltaY()>0?-1:1;
            chatBox.scroll(direction);
        });

        // Start the AnimationTimer:
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Initialize some bookkeeping variables:
                if(initializing){
                    nextAnimationFrameInstance = now;
                    nextLatencyTest = now;
                    nextSendInstance = now;
                    nextReport = now;
                    initializing = false;
                }

                // Incoming packets are processed every frame, if there are any:
                int[] packetsProcessingInfo;
                if(isHost) packetsProcessingInfo = processPacketsAsHost(gameData.getPause());
                else packetsProcessingInfo = processPacketsAsClient(gameData.getPause());
                numberOfPacketsProcessed += packetsProcessingInfo[0];
                numberOfPacketsRemaining += packetsProcessingInfo[1];
                ++MSSCalls;

                // Character and Orb animations are updated 24 times per second. Bots are also update at this time:
                if(now>nextAnimationFrameInstance){
                    // Process bot players. This is done only by the host to ensure a consistent outcome (if clients did the
                    // processing, you could end up with 2 clients computing different outcomes for a common robot ally. I
                    // suppose I could just make sure they use the same random number generator, but having the host do it
                    // is just easier and reduces the overall amount of computation for the clients.
                    if(!gameData.getPause() && isHost) processBots();

                    if(!gameData.getPause()){
                        // update each PlayPanel:
                        for(PlayPanel playPanel: playPanelMap.values()){
                            long time = System.nanoTime();
                            playPanel.tick();
                            time = System.nanoTime() - time;
                            playPanelTickTime[0]++;
                            playPanelTickTime[1]+=time;
                            if(time < playPanelTickTime[2]) playPanelTickTime[2] = time;
                            if(time > playPanelTickTime[3]) playPanelTickTime[3] = time;
                        }
                        // process inter-PlayPanel events (transferring orbs and determining victory/defeat):
                        tick();
                    }
                    nextAnimationFrameInstance += 1000000000L/ANIMATION_FRAME_RATE;
                }


                // Outgoing Packets are transmitted 10 times per second. The game also deals with dropped players at this time:
                if(now>nextSendInstance){
                    if(localPlayer.getPlayerData().isFiring()){
                        for(Orb orb: localPlayer.getPlayerData().getFiredOrbs()){
                            long prevSendInstance = nextSendInstance - 1000000000L/connectionManager.getPacketsSentPerSecond();
                            orb.computeTimeStamp(prevSendInstance);
                        }
                    }
                    nextSendInstance += 1000000000L/connectionManager.getPacketsSentPerSecond();
                    if(isHost){
                        prepareAndSendServerPacket(packetsProcessingInfo);
                    }
                    else prepareAndSendClientPacket(packetsProcessingInfo);
                    checkForDisconnectedPlayers();
                }

                // pause the game if doing so is indicated:
                if(gameData.getPause()) displayPauseMenu();
                else removePauseMenu();

                // Latency probes are sent by the host 3 times per second:
                if(isHost){
                    if(now>nextLatencyTest) {
                        nextLatencyTest += 1000000000L/connectionManager.getLatencyTestsPerSecond();
                        connectionManager.send(new LatencyPacket());
                    }
                }

                // A debugging message is displayed once per second:
                if(now> nextReport) {
                    nextReport += 1000000000L;
                    report(packetsProcessingInfo);
                }
            }
        };
        animationTimer.start();

    }

    private void processBots(){
        for(PlayPanel playPanel : playPanelMap.values()){
            for (Player player : playPanel.getPlayerList()){
                if (player instanceof BotPlayer){
                    ((BotPlayer) player).tick();
                }
            }
        }
    }

    // for debugging
    // contents of packetsProcessingInfo: {number of packets processed, number remaining, 0=host 1=client}
    private void report(int[] packetsProcessingInfo){
        System.out.println((packetsProcessingInfo[2]==0?"Host:":"Client:") + " Packets processed: "
                + numberOfPacketsProcessed + " Number of packets remaining: " + numberOfPacketsRemaining + " Timer called " + MSSCalls + " times");
        numberOfPacketsProcessed = 0;
        numberOfPacketsRemaining = 0;
        MSSCalls = 0;
        System.out.println("PlayPanel.tick() called " + playPanelTickTime[0] + " times. Average: " + (playPanelTickTime[1]/1000)/playPanelTickTime[0] + " microseconds. minimum: " + playPanelTickTime[2]/1000 + " microseconds. maximum: " + playPanelTickTime[3]/1000 + " microseconds");

        // compile all the individual bot retargeting times.
        long[] botRetargetTime = {0,0,Long.MAX_VALUE,0}; // number of times the retarget() method has been called on bots, the cumulative tiem (nanoseconds) for their executions, minimum execution time, maximum execution time.
        for(PlayPanel playPanel : playPanelMap.values()){
            for (Player player : playPanel.getPlayerList()){
                if (player instanceof BotPlayer){
                    long[] times = ((BotPlayer) player).getBotRetargetTime();
                    botRetargetTime[0] += times[0];
                    botRetargetTime[1] += times[1];
                    if(times[2]<botRetargetTime[2]) botRetargetTime[2] = times[2];
                    if(times[3]>botRetargetTime[3]) botRetargetTime[3] = times[3];
                }
            }
        }
        System.out.println("BotPlayer.retarget() called " + botRetargetTime[0] + " times. Average: " + (botRetargetTime[1]/1000)/Math.max(botRetargetTime[0],1) + " microseconds. minimum: " + botRetargetTime[2]/1000 + " microseconds. maximum: " + botRetargetTime[3]/1000 + " microseconds");

    }

    private int[] processPacketsAsHost(boolean isPaused){
        int[] packetsProcessingInfo = {0,0,0}; // {number of packets processed, number remaining, 0=host 1=client}.
        int packetsProcessed = 0;
        Packet packet = connectionManager.retrievePacket(packetsProcessingInfo);

        // Process Packets one at a time:
        while(packet!=null && packetsProcessed<connectionManager.MAX_PACKETS_PER_PLAYER*numPlayers){
            // Pop the PlayerData from the packet:
            PlayerData playerData = packet.popPlayerData();
            if(!isPaused){
                // Find the PlayPanel associated with the player:
                PlayPanel playPanel = playPanelMap.get(playerData.getTeam());
                // Update the PlayPanelData and the PlayerData:
                playPanel.updatePlayer(playerData, isHost);
            }
            // Reset the missed packets counter
            missedPacketsCount.replace(playerData.getPlayerID(),0);

            // Then process the GameData:
            GameData clientGameData = packet.getGameData();
            updateGameData(clientGameData, isHost);

            // Prepare for the next iteration:
            ++packetsProcessed;
            packet = connectionManager.retrievePacket(packetsProcessingInfo);
        }

        // For debugging:
        packetsProcessingInfo[0] = packetsProcessed;
        return packetsProcessingInfo;
    }

    private void prepareAndSendServerPacket(int[] packetsProcessingInfo){
        // We must create a copy of the local PlayerData, GameData, and PlayPanelData and add those copies to the packet. This
        // is to prevent the change flags within the local PlayerData and GameData from being reset before the packet is
        // sent (note the last 4 lines of this method).
        PlayerData localPlayerData = new PlayerData(localPlayer.getPlayerData());
        GameData localGameData = new GameData(gameData);
        PlayPanelData localPlayPanelData = new PlayPanelData(playPanelMap.get(localPlayerData.getTeam()).getPlayPanelData());
        Packet outPacket = new Packet(localPlayerData, localGameData, localPlayPanelData);

        for (Player player: players) {
            if(player.getPlayerData().getPlayerID()==0) continue; // we've already added the localPlayer's PlayerData to the packet.
            PlayerData playerData = new PlayerData(player.getPlayerData()); // Yes, these need to be *new* PlayerData instances because there may be locally-stored bot data. see note regarding local player data, above.
            outPacket.addPlayerData(playerData);
            player.getPlayerData().resetFlags(); // reset the local change flags so that they are only broadcast once
        }

        for (PlayPanel playPanel: playPanelMap.values()){
            if(playPanel.getPlayPanelData().getTeam()==localPlayerData.getTeam()) continue; // we've already added the localPlayer's PlayPanelData to the packet.
            PlayPanelData playPanelData = new PlayPanelData(playPanel.getPlayPanelData());
            outPacket.addOrbData(playPanelData);
            playPanel.getPlayPanelData().resetFlags(); // reset the local change flags so that they are only broadcast once
        }

        if(packetsProcessingInfo[1]>0){
            // ToDo: Host may be lagging. Slow down the game if necessary.
        }

        connectionManager.send(outPacket);

        // update the chatBox before resetting the flags and the message:
        if(gameData.isMessageRequested()) chatBox.addMessages(gameData.getMessages());

        // pause the game if doing so is indicated:
        if(gameData.isPauseRequested()){
            if(gameData.getPause()) displayPauseMenu();
            else removePauseMenu();
        }

        // reset the local change flags so that they are only broadcast once:
        localPlayer.getPlayerData().resetFlags();
        playPanelMap.get(localPlayerData.getTeam()).getPlayPanelData().resetFlags();
        gameData.resetFlags();
        gameData.resetMessages();
    }

    private void checkForDisconnectedPlayers(){
        for (Player player: players) {
            PlayerData playerData = player.getPlayerData();
            if(!(player instanceof RemotePlayer)) continue; // Only RemotePlayers are capable of having connection issues, so only check them.
            if(!isHost && playerData.getPlayerID()!=0) continue; // Clients only keep track of the host's connection to them.
            Integer missedPackets = missedPacketsCount.get(playerData.getPlayerID());

            // increment the missed packets count. The count will be reset whenever the next packet is received:
            missedPacketsCount.replace(playerData.getPlayerID(),missedPacketsCount.get(playerData.getPlayerID())+1);

            // If the player has been gone for too long (and they have not resigned), ask the host what to do:
            if(missedPackets==maxConsecutivePacketsMissed && !player.getPlayerData().getDefeated()){
                showConnectionLostDialog(player);
            }
        }
    }

    private int[] processPacketsAsClient(boolean isPaused){
        int packetsProcessed = 0;
        int[] packetsProcessingInfo = {0,0,1};

        // Process Packets one at a time:
        Packet packet = connectionManager.retrievePacket(packetsProcessingInfo);
        while(packet!=null && packetsProcessed<connectionManager.MAX_PACKETS_PER_PLAYER){


            // Within each Packet, process PlayerData one at a time in order:
            PlayerData playerData = packet.popPlayerData();
            while(playerData!=null){
                if(!gameData.getPause()){
                    // update the Player and his/her PlayPanel with the new playerData:
                    PlayPanel playPanel = playPanelMap.get(playerData.getTeam());
                    playPanel.updatePlayer(playerData, isHost);
                }
                // Reset the missed packets counter:
                if(playerData.getPlayerID()==0) missedPacketsCount.replace(playerData.getPlayerID(), 0);

                // Prepare for next iteration:
                playerData = packet.popPlayerData();
            }

            // Now process the PlayPanelData one at a time in order. Note: this is mostly just a check for consistency.
            // Most of the time, this loop won't actually change anything. If desynchronization is detected between host
            // and client, however, then the client's playpanel data will be discarded and replaced with the host's.
            PlayPanelData playPanelData = packet.popPlayPanelData();
            while(playPanelData !=null){
                // update the PlayPanel with the new playPanelData:
                PlayPanel playPanel = playPanelMap.get(playPanelData.getTeam());
                playPanel.getPlayPanelData().checkForConsistency(playPanelData);

                // Prepare for next iteration:
                playPanelData = packet.popPlayPanelData();
            }

            // Now process the GameData:
            GameData hostGameData = packet.getGameData();
            updateGameData(hostGameData, isHost);

            // Prepare for next iteration:
            ++packetsProcessed;
            packet = connectionManager.retrievePacket(packetsProcessingInfo);
        }

        // For debugging:
        packetsProcessingInfo[0] = packetsProcessed;
        return packetsProcessingInfo;
    }

    private void updateGameData(GameData gameDataIn, boolean isHost){
        if(isHost){
            if(gameDataIn.isFrameRateRequested()) gameData.changeFrameRate(gameDataIn.getFrameRate());
            if(gameDataIn.isMessageRequested()) gameData.changeAddMessages(gameDataIn.getMessages());
            if(gameDataIn.isPauseRequested()) gameData.changePause(gameDataIn.getPause());
        }
        else{
            if(gameDataIn.isFrameRateRequested()) gameData.setFrameRate(gameDataIn.getFrameRate());
            if(gameDataIn.isMessageRequested()) chatBox.addMessages(gameDataIn.getMessages());
            if(gameDataIn.isPauseRequested()) gameData.setPause(gameDataIn.getPause());
            if(gameDataIn.isCancelGameRequested()){
                cleanUp();
                SceneManager.switchToMainMenu();
                showGameCanceledDialog();
            }
        }
    }

    private void prepareAndSendClientPacket(int[] packetsProcessingInfo){
        // The network must create a copy of the local PlayerData and GameData and send those copies instead of the
        // originals. This is to prevent the change flags within the PlayerData and GameData from being reset before the
        // packet is sent.
        PlayerData localPlayerData = new PlayerData(localPlayer.getPlayerData());
        GameData localGameData = new GameData(gameData);
        PlayPanelData localPlayPanelData = new PlayPanelData(playPanelMap.get(localPlayerData.getTeam()).getPlayPanelData());

        Packet outPacket = new Packet(localPlayerData, localGameData, localPlayPanelData);
        if(packetsProcessingInfo[1]>0){
            // ToDo: Client may be lagging. Request slowdown if necessary.
        }
        connectionManager.send(outPacket);

        // reset local change flags so that they are sent only once:
        localPlayer.getPlayerData().resetFlags();
        playPanelMap.get(localPlayerData.getTeam()).getPlayPanelData().resetFlags();
        gameData.resetFlags();
        gameData.resetMessages();
    }

    // Todo: make this dialog automatically disappear if the player reconnects
    private void showConnectionLostDialog(Player player){
        boolean droppedPlayerIsHost = player.getPlayerData().getPlayerID()==0;

        // The dialog box is nonmodal and is constructed on a new stage:
        Stage dialogStage = new Stage();
        VBox dialogRoot = new VBox();
        dialogRoot.setSpacing(10.0);
        dialogRoot.setPadding(new Insets(10.0));
        dialogRoot.setAlignment(Pos.CENTER_RIGHT);
        Scene dialogScene = new Scene(dialogRoot);
        dialogStage.setScene(dialogScene);
        dialogStage.setTitle("Player Disconnection Detected");
        dialogStage.initStyle(StageStyle.UNDECORATED);

        // Add an informational label:
        Label dialogLabel = new Label("Communication with player " + player.getPlayerData().getUsername() + " seems to have been lost.\n What would you like to do?");
        if(droppedPlayerIsHost){
            dialogLabel.setText("Communication with the host seems to have been lost. What would you like to do?");
        }
        dialogRoot.getChildren().add(dialogLabel);

        // Add buttons for the user's decision:
        Button drop = new Button("Drop Player");
        if(droppedPlayerIsHost){
            drop.setText("Exit game");
        }
        Button wait = new Button("wait 10 seconds and see if they reconnect.");
        HBox buttonContainer = new HBox();
        buttonContainer.setSpacing(10.0);
        buttonContainer.getChildren().addAll(drop,wait);
        dialogRoot.getChildren().add(buttonContainer);

        // Add functionality to the buttons:
        drop.setOnAction((event -> {
            if(droppedPlayerIsHost){ // The player who disconnected was the host, so close the game.
                cleanUp();
                SceneManager.switchToMainMenu();
                showGameCanceledDialog();
            }
            else{
                player.changeResignPlayer();
            }
            dialogStage.close();
        }));

        wait.setOnAction((event) -> {
            missedPacketsCount.replace(player.getPlayerData().getPlayerID(),(int)(maxConsecutivePacketsMissed - connectionManager.getPacketsSentPerSecond()*10));
            dialogStage.close();
        });

        dialogStage.show();
    }

    private void showGameCanceledDialog(){
        Alert cancelAlert = new Alert(Alert.AlertType.CONFIRMATION);
        cancelAlert.initOwner(SceneManager.getPrimaryStage());
        cancelAlert.setTitle("Game Cancelled");
        cancelAlert.setHeaderText("The host is no longer connected. Game Cancelled.");
        ButtonType returnBtn = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
        cancelAlert.getButtonTypes().setAll(returnBtn);
        cancelAlert.setGraphic(null);
        cancelAlert.show();
    }

    private void displayPauseMenu(){
        if(rootNode.getChildren().contains(pauseOverlay)); // game is already paused, so do nothing.
        else rootNode.getChildren().addAll(pauseOverlay, pauseMenu);
    }

    private void removePauseMenu(){
        rootNode.getChildren().removeAll(pauseOverlay, pauseMenu);
    }

    public void cleanUp(){
        int[] packetsProcessingInfo = {0,0,0};
        if(isHost){
            gameData.changeCancelGame(true);
            prepareAndSendServerPacket(packetsProcessingInfo);
        }
        else{
            localPlayer.changeResignPlayer();
            prepareAndSendClientPacket(packetsProcessingInfo);
        }
        // wait a little bit to make sure the packet gets through:
        try{
            Thread.sleep(2000/connectionManager.getPacketsSentPerSecond());
        } catch (InterruptedException e){
            System.err.println("InterruptedException encountered while trying to leave the game. Other players" +
                    "might not be informed that you've left but... oh well, leaving anyways.");
            e.printStackTrace();
        }
        animationTimer.stop();
        connectionManager.cleanUp();
    }

    // Overlays the Scene with a transparent black rectangle when the game is paused:
    // todo: reduce the width of the pause overlay a little (and also set the width of the VBox in createPauseMenu to 1.0) so that the scroll buttons remain accessible.
    private Rectangle createPauseOverlay(){
        Rectangle pauseOverlay = new Rectangle();
        pauseOverlay.setFill(new Color(0,0,0,0.25));
        pauseOverlay.widthProperty().bind(rootNode.widthProperty());
        pauseOverlay.heightProperty().bind(rootNode.heightProperty());
        pauseOverlay.setMouseTransparent(true);
        return pauseOverlay;
    }

    private StackPane createPauseMenu(){
        // The root of the pause menu is a stackPane:
        StackPane pauseMenu = new StackPane();

        // Get the pause menu background:
        ImageView background = StaticBgImages.GAME_PAUSED.getImageView();
        pauseMenu.getChildren().add(background);

        // place the title and buttons:
        VBox buttonsHolder = new VBox();
        buttonsHolder.setAlignment(Pos.CENTER);
        buttonsHolder.setSpacing(3.0);
        Rectangle spacer = new Rectangle(1,120); // Spacer so that the buttons don't run into the title
        spacer.setFill(Color.TRANSPARENT);
        Button resignBtn = createButton(ButtonImages.RESIGN);
        Button unpauseBtn = createButton(ButtonImages.UNPAUSE);
        buttonsHolder.getChildren().addAll(spacer, resignBtn, unpauseBtn);
        pauseMenu.getChildren().add(buttonsHolder);

        /*// Make everything scale correctly when the window is resized:
        buttonsHolder.getTransforms().add(scaler);
        background.getTransforms().add(scaler);*/

        return pauseMenu;
    }

    private Button createButton(ButtonImages buttonEnum){
        Button btn = new Button();
        ImageView unselectedImage = buttonEnum.getUnselectedImage();
        ImageView selectedImage = buttonEnum.getSelectedImage();
        btn.setGraphic(unselectedImage);
        btn.setBackground(null);
        btn.setPadding(Insets.EMPTY);

        // choose appropriate action when the button is clicked:
        btn.setOnAction((event) -> {
            switch (buttonEnum) {
                case RESIGN:
                    System.out.println("resign pressed!");

                    Alert exitConfirmation = new Alert(Alert.AlertType.CONFIRMATION);
                    exitConfirmation.initOwner(SceneManager.getPrimaryStage());
                    exitConfirmation.setTitle("Close Application?");
                    exitConfirmation.setHeaderText("Are you sure you want to exit the game?");
                    ButtonType cancel = new ButtonType("Cancel");
                    ButtonType yes = new ButtonType("Exit");
                    exitConfirmation.getButtonTypes().setAll(yes,cancel);
                    exitConfirmation.setGraphic(null);
                    Optional<ButtonType> result = exitConfirmation.showAndWait();
                    if(result.isPresent() && result.get() == yes) {
                        cleanUp();
                        SceneManager.switchToMainMenu();
                    }
                    break;
                case UNPAUSE:
                    System.out.println("Unpause Button Pressed!");
                    //removePauseMenu();
                    gameData.changePause(false);
                    break;
                case RETURN_TO_MAIN_MENU_CLIENT:
                    System.out.println("We're done!");
                    cleanUp();
                    SceneManager.switchToMainMenu();
            }
        });

        // Change the button graphic when it is hovered over:
        btn.addEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> btn.setGraphic(selectedImage));
        btn.addEventHandler(MouseEvent.MOUSE_EXITED, (event) -> btn.setGraphic(unselectedImage));

        return btn;
    }

    // Called once after every PlayPanel has tick()'ed. This method processes inter-PlayPanel and game-wide events.
    private void tick(){
        // transfer the transferOutOrbs.
        for(PlayPanel fromPlayPanel : playPanelMap.values()){
            List<Orb> transferOutOrbs = fromPlayPanel.getPlayPanelData().getTransferOutOrbs();
            if(!transferOutOrbs.isEmpty()){
                for(PlayPanel toPlayPanel : playPanelMap.values()){
                    if(fromPlayPanel!=toPlayPanel){
                        toPlayPanel.changeAddTransferInOrbs(transferOutOrbs);
                    }
                }
            }
            transferOutOrbs.clear();
        }

        // Todo: what if 2 players declare victory at the exact same time?
        // check to see whether anybody's won a quick victory by clearing their PlayPanel:
        for(PlayPanel playPanel : playPanelMap.values()){
            if(playPanel.getPlayPanelData().isVictoriousChanged()){
                // Todo: host should check whether it's actually true.
                startVictoryPause(playPanel.playPanelData.getTeam());
                System.out.println("Hey, somebody won a quick victory!");
            }
        }

        // In competitive multiplayer (or Vs Computer) games, check to see whether there's only 1 live team left
        if(playPanelMap.values().size()>1){
            int numLiveTeams = 0;
            for(PlayPanel playPanel : playPanelMap.values()){
                boolean alive = false;
                for(Player player : playPanel.getPlayerList()){
                    if(!player.getPlayerData().getDefeated()){
                        alive = true;
                        break;
                    }
                }
                if(alive) numLiveTeams++;
                if(numLiveTeams>1) break;
            }
            if(numLiveTeams==1){
                System.out.println("There's only 1 team left. They've won the game!");
                // Todo: the following for loop is nearly-duplicated code. There's got to be a better way of doing this.
                for(PlayPanel playPanel : playPanelMap.values()){
                    for(Player player : playPanel.getPlayerList()){
                        if(!player.getPlayerData().getDefeated()){
                            startVictoryPause(playPanel.playPanelData.getTeam());
                            break;
                        }
                    }
                }
            }

            // Todo: in the offhand chance that everyone died at the exact same time, declare a tie or pick a winner at random from those who just died this turn (check the isDefeatedChanged flag).
            if(numLiveTeams == 0){
                System.out.println("WHOA!!! A tie!!!!");
            }
        }

        // In puzzle games, check to see whether the only existing team has lost:
        else{
            for(Player player : playPanelMap.get(localPlayer.getPlayerData().getTeam()).getPlayerList()){
                if(player.getPlayerData().getDefeated()){
                    startVictoryPause(-1);
                }
            }
        }

        // If someone has won, handle the delay before the victory graphics are actually displayed:
        if(victoryPauseStarted && !victoryDisplayStarted){
            if(((System.nanoTime() - victoryTime)/1000000000)>0.85) startVictoryDisplay(victoriousTeam);
        }
    }

    // If victoriousTeam == -1, that means nobody won (player failed a puzzle challenge, for example)
    // todo: what about ties? Am I gonna bother with those?
    private void startVictoryPause(int victoriousTeam){
        if(victoryPauseStarted) return; // To ensure that the effects of this method are only applied once.
        victoryTime = System.nanoTime();
        this.victoriousTeam = victoriousTeam;
        SoundManager.silenceMusic();
        SoundManager.silenceAllSoundEffects();
        SoundManager.playSoundEffect(SoundEffect.VICTORY_FLOURISH);

        // Disable all cannons and freeze all defeated players:
        for(PlayPanel playPanel: playPanelMap.values()){
            if(playPanel.getPlayPanelData().getTeam() == victoriousTeam){
                // freeze all players // todo: I don't like how freezing the player looks, but I don't want the bots to continue firing either. Create a disableCannon() method or something.
                for(Player victoriousPlayer : playPanel.getPlayerList()){
                    victoriousPlayer.changeDisableCannon();
                }
            }
            else {
                // set defeated = true for all other players:
                for(Player defeatedPlayer : playPanel.getPlayerList()){
                    defeatedPlayer.changeResignPlayer();
                }
            }
        }

        // clear any outstanding shooting orbs:
        for(PlayPanel playPanel: playPanelMap.values()){
            playPanel.getPlayPanelData().getShootingOrbs().clear();
        }

        victoryPauseStarted = true;
        System.out.println("team " + victoriousTeam + " has won.");
    }

    private void startVictoryDisplay(int victoriousTeam){
        for(PlayPanel playPanel: playPanelMap.values()){
            if(playPanel.getPlayPanelData().getTeam() == victoriousTeam){
                if(playPanelMap.values().size()==1) playPanel.displayVictoryResults(PlayPanel.VictoryType.PUZZLE_CLEARED);
                else playPanel.displayVictoryResults(PlayPanel.VictoryType.VS_WIN);
            }
            else {
                if(playPanelMap.values().size()==1) playPanel.displayVictoryResults(PlayPanel.VictoryType.PUZZLE_FAILED);
                else playPanel.displayVictoryResults(PlayPanel.VictoryType.VS_LOSE);
            }
        }
        if(localPlayer.getPlayerData().getTeam() == victoriousTeam) SoundManager.playSong(Music.GO_TAKE_FLIGHT,false);
        else SoundManager.playSong(Music.GAME_OVER,false);
        victoryDisplayStarted = true;

        VBox vBox = new VBox();
        vBox.setMaxWidth(1.0); // to prevent the VBox from covering the scroll buttons
        Button returnToMain = createButton(ButtonImages.RETURN_TO_MAIN_MENU_CLIENT);
        vBox.getChildren().add(returnToMain);
        vBox.setAlignment(Pos.TOP_CENTER);
        rootNode.getChildren().add(vBox);
    }

}

enum LocationType {
    NIGHTTIME(StaticBgImages.NIGHT_SKY,
            StaticBgImages.NIGHT_SKY_CLOUDS,
            StaticBgImages.PLAYPANEL_NIGHTSKY_FOREGROUND_CLOUDS,
            StaticBgImages.PLAYPANEL_NIGHTSKY_DROPCLOUD,
            StaticBgImages.PLAYPANEL_NIGHTSKY_SEPARATOR);

    private StaticBgImages background;
    private StaticBgImages midground;
    private StaticBgImages foregroundCloudsEnum;
    private StaticBgImages dropCloudEnum;
    private StaticBgImages separator;

    LocationType(StaticBgImages background, StaticBgImages midground, StaticBgImages foregroundCloudsEnum, StaticBgImages dropCloudEnum, StaticBgImages separator){
        this.background = background;
        this.midground = midground;
        this.foregroundCloudsEnum = foregroundCloudsEnum;
        this.dropCloudEnum = dropCloudEnum;
        this.separator = separator;
    }

    public StaticBgImages getBackground(){
        return background;
    }
    public StaticBgImages getMidground(){
        return midground;
    }
    public StaticBgImages getForegroundCloudsEnum(){
        return foregroundCloudsEnum;
    }
    public StaticBgImages getDropCloudEnum(){
        return dropCloudEnum;
    }
    public StaticBgImages getSeparator(){
        return separator;
    }
}

