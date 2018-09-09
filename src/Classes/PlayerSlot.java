package Classes;

import Classes.Images.ButtonType;
import Classes.Images.StaticBgImages;
import Classes.NetworkCommunication.PlayerData;
import Classes.PlayerTypes.BotPlayer;
import Classes.PlayerTypes.LocalPlayer;
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
 * Displays a player in the MultiplayerSelectionScene.
 */
public class PlayerSlot extends StackPane{

    // Character and Cannon positioning on the PlayerSlot:
    private final double CHARACTER_X = 218.0; // The x-position of the center of the character's hooves on the PlayerSlot.
    private final double CHARACTER_Y = 875.0; // The y-position of the center of the character's hooves on the PlayerSlot.
    private final double CANNON_X = 445.0; // The x-position of the cannon's axis of rotation on the PlayerSlot.
    private final double CANNON_Y = 822.0; // The y-position of the cannon's axis of rotation on the PlayerSlot.

    private PlayerData playerData;
    private ImageView characterImageView;
    private ImageView cannonImageView;

    // The PlayerSlot will have one of the following, for displaying a Player's team:
    private ComboBox<String> teamChoice;
    private Label teamLabel;

    // Displays latency time between the client and the host
    private Label latencyLabel;

    // A button for displaying the username:
    private Button usernameBtn;

    PlayerSlot(PlayerData playerData, boolean isHost){
        this.playerData = playerData;
        setAlignment(Pos.TOP_LEFT);

        // Add background image:
        ImageView background = StaticBgImages.PLAYER_SLOT_BACKGROUND.getImageView();
        getChildren().add(background);

        // Add CharacterAnimations and cannon images. They are placed on a Pane for more control over positioning:
        Pane characterAndCannonPositioner = new Pane();
        //characterAndCannonPositioner.getChildren().addAll(player.getCharacterSprite() , player.getCannonMovingPart());

        characterImageView = new ImageView();
        cannonImageView = new ImageView();
        characterAndCannonPositioner.getChildren().addAll(characterImageView, cannonImageView);

        /*player.relocateCharacter(CHARACTER_X-player.getPlayerData().getCharacterEnum().getHoovesX(),
                CHARACTER_Y-player.getPlayerData().getCharacterEnum().getHoovesY());*/
        playerData.relocateCharacter(CHARACTER_X, CHARACTER_Y);
        playerData.relocateCannon(CANNON_X, CANNON_Y);
        getChildren().add(characterAndCannonPositioner);

        // Scale the Cannon and Character:
        playerData.setScale(1.42);

        // The character and Cannon can be changed by clicking on them (but only by the player that owns them or by the
        // host if the player is a bot). Note: the eventHandlers here are named so that they can later be removed with
        // removeEventHandlers():
        if(playerData instanceof LocalPlayer || (playerData instanceof BotPlayer && isHost)){
            characterImageView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                System.out.println("clicky click on character");
                playerData.incrementCharacterEnum();
            });
            cannonImageView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                System.out.println("clicky click on cannon");
                playerData.incrementCannonEnum();
            });
        }

        // Add foreground image:
        ImageView foreground = StaticBgImages.PLAYER_SLOT_FOREGROUND.getImageView();
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
        usernameBtn.setBackground(null);
        gridPane.add(usernameBtn,0,7);

        // Display the player's latency value:
        latencyLabel = new Label("Latency: 0 milliseconds");
        latencyLabel.setFont(new Font(36.0));
        gridPane.add(latencyLabel,0,8);

        // If the player is the localPlayer, then allow the user to click on his/her username to modify it. Unfortunately,
        // the createButton() method can't be reused here, because usernameBtn is stored inside the Player class.
        if(playerData instanceof LocalPlayer){
            ImageView unselectedImage = ButtonType.USERNAME.getUnselectedImageView();
            ImageView selectedImage = ButtonType.USERNAME.getSelectedImageView();
            usernameBtn.setBackground(new Background(new BackgroundImage(unselectedImage.getImage(),BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT,BackgroundPosition.CENTER,BackgroundSize.DEFAULT)));
            usernameBtn.setMaxWidth(background.getImage().getWidth());
            usernameBtn.setOnAction((event) -> {
                String newName = displayChangeUsernameDialog(playerData.getUsername());
                playerData.changeUsername(newName);
            });
            usernameBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> {
                usernameBtn.setBackground(new Background(new BackgroundImage(selectedImage.getImage(),BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT,BackgroundPosition.CENTER,BackgroundSize.DEFAULT)));
            });
            usernameBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (event) -> {
                usernameBtn.setBackground(new Background(new BackgroundImage(unselectedImage.getImage(),BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT,BackgroundPosition.CENTER,BackgroundSize.DEFAULT)));
            });
        }

        // Add a "Remove Player" button to allow the host to remove the player. If this is not the host, hide it. The
        // host does not get a remove player button for their own character:
        Button removePlayerBtn = createButton(ButtonType.REMOVE_PLAYER);
        gridPane.add(removePlayerBtn,0,9);
        if(!isHost || playerData instanceof LocalPlayer){
            removePlayerBtn.setVisible(false);
        }

        // Add A ComboBox for the player to select a team, but only show it if this is the local player or if this is
        // the host and the player is a bot:
        if(playerData instanceof LocalPlayer || (isHost && playerData instanceof BotPlayer)){
            teamChoice = new ComboBox<>();
            teamChoice.getItems().addAll("Team 1", "Team 2", "Team 3", "Team 4", "Team 5", "Team 6", "Team 7", "Team 8", "Team 9", "Team 10");
            teamChoice.setStyle("-fx-font: 36.0px \"Comic Sans\";");
            gridPane.add(teamChoice,0,10);
            teamChoice.setOnAction((event)->{
                playerData.changeTeam(teamChoice.getSelectionModel().getSelectedIndex()+1);
            });
        }

        // ...otherwise, just display the team name of that player:
        else{
            teamLabel = new Label();
            teamLabel.setFont(new Font(48.0));
            teamLabel.setText("Team " + playerData.getTeam());
            gridPane.add(teamLabel,0,11);
        }

        // At the very bottom, include a label informing the player that he/she can change the character and cannon by
        // clicking on them:
        if(playerData instanceof LocalPlayer || (playerData instanceof BotPlayer && isHost)){
            Label clickToChange = new Label("Click cannon or character\n to change them.");
            clickToChange.setTextAlignment(TextAlignment.CENTER);
            clickToChange.setFont(new Font(36.0));
            gridPane.add(clickToChange,0,59);
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

        usernameBtn.setText(playerData.getUsername());

        if(playerData instanceof LocalPlayer || (isHost && playerData instanceof BotPlayer)){
            teamChoice.getSelectionModel().select(playerData.getTeam());
        }
        else{
            teamLabel.setText("Team " + playerData.getTeam());
        }
    }

    public PlayerData getPlayerData(){
        return playerData;
    }

    // Changing players is accomplished by replacing all the children of this PlayerSlot Node with the children of a
    // new PlayerSlot node.
    //todo: I'm sure I must have broken this. Fix it.
    public void changePlayer(PlayerData playerData, boolean isHost){
        getChildren().clear();
        this.playerData = playerData; //used to be: playerData = new RemotePlayer(playerData);
        getChildren().addAll((new PlayerSlot(playerData,isHost).getChildren()));
    }

    private Button createButton(ButtonType buttonEnum){
        Button btn = new Button();
        ImageView unselectedImage = buttonEnum.getUnselectedImageView();
        ImageView selectedImage = buttonEnum.getSelectedImageView();
        btn.setGraphic(unselectedImage);
        btn.setBackground(null);

        // choose appropriate action when the button is clicked:
        btn.setOnAction((event) -> {
            switch (buttonEnum) {
                case REMOVE_PLAYER:
                    System.out.println("Clicked Remove Player.");
                    playerData.changeResignPlayer();
                    break;
            }
        });

        // Change the button graphic when it is hovered over:
        btn.addEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> {
            btn.setGraphic(selectedImage);
        });
        btn.addEventHandler(MouseEvent.MOUSE_EXITED, (event) -> {
            btn.setGraphic(unselectedImage);
        });

        return btn;
    }

    private String displayChangeUsernameDialog(String oldName){
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
