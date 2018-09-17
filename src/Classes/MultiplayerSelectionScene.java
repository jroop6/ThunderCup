package Classes;

import Classes.Images.ButtonType;
import Classes.Animation.CharacterType;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static javafx.scene.layout.AnchorPane.setLeftAnchor;
import static javafx.scene.layout.AnchorPane.setRightAnchor;


/**
 * yo. Created by HydrusBeta on 7/26/2017.
 */
public class MultiplayerSelectionScene extends Scene {

    // misc variables accessed by different methods in this class:
    private ConnectionManager connectionManager;
    private LocalPlayer localPlayer;
    private GameData gameData;
    private ScrollableView<PlayerSlot> playerSlotContainer;
    private Scale scaler = new Scale();
    private ChatBox chatBox;
    private Alert kickAlert;
    private final int FRAME_RATE = 24;
    private long nextAnimationFrameInstance = 0; // Time at which all animations will be updated to the next frame (nanoseconds).
    boolean initializing = true;

    // Variables related to network communications:
    private boolean isHost; // Are we the host or a client?
    private long nextSendInstance = 0; // The time at which the next packet will be sent (nanoseconds).
    private final long maxConsecutivePacketsMissed; // If this many packets are missed consecutively from a particular player, alert the user.
    Map<Long,Integer> missedPacketsCount = new HashMap<>(); // maps playerIDs to the number of misssed packets for that player.
    private long nextLatencyTest = 0; // The time at which the next latency probe will be sent (nanoseconds).

    // for misc debugging:
    private long nextReport = 0; // The time at which miscellaneous debugging info will next be printed (nanoseconds).

    private AnimationTimer animationTimer;

    MultiplayerSelectionScene(boolean isHost, String username, String host, int port){
        super(new VBox());
        VBox rootNode = (VBox)getRoot();
        this.isHost = isHost;

        // set up connection framework:
        if(isHost) connectionManager = new HostConnectionManager(port);
        else connectionManager = new ClientConnectionManager(host, port);
        localPlayer = new LocalPlayer(username,isHost, connectionManager.getSynchronizer());
        connectionManager.setPlayerID(localPlayer.getPlayerID());

        maxConsecutivePacketsMissed = FRAME_RATE * 5;
        if (!connectionManager.isConnected()){
            SceneManager.switchToMainMenu();
            return;
        }

        gameData = new GameData(connectionManager.getSynchronizer());

        // Get a background for the PlayerSlot container:
        ImageView scrollableViewbackground = StaticBgImages.DAY_SKY.getImageView();

        // Create a ScrollableView and place PlayerSlots in it:
        playerSlotContainer = new ScrollableView<>(scrollableViewbackground,new Rectangle(0.0,0.0,Color.TRANSPARENT));
        rootNode.getChildren().add(playerSlotContainer);

        // If this is the host player, add his/her player to the playerSlotContainer, along with a button to add more players:
        if(isHost){
            PlayerSlot hostPlayerSlot = new PlayerSlot(localPlayer, true);
            playerSlotContainer.addItem(hostPlayerSlot);
            Button addPlayerBtn = new ThunderButton(ButtonType.ADD_PLAYER,(event)->{
                playerSlotContainer.addItem(new PlayerSlot(new UnclaimedPlayer(connectionManager.getSynchronizer()),isHost));
                ((HostConnectionManager)connectionManager).addOpenSlot();
            });
            Button addBotBtn = new ThunderButton(ButtonType.ADD_BOT, (event)->playerSlotContainer.addItem(new PlayerSlot(new BotPlayer(CharacterType.FILLY_BOT_MEDIUM, connectionManager.getSynchronizer()),true)));
            VBox addButtonHolder = new VBox();
            addButtonHolder.setAlignment(Pos.CENTER);
            addButtonHolder.getChildren().addAll(addPlayerBtn,addBotBtn);
            playerSlotContainer.addSpecialItem(addButtonHolder);
        }

        // There are one or more buttons directly beneath the PlayerSlots, which allow the player to return to the main
        // menu, ask for the computer's name and port (host only), and start the game (host only)
        AnchorPane buttonHolder = new AnchorPane();
        buttonHolder.setBackground(new Background(new BackgroundImage(StaticBgImages.MSS_BUTTONS_BACKDROP.getImageView().getImage(),null,null,null,null)));
        buttonHolder.setPickOnBounds(false);
        HBox rightSideButtonsHolder = new HBox();
        rightSideButtonsHolder.setPickOnBounds(false);
        ThunderButton start = new ThunderButton(ButtonType.START, (event)->{
            // Todo: check whether there are any unclaimed open spots before starting the game.
            System.out.println("pressed Start!");
            gameData.changeGameStarted(true);
            prepareAndSendServerPacket();
            // wait a little bit to make sure the packet gets through:
            try{
                Thread.sleep(2000/FRAME_RATE);
            } catch (InterruptedException e){
                System.err.println("InterruptedException encountered while trying to start the game. Other players" +
                        "might not be informed that the game has started but... oh well, starting anyways.");
                e.printStackTrace();
            }
            startGame();
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
        ImageView chatBoxBackground = StaticBgImages.CHATBOX_SCROLLPANE_BACKGROUND.getImageView();
        chatBox = new ChatBox(gameData, localPlayer, 200, true);
        rootNode.getChildren().add(chatBox);

        // Make everything scales correctly when the window is resized
        chatBoxBackground.getTransforms().add(scaler);
        returnToMainMenu.getTransforms().add(scaler);
        hostNameAndPort.getTransforms().add(scaler);
        start.getTransforms().add(scaler);
        rootNode.heightProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("New height of MultiplayerSelectionScene: " + newValue);
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
                    nextSendInstance = now;
                    nextReport = now;
                    initializing = false;
                }

                // Incoming packets are processed every frame, if there are any:
                if(isHost) processPacketsAsHost();
                else processPacketsAsClient();


                // Tasks that occur 24 times per second.
                if(now>nextAnimationFrameInstance){
                    nextAnimationFrameInstance += 1000000000L/ FRAME_RATE;

                    // copy newly-typed messages into gameData:
                    Message nextNewMessage;
                    while((nextNewMessage = chatBox.getNextNewMessageOut())!=null){
                        gameData.changeAddMessage(nextNewMessage);
                        if(isHost){ // The host prints his/her own messages immediately (clients only print incoming messages from the host).
                            chatBox.addNewMessageIn(new Message(nextNewMessage));
                        }
                    }

                    // Display new-received messages in the chat box:
                    chatBox.displayNewMessagesIn();

                    // update character animations:
                    for (PlayerSlot playerSlot : playerSlotContainer.getContents()){
                        playerSlot.tick(0);
                        playerSlot.repaint(isHost);
                    }
                }

                // Latency probes are sent by the host 3 times per second:
                if(isHost){
                    if(now>nextLatencyTest) {
                        nextLatencyTest += 1000000000L/connectionManager.getLatencyTestsPerSecond();
                        System.out.println("probing latencies");
                        connectionManager.send(new LatencyPacket());
                    }
                }

                // Outgoing Packets are transmitted 24 times per second. The game also deals with dropped players at this
                // time and displays messages to the chat box:
                if(now>nextSendInstance){
                    nextSendInstance += 1000000000L/FRAME_RATE;

                    // update the chatBox:
                    chatBox.displayNewMessagesIn();

                    if(isHost){
                        prepareAndSendServerPacket();
                        deleteRemovedPlayers();
                    }
                    else{
                        prepareAndSendClientPacket();
                    }
                    resetFlags(); // reset the local change flags so that they are only broadcast once:
                    connectionManager.getSynchronizer().resetChangedData();
                    checkForDisconnectedPlayers();
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

    private void resetFlags(){
        gameData.resetFlags();
        gameData.resetMessages();
        // The host needs to reset the flags of all players, but clients only need to reset their own:
        if(isHost){
            for(PlayerSlot playerSlot: playerSlotContainer.getContents()){
                playerSlot.getPlayerData().resetFlags();
            }
        }
        else localPlayer.resetFlags();
    }

    // for debugging
    // recall: {number of packets processed, number remaining, 0=host 1=client}
    public void report(){
        Map<Long,Long> latencies = connectionManager.getLatencies();
        System.out.print("Latencies: ");
        for(PlayerSlot playerSlot : playerSlotContainer.getContents()){
            PlayerData playerData = playerSlot.getPlayerData();
            if(latencies.containsKey(playerData.getPlayerID())){
                System.out.print(playerData.getUsername() + ": " + latencies.get(playerData.getPlayerID()) + " // ");
            }
        }
        System.out.print("\n");
    }

    private void processPacketsAsHost(){
        List<PlayerSlot> playerSlots = playerSlotContainer.getContents();
        Packet packet = connectionManager.retrievePacket();

        // Process Packets one at a time:
        while(packet!=null){
            // First process the PlayerData
            PlayerData playerData = packet.popPlayerData();
            PlayerSlot playerSlot = getPlayerSlotByID(playerData.getPlayerID(),playerSlots);
            if(playerSlot==null){
                // A new player has connected. Place him/her in an open slot. If there is no open slot, then ignore the
                // packet (this can happen if the player was just kicked by the host and the client doesn't realize it
                // yet and is still sending packets to the host):

                // note: this doesn't work. you must call changePlayer() or else your datastructures will lose track of this playerData.
                /*for(int i=0; i<playerSlots.size(); i++){
                    if(playerSlots.get(i).getPlayerData().getPlayerID()==-1){
                        playerSlots.remove(i);
                        playerSlots.add(i,new PlayerSlot(new RemotePlayer(playerData.getUsername(), playerData.getPlayerID(), connectionManager.getSynchronizer()), isHost));
                    }
                }*/

                PlayerSlot openPlayerSlot = getPlayerSlotByID(-1,playerSlots);
                if(openPlayerSlot!=null){
                    openPlayerSlot.changePlayer(playerData,isHost);
                    missedPacketsCount.put(playerData.getPlayerID(),0);
                    System.out.println("new player added to scene!");
                }
            }
            else{
                // The player already exists, so update his/her data. This is the most common case:
                playerSlot.getPlayerData().updateWithChangers(playerData, connectionManager.getLatencies());
                missedPacketsCount.replace(playerData.getPlayerID(),0); // reset the missed packets counter
            }

            // Then process the GameData:
            GameData clientGameData = packet.getGameData();
            if(clientGameData.isMessagesChanged()){
                List<Message> messages = clientGameData.getMessages();
                gameData.changeAddMessages(new LinkedList<>(messages)); // host re-broadcasts the messages out to all clients
                chatBox.addNewMessagesIn(new LinkedList<>(messages)); // the messages in this list will be displayed on the screen later this frame
            }

            connectionManager.getSynchronizer().synchronizeWith(packet.getSynchronizer(),isHost);
            // Prepare for the next iteration:
            packet = connectionManager.retrievePacket();
        }
    }

    private PlayerSlot getPlayerSlotByID(long playerID, List<PlayerSlot> playerSlots){
        for (PlayerSlot playerSlot: playerSlots) {
            if (playerSlot.getPlayerData().getPlayerID()==playerID) return playerSlot;
        }
        return null; // return null to indicate that the PlayerSlot was not found
    }

    private void prepareAndSendServerPacket(){
        // Go grab the latency data:
        Map<Long, Long> latencies = connectionManager.getLatencies();

        // We must create a copy of the local PlayerData and GameData and add those copies to the packet. This is to
        // prevent the change flags within the local PlayerData and GameData from being reset before the packet is sent.
        PlayerData localPlayerData = new PlayerData(localPlayer);
        GameData localGameData = new GameData(gameData);
        if(latencies.containsKey(localPlayerData.getPlayerID())){
            localPlayerData.changeLatency(latencies.get(localPlayerData.getPlayerID()));
        }
        Packet outPacket = new Packet(localPlayerData, localGameData, connectionManager.getSynchronizer());

        List<PlayerSlot> playerSlots = playerSlotContainer.getContents();
        for (PlayerSlot playerSlot: playerSlots) {
            if(playerSlot.getPlayerData().getPlayerID()==0) continue; // we've already added the localPlayer's PlayerData to the packet.
            PlayerData playerData = new PlayerData(playerSlot.getPlayerData()); // Yes, these need to be *new* PlayerData instances because there may be locally-stored bot data. see note regarding local player data, above.
            if(latencies.containsKey(playerData.getPlayerID())){
                playerData.changeLatency(latencies.get(playerData.getPlayerID()));
            }
            outPacket.addPlayerData(playerData);
        }

        connectionManager.send(outPacket);
    }

    private void checkForDisconnectedPlayers(){
        List<PlayerSlot> playerSlots = playerSlotContainer.getContents();
        for (PlayerSlot playerSlot: playerSlots) {
            PlayerData playerData = playerSlot.getPlayerData();
            if(!(playerSlot.getPlayerData() instanceof RemotePlayer)) continue; // Only RemotePlayers are capable of having connection issues, so only check them.
            if(!isHost && playerData.getPlayerID()!=0) continue; // Clients only keep track of the host's connection to them.
            Integer missedPackets = missedPacketsCount.get(playerData.getPlayerID());

            // increment the missed packets count. The count will be reset whenever the next packet is received:
            missedPacketsCount.replace(playerData.getPlayerID(),missedPacketsCount.get(playerData.getPlayerID())+1);

            // If the player has been gone for too long, ask the host what to do:
            if(missedPackets==maxConsecutivePacketsMissed){
                showConnectionLostDialog(playerSlot.getPlayerData());
            }
        }
    }

    // Todo: make this dialog automatically disappear if the player reconnects
    private void showConnectionLostDialog(PlayerData playerdata){
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
        Label dialogLabel = new Label("Communication with player " + playerdata.getUsername() + " seems to have been lost.\n What would you like to do?");
        if(playerdata.getPlayerID()==0){
            dialogLabel.setText("Communication with the host seems to have been lost. What would you like to do?");
        }
        dialogRoot.getChildren().add(dialogLabel);

        // Add buttons for the user's decision:
        Button drop = new Button("Drop Player");
        if(playerdata.getPlayerID()==0){
            drop.setText("Exit game");
        }
        Button wait = new Button("wait 10 seconds and see if they reconnect.");
        HBox buttonContainer = new HBox();
        buttonContainer.setSpacing(10.0);
        buttonContainer.getChildren().addAll(drop,wait);
        dialogRoot.getChildren().add(buttonContainer);

        // Add functionality to the buttons:
        drop.setOnAction((event -> {
            if(playerdata.getPlayerID()==0){ // The player who disconnected was the host, so close the game.
                cleanUp();
                SceneManager.switchToMainMenu();
                showGameCanceledDialog();
            }
            else{
                playerdata.changeResignPlayer();
            }
            dialogStage.close();
        }));

        wait.setOnAction((event) -> {
            missedPacketsCount.replace(playerdata.getPlayerID(),(int)(maxConsecutivePacketsMissed - FRAME_RATE*10));
            dialogStage.close();
        });

        dialogStage.show();
    }

    // Cycle through all players in the playerSlotContainer and delete any that have the defeated flag set:
    private void deleteRemovedPlayers(){
        List<PlayerSlot> playerSlots = playerSlotContainer.getContents();
        for (PlayerSlot playerSlot: playerSlots){
            if(playerSlot.getPlayerData().isDefeatedChanged()){
                if (playerSlot.getPlayerData().getPlayerID() == -1){
                    // An open slot is being removed, so decrement the open slot counter:
                    ((HostConnectionManager)connectionManager).removeOpenSlot();
                }
                else{
                    // An actual player is being removed, so remove their missed packets count from the record:
                    missedPacketsCount.remove(playerSlot.getPlayerData().getPlayerID());
                }
                System.out.println("INSIDE HERE!");
                playerSlotContainer.removeItem(playerSlot);
            }
        }
    }

    private void processPacketsAsClient(){
        // Process Packets, one at a time:
        Packet packet = connectionManager.retrievePacket();
        while(packet!=null){
            int currentPlayerSlotIndex = 0;
            PlayerSlot currentPlayerSlot;
            List<PlayerSlot> playerSlots;
            long currentPlayerID;

            // Within each Packet, process PlayerData one at a time in order:
            PlayerData serverPlayerData = packet.popPlayerData();
            do{
                playerSlots = playerSlotContainer.getContents();
                if(playerSlots.size()<currentPlayerSlotIndex+1){
                    // We have more playerDatas than PlayerSlots, so a new PlayerSlot needs to be added:
                    PlayerSlot newPlayerSlot = makeRemoteOrLocalPlayerSlot(serverPlayerData);
                    playerSlotContainer.addItem(newPlayerSlot);
                    if(serverPlayerData.getPlayerID()==0) missedPacketsCount.put(serverPlayerData.getPlayerID(),0);
                }
                else{
                    // check to see whether the playerID from the server playerData matches the playerID for this slot:
                    currentPlayerSlot = playerSlots.get(currentPlayerSlotIndex);
                    currentPlayerID = currentPlayerSlot.getPlayerData().getPlayerID();
                    if(serverPlayerData.getPlayerID() == currentPlayerID){
                        // It matches, so simply update this player with the new playerData (this is the most common case) :
                        currentPlayerSlot.getPlayerData().updateWithSetters(serverPlayerData, currentPlayerSlot.getPlayerData() instanceof LocalPlayer);
                        if(serverPlayerData.getPlayerID()==0) missedPacketsCount.replace(serverPlayerData.getPlayerID(),0);
                    }
                    else{
                        // It doesn't match. Find out whether this playerID already exists further down the playerSlotContainer:
                        PlayerSlot oldSlot = getPlayerSlotByID(serverPlayerData.getPlayerID(),playerSlots);
                        if(oldSlot == null){
                            // The player doesn't exist yet, so add him/her here. Add the player as a LocalPlayer instance if the ID matches the one for localPlayer:
                            PlayerSlot newPlayerSlot = makeRemoteOrLocalPlayerSlot(serverPlayerData);
                            playerSlotContainer.addItem(currentPlayerSlotIndex, newPlayerSlot);
                            if(serverPlayerData.getPlayerID()==0) missedPacketsCount.put(serverPlayerData.getPlayerID(),0);
                        }
                        else{
                            // The player does exist and is in the wrong spot. Relocate him/her here and update:
                            playerSlotContainer.removeItem(oldSlot);
                            playerSlotContainer.addItem(currentPlayerSlotIndex,oldSlot);
                            currentPlayerSlot.getPlayerData().updateWithSetters(serverPlayerData, currentPlayerSlot.getPlayerData() instanceof LocalPlayer);
                            if(serverPlayerData.getPlayerID()==0) missedPacketsCount.replace(serverPlayerData.getPlayerID(),0);
                        }
                    }
                }

                // Prepare for next iteration:
                serverPlayerData = packet.popPlayerData();
                ++currentPlayerSlotIndex;
            } while(serverPlayerData!=null);

            // At this point, if there are still more slots in the playerSlotContainer, destroy them. Those players must
            // been removed by the host.
            playerSlots = playerSlotContainer.getContents();
            while (playerSlots.size()>currentPlayerSlotIndex){
                currentPlayerSlot = playerSlots.get(currentPlayerSlotIndex);
                if(currentPlayerSlot.getPlayerData().getPlayerID() == localPlayer.getPlayerID()){
                    // Uh-oh, that's us! Well, it seems that host has kicked us from the game, so return to the main menu:
                    // playerSlotContainer.removeItem(currentPlayerSlot);
                    cleanUp();
                    SceneManager.switchToMainMenu();
                    showKickDialog();
                    break;
                }
                else{
                    playerSlotContainer.removeItem(currentPlayerSlot);
                    if(currentPlayerSlot.getPlayerData().getPlayerID()==0) missedPacketsCount.remove(serverPlayerData.getPlayerID());
                }
                playerSlots = playerSlotContainer.getContents();
            }

            // Now process the GameData:
            GameData hostGameData = packet.getGameData();
            chatBox.displayNewMessagesIn();
            if(hostGameData.isCancelGameRequested()){
                cleanUp();
                SceneManager.switchToMainMenu();
                showGameCanceledDialog();
            }
            if(hostGameData.isGameStartedRequested()){
                System.out.println("host has started the game!");
                startGame();
            }
            if(hostGameData.isMessagesChanged()){
                List<Message> messages = hostGameData.getMessages();
                chatBox.addNewMessagesIn(new LinkedList<>(messages)); // the messages in this list will be displayed on the screen later this frame
            }


            connectionManager.getSynchronizer().synchronizeWith(packet.getSynchronizer(),isHost);

            // Prepare for next iteration:
            packet = connectionManager.retrievePacket();
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

    private PlayerSlot makeRemoteOrLocalPlayerSlot(PlayerData serverPlayerData){
        PlayerSlot newPlayerSlot;
        if(serverPlayerData.getPlayerID()==localPlayer.getPlayerID()){
            newPlayerSlot = new PlayerSlot(localPlayer,isHost);
        }
        else{
            newPlayerSlot = new PlayerSlot(serverPlayerData,isHost);
        }
        return newPlayerSlot;
    }

    private void prepareAndSendClientPacket(){
        // The network must create a copy of the local PlayerData and GameData now and send those copies. This is to
        // prevent the change flags within the PlayerData and GameData from being reset before the packet is sent.
        PlayerData localPlayerData = new PlayerData(localPlayer);
        GameData localGameData = new GameData(gameData);

        Packet outPacket = new Packet(localPlayerData, localGameData, connectionManager.getSynchronizer());
        connectionManager.send(outPacket);
    }

    public boolean isConnected(){
        return connectionManager.isConnected();
    }

    public void cleanUp(){
        if(isHost){
            gameData.changeCancelGame(true);
            prepareAndSendServerPacket();
        }
        else{
            localPlayer.changeResignPlayer();
            prepareAndSendClientPacket();
        }
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
        List<PlayerData> players = new LinkedList<>();
        for (PlayerSlot playerSlot: playerSlotContainer.getContents()){
            players.add(playerSlot.getPlayerData());
        }
        SceneManager.startMultiplayerGame(isHost,connectionManager,players);
        animationTimer.stop();
    }
}