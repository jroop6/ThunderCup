package Classes;

import Classes.Animation.OrbImages;
import Classes.Images.StaticBgImages;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;


import static Classes.GameScene.ANIMATION_FRAME_RATE;
import static Classes.NetworkCommunication.PlayPanelData.ARRAY_HEIGHT;
import static Classes.NetworkCommunication.PlayPanelData.ARRAY_WIDTH_PER_CHARACTER;
import static Classes.Orb.NULL;
import static Classes.Orb.ORB_RADIUS;
import static Classes.PlayPanel.PLAYPANEL_HEIGHT;
import static Classes.PlayPanel.PLAYPANEL_WIDTH_PER_PLAYER;
import static Classes.PlayPanel.ROW_HEIGHT;

public class PuzzleCreatorScene extends Scene {

    private final Orb EMPTY = new Orb(OrbImages.BLACK_ORB,-2,-2,Orb.BubbleAnimationType.STATIC);
    private long nextAnimationFrameInstance = 0;
    private boolean initializing = false;
    private Scale scaler = new Scale(1,1);
    private AnimationTimer animationTimer;
    private Orb[][] orbArray;
    private Canvas orbCanvas;

    PuzzleCreatorScene(int numPlayers){
        super(new Pane());
        Pane rootNode = (Pane)getRoot();

        // Give everything a nice background:
        ImageView nightSky = StaticBgImages.NIGHT_SKY.getImageView();
        rootNode.getChildren().add(nightSky);
        //nightSky.fitHeightProperty().bind(rootNode.heightProperty());

        // Most things are arranged on an HBox
        HBox hBox = new HBox();
        rootNode.getChildren().add(hBox);

        // The Orbs are placed on a Canvas. Initialize the Canvas:
        orbCanvas = new Canvas();
        orbCanvas.setWidth(PLAYPANEL_WIDTH_PER_PLAYER*numPlayers + ORB_RADIUS);
        orbCanvas.setHeight(PLAYPANEL_HEIGHT + ROW_HEIGHT*3*(numPlayers-2));
        GraphicsContext orbDrawer = orbCanvas.getGraphicsContext2D();
        orbDrawer.setStroke(Color.GREEN);
        hBox.getChildren().add(orbCanvas);

        // Initialize the Array:
        orbArray = new Orb[ARRAY_HEIGHT+3*numPlayers][ARRAY_WIDTH_PER_CHARACTER*numPlayers];
        for(int i=0; i<orbArray.length; ++i){
            for(int j=0; j<orbArray[i].length; j++){
                if(j%2==i%2) orbArray[i][j] = EMPTY;
                else orbArray[i][j] = NULL;
            }
        }
        for(int i=ARRAY_HEIGHT; i<orbArray.length; i+=3){
            for(int j=0; j<orbArray[i].length; j++){
                orbArray[i][j] = NULL;
            }
        }

        // MouseListener figures out where you clicked and updates the appropriate Orb.
        addEventHandler(MouseEvent.MOUSE_PRESSED, (event) ->{
            // Determine the [i][j] coordinates of the clicked location:
            Point2D localLoc = orbCanvas.sceneToLocal(event.getX(), event.getY());
            int iPos = (int)Math.round((localLoc.getY()-ORB_RADIUS)/ROW_HEIGHT);
            double jRaw = (localLoc.getX()-ORB_RADIUS)/ORB_RADIUS;
            int jPos = (int)Math.round(jRaw);
            if(iPos%2==0 && jPos%2!=0) jPos = jPos + (jRaw>jPos?1:-1);
            else if(iPos%2==1 && jPos%2!=1) jPos = jPos + (jRaw>jPos?1:-1);
            System.out.println("mouse was pressed at i=" + iPos + " j=" + jPos);
            if(iPos>=orbArray.length || jPos>=ARRAY_WIDTH_PER_CHARACTER*numPlayers) return;

            // Change the OrbImage enum:
            if(orbArray[iPos][jPos] == NULL);
            else if(orbArray[iPos][jPos]==EMPTY){
                OrbImages newEnum;
                if(event.isPrimaryButtonDown()) newEnum = OrbImages.values()[0]; // left-click
                else newEnum = OrbImages.values()[OrbImages.values().length-1]; // right-click
                orbArray[iPos][jPos] = new Orb(newEnum,iPos,jPos,Orb.BubbleAnimationType.STATIC);
            }
            else{
                OrbImages newEnum;
                if(event.isPrimaryButtonDown()) newEnum = orbArray[iPos][jPos].getOrbEnum().next(); // left-click
                else newEnum = orbArray[iPos][jPos].getOrbEnum().previous(); // right-click
                if(newEnum == null) orbArray[iPos][jPos] = EMPTY;
                else orbArray[iPos][jPos].setOrbEnum(newEnum);
            }
        });

        // There is a button to the right that, when pressed, causes the puzzle to be output as a string on a text field.
        VBox vBox = new VBox();
        Button generate = new Button("Generate");
        TextArea textArea = new TextArea();
        generate.setOnAction((event)->{
            // Generate the puzzle
            int maxRow = getMaxRow();
            String puzzle = "***PUZZLE***\n";
            for(int i=0; i<=maxRow; i++){
                for(int j=0; j<orbArray[i].length; j++){
                    if(orbArray[i][j]==NULL || orbArray[i][j]==EMPTY) puzzle += ' ';
                    else puzzle += orbArray[i][j].getOrbEnum().getSymbol();
                }
                puzzle += '\n';
            }

            // Generate the shooter orbs
            puzzle += "\n***SHOOTER_ORBS***\n";
            for(int playerIndex=0; playerIndex<numPlayers; playerIndex++){
                for(int i=ARRAY_HEIGHT+1+3*playerIndex; i<=ARRAY_HEIGHT+2+3*playerIndex; i++){
                    for(int j=i%2; j<orbArray[i].length; j+=2){
                        if(orbArray[i][j]!=EMPTY) puzzle += orbArray[i][j].getOrbEnum().getSymbol();
                    }
                }
                puzzle += '\n';
            }
            textArea.setText(puzzle);
        });
        vBox.getChildren().addAll(generate,textArea);
        hBox.getChildren().add(vBox);

        // Make everything scale correctly when the window is resized:
        rootNode.getTransforms().add(scaler);
        rootNode.heightProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("New height of PuzzleCreatorScene: " + newValue);
            double scaleValue = (double)newValue/(orbCanvas.getHeight());
            scaler.setX(scaleValue);
            scaler.setY(scaleValue);
        });

        // An AnimationTimer updates the display:
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (initializing) {
                    nextAnimationFrameInstance = now;
                    initializing = false;
                }

                orbDrawer.clearRect(0,0,orbCanvas.getWidth(),orbCanvas.getHeight());
                if (now > nextAnimationFrameInstance) {
                    // paint Array orbs:
                    for(int i=0; i<orbArray.length; ++i){
                        for(int j=0; j<orbArray[i].length; j++){
                            if(orbArray[i][j] == EMPTY) orbDrawer.strokeOval(ORB_RADIUS*j,ROW_HEIGHT*i,2*ORB_RADIUS, 2*ORB_RADIUS);
                            else orbArray[i][j].drawSelf(orbDrawer, 0);
                        }
                    }
                    nextAnimationFrameInstance += 1000000000L/ANIMATION_FRAME_RATE;
                }
            }
        };
        animationTimer.start();

    }

    private int getMaxRow() {
        int maxRow = 0;
        for (int i = 0; i < ARRAY_HEIGHT; i++) {
            for (int j = 0; j < orbArray[i].length; j++) {
                if(orbArray[i][j] != NULL && orbArray[i][j] != EMPTY) {
                    maxRow = i;
                    break;
                }
            }
        }
        System.out.println("max row is " + maxRow);
        return maxRow;
    }

    void cleanUp(){
        animationTimer.stop();
    }
}
