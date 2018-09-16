package Classes;

import Classes.Audio.Music;
import Classes.Audio.SoundEffect;
import Classes.Audio.SoundManager;
import Classes.Images.ButtonType;
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
import java.util.concurrent.*;

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
    private GameData gameData;
    private ChatBox chatBox;
    private LocalPlayer localPlayer;
    private List<PlayerData> players;

    // Variables related to animation and timing:
    private AnimationTimer animationTimer;
    private boolean initializing = true;
    public static final int FRAME_RATE = 24;
    private long nextAnimationFrameInstance = 0; // Time at which all animations will be updated to the next frame (nanoseconds)

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
    private Future<FrameResult> resultHolder;

    private boolean victoryPauseStarted2 = false;
    private boolean victoryDisplayStarted2 = false;

    // a negative value for puzzleGroupIndex indicates that a RANDOM puzzle with -puzzleGroupIndex rows should be created.
    // todo: don't use indices for the puzzles. Instead, pass a PuzzleSet enum, and have the PuzzleSet store pointers to the various puzzles. Include special enums for random puzzles.
    public GameScene(boolean isHost, ConnectionManager connectionManager, List<PlayerData> players, LocationType locationType, int puzzleGroupIndex){
        super(new StackPane());
        rootNode = (StackPane) getRoot();
        this.isHost = isHost;
        this.connectionManager = connectionManager;
        gameData = new GameData(connectionManager.getSynchronizer());
        this.players = players;
        maxConsecutivePacketsMissed = FRAME_RATE * 5;

        // Arrange the players into their teams by adding each one to an appropriate List:
        Map<Integer, List<PlayerData>> teams = new HashMap<>();
        for (PlayerData player: players) {
            int team = player.getTeam();
            if(teams.containsKey(team)){
                teams.get(team).add(player);
            }
            else{
                List<PlayerData> newTeam = new LinkedList<>();
                newTeam.add(player);
                teams.put(team,newTeam);
            }

            // while we're here, check to see if this is the LocalPlayer.
            if(player instanceof LocalPlayer) localPlayer = (LocalPlayer) player;
            gameData.getMissedPacketsCount().put(player.getPlayerID(),0);
        }

        // Now create one PlayPanel for each team and assign its players:
        for (List<PlayerData> playerList: teams.values()){
            int team = playerList.get(0).getTeam();
            String puzzleURL;
            if(puzzleGroupIndex<0) puzzleURL = "RANDOM_" + (-puzzleGroupIndex);
            else puzzleURL = String.format("res/data/puzzles/puzzle_%02d_%02d_01", playerList.size(), puzzleGroupIndex);
            System.out.println("puzzle url: " + puzzleURL);
            PlayPanel newPlayPanel = new PlayPanel(team, playerList, LocationType.NIGHTTIME,SEED, puzzleURL);
            playPanelMap.put(team,newPlayPanel);
        }

        // Add the PlayPanels to the A ScrollableView on the Scene:
        ScrollableView<PlayPanel> playPanels = new ScrollableView<>(locationType.getBackground().getImageView(), locationType.getMidground().getImageView(), locationType.getSeparator());
        playPanels.addItems(this.playPanelMap.values());
        rootNode.getChildren().add(playPanels);

        // Add a chat overlay. Put it at the bottom of the screen, with no background:
        chatBox = new ChatBox(gameData, localPlayer, 125, false);
        chatBox.addNewMessageIn(new Message("Use mouse wheel to scroll messages", localPlayer.getPlayerID()));
        chatBox.addNewMessageIn(new Message("Press p to pause", localPlayer.getPlayerID()));

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
                    if(!chatBox.isTextFieldShowing()){
                        chatBox.showTextField();
                    }
                    break;
                case P:
                    System.out.println("Pause pressed!");
                    gameData.changePause(true); // ToDo: Temporary. This should actually TOGGLE the boolean, not just set it to true.
                    break;
                case U: // A temporary unpause function. ToDo: remove this.
                    gameData.changePause(false);
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
                    nextLatencyTest = now;
                    nextSendInstance = now;
                    nextReport = now;

                    // Initialize resultHolder by computing the first frame:
                    resultHolder = workerThread.submit(updateFrameTasks);

                    initializing = false;
                }

                // Incoming packets are processed as often as possible:
                workerThread.submit(receivePacketsTasks);

                // Visuals are updated 24 times per second.
                if(now>nextAnimationFrameInstance){
                    nextAnimationFrameInstance += 1000000000L/ FRAME_RATE;
                    try{
                        // Retrieve the outcome of the previous frame and have the worker thread start computing the next frame:
                        FrameResult previousResult = resultHolder.get();
                        resultHolder = workerThread.submit(updateFrameTasks);

                        // update visuals corresponding to the PlayPanelData and PlayerData:
                        updatePlayPanelViews(previousResult.playPanelDataListCopy, previousResult.playerDataListCopy);

                        // Play sound effects:
                        for(SoundEffect soundEffect : previousResult.soundEffectsToPlay) SoundManager.playSoundEffect(soundEffect);

                        // update visuals corresponding to GameData:
                        updateGameView(previousResult.gameDataCopy);
                    } catch (ExecutionException | InterruptedException e){
                        e.printStackTrace();
                    }
                }

                // Outgoing Packets are transmitted 10 times per second. The game also deals with dropped players at this time:
                if(now>nextSendInstance){
                    nextSendInstance += 1000000000L/connectionManager.getPacketsSentPerSecond();
                    workerThread.submit(sendPacketsTasks);
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

    private void updateGameView(GameData gameData){
        // pause the game if doing so is indicated:
        if(gameData.getPause()) displayPauseMenu();
        else removePauseMenu();

        // Display new chat messages in the chat box:
        chatBox.displayNewMessagesIn();

        // If the host has canceled the game, return to the main menu:
        if(gameData.isCancelGameRequested()){
            cleanUp();
            SceneManager.switchToMainMenu();
            showGameCanceledDialog();
        }

        // Check whether victory has been declared:
        // todo: there's got to be a better way of ensuring that these methods are only called once...
        if(gameData.getVictoryPauseStarted() && !gameData.getVictoryDisplayStarted()) startVictoryPause_View();
        else if(gameData.getVictoryDisplayStarted()) startVictoryDisplay_View(gameData.getVictoriousTeam());
    }

    private void updatePlayPanelViews(List<PlayPanelData> playPanelDataList, List<PlayerData> playerDataList){
        for(PlayPanelData playPanelData : playPanelDataList){
            // Repaint every PlayPanel:
            PlayPanel playPanel = playPanelMap.get(playPanelData.getTeam());
            playPanel.repaint(playPanelData, playerDataList);

            // If a player has been gone for too long (and they have not resigned), ask the host what to do:
            for(PlayerData player : playPanel.getPlayerList()){
                Integer missedPackets = gameData.getMissedPacketsCount(player.getPlayerID());
                if(missedPackets==maxConsecutivePacketsMissed && !player.getDefeated()){
                    showConnectionLostDialog(player);
                }
            }

        }
    }

    private void checkForVictory_Model(){
        // Todo: what if 2 players declare victory at the exact same time?
        // check to see whether anybody's won a quick victory by clearing their PlayPanel:
        for(PlayPanel playPanel : playPanelMap.values()){
            if(playPanel.getPlayPanelData().isVictoriousChanged()){
                // Todo: host should check whether it's actually true.
                startVictoryPause_Model(playPanel.getPlayPanelData().getTeam());
                System.out.println("Hey, somebody won a quick victory!");
            }
        }

        // In competitive multiplayer (or Vs Computer) games, check to see whether there's only 1 live team left
        if(playPanelMap.values().size()>1){
            Set<Integer> liveTeams = new HashSet<>();
            for(PlayerData player : players){
                if(!player.getDefeated()) liveTeams.add(player.getTeam());
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
            if(players.get(0).getDefeated()) startVictoryPause_Model(-1);
        }

        // If someone has won, handle the delay before the victory graphics are actually displayed:
        if(gameData.getVictoryPauseStarted() && !gameData.getVictoryDisplayStarted()){
            if(((System.nanoTime() - gameData.getVictoryTime())/1000000000)>0.85) startVictoryDisplay_Model();
        }
    }

    private class ReceivePacketsTasks implements Callable<Void>{
        @Override
        public Void call(){
            if(isHost) processPacketsAsHost();
            else processPacketsAsClient();
            return null;
        }
    }

    private class UpdateFrameTasks implements Callable<FrameResult> {
        @Override
        public FrameResult call(){
            // Process bot players. This is done only by the host to ensure a consistent outcome (if clients did the
            // processing, you could end up with 2 clients computing different outcomes for a common robot ally. I
            // suppose I could just make sure they use the same random number generator, but having the host do it
            // is just easier and reduces the overall amount of computation for the clients.
            if(!gameData.getPause() && isHost) processBots();

            Set<SoundEffect> soundEffectsToPlay = EnumSet.noneOf(SoundEffect.class); // Sound effects to play this frame.
            if(!gameData.getPause()){
                // update each PlayPanel:
                for(PlayPanel playPanel: playPanelMap.values()){
                    long time = System.nanoTime();
                    playPanel.tick(soundEffectsToPlay);
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

            // Copy the playPanelData
            List<PlayPanelData> playPanelDataList = new LinkedList<>();
            for(PlayPanel playPanel : playPanelMap.values()){
                playPanelDataList.add(new PlayPanelData(playPanel.getPlayPanelData()));
            }

            // Copy the playerData
            List<PlayerData> playerDataList = new LinkedList<>();
            for(PlayerData player : players){
                playerDataList.add(new PlayerData(player));
            }

            return new FrameResult(soundEffectsToPlay, new GameData(gameData) , playPanelDataList, playerDataList);
        }
    }

    private class FrameResult{
        Set<SoundEffect> soundEffectsToPlay;
        GameData gameDataCopy;
        List<PlayPanelData> playPanelDataListCopy;
        List<PlayerData> playerDataListCopy;
        FrameResult(Set<SoundEffect> soundEffectsToPlay, GameData gameData, List<PlayPanelData> playPanelDataList, List<PlayerData> playerDataList){
            this.soundEffectsToPlay = soundEffectsToPlay;
            gameDataCopy = gameData;
            playPanelDataListCopy = playPanelDataList;
            playerDataListCopy = playerDataList;
        }
    }

    private class SendPacketsTasks implements Callable<Void>{
        @Override
        public Void call(){
            // Put a timestamp on fired Orbs:
            /*if(localPlayer.getPlayerData().isFiring()){
                for(Orb orb: localPlayer.getPlayerData().getFiredOrbs()){
                    long prevSendInstance = nextSendInstance - 1000000000L/connectionManager.getPacketsSentPerSecond();
                    orb.computeTimeStamp(prevSendInstance);
                }
            }*/

            // Process outgoing Packets
            if(isHost) prepareAndSendServerPacket();
            else prepareAndSendClientPacket();

            // Increment the missed packets count (used for detecting dropped players):
            incrementMissedPacketsCounts();

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
                for (PlayerData player : playPanel.getPlayerList()){
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
            for (PlayerData player : playPanel.getPlayerList()){
                if (player instanceof BotPlayer){
                    ((BotPlayer) player).tick();
                }
            }
        }
    }

    private void processPacketsAsHost(){
        Packet packet = connectionManager.retrievePacket();

        // Process Packets one at a time:
        while(packet!=null){
            // Pop the PlayerData from the packet:
            PlayerData playerData = packet.popPlayerData();
            if(!gameData.getPause()){
                // Find the PlayPanel associated with the player:
                PlayPanel playPanel = playPanelMap.get(playerData.getTeam());
                // Update the PlayPanelData and the PlayerData:
                playPanel.updatePlayer(playerData, isHost);
            }
            // Reset the missed packets counter
            gameData.setMissedPacketsCount(playerData.getPlayerID(),0);

            // Then process the GameData:
            GameData clientGameData = packet.getGameData();
            updateGameData(clientGameData, isHost);

            // Prepare for the next iteration:
            packet = connectionManager.retrievePacket();
        }
    }

    private void prepareAndSendServerPacket(){
        // We must create a copy of the local PlayerData, GameData, and PlayPanelData and add those copies to the packet. This
        // is to prevent the change flags within the local PlayerData and GameData from being reset before the packet is
        // sent (note the last 4 lines of this method).
        PlayerData localPlayerData = new PlayerData(localPlayer);
        GameData localGameData = new GameData(gameData);
        PlayPanelData localPlayPanelData = new PlayPanelData(playPanelMap.get(localPlayerData.getTeam()).getPlayPanelData());
        Packet outPacket = new Packet(localPlayerData, localGameData, localPlayPanelData);

        for (PlayerData player: players) {
            if(player.getPlayerID()==0) continue; // we've already added the localPlayer's PlayerData to the packet.
            PlayerData playerData = new PlayerData(player); // Yes, these need to be *new* PlayerData instances because there may be locally-stored bot data. see note regarding local player data, above.
            outPacket.addPlayerData(playerData);
            player.resetFlags(); // reset the local change flags so that they are only broadcast once
        }

        for (PlayPanel playPanel: playPanelMap.values()){
            if(playPanel.getPlayPanelData().getTeam()==localPlayerData.getTeam()) continue; // we've already added the localPlayer's PlayPanelData to the packet.
            PlayPanelData playPanelData = new PlayPanelData(playPanel.getPlayPanelData());
            outPacket.addOrbData(playPanelData);
            playPanel.getPlayPanelData().resetFlags(); // reset the local change flags so that they are only broadcast once
        }

        connectionManager.send(outPacket);

        // reset the local change flags so that they are only broadcast once:
        localPlayer.resetFlags();
        playPanelMap.get(localPlayerData.getTeam()).getPlayPanelData().resetFlags();
        gameData.resetFlags();
        gameData.resetMessages();
    }

    private void incrementMissedPacketsCounts(){
        for (PlayerData player: players) {
            if(!(player instanceof RemotePlayer)) continue; // Only RemotePlayers are capable of having connection issues, so only check them.
            if(!isHost && player.getPlayerID()!=0) continue; // Clients only keep track of the host's connection to them.

            // increment the missed packets count. The count will be reset whenever the next packet is received:
            gameData.incrementMissedPacketsCount(player.getPlayerID());
        }
    }

    private void processPacketsAsClient(){
        // Process Packets one at a time:
        Packet packet = connectionManager.retrievePacket();
        while(packet!=null){
            // Within each Packet, process PlayerData one at a time in order:
            PlayerData playerData = packet.popPlayerData();
            while(playerData!=null){
                if(!gameData.getPause()){
                    // update the Player and his/her PlayPanel with the new playerData:
                    PlayPanel playPanel = playPanelMap.get(playerData.getTeam());
                    playPanel.updatePlayer(playerData, isHost);
                }
                // Reset the missed packets counter:
                if(playerData.getPlayerID()==0) gameData.setMissedPacketsCount(playerData.getPlayerID(),0);

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
            packet = connectionManager.retrievePacket();
        }
    }

    private void updateGameData(GameData gameDataIn, boolean isHost){
        if(isHost){
            if(gameDataIn.isMessagesChanged()){
                List<Message> messages = gameDataIn.getMessages();
                System.out.println("host received messages. adding them to both messagesIn and messagesOut");
                gameData.changeAddMessages(new LinkedList<>(messages)); // host re-broadcasts the messages out to all clients
                chatBox.addNewMessagesIn(new LinkedList<>(messages)); // the messages in this list will be displayed on the screen later this frame
            }
            if(gameDataIn.isPauseRequested()) gameData.changePause(gameDataIn.getPause());
        }
        else{
            if(gameDataIn.isMessagesChanged()){
                List<Message> messages = gameDataIn.getMessages();
                System.out.println("client received messages. Adding them to messagesIn");
                chatBox.addNewMessagesIn(new LinkedList<>(messages)); // the messages in this list will be displayed on the screen later this frame
            }
            if(gameDataIn.isPauseRequested()) gameData.setPause(gameDataIn.getPause());
        }
    }

    private void prepareAndSendClientPacket(){
        // The network must create a copy of the local PlayerData and GameData and send those copies instead of the
        // originals. This is to prevent the change flags within the PlayerData and GameData from being reset before the
        // packet is sent.
        PlayerData localPlayerData = new PlayerData(localPlayer);
        GameData localGameData = new GameData(gameData);
        PlayPanelData localPlayPanelData = new PlayPanelData(playPanelMap.get(localPlayerData.getTeam()).getPlayPanelData());

        Packet outPacket = new Packet(localPlayerData, localGameData, localPlayPanelData);
        connectionManager.send(outPacket);

        // reset local change flags so that they are sent only once:
        localPlayer.resetFlags();
        playPanelMap.get(localPlayerData.getTeam()).getPlayPanelData().resetFlags();
        gameData.resetFlags();
        gameData.resetMessages();
    }

    // Todo: make this dialog automatically disappear if the player reconnects
    private void showConnectionLostDialog(PlayerData player){
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
        Label dialogLabel = new Label("Communication with player " + player.getUsername() + " seems to have been lost.\n What would you like to do?");
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
            gameData.setMissedPacketsCount(player.getPlayerID(), (int)(maxConsecutivePacketsMissed - FRAME_RATE*10));
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
            gameData.changeCancelGame(true);
            prepareAndSendServerPacket();
        }
        else{
            localPlayer.changeResignPlayer();
            prepareAndSendClientPacket();
        }

        // wait a little bit to make sure the packet gets through:
        // todo: replace this with a blocking send() method of some sort?
        try{
            Thread.sleep(2000/FRAME_RATE);
        } catch (InterruptedException e){
            System.err.println("InterruptedException encountered while trying to leave the game. Other players" +
                    "might not be informed that you've left but... oh well, leaving anyways.");
            e.printStackTrace();
        }

        // Stop the other threads that are running:
        animationTimer.stop();
        connectionManager.cleanUp(); // stops the receiver and sender workers in the connectionManager
        for(PlayPanel playPanel : playPanelMap.values()){
            for(PlayerData player : playPanel.getPlayerList()){
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
        ImageView background = StaticBgImages.PAUSE_MENU_BACKDROP.getImageView();
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
            List<OrbData> transferOutOrbs = fromPlayPanel.getPlayPanelData().getTransferOutOrbs();
            if(!transferOutOrbs.isEmpty()){
                for(PlayPanel toPlayPanel : playPanelMap.values()){
                    if(fromPlayPanel!=toPlayPanel){
                        toPlayPanel.changeAddTransferInOrbs(transferOutOrbs);
                    }
                }
            }
            transferOutOrbs.clear();
        }

        // Copy messages from the chatBox to the gameData so it can be forwarded to other players:
        Message nextNewMessage;
        while((nextNewMessage = chatBox.getNextNewMessageOut())!=null){
            System.out.println("adding messages that the player typed into messagesOut");
            gameData.changeAddMessage(nextNewMessage);
            if(isHost){ // The host prints his/her own messages immediately (clients only print incoming messages from the host).
                chatBox.addNewMessageIn(new Message(nextNewMessage));
            }
        }
    }

    // If victoriousTeam == -1, that means nobody won (player failed a puzzle challenge, for example)
    // todo: what about ties? Am I gonna bother with those?
    private void startVictoryPause_Model(int victoriousTeam){
        if(gameData.getVictoryPauseStarted()) return; // To ensure that the effects of this method are only applied once.
        gameData.setVictoryTime(System.nanoTime());
        gameData.setVictoriousTeam(victoriousTeam);

        // Disable all cannons and freeze all defeated players:
        for(PlayerData player : players){
            //if(player.getPlayerData().getTeam() == victoriousTeam){
            player.changeDisableCannon();
            //}
        }

        // clear any outstanding shooting orbs:
        for(PlayPanel playPanel: playPanelMap.values()){
            playPanel.getPlayPanelData().getShootingOrbs().clear();
        }

        gameData.setVictoryPauseStarted(true);
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

    private void startVictoryDisplay_Model(){
        gameData.setVictoryDisplayStarted(true);
    }

    private void startVictoryDisplay_View(int victoriousTeam){
        if(victoryDisplayStarted2) return;
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
        if(localPlayer.getTeam() == victoriousTeam) SoundManager.playSong(Music.GO_TAKE_FLIGHT,false);
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

