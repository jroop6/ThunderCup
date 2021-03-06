package Classes;

import Classes.Images.ButtonType;
import Classes.Animation.CharacterType;
import Classes.Images.DrawingName;
import Classes.NetworkCommunication.*;
import Classes.PlayerTypes.*;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static Classes.Player.GAME_ID;
import static Classes.Player.HOST_ID;
import static javafx.scene.layout.AnchorPane.setLeftAnchor;
import static javafx.scene.layout.AnchorPane.setRightAnchor;


/**
 * Created by Jonathan Roop on 7/26/2017.
 */
//todo: there are a couple of places where I use synchronized(). The current implementation of LobbyScene is single-threaded, however. Consider removing these in the final version of the game if it stays single-threaded.
public class LobbyScene extends Scene {

    // misc variables accessed by different methods in this class:
    private ConnectionManager connectionManager;
    private Player localPlayer;
    private ScrollableView<PlayerSlot> playerSlotContainer;
    private Scale scaler = new Scale();
    private ChatBox chatBox;
    private final int FRAME_RATE = 24;
    private long nextAnimationFrameInstance = 0; // Time at which all animations will be updated to the next frame (nanoseconds).
    private boolean initializing = true;
    private SynchronizedComparable<Boolean> gameCanceled;
    private SynchronizedComparable<Boolean> gameStarted;

    private Alert kickAlert;
    private int kickTimeout = FRAME_RATE*3;

    // Variables related to network communications:
    private boolean isHost; // Are we the host or a client?
    private final long maxConsecutivePacketsMissed = FRAME_RATE * 5; // If this many packets are missed consecutively from a particular player, alert the user.
    private long nextLatencyTest = 0; // The time at which the next latency probe will be sent (nanoseconds).

    // for misc debugging:
    private long nextReport = 0; // The time at which miscellaneous debugging info will next be printed (nanoseconds).

    private AnimationTimer animationTimer;

    LobbyScene(boolean isHost, String username, String host, int port){
        super(new VBox());
        VBox rootNode = (VBox)getRoot();
        this.isHost = isHost;

        // set up connection framework:
        if(isHost){
            connectionManager = new HostConnectionManager(port);
            localPlayer = new Player(username, Player.PlayerType.LOCAL, connectionManager.getSynchronizer().getId(), connectionManager.getSynchronizer());
        }
        else{
            connectionManager = new ClientConnectionManager(Player.createID(), host, port);
            localPlayer = new Player(username, Player.PlayerType.LOCAL, connectionManager.getSynchronizer().getId(), connectionManager.getSynchronizer());
        }
        connectionManager.setPlayerID(localPlayer.getPlayerID());
        System.out.println("our ID is " + localPlayer.getPlayerID());

        if (!connectionManager.isConnected()){
            SceneManager.switchToMainMenu();
            return;
        }

        synchronized (connectionManager.getSynchronizer()){
            gameCanceled = new SynchronizedComparable<>("cancelGame", false, SynchronizedData.Precedence.HOST, GAME_ID, connectionManager.getSynchronizer());
            gameStarted = new SynchronizedComparable<>("gameStarted", false, SynchronizedData.Precedence.HOST, GAME_ID, connectionManager.getSynchronizer());
        }

        // Get a background for the PlayerSlot container:
        ImageView scrollableViewbackground = DrawingName.DAY_SKY.getImageView();

        // Create a ScrollableView and place PlayerSlots in it:
        playerSlotContainer = new ScrollableView<>(scrollableViewbackground,new Rectangle(0.0,0.0,Color.TRANSPARENT));
        rootNode.getChildren().add(playerSlotContainer);

        // If this is the host player, add his/her player to the playerSlotContainer, along with a button to add more players:
        if(isHost){
            PlayerSlot hostPlayerSlot = new PlayerSlot(localPlayer, true);
            playerSlotContainer.addItem(hostPlayerSlot);
            Button addPlayerBtn = new ThunderButton(ButtonType.ADD_PLAYER,(event)->{
                playerSlotContainer.addItem(new PlayerSlot(new Player("Open Slot", Player.PlayerType.UNCLAIMED, Player.createID(), connectionManager.getSynchronizer()), isHost));
                ((HostConnectionManager)connectionManager).addOpenSlot();
            });
            Button addBotBtn = new ThunderButton(ButtonType.ADD_BOT, (event)->{
                synchronized (connectionManager.getSynchronizer()){
                    playerSlotContainer.addItem(new PlayerSlot(new BotPlayer(CharacterType.FILLY_BOT_MEDIUM, connectionManager.getSynchronizer()),true));
                }
            });
            VBox addButtonHolder = new VBox();
            addButtonHolder.setAlignment(Pos.CENTER);
            addButtonHolder.getChildren().addAll(addPlayerBtn,addBotBtn);
            playerSlotContainer.addSpecialItem(addButtonHolder);
        }
        // otherwise, add both a remote player (representing the host) and a localplayer. The hostplayer's data will eventually be updated:
        else{
            Player hostPlayer = new Player("???", Player.PlayerType.REMOTE_CLIENTVIEW, HOST_ID, connectionManager.getSynchronizer());
            PlayerSlot hostPlayerSlot = new PlayerSlot(hostPlayer,isHost);
            PlayerSlot localPlayerSlot = new PlayerSlot(localPlayer,isHost);
            playerSlotContainer.addItem(hostPlayerSlot);
            playerSlotContainer.addItem(localPlayerSlot);
        }

        // There are one or more buttons directly beneath the PlayerSlots, which allow the player to return to the main
        // menu, ask for the computer's name and port (host only), and start the game (host only)
        AnchorPane buttonHolder = new AnchorPane();
        buttonHolder.setBackground(new Background(new BackgroundImage(DrawingName.MSS_BUTTONS_BACKDROP.getImageView().getImage(),null,null,null,null)));
        buttonHolder.setPickOnBounds(false);
        HBox rightSideButtonsHolder = new HBox();
        rightSideButtonsHolder.setPickOnBounds(false);
        ThunderButton start = new ThunderButton(ButtonType.START, (event)->{
            // Todo: check whether there are any unclaimed open spots before starting the game.
            System.out.println("pressed Start!");
            gameStarted.changeTo(true);
        });
        start.setScaleX(-1.0);
        ThunderButton hostNameAndPort = new ThunderButton(ButtonType.HOST_AND_PORT, (event)->{
            System.out.println("pressed Hostname/Port!");
            showHostNameDialog();
        });
        hostNameAndPort.setScaleX(-1.0);
        if(isHost) rightSideButtonsHolder.getChildren().addAll(hostNameAndPort, start);
        ThunderButton returnToMainMenu;
        if(isHost) returnToMainMenu = new ThunderButton(ButtonType.RETURN_TO_MAIN_MENU_HOST, (event)->{
            System.out.println("pressed Exit!");
            cleanUp();
            SceneManager.switchToMainMenu();
        });
        else returnToMainMenu = new ThunderButton(ButtonType.RETURN_TO_MAIN_MENU_CLIENT, (event)->{
            System.out.println("pressed Exit!");
            cleanUp();
            SceneManager.switchToMainMenu();
        });
        returnToMainMenu.setPadding(Insets.EMPTY);
        buttonHolder.getChildren().addAll(returnToMainMenu,rightSideButtonsHolder);
        setLeftAnchor(returnToMainMenu, 0.0);
        setRightAnchor(rightSideButtonsHolder, 0.0);
        buttonHolder.setBorder(new Border(new BorderStroke(Color.BLACK,BorderStrokeStyle.SOLID,new CornerRadii(0.0),new BorderWidths(2.0))));
        rootNode.getChildren().add(buttonHolder);

        // A ChatBox is located under the Buttons:
        ImageView chatBoxBackground = DrawingName.CHATBOX_SCROLLPANE_BACKGROUND.getImageView();
        chatBox = new ChatBox(localPlayer, 200, true);
        rootNode.getChildren().add(chatBox);

        // Make everything scales correctly when the window is resized
        chatBoxBackground.getTransforms().add(scaler);
        returnToMainMenu.getTransforms().add(scaler);
        hostNameAndPort.getTransforms().add(scaler);
        start.getTransforms().add(scaler);
        rootNode.heightProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("New height of LobbyScene: " + newValue);
            double scaleValue = (double)newValue/1080.0;
            scaler.setX(scaleValue);
            scaler.setY(scaleValue);
            // The buttonHolder's minHeight property needs to be specially set, because scaling works weird with Buttons
            // (although the Buttons are visually scaled, the amount of space they take up stays the same. Setting the
            // minHeightProperty here remedies that problem).
            //Todo: This is buggy right now and I have no idea why.
            //buttonHolder.setMinHeight(hostNameAndPort.getHeight()*scaleValue);
        });
        // Alternative to the code that's inside the ChangeListener. This is also buggy.
        // buttonHolder.minHeightProperty().bind(Bindings.multiply(hostNameAndPort.heightProperty(),scaler.yProperty()));

        // Create the kick dialog in case the player gets kicked (but don't show it yet, of course):
        kickAlert = createKickDialog();

        // Start network communications:
        if(connectionManager.isConnected()) {
            System.out.println("starting network communications");
            connectionManager.start();
        }

        // Start the AnimationTimer:
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Initialize some bookkeeping variables:
                if(initializing){
                    nextAnimationFrameInstance = now;
                    nextLatencyTest = now;
                    nextReport = now;
                    initializing = false;
                }

                // Incoming packets are processed every frame, if there are any:
                if(isHost) processPacketsAsHost();
                else processPacketsAsClient();

                // Animations are updated and packets are sent 24 times per second:
                if(now>nextAnimationFrameInstance){
                    nextAnimationFrameInstance += 1000000000L/ FRAME_RATE;

                    // update character animations and display messages:
                    for (PlayerSlot playerSlot : playerSlotContainer.getContents()){
                        playerSlot.tick(0);
                        playerSlot.repaint(isHost);
                        chatBox.displayMessages(playerSlot.getPlayer().getMessagesOut().getData());
                    }

                    if(isHost)deleteRemovedPlayers();

                    prepareAndSendPacket();
                    connectionManager.getSynchronizer().resetChangedData();

                    // Perform special processing for game data:
                    if(gameCanceled.getData()){
                       cleanUp();
                       SceneManager.switchToMainMenu();
                       if(!isHost)showGameCanceledDialog();
                    }
                    if(gameStarted.getData()) startGame();
                    checkForDisconnectedPlayers();
                }

                // Latency probes are sent by the host 3 times per second:
                if(isHost){
                    if(now>nextLatencyTest) {
                        nextLatencyTest += 1000000000L/connectionManager.getLatencyTestsPerSecond();
                        System.out.println("probing latencies");
                        connectionManager.send(new LatencyPacket());
                    }
                }

                // A debugging message is displayed once per second:
                if(now> nextReport) {
                    nextReport += 1000000000L;
                    report();
                }
            }
        };
        animationTimer.start();
    }

    // for debugging
    // recall: {number of packets processed, number remaining, 0=host 1=client}
    public void report(){
        Map<Long,Long> latencies = connectionManager.getLatencies();
        System.out.print("Latencies: ");
        for(PlayerSlot playerSlot : playerSlotContainer.getContents()){
            Player player = playerSlot.getPlayer();
            if(latencies.containsKey(player.getPlayerID())){
                System.out.print(player.getUsername().getData() + ": " + latencies.get(player.getPlayerID()) + " // ");
            }
        }
        System.out.print("\n");
    }

    private void processPacketsAsHost(){
        Synchronizer receivedSynchronizer = connectionManager.retrievePacket();
        Synchronizer localSynchronizer = connectionManager.getSynchronizer();

        // Process Packets one at a time:
        while(receivedSynchronizer!=null){
            // check to see whether the local player in the received packet exists in our playerSlots list:
            long id = receivedSynchronizer.getId();
            synchronized (localSynchronizer){
                if(localSynchronizer.get(id,"username")==null){
                    // this is a new player, so see if there's an open slot available:
                    PlayerSlot availableSlot = getUnclaimedSlot();
                    if(availableSlot!=null){
                        // create the player and put him/her in the slot:
                        String username = (String)receivedSynchronizer.get(id,"username").getData();
                        Player newRemotePlayer = new Player(username, Player.PlayerType.REMOTE_HOSTVIEW, id, localSynchronizer);
                        availableSlot.changePlayer(newRemotePlayer, isHost);
                    }
                }
            }
            // Synchronize the Data:
            localSynchronizer.synchronizeWith(receivedSynchronizer,isHost);

            // Prepare for the next iteration:
            receivedSynchronizer = connectionManager.retrievePacket();
        }
    }

    private PlayerSlot getUnclaimedSlot(){
        for(PlayerSlot playerSlot : playerSlotContainer.getContents()){
            if(playerSlot.getPlayer().getPlayerType().getData() == Player.PlayerType.UNCLAIMED) return playerSlot;
        }
        return null;
    }

    private void checkForDisconnectedPlayers(){
        List<Long> disconnectedPlayerIDs = connectionManager.getSynchronizer().getDisconnectedIDs(maxConsecutivePacketsMissed);

        List<PlayerSlot> playerSlots = playerSlotContainer.getContents();
        for (PlayerSlot playerSlot: playerSlots) {
            Player player = playerSlot.getPlayer();
            Player.PlayerType playerType = playerSlot.getPlayer().getPlayerType().getData();
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
    private void showConnectionLostDialog(Player playerdata){
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
        Label dialogLabel = new Label("Communication with player " + playerdata.getUsername().getData() + " seems to have been lost.\n What would you like to do?");
        if(playerdata.getPlayerID()==HOST_ID){
            dialogLabel.setText("Communication with the host seems to have been lost. What would you like to do?");
        }
        dialogRoot.getChildren().add(dialogLabel);

        // Add buttons for the user's decision:
        Button drop = new Button("Drop Player");
        if(playerdata.getPlayerID()==HOST_ID){
            drop.setText("Exit game");
        }
        Button wait = new Button("wait 10 seconds and see if they reconnect.");
        HBox buttonContainer = new HBox();
        buttonContainer.setSpacing(10.0);
        buttonContainer.getChildren().addAll(drop,wait);
        dialogRoot.getChildren().add(buttonContainer);

        // Add functionality to the buttons:
        drop.setOnAction((event -> {
            if(playerdata.getPlayerID()==HOST_ID){ // The player who disconnected was the host, so close the game.
                cleanUp();
                SceneManager.switchToMainMenu();
                showGameCanceledDialog();
            }
            else{
                playerdata.getPlayerStatus().changeTo(Player.PlayerStatus.DISCONNECTED);
            }
            dialogStage.close();
        }));

        wait.setOnAction((event) -> {
            connectionManager.getSynchronizer().waitForReconnect(playerdata.getPlayerID(),maxConsecutivePacketsMissed - FRAME_RATE*10);
            dialogStage.close();
        });

        dialogStage.show();
    }

    // Cycle through all players in the playerSlotContainer and delete any that have the defeated flag set:
    private void deleteRemovedPlayers(){
        List<PlayerSlot> playerSlots = playerSlotContainer.getContents();
        for (PlayerSlot playerSlot: playerSlots){
            if(playerSlot.getPlayer().getPlayerStatus().getData()== Player.PlayerStatus.DISCONNECTED){
                if (playerSlot.getPlayer().getPlayerType().getData()== Player.PlayerType.UNCLAIMED){
                    // An open slot is being removed, so decrement the open slot counter:
                    ((HostConnectionManager)connectionManager).removeOpenSlot();
                }
                connectionManager.getSynchronizer().deRegisterAllWithID(playerSlot.getPlayer().getPlayerID());
                System.out.println("INSIDE HERE!");
                playerSlotContainer.removeItem(playerSlot);
            }
        }
    }

    private void processPacketsAsClient(){
        // Process Packets, one at a time:
        Synchronizer receivedSynchronizer = connectionManager.retrievePacket();
        Synchronizer localSynchronizer = connectionManager.getSynchronizer();

        while(receivedSynchronizer!=null){
            // Look for new players in the received Synchronizer:
            for(Map.Entry<Long, HashMap<String, SynchronizedData>> entry : receivedSynchronizer.getAll().entrySet()){
                long id = entry.getKey();
                if(id>=0 || id == HOST_ID) continue; // 0 is GAME_ID an anything >0 indicates a team, not a player. We've also already added the host player.
                synchronized (localSynchronizer){
                    if(localSynchronizer.get(id,"username")==null){
                        // this is a new player, so create it:
                        String username = (String)receivedSynchronizer.get(id,"username").getData();
                        Player.PlayerType playerType = (Player.PlayerType)receivedSynchronizer.get(id,"playerType").getData();
                        if(playerType!= Player.PlayerType.UNCLAIMED) playerType = Player.PlayerType.REMOTE_CLIENTVIEW;
                        Player newRemotePlayer = new Player(username, playerType, id, localSynchronizer);
                        playerSlotContainer.addItem(new PlayerSlot(newRemotePlayer,isHost));
                    }
                }
            }

            // Look for players that the host has dropped:
            List<PlayerSlot> playerSlotsToRemove = new LinkedList<>();
            synchronized (localSynchronizer){
                for(PlayerSlot playerSlot : playerSlotContainer.getContents()){
                    long id = playerSlot.getPlayer().getPlayerID();
                    if(receivedSynchronizer.get(id,"username")==null){
                        System.out.println("dropped player detected: " + playerSlot.getPlayer().getUsername().getData() + " id: " + playerSlot.getPlayer().getPlayerID());
                        // This player was dropped by the host, so let's drop them too:
                        if(playerSlot.getPlayer() == localPlayer){
                            // Uh-oh, that's us! Let's wait a few seconds, first; maybe we *just* connected and the host
                            // hasn't instantiated our player yet.
                            kickTimeout--;
                            if(kickTimeout<=0){
                                cleanUp();
                                SceneManager.switchToMainMenu();
                                showKickDialog();
                                break;
                            }
                        }
                        else{
                            playerSlotsToRemove.add(playerSlot);
                        }
                    }
                }
                for(PlayerSlot playerSlot : playerSlotsToRemove){
                    playerSlotContainer.removeItem(playerSlot);
                    connectionManager.getSynchronizer().deRegisterAllWithID(playerSlot.getPlayer().getPlayerID());
                }
            }

            // Synchronize our data with the host:
            localSynchronizer.synchronizeWith(receivedSynchronizer,isHost);

            // Prepare for the next iteration:
            receivedSynchronizer = connectionManager.retrievePacket();
        }
    }

    private void showHostNameDialog(){
        Alert hostNameDialog = new Alert(Alert.AlertType.CONFIRMATION);
        hostNameDialog.setTitle("Your host name and port number.");
        try{
        hostNameDialog.setHeaderText("Name of this computer: " + InetAddress.getLocalHost().getHostName() + "\n" +
                "Port you are using: " + ((HostConnectionManager)connectionManager).getPort());
    } catch(UnknownHostException e){
        hostNameDialog.setHeaderText("Port you are using: " + ((HostConnectionManager)connectionManager).getPort() + "\n" +
                "(Your computer name could not be determined...)");
    }
    javafx.scene.control.ButtonType returnBtn = new javafx.scene.control.ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        hostNameDialog.getButtonTypes().setAll(returnBtn);
        hostNameDialog.setGraphic(null);
        hostNameDialog.showAndWait();
}

    private Alert createKickDialog(){
        Alert kickAlert = new Alert(Alert.AlertType.CONFIRMATION);
        kickAlert.setTitle("You've been kicked");
        kickAlert.setHeaderText("The host has removed you from the game.");
        javafx.scene.control.ButtonType returnBtn = new javafx.scene.control.ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
        kickAlert.getButtonTypes().setAll(returnBtn);
        kickAlert.setGraphic(null);
        return kickAlert;
    }

    private void showKickDialog(){
        if(!kickAlert.isShowing()) kickAlert.show();
    }

    private void showGameCanceledDialog(){
        Alert cancelAlert = new Alert(Alert.AlertType.CONFIRMATION);
        cancelAlert.setTitle("Game Cancelled");
        cancelAlert.setHeaderText("The host is no longer connected. Game Cancelled.");
        javafx.scene.control.ButtonType returnBtn = new javafx.scene.control.ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
        cancelAlert.getButtonTypes().setAll(returnBtn);
        cancelAlert.setGraphic(null);
        cancelAlert.show();
    }

    private void prepareAndSendPacket(){
        connectionManager.send(connectionManager.getSynchronizer());
        connectionManager.getSynchronizer().clearSendOnceData();
    }

    public boolean isConnected(){
        return connectionManager.isConnected();
    }

    public void cleanUp(){
        if(isHost){
            gameCanceled.changeTo(true);
        }
        else{
            localPlayer.getPlayerStatus().changeTo(Player.PlayerStatus.DISCONNECTED);
        }
        prepareAndSendPacket();
        // wait a little bit to make sure the packet gets through:
        try{
            Thread.sleep(2000/FRAME_RATE);
        } catch (InterruptedException e){
            System.err.println("InterruptedException encountered while trying to leave the game. Other players" +
                    "might not be informed that you've left but... oh well, leaving anyways.");
            e.printStackTrace();
        }
        animationTimer.stop();
        connectionManager.cleanUp();
    }

    private void startGame(){
        List<Player> players = new LinkedList<>();
        for (PlayerSlot playerSlot: playerSlotContainer.getContents()){
            players.add(playerSlot.getPlayer());
        }
        connectionManager.getSynchronizer().deRegisterAllWithID(GAME_ID); // deletes any networked data that we no longer need, such as gameStarted.
        animationTimer.stop();
        SceneManager.startMultiplayerGame(isHost,connectionManager,players);
    }
}