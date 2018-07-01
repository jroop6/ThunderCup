package Classes;

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
    private GameData gameData = new GameData();
    private ScrollableView<PlayerSlot> playerSlotContainer;
    private Scale scaler = new Scale();
    private ChatBox chatBox;
    private Alert kickAlert;
    private final int ANIMATION_FRAME_RATE = 24;
    private long nextAnimationFrameInstance = 0; // Time at which all animations will be updated to the next frame (nanoseconds).
    boolean initializing = true;

    // Variables related to network communications:
    private boolean isHost; // Are we the host or a client?
    private long nextSendInstance = 0; // The time at which the next packet will be sent (nanoseconds).
    private final long maxConsecutivePacketsMissed; // If this many packets are missed consecutively from a particular player, alert the user.
    Map<Long,Integer> missedPacketsCount = new HashMap<>(); // maps playerIDs to the number of misssed packets for that player.
    private long nextLatencyTest = 0; // The time at which the next latency probe will be sent (nanoseconds).

    // for misc debugging:
    private int numberOfPacketsProcessed = 0;
    private int numberOfPacketsRemaining = 0;
    private int MSSCalls = 0;
    private long nextReport = 0; // The time at which miscellaneous debugging info will next be printed (nanoseconds).

    private AnimationTimer animationTimer;

    MultiplayerSelectionScene(boolean isHost, String username, String host, int port){
        super(new VBox());
        VBox rootNode = (VBox)getRoot();
        localPlayer = new LocalPlayer(username,isHost);
        this.isHost = isHost;

        // set up connection framework:
        if(isHost) connectionManager = new HostConnectionManager(port, localPlayer.getPlayerData().getPlayerID());
        else connectionManager = new ClientConnectionManager(host, port, localPlayer.getPlayerData().getPlayerID());
        maxConsecutivePacketsMissed = connectionManager.getPacketsSentPerSecond() * 5;
        if (!connectionManager.isConnected()){
            SceneManager.switchToMainMenu();
            return;
        }

        // Get a background for the PlayerSlot container:
        ImageView scrollableViewbackground = StaticBgImages.DAY_SKY.getImageView();

        // Create a ScrollableView and place PlayerSlots in it:
        playerSlotContainer = new ScrollableView<>(scrollableViewbackground,new Rectangle(0.0,0.0,Color.TRANSPARENT));
        rootNode.getChildren().add(playerSlotContainer);

        // If this is the host player, add his/her player to the playerSlotContainer, along with a button to add more players:
        if(isHost){
            PlayerSlot hostPlayerSlot = new PlayerSlot(localPlayer, true);
            playerSlotContainer.addItem(hostPlayerSlot);
            Button addPlayerBtn = createButton(ButtonImages.ADD_PLAYER);
            Button addBotBtn = createButton(ButtonImages.ADD_BOT);
            VBox addButtonHolder = new VBox();
            addButtonHolder.setAlignment(Pos.CENTER);
            addButtonHolder.getChildren().addAll(addPlayerBtn,addBotBtn);
            playerSlotContainer.addSpecialItem(addButtonHolder);
        }

        // There are one or more buttons directly beneath the PlayerSlots, which allow the player to return to the main
        // menu, ask for the computer's name and port (host only), and start the game (host only)
        AnchorPane buttonHolder = new AnchorPane();
        buttonHolder.setBackground(new Background(new BackgroundImage(StaticBgImages.MSS_BUTTONS_BACKGROUND.getImageView().getImage(),null,null,null,null)));
        buttonHolder.setPickOnBounds(false);
        HBox rightSideButtonsHolder = new HBox();
        rightSideButtonsHolder.setPickOnBounds(false);
        Button start = createButton(ButtonImages.START);
        start.setScaleX(-1.0);
        Button hostNameAndPort = createButton(ButtonImages.HOST_AND_PORT);
        hostNameAndPort.setScaleX(-1.0);
        if(isHost) rightSideButtonsHolder.getChildren().addAll(hostNameAndPort, start);
        Button returnToMainMenu = createButton(ButtonImages.RETURN_TO_MAIN_MENU_CLIENT);
        if(isHost) returnToMainMenu = createButton(ButtonImages.RETURN_TO_MAIN_MENU_HOST); // The host gets a special, cancel game button.
        returnToMainMenu.setPadding(Insets.EMPTY);
        buttonHolder.getChildren().addAll(returnToMainMenu,rightSideButtonsHolder);
        setLeftAnchor(returnToMainMenu, 0.0);
        setRightAnchor(rightSideButtonsHolder, 0.0);
        buttonHolder.setBorder(new Border(new BorderStroke(Color.BLACK,BorderStrokeStyle.SOLID,new CornerRadii(0.0),new BorderWidths(2.0))));
        rootNode.getChildren().add(buttonHolder);

        // A ChatBox is located under the Buttons:
        ImageView chatBoxBackground = StaticBgImages.CHATBOX_SCROLLPANE_BACKGROUND.getImageView();
        chatBox = new ChatBox(gameData, localPlayer.getPlayerData(), 200, true);
        rootNode.getChildren().add(chatBox);

        // Make everything scale correctly when the window is resized
        chatBoxBackground.getTransforms().add(scaler);
        returnToMainMenu.getTransforms().add(scaler);
        hostNameAndPort.getTransforms().add(scaler);
        start.getTransforms().add(scaler);
        rootNode.heightProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("New height of MultiplayerSelectionScene: " + newValue);
            double scaleValue = (double)newValue/1080.0;
            scaler.setX(scaleValue);
            scaler.setY(scaleValue);
            // The buttonHolder's minHeight property need to be specially set, because scaling works weird with Buttons
            // (although the Buttons are visually scaled, the amount of space they take up stays the same. Setting the
            // minHeightProperty here remedies that problem).
            //Todo: This is buggy right now and I have no idea why.
            //buttonHolder.setMinHeight(hostNameAndPort.getHeight()*scaleValue);
        });
        // Alternative to the code that's inside the ChangeListener. This is also buggy.
        // buttonHolder.minHeightProperty().bind(Bindings.multiply(hostNameAndPort.heightProperty(),scaler.yProperty()));

        // Create the kick dialog in case the player gets kicked (but don't show it yet, of course):
        createKickDialog();

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

                int[] packetsProcessingInfo;

                // Incoming packets are processed every frame, if there are any:
                if(isHost) packetsProcessingInfo = processPacketsAsHost();
                else packetsProcessingInfo = processPacketsAsClient();
                numberOfPacketsProcessed += packetsProcessingInfo[0];
                numberOfPacketsRemaining += packetsProcessingInfo[1];
                ++MSSCalls;

                // Character and Bubble animations are updated 24 times per second:
                if(now>nextAnimationFrameInstance){
                    nextAnimationFrameInstance += 1000000000L/ANIMATION_FRAME_RATE;
                }

                // Latency probes are sent by the host 3 times per second:
                if(isHost){
                    if(now>nextLatencyTest) {
                        nextLatencyTest += 1000000000L/connectionManager.getLatencyTestsPerSecond();
                        System.out.println("probing latencies");
                        connectionManager.send(new LatencyPacket());
                    }
                }

                // Outgoing Packets are transmitted 10 times per second. The game also deals with dropped players at this
                // time and displays messages to the chat box:
                if(now>nextSendInstance){
                    nextSendInstance += 1000000000L/connectionManager.getPacketsSentPerSecond();

                    // update the chatBox:
                    if(gameData.isMessageRequested()) chatBox.addMessages(gameData.getMessages());

                    if(isHost){
                        prepareAndSendServerPacket(packetsProcessingInfo);
                        deleteRemovedPlayers();
                        resetFlags(); // reset the local change flags so that they are only broadcast once:
                    }
                    else{
                        prepareAndSendClientPacket(packetsProcessingInfo);
                        resetFlags(); // reset the local change flags so that they are only sent to the server once:
                    }
                    checkForDisconnectedPlayers();
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

    private void resetFlags(){
        gameData.resetFlags();
        gameData.resetMessages();
        // The host needs to reset the flags of all players, but clients only need to reset their own:
        if(isHost){
            for(PlayerSlot playerSlot: playerSlotContainer.getContents()){
                playerSlot.getPlayer().getPlayerData().resetFlags();
            }
        }
        else localPlayer.getPlayerData().resetFlags();
    }

    // for debugging
    // recall: {number of packets processed, number remaining, 0=host 1=client}
    public void report(int[] packetsProcessingInfo){
        System.out.println((packetsProcessingInfo[2]==0?"Host:":"Client:") + " Packets processed: "
             + numberOfPacketsProcessed + " Number of packets remaining: " + numberOfPacketsRemaining + " Timer called " + MSSCalls + " times");
        Map<Long,Long> latencies = connectionManager.getLatencies();
        System.out.print("Latencies: ");
        for(PlayerSlot playerSlot : playerSlotContainer.getContents()){
            PlayerData playerData = playerSlot.getPlayer().getPlayerData();
            if(latencies.containsKey(playerData.getPlayerID())){
                System.out.print(playerData.getUsername() + ": " + latencies.get(playerData.getPlayerID()) + " // ");
            }
        }
        System.out.print("\n");
        numberOfPacketsProcessed = 0;
        numberOfPacketsRemaining = 0;
        MSSCalls = 0;
    }

    //ToDo: check for player disconnection. Maybe have a counter that increments any time a packet is NOT received from a
    //TODO: player. If the counter goes above a prescribed amount, then resign that player (or ask the host what to do?)
    public int[] processPacketsAsHost(){
        int[] packetsProcessingInfo = {0,0,0}; // {number of packets processed, number remaining, 0=host 1=client}.
        int packetsProcessed = 0;
        List<PlayerSlot> playerSlots = playerSlotContainer.getContents();
        int numPlayers = playerSlots.size();
        Packet packet = connectionManager.retrievePacket(packetsProcessingInfo);

        // Process Packets one at a time:
        while(packet!=null && packetsProcessed<connectionManager.MAX_PACKETS_PER_PLAYER*numPlayers){

            // First process the PlayerData
            PlayerData playerData = packet.popPlayerData();
            PlayerSlot playerSlot = getPlayerSlotByID(playerData.getPlayerID(),playerSlots);
            if(playerSlot==null){
                // A new player has connected. Place him/her in an open slot. If there is no open slot, then ignore the
                // packet (this can happen if the player was just kicked by the host and the client doesn't realize it
                // yet and is still sending packets to the host):
                PlayerSlot openPlayerSlot = getPlayerSlotByID(-1,playerSlots);
                if(openPlayerSlot!=null){
                    openPlayerSlot.changePlayer(playerData,isHost);
                    missedPacketsCount.put(playerData.getPlayerID(),0);
                    System.out.println("new player added to scene!");
                }
            }
            else{
                // The player already exists, so update his/her data. This is the most common case:
                playerSlot.getPlayer().updateWithChangers(playerData, connectionManager.getLatencies());
                missedPacketsCount.replace(playerData.getPlayerID(),0); // reset the missed packets counter
            }

            // Then process the GameData:
            GameData clientGameData = packet.getGameData();
            if(clientGameData.isFrameRateRequested()){
                gameData.changeFrameRate(clientGameData.getFrameRate());
            }
            if(clientGameData.isMessageRequested()){
                gameData.changeAddMessages(clientGameData.getMessages());
            }

            // Prepare for the next iteration:
            ++packetsProcessed;
            packet = connectionManager.retrievePacket(packetsProcessingInfo);
        }

        // For debugging:
        packetsProcessingInfo[0] = packetsProcessed;
        return packetsProcessingInfo;
    }

    private PlayerSlot getPlayerSlotByID(long playerID, List<PlayerSlot> playerSlots){
        for (PlayerSlot playerSlot: playerSlots) {
            if (playerSlot.getPlayer().getPlayerData().getPlayerID()==playerID) return playerSlot;
        }
        return null; // return null to indicate that the PlayerSlot was not found
    }

    private void prepareAndSendServerPacket(int[] packetsProcessingInfo){
        // Go grab the latency data:
        Map<Long, Long> latencies = connectionManager.getLatencies();

        // We must create a copy of the local PlayerData and GameData and add those copies to the packet. This is to
        // prevent the change flags within the local PlayerData and GameData from being reset before the packet is sent.
        PlayerData localPlayerData = new PlayerData(localPlayer.getPlayerData());
        GameData localGameData = new GameData(gameData);
        if(latencies.containsKey(localPlayerData.getPlayerID())){
            localPlayerData.changeLatency(latencies.get(localPlayerData.getPlayerID()));
        }
        Packet outPacket = new Packet(localPlayerData, localGameData);

        List<PlayerSlot> playerSlots = playerSlotContainer.getContents();
        for (PlayerSlot playerSlot: playerSlots) {
            if(playerSlot.getPlayer().getPlayerData().getPlayerID()==0) continue; // we've already added the localPlayer's PlayerData to the packet.
            PlayerData playerData = new PlayerData(playerSlot.getPlayer().getPlayerData()); // Yes, these need to be *new* PlayerData instances because there may be locally-stored bot data. see note regarding local player data, above.
            if(latencies.containsKey(playerData.getPlayerID())){
                playerData.changeLatency(latencies.get(playerData.getPlayerID()));
            }
            outPacket.addPlayerData(playerData);
        }

        if(packetsProcessingInfo[1]>0){
            // ToDo: Host may be lagging. Slow down the game if necessary.
        }
        connectionManager.send(outPacket);
    }

    private void checkForDisconnectedPlayers(){
        List<PlayerSlot> playerSlots = playerSlotContainer.getContents();
        for (PlayerSlot playerSlot: playerSlots) {
            PlayerData playerData = playerSlot.getPlayer().getPlayerData();
            if(!(playerSlot.getPlayer() instanceof RemotePlayer)) continue; // Only RemotePlayers are capable of having connection issues, so only check them.
            if(!isHost && playerData.getPlayerID()!=0) continue; // Clients only keep track of the host's connection to them.
            Integer missedPackets = missedPacketsCount.get(playerData.getPlayerID());

            // increment the missed packets count. The count will be reset whenever the next packet is received:
            missedPacketsCount.replace(playerData.getPlayerID(),missedPacketsCount.get(playerData.getPlayerID())+1);

            // If the player has been gone for too long, ask the host what to do:
            if(missedPackets==maxConsecutivePacketsMissed){
                showConnectionLostDialog(playerSlot.getPlayer());
            }
        }
    }

    // Todo: make this dialog automatically disappear if the player reconnects
    private void showConnectionLostDialog(Player player){
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
        if(player.getPlayerData().getPlayerID()==0){
            dialogLabel.setText("Communication with the host seems to have been lost. What would you like to do?");
        }
        dialogRoot.getChildren().add(dialogLabel);

        // Add buttons for the user's decision:
        Button drop = new Button("Drop Player");
        if(player.getPlayerData().getPlayerID()==0){
            drop.setText("Exit game");
        }
        Button wait = new Button("wait 10 seconds and see if they reconnect.");
        HBox buttonContainer = new HBox();
        buttonContainer.setSpacing(10.0);
        buttonContainer.getChildren().addAll(drop,wait);
        dialogRoot.getChildren().add(buttonContainer);

        // Add functionality to the buttons:
        drop.setOnAction((event -> {
            if(player.getPlayerData().getPlayerID()==0){ // The player who disconnected was the host, so close the game.
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

    // Cycle through all players in the playerSlotContainer and delete any that have the defeated flag set:
    private void deleteRemovedPlayers(){
        List<PlayerSlot> playerSlots = playerSlotContainer.getContents();
        for (PlayerSlot playerSlot: playerSlots){
            if(playerSlot.getPlayer().getPlayerData().isDefeatedChanged()){
                if (playerSlot.getPlayer().getPlayerData().getPlayerID() == -1){
                    // An open slot is being removed, so decrement the open slot counter:
                    ((HostConnectionManager)connectionManager).removeOpenSlot();
                }
                else{
                    // An actual player is being removed, so remove their missed packets count from the record:
                    missedPacketsCount.remove(playerSlot.getPlayer().getPlayerData().getPlayerID());
                }
                System.out.println("INSIDE HERE!");
                playerSlotContainer.removeItem(playerSlot);
            }
        }
    }

    private int[] processPacketsAsClient(){
        int packetsProcessed = 0;
        int[] packetsProcessingInfo = {0,0,1};

        // Process Packets, one at a time:
        Packet packet = connectionManager.retrievePacket(packetsProcessingInfo);
        while(packet!=null && packetsProcessed<connectionManager.MAX_PACKETS_PER_PLAYER){
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
                    currentPlayerID = currentPlayerSlot.getPlayer().getPlayerData().getPlayerID();
                    if(serverPlayerData.getPlayerID() == currentPlayerID){
                        // It matches, so simply update this player with the new playerData (this is the most common case) :
                        currentPlayerSlot.getPlayer().updateWithSetters(serverPlayerData, currentPlayerSlot.getPlayer() instanceof LocalPlayer);
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
                            currentPlayerSlot.getPlayer().updateWithSetters(serverPlayerData, currentPlayerSlot.getPlayer() instanceof LocalPlayer);
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
                if(currentPlayerSlot.getPlayer().getPlayerData().getPlayerID() == localPlayer.getPlayerData().getPlayerID()){
                    // Uh-oh, that's us! Well, it seems that host has kicked us from the game, so return to the main menu:
                    // playerSlotContainer.removeItem(currentPlayerSlot);
                    cleanUp();
                    SceneManager.switchToMainMenu();
                    showKickDialog();
                    break;
                }
                else{
                    playerSlotContainer.removeItem(currentPlayerSlot);
                    if(currentPlayerSlot.getPlayer().getPlayerData().getPlayerID()==0) missedPacketsCount.remove(serverPlayerData.getPlayerID());
                }
                playerSlots = playerSlotContainer.getContents();
            }

            // Now process the GameData:
            GameData hostGameData = packet.getGameData();
            if(hostGameData.isFrameRateRequested()){
                gameData.setFrameRate(hostGameData.getFrameRate());
            }
            if(hostGameData.isMessageRequested()){
                chatBox.addMessages(hostGameData.getMessages());
            }
            if(hostGameData.isCancelGameRequested()){
                cleanUp();
                SceneManager.switchToMainMenu();
                showGameCanceledDialog();
            }
            if(hostGameData.isGameStartedRequested()){
                System.out.println("host has started the game!");
                startGame();
            }

            // Prepare for next iteration:
            ++packetsProcessed;
            packet = connectionManager.retrievePacket(packetsProcessingInfo);
        }

        // For debugging:
        packetsProcessingInfo[0] = packetsProcessed;
        return packetsProcessingInfo;
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
    ButtonType returnBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        hostNameDialog.getButtonTypes().setAll(returnBtn);
        hostNameDialog.setGraphic(null);
        hostNameDialog.showAndWait();
}

    private void createKickDialog(){
        kickAlert = new Alert(Alert.AlertType.CONFIRMATION);
        kickAlert.setTitle("You've been kicked");
        kickAlert.setHeaderText("The host has removed you from the game.");
        ButtonType returnBtn = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
        kickAlert.getButtonTypes().setAll(returnBtn);
        kickAlert.setGraphic(null);
    }

    private void showKickDialog(){
        if(!kickAlert.isShowing()) kickAlert.show();
    }

    private void showGameCanceledDialog(){
        Alert cancelAlert = new Alert(Alert.AlertType.CONFIRMATION);
        cancelAlert.setTitle("Game Cancelled");
        cancelAlert.setHeaderText("The host is no longer connected. Game Cancelled.");
        ButtonType returnBtn = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
        cancelAlert.getButtonTypes().setAll(returnBtn);
        cancelAlert.setGraphic(null);
        cancelAlert.show();
    }

    private PlayerSlot makeRemoteOrLocalPlayerSlot(PlayerData serverPlayerData){
        PlayerSlot newPlayerSlot;
        if(serverPlayerData.getPlayerID()==localPlayer.getPlayerData().getPlayerID()){
            newPlayerSlot = new PlayerSlot(localPlayer,isHost);
        }
        else{
            newPlayerSlot = new PlayerSlot(new RemotePlayer(serverPlayerData),isHost);
        }
        return newPlayerSlot;
    }

    private void prepareAndSendClientPacket(int[] packetsProcessingInfo){
        // The network must create a copy of the local PlayerData and GameData now and send those copies. This is to
        // prevent the change flags within the PlayerData and GameData from being reset before the packet is sent.
        PlayerData localPlayerData = new PlayerData(localPlayer.getPlayerData());
        GameData localGameData = new GameData(gameData);

        Packet outPacket = new Packet(localPlayerData, localGameData);
        if(packetsProcessingInfo[1]>0){
            // ToDo: Client may be lagging. Request slowdown if necessary.
        }
        connectionManager.send(outPacket);
    }

    public boolean isConnected(){
        return connectionManager.isConnected();
    }

    private Button createButton(ButtonImages buttonEnum){
        Button btn = new Button();
        ImageView unselectedImage = buttonEnum.getUnselectedImage();
        ImageView selectedImage = buttonEnum.getSelectedImage();
        /*btn.setBackground(new Background(new BackgroundImage(unselectedImage.getImage(),BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT,BackgroundPosition.CENTER,BackgroundSize.DEFAULT)));
        btn.setPrefWidth(unselectedImage.getImage().getWidth());
        btn.setPrefWidth(unselectedImage.getImage().getWidth());
        btn.setPrefHeight(unselectedImage.getImage().getHeight());
        btn.setPrefHeight(unselectedImage.getImage().getHeight());*/
        btn.setGraphic(unselectedImage);
        btn.setBackground(null);
        btn.setPadding(Insets.EMPTY);


        // choose appropriate action when the button is clicked:
        btn.setOnAction((event) -> {
            switch (buttonEnum) {
                case ADD_PLAYER:
                    playerSlotContainer.addItem(new PlayerSlot(new UnclaimedPlayer(),isHost));
                    ((HostConnectionManager)connectionManager).addOpenSlot();
                    break;
                case ADD_BOT:
                    playerSlotContainer.addItem(new PlayerSlot(new BotPlayer(BotPlayer.Difficulty.HARD),isHost));
                    break;
                case START:
                    // Todo: check whether there are any unclaimed open spots before starting the game.
                    System.out.println("pressed Start!");
                    gameData.changeGameStarted(true);
                    int[] packetsProcessingInfo = {0,0,0};
                    prepareAndSendServerPacket(packetsProcessingInfo);
                    // wait a little bit to make sure the packet gets through:
                    try{
                        Thread.sleep(2000/connectionManager.getPacketsSentPerSecond());
                    } catch (InterruptedException e){
                        System.err.println("InterruptedException encountered while trying to start the game. Other players" +
                                "might not be informed that the game has started but... oh well, starting anyways.");
                        e.printStackTrace();
                    }
                    startGame();
                    break;
                case RETURN_TO_MAIN_MENU_HOST:
                    System.out.println("pressed Exit!");
                    cleanUp();
                    SceneManager.switchToMainMenu();
                    break;
                case RETURN_TO_MAIN_MENU_CLIENT:
                    System.out.println("pressed Exit!");
                    cleanUp();
                    SceneManager.switchToMainMenu();
                    break;
                case HOST_AND_PORT:
                    System.out.println("pressed Hostname/Port!");
                    showHostNameDialog();
                    break;
                case MUTE:
                    System.out.println("pressed Mute!");
                    break;
            }
        });

        // Change the button graphic when it is hovered over:
        btn.addEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> btn.setGraphic(selectedImage));
        btn.addEventHandler(MouseEvent.MOUSE_EXITED, (event) -> btn.setGraphic(unselectedImage));

        return btn;
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

    private void startGame(){
        List<Player> players = new LinkedList<>();
        for (PlayerSlot playerSlot: playerSlotContainer.getContents()){
            players.add(playerSlot.getPlayer());
            playerSlot.getPlayer().makeEnumsNotChangeable();
        }
        SceneManager.startMultiplayerGame(isHost,connectionManager,players);
        animationTimer.stop();
    }
}