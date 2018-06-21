package Classes;

import Classes.Audio.Music;
import Classes.Audio.SoundManager;
import Classes.NetworkCommunication.ConnectionManager;
import Classes.NetworkCommunication.NullConnectionManager;
import Classes.PlayerTypes.BotPlayer;
import Classes.PlayerTypes.LocalPlayer;
import Classes.PlayerTypes.Player;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.*;


/*
 The entry point of the application.
 Selects which scenes should go onto the primary stage.
 */
public class SceneManager extends Application {

    private static Stage primaryStage;

    public static Stage getPrimaryStage(){
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage applicationStage)
    {
        primaryStage = applicationStage;
        primaryStage.setTitle("Thunder Cup");

        // Determine the memory constraints of the machine and choose an appropriate resolution:
        long max = Runtime.getRuntime().maxMemory();
        System.out.println("SceneManager: max heap = " + max);

        if(max < GameSettings.HIGHRES_MEMORY_CUTOFF) GameSettings.setImageResolution(GameSettings.ImageResolution.LOW);
        else GameSettings.setImageResolution(GameSettings.ImageResolution.HIGH);

        // Perform a graceful shutdown if the X button is pressed (or the player hits ALT+f4):
        applicationStage.setOnCloseRequest((event)-> {
                if(!confirmClose()) event.consume();
        });

        //Adjust the stage size to fit the computer's primary screen nicely:
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        /* Fullscreen mode is annoying for development, so use these settings for now*/
        primaryStage.setHeight(primaryScreenBounds.getHeight()*0.666);
        primaryStage.setWidth((primaryScreenBounds.getWidth())*0.666);

        /* Use these settings for the final release */
        /*primaryStage.setHeight(primaryScreenBounds.getHeight());
        primaryStage.setWidth((primaryScreenBounds.getWidth()));
        primaryStage.setMaximized(true); // to reduce the severity of flicker when switching scenes.
        primaryStage.setFullScreen(true);*/

        //Build and display the main menu:
        switchToMainMenu();
        primaryStage.show();
    }

    // A confirmation message is displayed any time the user performs a standard closing action (clicking the exit
    // button, hitting the "X" at the top right, or typing ALT+F4. If the user confirms, then the application is shut down
    // gracefully, ensuring that all threads are stopped.
    public static boolean confirmClose(){
        Alert closeConfirmation = new Alert(Alert.AlertType.CONFIRMATION);
        closeConfirmation.initOwner(primaryStage);
        closeConfirmation.setTitle("Close Application?");
        closeConfirmation.setHeaderText("Are you sure you want to exit the game?");
        ButtonType cancel = new ButtonType("Cancel");
        ButtonType yes = new ButtonType("Exit");
        closeConfirmation.getButtonTypes().setAll(yes,cancel);
        closeConfirmation.setGraphic(null);
        Optional<ButtonType> result = closeConfirmation.showAndWait();
        if(result.isPresent()) {
            if (result.get() == yes) {
                SoundManager.stopMusic();
                Scene currentScene = primaryStage.getScene();
                if(currentScene.getClass() == MultiplayerSelectionScene.class){
                    ((MultiplayerSelectionScene)currentScene).cleanUp();
                }
                else if(currentScene.getClass() == GameScene.class){
                    ((GameScene)currentScene).cleanUp();
                }
                return true;
            }
        }
        return false;
    }

    public static void switchToMainMenu(){
        System.out.println("Building main menu scene...");
        setSceneWorkaround(new MainMenuScene());
        SoundManager.playSong(Music.THE_PERFECT_STALLION, true);
    }

    // There is a bug in JavaFx where fullscreen mode is turned off whenever you call setScene. The only workaround I
    // know of is to immediately call setFullScreen afterwards. This unfortunately causes a flicker and the chrome is
    // visible for a moment. Oh well...
    private static void setSceneWorkaround(Scene nextScene){
        boolean isFullScreen = primaryStage.isFullScreen();
        primaryStage.setScene(nextScene);
        primaryStage.setFullScreen(isFullScreen);
    }

    static void switchToPuzzleMode(){
        System.out.println("building puzzle mode...");

        List<Player> playerList = new LinkedList<>();
        playerList.add(new LocalPlayer("YOU",true));

        // Create the GameScene, passing the PlayPanel to it:
        GameScene gameScene = new GameScene(true, new NullConnectionManager(), playerList, LocationType.NIGHTTIME, 1);
        setSceneWorkaround(gameScene);
        SoundManager.playRandomSongs();
    }

    static void switchToPuzzleSelectionMode(){
        System.out.println("entering puzzle selection mode...");
        PuzzleSelectionScene puzzleSelectionScene = new PuzzleSelectionScene();
        setSceneWorkaround(puzzleSelectionScene);
        SoundManager.playSong(Music.THE_PERFECT_STALLION, true);
    }

    static void switchToMultiplayerMode(boolean isHost, String hostName, int port, String username){
        System.out.println("building multiplayer selection scene...");
        MultiplayerSelectionScene multiplayerSelectionScene = new MultiplayerSelectionScene(isHost, username, hostName, port);
        if(multiplayerSelectionScene.isConnected()){
            setSceneWorkaround(multiplayerSelectionScene);
            SoundManager.stopMusic();
        }
    }

    static void startMultiplayerGame(boolean isHost, ConnectionManager connectionManager, List<Player> players){
        GameScene gameScene = new GameScene(isHost, connectionManager, players, LocationType.NIGHTTIME, -1);
        setSceneWorkaround(gameScene);
        SoundManager.playRandomSongs();
    }

    // Todo: multi-cannon mode is currently broken.
    static void switchTo2PlayerTestMode(){
        // add two players, controlled by the mouse:
        List<Player> playerList = new LinkedList<>();
        playerList.add(new LocalPlayer("YOU",true));
        playerList.add(new LocalPlayer("YOU",false));

        // Create the GameScene, passing the playerList to it:
        GameScene gameScene = new GameScene(true, new NullConnectionManager(), playerList, LocationType.NIGHTTIME, -5);
        setSceneWorkaround(gameScene);
        SoundManager.playRandomSongs();
    }

    static void switchToPuzzleVsMode(){
        // add a player and a bot, each with the same puzzle:
        List<Player> playerList = new LinkedList<>();
        playerList.add(new LocalPlayer("YOU",true));
        BotPlayer botPlayer = new BotPlayer();
        botPlayer.changeTeam(2);
        playerList.add(botPlayer);

        // Create the GameScene, passing the playerList to it:
        GameScene gameScene = new GameScene(true, new NullConnectionManager(), playerList, LocationType.NIGHTTIME, 1);
        setSceneWorkaround(gameScene);
        SoundManager.playRandomSongs();
    }

    static void switchToPuzzleMode(int puzzleGroupIndex){
        System.out.println("switching to puzzle mode...");

        List<Player> playerList = new LinkedList<>();
        playerList.add(new LocalPlayer("YOU",true));

        // Create the GameScene, passing the PlayPanel to it:
        GameScene gameScene = new GameScene(true, new NullConnectionManager(), playerList, LocationType.NIGHTTIME, puzzleGroupIndex);
        setSceneWorkaround(gameScene);
        SoundManager.playRandomSongs();
    }
}
