package Classes;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class PuzzleCreatorUtility extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage)
    {
        primaryStage.setTitle("Puzzle Creator");

        //Adjust the stage size to fit the computer's primary screen nicely:
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setHeight(primaryScreenBounds.getHeight()*0.666);
        primaryStage.setWidth((primaryScreenBounds.getWidth())*0.666);


        PuzzleCreatorScene scene = new PuzzleCreatorScene(1);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Perform a graceful shutdown
        primaryStage.setOnCloseRequest((event)-> {
            scene.cleanUp();
        });

    }
}
