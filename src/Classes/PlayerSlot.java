package Classes;

import Classes.Images.ButtonType;
import Classes.Images.Drawing;
import Classes.NetworkCommunication.PlayerData;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Optional;

/**
 * Displays a player in the LobbyScene.
 */
//todo: there are a couple of places where I use synchronized(). The current implementation of LobbyScene is single-threaded, however. Consider removing these in the final version of the game if it stays single-threaded.
public class PlayerSlot extends StackPane{

    // Character and Cannon positioning on the PlayerSlot:
    private final double CHARACTER_X = 218.0; // The x-position of the center of the character's hooves on the PlayerSlot.
    private final double CHARACTER_Y = 875.0; // The y-position of the center of the character's hooves on the PlayerSlot.
    private final double CANNON_X = 445.0; // The x-position of the cannon's axis of rotation on the PlayerSlot.
    private final double CANNON_Y = 822.0; // The y-position of the cannon's axis of rotation on the PlayerSlot.

    private PlayerData playerData;

    // Various JavaFX nodes on this component:
    private ImageView characterImageView;
    private ImageView cannonImageView;
    private ComboBox<String> teamChoice;
    private Label teamLabel;
    private Label latencyLabel;
    private Button usernameBtn;
    private Label clickToChange;
    private Button removePlayerBtn;

    // EventHandlers whose references are needed so they can be de-registered:
    private EventHandler<MouseEvent> characterClickHandler;
    private EventHandler<MouseEvent> cannonClickHandler;

    PlayerSlot(PlayerData playerData, boolean isHost){
        setAlignment(Pos.TOP_LEFT);

        // Add background image:
        ImageView background = Drawing.PLAYER_SLOT_BACKGROUND.getImageView();
        getChildren().add(background);

        // Add CharacterAnimations and cannon images. They are placed on a Pane for more control over positioning:
        Pane characterAndCannonPositioner = new Pane();
        characterImageView = new ImageView();
        cannonImageView = new ImageView();
        characterAndCannonPositioner.getChildren().addAll(characterImageView, cannonImageView);
        getChildren().add(characterAndCannonPositioner);

        // Add foreground image:
        ImageView foreground = Drawing.PLAYER_SLOT_FOREGROUND.getImageView();
        getChildren().add(foreground);

        // A GridPane is used to layout information and selectable options:
        GridPane gridPane = new GridPane();
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(100);
        column.setHalignment(HPos.CENTER);
        gridPane.getColumnConstraints().add(column);
        //gridPane.setGridLinesVisible(true);
        gridPane.setVgap(10.0);
        getChildren().add(gridPane);
        gridPane.setPickOnBounds(false); // so the user can click on the character and cannon images underneath.

        // Display the username at the top of the player slot:
        usernameBtn = new Button();
        usernameBtn.setFont(new Font(48.0));
        ImageView unselectedImage = ButtonType.USERNAME.getUnselectedImageView();
        ImageView selectedImage = ButtonType.USERNAME.getSelectedImageView();
        usernameBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> usernameBtn.setBackground(new Background(new BackgroundImage(selectedImage.getImage(),BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT,BackgroundPosition.CENTER,BackgroundSize.DEFAULT))));
        usernameBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (event) -> usernameBtn.setBackground(new Background(new BackgroundImage(unselectedImage.getImage(),BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT,BackgroundPosition.CENTER,BackgroundSize.DEFAULT))));
        usernameBtn.setBackground(new Background(new BackgroundImage(unselectedImage.getImage(),BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT,BackgroundPosition.CENTER,BackgroundSize.DEFAULT)));
        usernameBtn.setMaxWidth(background.getImage().getWidth());
        gridPane.add(usernameBtn,0,7);

        // Display the player's latency value:
        latencyLabel = new Label("Latency: 0 milliseconds");
        latencyLabel.setFont(new Font(36.0));
        gridPane.add(latencyLabel,0,8);

        // Add a "Remove Player" button to allow the host to remove the player.
        removePlayerBtn = createButton(ButtonType.REMOVE_PLAYER);
        gridPane.add(removePlayerBtn,0,9);

        // Add A ComboBox for the player to select a team. It will only be shown it if this is the local player or if this is
        // the host and the player is a bot:
        teamChoice = new ComboBox<>();
        teamChoice.getItems().addAll("Team 1", "Team 2", "Team 3", "Team 4", "Team 5", "Team 6", "Team 7", "Team 8", "Team 9", "Team 10");
        teamChoice.setStyle("-fx-font: 36.0px \"Comic Sans\";");
        gridPane.add(teamChoice, 0, 10);
        // otherwise, just the team name of that player will be shown:
        teamLabel = new Label();
        teamLabel.setFont(new Font(48.0));
        gridPane.add(teamLabel,0,11);

        // At the very bottom, include a label informing the player that he/she can change the character and cannon by
        // clicking on them:
        clickToChange = new Label("Click cannon or character\n to change them.");
        clickToChange.setTextAlignment(TextAlignment.CENTER);
        clickToChange.setFont(new Font(36.0));
        gridPane.add(clickToChange, 0, 59);

        // Link the playerData to the controls in this PlayerSlot:
        changePlayer(playerData, isHost);
    }

    // Updates the PlayerSlot to reflect the type of PlayerData, and links up the playerData to various EventHandlers.
    public void changePlayer(PlayerData newPlayerData, boolean isHost){

        System.out.println("Calling changePlayer!!!!");

        // if playerData is not null, then de-register the old player's data. This happens when an unclaimed playerslot
        // is claimed by a new remote player.
        if(playerData!=null){
            newPlayerData.getSynchronizer().deRegisterAllWithID(playerData.getPlayerID());
        }

        playerData = newPlayerData;

        // make sure the character and cannon are positioned and scaled appropriately:
        playerData.relocateCharacter(CHARACTER_X, CHARACTER_Y);
        playerData.relocateCannon(CANNON_X, CANNON_Y);
        playerData.setScale(1.42);

        // The character and Cannon can be changed by clicking on them (but only by the player that owns them or by the
        // host if the player is a bot). Any old event handlers are de-registered and new ones are registered with the
        // new PlayerData.
        if(characterClickHandler!=null) characterImageView.removeEventHandler(MouseEvent.MOUSE_CLICKED, characterClickHandler);
        if(cannonClickHandler!=null) cannonImageView.removeEventHandler(MouseEvent.MOUSE_CLICKED, cannonClickHandler);
        if(playerData.getPlayerType().getData() == PlayerData.PlayerType.LOCAL || (playerData.getPlayerType().getData() == PlayerData.PlayerType.BOT && isHost)) {
            characterClickHandler = event -> {
                System.out.println("clicky click on character");
                playerData.incrementCharacterEnum();
            };
            characterImageView.addEventHandler(MouseEvent.MOUSE_CLICKED, characterClickHandler);
            cannonClickHandler = event -> {
                System.out.println("clicky click on cannon");
                playerData.incrementCannonEnum();
            };
            cannonImageView.addEventHandler(MouseEvent.MOUSE_CLICKED, cannonClickHandler);
            clickToChange.setVisible(true);
        }
        else clickToChange.setVisible(false);

        // If the player is the localPlayer, then allow the user to click on his/her username to modify it. Any old
        // EventHandler is de-registered before the new one is registered with the new PlayerData.
        usernameBtn.setOnAction(null);
        if(playerData.getPlayerType().getData() == PlayerData.PlayerType.LOCAL){
            usernameBtn.setOnAction((event) -> {
                String newName = displayChangeUsernameDialog(playerData);
                playerData.getUsername().changeTo(newName);
            });
        }
        else usernameBtn.setBackground(null);

        // If this is not the host, hide the remove player button. The host does not get a remove player button for
        // their own character. De-register any existing ActionListener before registering one to the new PlayerData:
        removePlayerBtn.setOnAction(null);
        if(!isHost || playerData.getPlayerType().getData() == PlayerData.PlayerType.LOCAL){
            removePlayerBtn.setVisible(false);
        }
        else{
            removePlayerBtn.setVisible(true);
            removePlayerBtn.setOnAction((event) -> {
                System.out.println("Clicked Remove Player.");
                playerData.getState().changeTo(PlayerData.State.DISCONNECTED);
            });
        }

        // Register the EventHandler for the teamChoice combo box. De-register any existing one, first:
        teamChoice.setOnAction(null);
        if(playerData.getPlayerType().getData() == PlayerData.PlayerType.LOCAL || (isHost && playerData.getPlayerType().getData() == PlayerData.PlayerType.BOT)){
            teamChoice.setVisible(true);
            teamLabel.setVisible(false);
            teamChoice.setOnAction((event)-> playerData.getTeam().changeTo(teamChoice.getSelectionModel().getSelectedIndex()+1));
        }
        else {
            System.out.println("team choice is being turned off");
            teamChoice.setVisible(false);
            teamLabel.setVisible(true);
            int team;
            synchronized (playerData.getSynchronizer()){ // make sure no one else is modifying the data as we try to access it.
                team = playerData.getTeam().getData();
            }
            teamLabel.setText("Team " + team);
        }
    }

    // todo: finish implementing this
    public void tick(int lowestRow){
        playerData.tick(lowestRow);
    }

    // todo: finish implementing this
    public void repaint(boolean isHost){
        playerData.drawSelf(characterImageView, cannonImageView);

        long latency = playerData.getLatency();
        if(latency<1000000) latencyLabel.setText(String.format("Latency: %d microseconds",latency/1000L));
        else latencyLabel.setText(String.format("Latency: %d milliseconds",latency/1000000L));

        String username;
        PlayerData.PlayerType playerType;
        synchronized (playerData.getSynchronizer()){ // make sure no one is changing the data while we try to access it
            username = playerData.getUsername().getData();
            playerType = playerData.getPlayerType().getData();
        }
        usernameBtn.setText(username);

        if(playerType == PlayerData.PlayerType.LOCAL || (isHost && playerType == PlayerData.PlayerType.BOT)){
            teamChoice.getSelectionModel().select(playerData.getTeam().getData()-1);
        }
        else{
            teamLabel.setText("Team " + playerData.getTeam().getData());
        }
    }

    public PlayerData getPlayerData(){
        return playerData;
    }

    private Button createButton(ButtonType buttonEnum){
        Button btn = new Button();
        ImageView unselectedImage = buttonEnum.getUnselectedImageView();
        ImageView selectedImage = buttonEnum.getSelectedImageView();
        btn.setGraphic(unselectedImage);
        btn.setBackground(null);

        // Change the button graphic when it is hovered over:
        btn.addEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> btn.setGraphic(selectedImage));
        btn.addEventHandler(MouseEvent.MOUSE_EXITED, (event) -> btn.setGraphic(unselectedImage));

        return btn;
    }

    private String displayChangeUsernameDialog(PlayerData playerData){
        String oldName;
        synchronized (playerData.getSynchronizer()){ // make sure no one else is changing the data while we try to access it.
            oldName = playerData.getUsername().getData();
        }
        String newName = oldName;
        TextInputDialog newNameAsker = new TextInputDialog(oldName);
        newNameAsker.setTitle("Change username");
        newNameAsker.setHeaderText("Enter the new name you would like to use:");
        newNameAsker.setGraphic(null);

        Optional<String> result = newNameAsker.showAndWait();
        if(result.isPresent()){
            newName = result.get();
        }
        return newName;
    }
}
