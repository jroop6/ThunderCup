package Classes;

import Classes.Animation.*;
import Classes.Audio.SoundManager;
import Classes.Images.ButtonType;
import Classes.Images.StaticBgImages;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static Classes.GameScene.FRAME_RATE;
import static Classes.NetworkCommunication.PlayerData.HOST_ID;

/**
 * Created by HydrusBeta on 7/23/2017.
 */
public class MainMenuScene extends Scene {

    // Some distance constants for the layout of the buttons (in unscaled pixels):
    private final int BUTTONS_VERTICAL_STARTING_POSITION = 460;
    private final int BUTTONS_HORIZONTAL_STARTING_POSITION = 1480;
    private final int BUTTON_HEIGHT = 82;
    private final int EXIT_BUTTON_SEPARATION_DISTANCE = 30;
    private final String PROGRAMMER_WEBSITE_URL = "https://jonathanroop.wordpress.com/";
    private final String COMPOSER_WEBSITE_URL = "https://alllevelsatonce.com";

    private Pane scaledRoot;
    // If one dimension (width or height) of the application window is scaled, the scaledRoot Node may or may not need to be
    // scaled, depending on the size of the other dimension. Separate EventHandlers for the height and width make
    // assertions as to which scaling factor should be used; in the end, the smaller of the two is used. This ensures
    // that the entire main menu is visible in the window, no matter how the window is resized.
    private double scaleAssertionFromWidth = 1.0;
    private double scaleAssertionFromHeight = 1.0;
    private Scale scaler = new Scale(1.0,1.0);

    // variables related to animation timing:
    private AnimationTimer animationTimer;
    private boolean initializing = true;
    private long nextAnimationFrameInstance = 0;

    // Creates the main menu, from which the player selects the type of game he/she would like to play:
    MainMenuScene(){
        super(new StackPane());
        StackPane rootNode = (StackPane)getRoot();

        // Get and add the sky:
        Rectangle sky = new Rectangle();
        sky.setFill(new LinearGradient(0.5,0.0,0.5,1.0,true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("0x7093ff")),
                    new Stop(0.5,Color.web("0x94bbe3")),
                    new Stop(1.0,Color.web("0x91c6dd"))));
        sky.widthProperty().bind(rootNode.widthProperty());
        sky.heightProperty().bind(rootNode.heightProperty());
        rootNode.getChildren().add(sky);

        // Everything except the sky is scaled in a special way. Put everything except the sky on a Pane:
        scaledRoot = new Pane();
        rootNode.getChildren().add(scaledRoot);

        // Add slowly-moving background clouds:
        createCloud(StaticBgImages.FAR_CLOUD_1,635.0,210000,0);
        createCloud(StaticBgImages.FAR_CLOUD_2,615.0,200000,118000);
        createCloud(StaticBgImages.FAR_CLOUD_3,706.0, 190000,69200);
        createCloud(StaticBgImages.FAR_CLOUD_WITH_BUBBLES_2,20,180000,63600);
        createCloud(StaticBgImages.FAR_CLOUD_WITH_BUBBLES_1,-20,170000,130000);

        // Add the foreground cloud and character:
        ImageView nearCloud = StaticBgImages.NEAR_CLOUD_1.getImageView();
        nearCloud.relocate(7,734);
        ImageView characterView = new ImageView();
        CharacterData characterData = new CharacterData(CharacterType.BLITZ, HOST_ID);
        characterData.relocate(550,850);
        scaledRoot.getChildren().addAll(nearCloud, characterView);

        // Add the animated title:
        AnimationData title = new AnimationData(Classes.Animation.Animation.TITLE,1920/2, 550/2, PlayOption.PLAY_ONCE_THEN_PAUSE);
        ImageView titleView = new ImageView();
        scaledRoot.getChildren().add(titleView);

        // Create buttons and add them to the scene:
        ThunderButton puzzleModeButton = new ThunderButton(ButtonType.PUZZLE, (event) -> {
            System.out.println("clicked puzzle mode");
            SceneManager.switchToPuzzleSelectionMode();
        });
        ThunderButton randomPuzzleModeButton = new ThunderButton(ButtonType.RANDOM_PUZZLE, (event) -> {
            System.out.println("clicked random puzzle mode");
            animationTimer.stop();
            SceneManager.switchToPuzzleMode(-5);
        });
        ThunderButton multiplayerModeButton = new ThunderButton(ButtonType.MULTIPLAYER, (event) -> {
            System.out.println("clicked multiplayer mode");
            boolean[] canceled = {false}; // detects when the user cancels a dialog to return to the main menu.
            boolean isHost;
            String hostName = "";
            int port = 5000;
            String username = "";

            // Ask whether the user wishes to host a new game or join an existing game:
            isHost = showHostOrJoinDialog(canceled);

            // If the user is NOT the host, ask him/her for a computer name to connect to:
            if(!canceled[0] && !isHost) hostName = showHostNameDialog(canceled);

            // Ask the user for a port number to connect with:
            if(!canceled[0]) port = showPortNumberDialog(canceled);

            // Ask the player for his/her name:
            if(!canceled[0]) username = showUsernameDialog(canceled);

            // Switch to the multiplayer staging area:
            if(!canceled[0]){
                animationTimer.stop();
                SceneManager.switchToMultiplayerMode(isHost, hostName, port, username);
            }
        });
        ThunderButton vsComputerModeButton = new ThunderButton(ButtonType.VS_COMPUTER, (event) -> {
            System.out.println("clicked vs computer");
            animationTimer.stop();
            SceneManager.switchToPuzzleVsMode();
        });
        ThunderButton adventureModeButton = new ThunderButton(ButtonType.ADVENTURE, (event) -> {
            System.out.println("clicked adventure mode");
        });
        ThunderButton tutorialModeButton = new ThunderButton(ButtonType.TUTORIAL, (event) -> {
            System.out.println("clicked tutorial mode");
        });
        ThunderButton creditsButton = new ThunderButton(ButtonType.CREDITS, (event) -> {
            showCreditsDialog();
        });
        Rectangle spacer = new Rectangle(1,30,Color.TRANSPARENT);
        ThunderButton exitButton = new ThunderButton(ButtonType.EXIT, (event) -> {
            System.out.println("clicked exit");
            if(SceneManager.confirmClose()){
                System.out.println("exiting with code 0");
                System.exit(0);
            }
        });
        ThunderButton muteButton = new ThunderButton(ButtonType.MUTE, null);
        muteButton.setOnAction((event) -> {
            if(SoundManager.isMuted()){
                SoundManager.unMuteMusic();
                muteButton.setButtonType(ButtonType.MUTE);
            }
            else{
                SoundManager.muteMusic();
                muteButton.setButtonType(ButtonType.MUTED);
            }
        });
        VBox buttonHolder = new VBox();
        buttonHolder.getChildren().addAll(puzzleModeButton,randomPuzzleModeButton,multiplayerModeButton,vsComputerModeButton,
                creditsButton,spacer,exitButton);
        buttonHolder.relocate(BUTTONS_HORIZONTAL_STARTING_POSITION,BUTTONS_VERTICAL_STARTING_POSITION);
        muteButton.relocate(10,955);
        scaledRoot.getChildren().addAll(buttonHolder,muteButton);

        // listen for window resizing and scale content accordingly:
        scaledRoot.getTransforms().add(scaler);
        scaledRoot.widthProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("New width:  " + newValue);
            scaleAssertionFromWidth = (double)newValue/1920.0;
            scale();
        });
        scaledRoot.heightProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("New height: " + newValue);
            scaleAssertionFromHeight = (double)newValue/1080.0;
            scale();
        });

        // Start the animation timer, which will play the title animation
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if(initializing){
                    nextAnimationFrameInstance = now;
                    initializing = false;
                }

                // Animations are updated 24 times per second:
                if(now>nextAnimationFrameInstance){
                    title.tick();
                    title.drawSelf(titleView);
                    characterData.tick(0);
                    characterData.drawSelf(characterView);
                    nextAnimationFrameInstance += 1000000000L/ FRAME_RATE;
                }
            }
        };
        animationTimer.start();
    }

    private void createCloud(StaticBgImages imageEnum, double yPos, int durationInMillis, int startTimeInMillis){
        ImageView cloud = imageEnum.getImageView();
        TranslateTransition cloudTranslation = new TranslateTransition(Duration.millis(durationInMillis),cloud);
        cloudTranslation.setFromX(-cloud.getImage().getWidth());
        cloudTranslation.setFromY(yPos);
        cloudTranslation.setByX(1920.0 + cloud.getImage().getWidth());
        cloudTranslation.setCycleCount(Animation.INDEFINITE);
        cloudTranslation.setInterpolator(Interpolator.LINEAR);
        cloudTranslation.jumpTo(Duration.millis(startTimeInMillis));
        cloudTranslation.play();
        scaledRoot.getChildren().add(cloud);
    }

    private void scale(){
        double scaleValue = Math.min(scaleAssertionFromHeight, scaleAssertionFromWidth);
        scaler.setX(scaleValue);
        scaler.setY(scaleValue);
    }

    private boolean showHostOrJoinDialog(boolean[] canceled){
        boolean isHost = true;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(SceneManager.getPrimaryStage());
        alert.setTitle("Select multiplayer option");
        alert.setHeaderText("Select an option:");
        javafx.scene.control.ButtonType createGame = new javafx.scene.control.ButtonType("Create new Game");
        javafx.scene.control.ButtonType joinGame = new javafx.scene.control.ButtonType("Join a game");
        javafx.scene.control.ButtonType returnBtn = new javafx.scene.control.ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(createGame,joinGame,returnBtn);
        alert.setGraphic(null);
        Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if(result.isPresent()){
            if(result.get()==createGame){
                System.out.println("create Game clicked");
                isHost = true;
            }
            else if(result.get()==joinGame){
                System.out.println("join game clicked");
                isHost = false;
            }
            else canceled[0] = true;
        }
        return isHost;
    }

    private String showHostNameDialog(boolean[] canceled){
        String host = "";
        TextInputDialog hostNameAsker = new TextInputDialog();
        hostNameAsker.initOwner(SceneManager.getPrimaryStage());
        hostNameAsker.setTitle("Identify Host");
        hostNameAsker.setHeaderText("Enter the name of the host computer:");
        hostNameAsker.setGraphic(null);
        Optional<String> result2 = hostNameAsker.showAndWait();
        if(result2.isPresent()){
            host = result2.get();
        }
        else canceled[0] = true;
        return host;
    }

    private int showPortNumberDialog(boolean[] canceled){
        String portNumber;
        int portNumberAsInt = 5000;
        TextInputDialog portAsker = new TextInputDialog("5000");
        portAsker.initOwner(SceneManager.getPrimaryStage());
        portAsker.setTitle("Identify PortNumber");
        portAsker.setHeaderText("Enter the port Number to use (default is 5000):");
        portAsker.setGraphic(null);
        Optional<String> result2 = portAsker.showAndWait();
        if(result2.isPresent()){
            portNumber = result2.get();
            try{
                portNumberAsInt = Integer.parseInt(portNumber);
            } catch(NumberFormatException e) {
                canceled[0] = true;
                Alert errorAlert = new Alert(Alert.AlertType.CONFIRMATION);
                errorAlert.initOwner(SceneManager.getPrimaryStage());
                errorAlert.setTitle("Could not understand input");
                errorAlert.setHeaderText("Parser could not convert your input into an integer value. Please try again with an integer value.");
                javafx.scene.control.ButtonType returnBtn = new javafx.scene.control.ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                errorAlert.getButtonTypes().setAll(returnBtn);
                errorAlert.setGraphic(null);
                Optional<javafx.scene.control.ButtonType> result = errorAlert.showAndWait();
            }
        }
        else canceled[0] = true;
        return portNumberAsInt;
    }

    private String showUsernameDialog(boolean[] canceled){
        String username = "";
        TextInputDialog nameAsker = new TextInputDialog();
        nameAsker.initOwner(SceneManager.getPrimaryStage());
        nameAsker.setTitle("Choose a username");
        nameAsker.setHeaderText("Your name:");
        nameAsker.setGraphic(null);
        Optional<String> result3 = nameAsker.showAndWait();
        if(result3.isPresent()){
            username = result3.get();
        }
        else canceled[0] = true;
        return username;
    }

    private void showCreditsDialog(){
        Alert creditsDialog = new Alert(Alert.AlertType.CONFIRMATION);
        creditsDialog.initOwner(SceneManager.getPrimaryStage());
        creditsDialog.setTitle("Credits");
        creditsDialog.setHeaderText("");
        VBox vBox = new VBox();
        FlowPane credits1 = new FlowPane();
        Label label1 = new Label("Thunder Cup is a bubble breaker-style game programmed by Jonathan Roop as " +
                "part of his 2018 portfolio. All art and animation used in the game were also created by " +
                "him. Character styles were inspired by the television series My Little Pony: " +
                "Friendship is Magic.");
        label1.setWrapText(true);
        label1.setPrefWidth(500);
        label1.setTextAlignment(TextAlignment.JUSTIFY);
        Hyperlink programmerWebsite = new Hyperlink("Check out Jonathan's other works on his portfolio website");
        programmerWebsite.setOnAction((event -> {
            try{
                Desktop.getDesktop().browse(new URI(PROGRAMMER_WEBSITE_URL));
            }
            catch(URISyntaxException | IOException e){
                e.printStackTrace();
            }
        }));
        Label label2 = new Label("\n\nThe music used in the game was created by AllLevelsAtOnce. Many of the " +
                "tracks were based on songs from the My Little Pony: Friendship is Magic television series. ");
        label2.setWrapText(true);
        label2.setPrefWidth(500);
        label2.setTextAlignment(TextAlignment.JUSTIFY);
        Hyperlink composerWebsite = new Hyperlink("Visit alllevelsatonce.com for more works by the composer.");
        composerWebsite.setOnAction((event -> {
            try{
                Desktop.getDesktop().browse(new URI(COMPOSER_WEBSITE_URL));
            }
            catch(URISyntaxException | IOException e){
                e.printStackTrace();
            }
        }));
        Label label3 = new Label("\n\nSound effects were downloaded from freeSound.org and modified. Many thanks " +
                "to the original uploaders: Arctura, Figowitz, LittleRobotSoundFactory, Timbre, " +
                "suntemple, Bykgames, suonho, Benboncan, Komit, and noirenex. Various permissive " +
                "licenses were used by these contributors; links to these licenses can be found below: ");
        label3.setWrapText(true);
        label3.setPrefWidth(500);
        label3.setTextAlignment(TextAlignment.JUSTIFY);

        credits1.getChildren().addAll(label1, programmerWebsite, label2, composerWebsite, label3);


        HBox links = new HBox();
        Hyperlink attrLicLink = new Hyperlink("Attribution License");
        attrLicLink.setOnAction((event)->{
            try{
                Desktop.getDesktop().browse(new URI("https://creativecommons.org/licenses/by/3.0/"));
            }
            catch(URISyntaxException | IOException e){
                e.printStackTrace();
            }
        });
        Label linkSeparator1 = new Label(" || ");
        Hyperlink cc0LicLink = new Hyperlink("CC0 License");
        cc0LicLink.setOnAction((event)->{
            try{
                Desktop.getDesktop().browse(new URI("https://creativecommons.org/publicdomain/zero/1.0/"));
            }
            catch(URISyntaxException | IOException e){
                e.printStackTrace();
            }
        });
        Label linkSeparator2 = new Label(" || ");
        Hyperlink attrNCLicLink = new Hyperlink("Attribution NonCommercial License");
        attrNCLicLink.setOnAction((event)->{
            try{
                Desktop.getDesktop().browse(new URI("https://creativecommons.org/licenses/by-nc/3.0/"));
            }
            catch(URISyntaxException | IOException e){
                e.printStackTrace();
            }
        });
        links.getChildren().addAll(attrLicLink, linkSeparator1, cc0LicLink, linkSeparator2, attrNCLicLink);
        links.setAlignment(Pos.CENTER);
        Label credits2 = new Label(
                "\nDisclaimer: Thunder Cup and its contributors are not affiliated with Hasbro or \n" +
                "the My Little Pony brand. This game was created for the purpose of showcasing the \n" +
                "programmer's Java coding ability and also as a labor of love for the incredible \n" +
                "brony fandom.\n\n"
        );
        HBox versionContainer = new HBox();
        Label version = new Label("version 0.2 - Development Release");
        versionContainer.getChildren().add(version);
        versionContainer.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(credits1, links, credits2, versionContainer);
        creditsDialog.getDialogPane().contentProperty().set(vBox);
        javafx.scene.control.ButtonType returnBtn = new javafx.scene.control.ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        creditsDialog.getButtonTypes().setAll(returnBtn);
        creditsDialog.setGraphic(null);
        creditsDialog.showAndWait();
    }
}

