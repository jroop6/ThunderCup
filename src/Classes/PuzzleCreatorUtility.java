package Classes;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.*;

public class PuzzleCreatorUtility extends Application {

    static PuzzleCreatorScene scene;
    private static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage)
    {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Puzzle Creator");

        //Adjust the stage size to fit the computer's primary screen nicely:
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setHeight(primaryScreenBounds.getHeight()*0.666);
        primaryStage.setWidth((primaryScreenBounds.getWidth())*0.666);

        // Start with an empty default scene:
        Scene tempScene = new Scene(new Pane());
        primaryStage.setScene(tempScene);

        // get the number of players and create the scene
        askNumPlayers();

        // Perform a graceful shutdown
        primaryStage.setOnCloseRequest((event)-> {
            scene.cleanUp();
        });
    }

    public static void askNumPlayers(){
        // Ask the player how many players this puzzle is for:
        List<Integer> choices;
        choices = Arrays.asList(1,2,3);
        ChoiceDialog<Integer> numPlayersDialog = new ChoiceDialog<>(1,choices);
        numPlayersDialog.initOwner(primaryStage);
        numPlayersDialog.setTitle("Number of Players");
        numPlayersDialog.setHeaderText("How many players is this puzzle for?");
        numPlayersDialog.setGraphic(null);
        Optional<Integer> result = numPlayersDialog.showAndWait();

        // Create a new scene with that number of players and switch to it:
        int numPlayers = 1;
        if(result.isPresent()){
            numPlayers = result.get();
        }
        scene = new PuzzleCreatorScene(numPlayers);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static Stage getPrimaryStage(){
        return primaryStage;
    }
}
