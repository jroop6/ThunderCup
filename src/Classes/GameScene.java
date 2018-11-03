package Classes;

import Classes.Animation.OrbColor;
import Classes.Audio.Music;
import Classes.Audio.SoundEffect;
import Classes.Audio.SoundManager;
import Classes.Images.ButtonType;
import Classes.Images.CannonType;
import Classes.Images.DrawingName;
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
import java.util.concurrent.*;

import static Classes.Animation.CharacterType.CharacterAnimationState.DEFEATED;
import static Classes.Animation.CharacterType.CharacterAnimationState.VICTORIOUS;
import static Classes.Orb.NULL;
import static Classes.PlayPanel.ARRAY_WIDTH_PER_CHARACTER;
import static Classes.Player.GAME_ID;
import static Classes.Player.HOST_ID;
import static javafx.scene.layout.AnchorPane.setBottomAnchor;
import static javafx.scene.layout.AnchorPane.setLeftAnchor;
import static javafx.scene.layout.AnchorPane.setRightAnchor;

/**
 * Created by Jonathan Roop on 7/22/2017.
 */
public class GameScene extends Scene {
    static final double GRAVITY = 1000.0; // pixels per second squared
    private final int SEED = 14; // todo: temporary seed value passed to each PlayPanel

    // Fields related to layout:
    private StackPane rootNode;

    private Map<Integer, PlayPanel> playPanelMap = new HashMap<>(); // For quick access to a PlayPanel using the team number
    private ChatBox chatBox;
    private Player localPlayer;
    private List<Player> players;

    private SynchronizedComparable<Boolean> pause;
    private SynchronizedComparable<Boolean> gameCanceled;

    // Variables related to displaying victory/defeat graphics:
    private boolean victoryPauseStarted = false;
    private boolean victoryDisplayStarted = false;
    private boolean victoryPauseStarted2 = false;
    private boolean victoryDisplayStarted2 = false;
    private long victoryTime = 0;
    private int victoriousTeam;

    // Sound effects to play this frame.
    Set<SoundEffect> soundEffectsToPlay = EnumSet.noneOf(SoundEffect.class);

    // Variables related to animation and timing:
    private AnimationTimer animationTimer;
    private boolean initializing = true;
    public static final int DATA_FRAME_RATE = 24;
    public static final int VISUAL_FRAME_RATE = 60;
    private long nextDataUpdateInstance = 0; // Time at which The next animation frame will be computed.
    private long nextAnimationFrameInstance = 0; // Time at which all visuals will be repainted(nanoseconds)

    // Variables related to network communications:
    private ConnectionManager connectionManager;
    private boolean isHost;
    private long nextLatencyTest = 0; // The time at which the next latency probe will be sent out (nanoseconds).
    private long nextSendInstance = 0; // The time at which the next packet will be sent (nanoseconds).
    private final long maxConsecutivePacketsMissed; // If this many packets are missed consecutively from a particular player, alert the user.

    // for misc debugging:
    private long nextReport = 0; // The time at which miscellaneous debugging info will next be printed (nanoseconds).
    private long[] playPanelTickTime = {0,0,Long.MAX_VALUE,0}; // number of times the tick() method has been called, the cumulative time (nanoseconds) for their executions, minimum execution time, maximum execution time.

    // for pause menu
    private Rectangle pauseOverlay;
    private StackPane pauseMenu;

    // for concurrent execution:
    private ReceivePacketsTasks receivePacketsTasks = new ReceivePacketsTasks();
    private UpdateFrameTasks updateFrameTasks = new UpdateFrameTasks();
    private SendPacketsTasks sendPacketsTasks = new SendPacketsTasks();
    private ReportingTasks reportingTasks = new ReportingTasks();
    private ExecutorService workerThread = Executors.newSingleThreadExecutor();

    // a negative value for puzzleGroupIndex indicates that a RANDOM puzzle with -puzzleGroupIndex rows should be created.
    // todo: don't use indices for the puzzles. Instead, pass a PuzzleSet enum, and have the PuzzleSet store pointers to the various puzzles. Include special enums for random puzzles.
    public GameScene(boolean isHost, ConnectionManager connectionManager, List<Player> players, LocationType locationType, int puzzleGroupIndex){
        super(new StackPane());
        rootNode = (StackPane) getRoot();
        this.isHost = isHost;
        this.connectionManager = connectionManager;

        gameCanceled = new SynchronizedComparable<>("cancelGame", false, SynchronizedData.Precedence.HOST, GAME_ID, connectionManager.getSynchronizer());

        pause = new SynchronizedComparable<>("pause", false, SynchronizedData.Precedence.CLIENT, GAME_ID, connectionManager.getSynchronizer());
        this.players = players;
        maxConsecutivePacketsMissed = DATA_FRAME_RATE * 5;

        // Arrange the players into their teams by adding each one to an appropriate List:
        Map<Integer, List<Player>> teams = new HashMap<>();
        for (Player player: players) {
            int team = player.getTeam().getData();
            if(teams.containsKey(team)){
                teams.get(team).add(player);
            }
            else{
                List<Player> newTeam = new LinkedList<>();
                newTeam.add(player);
                teams.put(team,newTeam);
            }

            // while we're here, check to see if this is the LocalPlayer.
            if(player.getPlayerType().getData() == Player.PlayerType.LOCAL){
                if(localPlayer!=null) System.err.println("Warning! There's more than one localplayer!");
                localPlayer = player;
            }
        }

        // Now create one PlayPanel for each team and assign its players:
        for (List<Player> playerList: teams.values()){
            int team = playerList.get(0).getTeam().getData();
            String puzzleURL;
            if(puzzleGroupIndex<0) puzzleURL = "RANDOM_" + (-puzzleGroupIndex);
            else puzzleURL = String.format("res/data/puzzles/puzzle_%02d_%02d_01", playerList.size(), puzzleGroupIndex);
            System.out.println("puzzle url: " + puzzleURL);
            PlayPanel newPlayPanel = new PlayPanel(team, playerList, SEED, puzzleURL, connectionManager.getSynchronizer(), LocationType.NIGHTTIME);
            playPanelMap.put(team,newPlayPanel);
        }

        // Add the PlayPanels to the A ScrollableView on the Scene:
        ScrollableView<PlayPanel> playPanels = new ScrollableView<>(locationType.getBackground().getImageView(), locationType.getMidground().getImageView(), locationType.getSeparator());
        playPanels.addItems(this.playPanelMap.values());
        rootNode.getChildren().add(playPanels);

        // Add a chat overlay. Put it at the bottom of the screen, with no background:
        chatBox = new ChatBox(localPlayer, 125, false);
        chatBox.displayMessage(new Message("Use mouse wheel to scroll messages", localPlayer.getPlayerID()));
        chatBox.displayMessage(new Message("Press p to pause", localPlayer.getPlayerID()));
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
        addEventHandler(MouseEvent.MOUSE_MOVED,(event)-> localPlayer.pointCannon(event.getX(), event.getY()));
        addEventHandler(MouseEvent.MOUSE_PRESSED, (event) ->{
            localPlayer.pointCannon(event.getX(), event.getY());
            localPlayer.changeFireCannon();
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED,(event)-> localPlayer.pointCannon(event.getX(), event.getY()));

        // Add keyboard listeners:
        setOnKeyPressed(event -> {
            switch(event.getCode()){
                case ENTER:
                    System.out.println("enter pressed!");
                    if(!chatBox.isTextFieldShowing()){
                        chatBox.showTextField();
                    }
                    break;
                case P:
                    System.out.println("Pause pressed!");
                    pause.changeTo(true); // ToDo: Temporary. This should actually TOGGLE the boolean, not just set it to true.
                    break;
                case U: // A temporary unpause function. ToDo: remove this.
                    pause.changeTo(false);
                    break;
                case F: // For testing. Intentionally de-synchronizes a client's ammunitionOrbs list to see whether it will eventually be overwritten with the host's data:
                    localPlayer.desynchronizeAmmunitionOrbs();
                    break;
                case A: // For testing. Intentionally de-synchronizes a client's orbArray to see whether it will eventually be overwritten with the host's data.
                    playPanelMap.get(localPlayer.team.getData()).getOrbArray().getData()[0][0] = NULL;
                    break;
                case T: // For testing. Intentionally de-synchronizes a client's transferInOrbs to see whether it will eventually be overwritten with the host's data.
                    playPanelMap.get(localPlayer.team.getData()).getOrbArray().setModify(10,0, new Orb(OrbColor.BLUE,10,0, Orb.OrbAnimationState.STATIC));
                    /*for (int j=0; j<ARRAY_WIDTH_PER_CHARACTER*1; j+=2){
                        playPanelMap.get(localPlayer.team.getData()).getOrbArray().setModify(10,j, new Orb(OrbColor.BLUE,10,j, Orb.OrbAnimationState.STATIC));
                    }
                    for (int j=0; j<ARRAY_WIDTH_PER_CHARACTER*1; j+=2){
                        playPanelMap.get(localPlayer.team.getData()).getOrbArray().setModify(10,j, new Orb(OrbColor.BLUE,10,j, Orb.OrbAnimationState.STATIC))
                    }*/
                    break;
                case R:
                    playPanelMap.get(localPlayer.team.getData()).printOrbArray();
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
                if(initializing){
                    // Initialize some bookkeeping variables:
                    nextAnimationFrameInstance = now;
                    nextDataUpdateInstance = now;
                    nextLatencyTest = now;
                    nextSendInstance = now;
                    nextReport = now;

                    initializing = false;
                }

                // Incoming packets are processed as often as possible:
                workerThread.submit(receivePacketsTasks);

                // Data is updated and packets are sent 24 times per second
                if(now>nextDataUpdateInstance){
                    nextDataUpdateInstance += 1000000000L/ DATA_FRAME_RATE;

                    // Have the worker thread start computing the next frame:
                    workerThread.submit(updateFrameTasks);

                    // Play sound effects:
                    playSoundEffects();

                    workerThread.submit(sendPacketsTasks);
                }

                // Visuals are updated 48 times per second.
                if(now>nextAnimationFrameInstance){
                    nextAnimationFrameInstance += 1000000000L/ VISUAL_FRAME_RATE;
                    updatePlayPanelViews();
                    updateGameView();
                }

                // Latency probes are sent by the host 3 times per second:
                if(isHost){
                    if(now>nextLatencyTest) {
                        nextLatencyTest += 1000000000L/connectionManager.getLatencyTestsPerSecond();
                        connectionManager.send(new LatencyPacket());
                    }
                }

                // A debugging message is displayed approximately once per second:
                if(now> nextReport) {
                    nextReport += 1000000000L;
                    workerThread.submit(reportingTasks);
                }
            }
        };
        animationTimer.start();
    }

    private void playSoundEffects(){
        synchronized (connectionManager.getSynchronizer()){ // we don't want a new sound effect to be added just before we clear the Set.
            for(SoundEffect soundEffect : soundEffectsToPlay) SoundManager.playSoundEffect(soundEffect);
            soundEffectsToPlay.clear();
        }
    }

    private void updateGameView(){
        // pause the game if doing so is indicated:
        if(pause.getData()) displayPauseMenu();
        else removePauseMenu();

        // If the host has canceled the game, return to the main menu:
        if(gameCanceled.getData()){
            cleanUp();
            SceneManager.switchToMainMenu();
            showGameCanceledDialog();
        }

        // Check whether victory has been declared:
        // todo: there's got to be a better way of ensuring that these methods are only called once...
        if(victoryPauseStarted && !victoryDisplayStarted) startVictoryPause_View();
        else if(victoryPauseStarted) startVictoryDisplay_View(victoriousTeam);
    }

    private void updatePlayPanelViews(){
        for(PlayPanel playPanel : playPanelMap.values()){
            playPanel.repaint();
        }
    }

    private void checkForVictory_Model(){
        // Todo: what if 2 players declare victory at the exact same time?
        // check to see whether anybody's won a quick victory by clearing their PlayPanel:
        for(PlayPanel playPanel : playPanelMap.values()){
            if(playPanel.getTeamState().getData()==PlayPanel.TeamState.VICTORIOUS){
                // Todo: host should check whether it's actually true.
                startVictoryPause_Model(playPanel.getTeam());
                System.out.println("Hey, somebody won a quick victory!");
            }
        }

        // In competitive multiplayer (or Vs Computer) games, check to see whether there's only 1 live team left
        if(playPanelMap.values().size()>1){
            Set<Integer> liveTeams = new HashSet<>();
            for(Player player : players){
                Player.PlayerStatus playerPlayerStatus = player.getPlayerStatus().getData();
                if(playerPlayerStatus != Player.PlayerStatus.DEFEATED && playerPlayerStatus != Player.PlayerStatus.DISCONNECTED) liveTeams.add(player.getTeam().getData());
            }
            if(liveTeams.size()==1){
                System.out.println("There's only 1 team left. They've won the game!");
                startVictoryPause_Model(liveTeams.iterator().next());
            }

            // Todo: in the offhand chance that everyone died at the exact same time, declare a tie or pick a winner at random from those who just died this turn (check the isDefeatedChanged flag).
            else if(liveTeams.size() == 0){
                System.out.println("WHOA!!! A tie!!!!");
            }
        }

        // In puzzle games, check to see whether the only existing team has lost:
        else{
            if(players.get(0).getPlayerStatus().getData()== Player.PlayerStatus.DEFEATED) startVictoryPause_Model(-1);
        }

        // If someone has won, handle the delay before the victory graphics are actually displayed:
        if(victoryPauseStarted && !victoryDisplayStarted){
            if(((System.nanoTime() - victoryTime)/1000000000)>0.85) victoryDisplayStarted = true;
        }
    }

    private class ReceivePacketsTasks implements Callable<Void>{
        @Override
        public Void call(){
            processPackets();
            return null;
        }
    }

    private class UpdateFrameTasks implements Callable<Void> {
        @Override
        public Void call(){
            // Process bot players. This is done only by the host to ensure a consistent outcome (if clients did the
            // processing, you could end up with 2 clients computing different outcomes for a common robot ally. I
            // suppose I could just make sure they use the same random number generator, but having the host do it
            // is just easier and reduces the overall amount of computation for the clients.
            if(!pause.getData() && isHost) processBots();

            if(!pause.getData()){
                // update each PlayPanel:
                for(PlayPanel playPanel: playPanelMap.values()){
                    long time = System.nanoTime();
                    PlayPanel.Outcome outcome = playPanel.tick();
                    synchronized (connectionManager.getSynchronizer()){ // The applicationThread plays the sounds and clears this list.
                        soundEffectsToPlay.addAll(outcome.soundEffectsToPlay);
                    }
                    time = System.nanoTime() - time;
                    playPanelTickTime[0]++;
                    playPanelTickTime[1]+=time;
                    if(time < playPanelTickTime[2]) playPanelTickTime[2] = time;
                    if(time > playPanelTickTime[3]) playPanelTickTime[3] = time;
                }
                // process inter-PlayPanel events (transferring orbs and determining victory/defeat):
                tick();
            }

            // Check for victory conditions:
            checkForVictory_Model();
            return null;
        }
    }

    private class FrameResult{
        Set<SoundEffect> soundEffectsToPlay;
        List<PlayPanel> playPanelListCopy;
        List<Player> playerListCopy;
        FrameResult(Set<SoundEffect> soundEffectsToPlay, List<PlayPanel> playPanelList, List<Player> playerList){
            this.soundEffectsToPlay = soundEffectsToPlay;
            playPanelListCopy = playPanelList;
            playerListCopy = playerList;
        }
    }

    private class SendPacketsTasks implements Callable<Void>{
        @Override
        public Void call(){
            // Put a timestamp on fired Orbs:
            /*if(localPlayer.getPlayer().isFiring()){
                for(Orb orb: localPlayer.getPlayer().getFiredOrbs()){
                    long prevSendInstance = nextSendInstance - 1000000000L/connectionManager.getPacketsSentPerSecond();
                    orb.computeTimeStamp(prevSendInstance);
                }
            }*/

            // Display new chat messages in the chat box
            for(Player player : players){
                chatBox.displayMessages(player.getMessagesOut().getData());
            }

            // Process outgoing Packets
            connectionManager.send(new Packet(connectionManager.getSynchronizer()));

            checkForDisconnectedPlayers();

            // clear changedData so that changes are only broadcast once:
            connectionManager.getSynchronizer().resetChangedData();

            return null;
        }
    }

    // for debugging
    private class ReportingTasks implements Callable<Void>{
        @Override
        public Void call(){
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
            return null;
        }
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

    private void processPackets(){
        Packet packet = connectionManager.retrievePacket();
        Synchronizer localSynchronizer = connectionManager.getSynchronizer();

        // Process Packets one at a time:
        while(packet!=null){
            Synchronizer receivedSynchronizer = packet.getSynchronizer();

            localSynchronizer.synchronizeWith(receivedSynchronizer, isHost);

            // Prepare for the next iteration:
            packet = connectionManager.retrievePacket();
        }
    }

    private void checkForDisconnectedPlayers(){
        List<Long> disconnectedPlayerIDs = connectionManager.getSynchronizer().getDisconnectedIDs(maxConsecutivePacketsMissed);

        for (Player player: players) {
            Player.PlayerType playerType = player.getPlayerType().getData();
            if(!(playerType == Player.PlayerType.REMOTE_CLIENTVIEW || playerType == Player.PlayerType.REMOTE_HOSTVIEW)) continue; // Only RemotePlayers are capable of having connection issues, so only check them.
            if(!isHost && player.getPlayerID()!=HOST_ID) continue; // Clients only keep track of the host's connection to them.

            // If the player has been gone for too long, ask the user what to do:
            if(disconnectedPlayerIDs.contains(player.getPlayerID())){
                connectionManager.getSynchronizer().printDisconnectedPlayer(player.getPlayerID());
                showConnectionLostDialog(player);
            }
        }
    }

    // Todo: make this dialog automatically disappear if the player reconnects
    private void showConnectionLostDialog(Player player){
        boolean droppedPlayerIsHost = player.getPlayerID()==0;

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
        Label dialogLabel = new Label("Communication with player " + player.getUsername().getData() + " seems to have been lost.\n What would you like to do?");
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
                player.getPlayerStatus().changeTo(Player.PlayerStatus.DEFEATED);
            }
            dialogStage.close();
        }));

        wait.setOnAction((event) -> {
            connectionManager.getSynchronizer().waitForReconnect(player.getPlayerID(),maxConsecutivePacketsMissed - DATA_FRAME_RATE*10);
            dialogStage.close();
        });

        dialogStage.show();
    }

    private void showGameCanceledDialog(){
        Alert cancelAlert = new Alert(Alert.AlertType.CONFIRMATION);
        cancelAlert.initOwner(SceneManager.getPrimaryStage());
        cancelAlert.setTitle("Game Cancelled");
        cancelAlert.setHeaderText("The host is no longer connected. Game Cancelled.");
        javafx.scene.control.ButtonType returnBtn = new javafx.scene.control.ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
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
        // Tell the other players that we're leaving:
        if(isHost){
            gameCanceled.changeTo(true);
        }
        else{
            localPlayer.getPlayerStatus().changeTo(Player.PlayerStatus.DISCONNECTED);
        }
        connectionManager.send(new Packet(connectionManager.getSynchronizer()));

        // wait a little bit to make sure the packet gets through:
        // todo: replace this with a blocking send() method of some sort?
        try{
            Thread.sleep(2000/ DATA_FRAME_RATE);
        } catch (InterruptedException e){
            System.err.println("InterruptedException encountered while trying to leave the game. Other players" +
                    "might not be informed that you've left but... oh well, leaving anyways.");
            e.printStackTrace();
        }

        // Stop the other threads that are running:
        animationTimer.stop();
        connectionManager.cleanUp(); // stops the receiver and sender workers in the connectionManager
        for(PlayPanel playPanel : playPanelMap.values()){
            for(Player player : playPanel.getPlayerList()){
                if(player instanceof BotPlayer){
                    ((BotPlayer) player).cleanUp(); // shuts down the BotPlayer's thread pool
                }
            }
        }
        workerThread.shutdown();
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
        ImageView background = DrawingName.PAUSE_MENU_BACKDROP.getImageView();
        pauseMenu.getChildren().add(background);

        // place the title and buttons:
        VBox buttonsHolder = new VBox();
        buttonsHolder.setAlignment(Pos.CENTER);
        buttonsHolder.setSpacing(3.0);
        Rectangle spacer = new Rectangle(1,120); // Spacer so that the buttons don't run into the title
        spacer.setFill(Color.TRANSPARENT);
        Button resignBtn = createButton(ButtonType.RESIGN);
        Button unpauseBtn = createButton(ButtonType.UNPAUSE);
        buttonsHolder.getChildren().addAll(spacer, resignBtn, unpauseBtn);
        pauseMenu.getChildren().add(buttonsHolder);

        return pauseMenu;
    }

    private Button createButton(ButtonType buttonEnum){
        Button btn = new Button();
        ImageView unselectedImage = buttonEnum.getUnselectedImageView();
        ImageView selectedImage = buttonEnum.getSelectedImageView();
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
                    javafx.scene.control.ButtonType cancel = new javafx.scene.control.ButtonType("Cancel");
                    javafx.scene.control.ButtonType yes = new javafx.scene.control.ButtonType("Exit");
                    exitConfirmation.getButtonTypes().setAll(yes,cancel);
                    exitConfirmation.setGraphic(null);
                    Optional<javafx.scene.control.ButtonType> result = exitConfirmation.showAndWait();
                    if(result.isPresent() && result.get() == yes) {
                        cleanUp();
                        SceneManager.switchToMainMenu();
                    }
                    break;
                case UNPAUSE:
                    System.out.println("Unpause Button Pressed!");
                    //removePauseMenu();
                    pause.changeTo(false);
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
            List<Orb> transferOutOrbs = fromPlayPanel.getTransferOutOrbs();
            if(!transferOutOrbs.isEmpty()){
                for(PlayPanel toPlayPanel : playPanelMap.values()){
                    if(fromPlayPanel!=toPlayPanel){
                        SynchronizedList<Orb> transferInOrbs = toPlayPanel.getTransferInOrbs();
                        Random randomTransferOrbGenerator = toPlayPanel.getRandomTransferOrbGenerator();
                        Orb[][] orbArray = toPlayPanel.getOrbArray().getData();
                        Orb[] deathOrbs = toPlayPanel.getDeathOrbs();
                        toPlayPanel.transferOrbs(transferOutOrbs,transferInOrbs.getData(),randomTransferOrbGenerator,orbArray,deathOrbs);
                    }
                }
            }
            transferOutOrbs.clear();
        }
    }

    // If victoriousTeam == -1, that means nobody won (player failed a puzzle challenge, for example)
    // todo: what about ties? Am I gonna bother with those?
    private void startVictoryPause_Model(int victoriousTeam){
        if(victoryPauseStarted) return; // To ensure that the effects of this method are only applied once.
        victoryTime = System.nanoTime();
        this.victoriousTeam = victoriousTeam;

        // Disable all cannons and freeze all defeated players:
        for(Player player : players){
            if(player.getTeam().getData() == victoriousTeam){
                player.getCharacter().setCharacterAnimationState(VICTORIOUS);
            }
            else{
                player.getCharacter().setCharacterAnimationState(DEFEATED);
                player.getCannon().setCannonAnimationState(CannonType.CannonAnimationState.DEFEATED);
            }
        }

        // clear any outstanding shooting orbs:
        for(PlayPanel playPanel: playPanelMap.values()){
            playPanel.getShootingOrbs().clear();
        }

        victoryPauseStarted = true;
        System.out.println("team " + victoriousTeam + " has won.");
    }

    // If victoriousTeam == -1, that means nobody won (player failed a puzzle challenge, for example)
    // todo: what about ties? Am I gonna bother with those?
    private void startVictoryPause_View(){
        if(victoryPauseStarted2) return;
        SoundManager.silenceMusic();
        SoundManager.silenceAllSoundEffects();
        SoundManager.playSoundEffect(SoundEffect.VICTORY_FLOURISH);
        victoryPauseStarted2 = true;
    }

    private void startVictoryDisplay_View(int victoriousTeam){
        if(victoryDisplayStarted2) return;
        for(PlayPanel playPanel: playPanelMap.values()){
            if(playPanel.getTeam() == victoriousTeam){
                if(playPanelMap.values().size()==1) playPanel.displayVictoryResults(PlayPanel.VictoryType.PUZZLE_CLEARED);
                else playPanel.displayVictoryResults(PlayPanel.VictoryType.VS_WIN);
            }
            else {
                if(playPanelMap.values().size()==1) playPanel.displayVictoryResults(PlayPanel.VictoryType.PUZZLE_FAILED);
                else playPanel.displayVictoryResults(PlayPanel.VictoryType.VS_LOSE);
            }
        }
        if(localPlayer.getTeam().getData() == victoriousTeam) SoundManager.playSong(Music.GO_TAKE_FLIGHT,false);
        else SoundManager.playSong(Music.GAME_OVER,false);

        VBox vBox = new VBox();
        vBox.setMaxWidth(1.0); // to prevent the VBox from covering the scroll buttons
        Button returnToMain = createButton(ButtonType.RETURN_TO_MAIN_MENU_CLIENT);
        vBox.getChildren().add(returnToMain);
        vBox.setAlignment(Pos.TOP_CENTER);
        rootNode.getChildren().add(vBox);

        victoryDisplayStarted2 = true;
    }
}

enum LocationType {
    NIGHTTIME(DrawingName.NIGHT_SKY,
            DrawingName.NIGHT_SKY_CLOUDS,
            DrawingName.PLAYPANEL_NIGHTSKY_FOREGROUND_CLOUDS,
            DrawingName.PLAYPANEL_NIGHTSKY_DROPCLOUD,
            DrawingName.PLAYPANEL_NIGHTSKY_SEPARATOR);

    private DrawingName background;
    private DrawingName midground;
    private DrawingName foregroundCloudsEnum;
    private DrawingName dropCloudEnum;
    private DrawingName separator;

    LocationType(DrawingName background, DrawingName midground, DrawingName foregroundCloudsEnum, DrawingName dropCloudEnum, DrawingName separator){
        this.background = background;
        this.midground = midground;
        this.foregroundCloudsEnum = foregroundCloudsEnum;
        this.dropCloudEnum = dropCloudEnum;
        this.separator = separator;
    }

    public DrawingName getBackground(){
        return background;
    }
    public DrawingName getMidground(){
        return midground;
    }
    public DrawingName getForegroundCloudsEnum(){
        return foregroundCloudsEnum;
    }
    public DrawingName getDropCloudEnum(){
        return dropCloudEnum;
    }
    public DrawingName getSeparator(){
        return separator;
    }
}

