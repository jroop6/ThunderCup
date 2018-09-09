package Classes;

import Classes.Audio.SoundManager;
import Classes.Images.ButtonType;
import Classes.Images.PuzzleSet;
import Classes.Images.StaticBgImages;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static javafx.scene.layout.AnchorPane.*;

public class PuzzleSelectionScene extends Scene {

    public PuzzleSelectionScene(){
        super(new VBox());
        VBox rootNode = (VBox)getRoot();

        // Get a background for the PlayerSlot container:
        ImageView scrollableViewBackground = StaticBgImages.DAY_SKY.getImageView();

        // Create a ScrollableView and place big Buttons in it:
        ScrollableView<Button> puzzleButtonContainer = new ScrollableView<>(scrollableViewBackground,new Rectangle(0.0,0.0,Color.TRANSPARENT));
        rootNode.getChildren().add(puzzleButtonContainer);
        for(PuzzleSet puzzleSet : PuzzleSet.values()){
            ThunderButton btn = new ThunderButton(puzzleSet.getButtonType(), (event)->SceneManager.switchToPuzzleMode(puzzleSet.getPuzzleGroupIndex()));
            puzzleButtonContainer.addItem(btn);
        }

        // There are buttons directly beneath the ScrollPanel which allow the player to either return to the main menu or mute music.
        AnchorPane buttonHolder = new AnchorPane();
        buttonHolder.setBackground(new Background(new BackgroundImage(StaticBgImages.MSS_BUTTONS_BACKDROP.getImageView().getImage(),null,null,null,null)));
        buttonHolder.setPickOnBounds(false);
        HBox leftSideButtonsHolder = new HBox();
        leftSideButtonsHolder.setPickOnBounds(false);
        ThunderButton returnToMainMenu = new ThunderButton(ButtonType.RETURN_TO_MAIN_MENU_HOST, (event)->{
            System.out.println("pressed Exit!");
            SceneManager.switchToMainMenu();
        });
        returnToMainMenu.setAlignment(Pos.CENTER_LEFT);
        leftSideButtonsHolder.getChildren().add(returnToMainMenu);
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
        buttonHolder.getChildren().addAll(leftSideButtonsHolder, muteButton);
        setLeftAnchor(leftSideButtonsHolder, 0.0);
        setRightAnchor(muteButton,0.0);
        buttonHolder.setBorder(new Border(new BorderStroke(Color.BLACK,BorderStrokeStyle.SOLID,new CornerRadii(0.0),new BorderWidths(2.0))));
        rootNode.getChildren().add(buttonHolder);
    }
}
