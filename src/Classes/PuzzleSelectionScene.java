package Classes;

import Classes.Audio.SoundManager;
import Classes.Images.ButtonImages;
import Classes.Images.PuzzleSetSelectorImages;
import Classes.Images.StaticBgImages;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static javafx.scene.layout.AnchorPane.setLeftAnchor;

public class PuzzleSelectionScene extends Scene {

    public PuzzleSelectionScene(){
        super(new VBox());
        VBox rootNode = (VBox)getRoot();

        // Get a background for the PlayerSlot container:
        ImageView scrollableViewBackground = StaticBgImages.DAY_SKY.getImageView();

        // Create a ScrollableView and place big Buttons in it:
        ScrollableView<Button> puzzleButtonContainer = new ScrollableView<>(scrollableViewBackground,new Rectangle(0.0,0.0,Color.TRANSPARENT));
        rootNode.getChildren().add(puzzleButtonContainer);
        for(PuzzleSetSelectorImages puzzleSet : PuzzleSetSelectorImages.values()){
            Button btn = createGameChoiceButton(puzzleSet);
            puzzleButtonContainer.addItem(btn);
        }

        // There are buttons directly beneath the ScrollPanel which allow the player to either return to the main menu or mute music.
        AnchorPane buttonHolder = new AnchorPane();
        buttonHolder.setBackground(new Background(new BackgroundImage(StaticBgImages.MSS_BUTTONS_BACKGROUND.getImageView().getImage(),null,null,null,null)));
        buttonHolder.setPickOnBounds(false);
        HBox leftSideButtonsHolder = new HBox();
        leftSideButtonsHolder.setPickOnBounds(false);
        Button returnToMainMenu = createButton(ButtonImages.RETURN_TO_MAIN_MENU_HOST);
        Button mute = createButton(ButtonImages.MUTE);
        leftSideButtonsHolder.getChildren().addAll(returnToMainMenu, mute);
        buttonHolder.getChildren().add(leftSideButtonsHolder);
        setLeftAnchor(leftSideButtonsHolder, 0.0);
        buttonHolder.setBorder(new Border(new BorderStroke(Color.BLACK,BorderStrokeStyle.SOLID,new CornerRadii(0.0),new BorderWidths(2.0))));
        rootNode.getChildren().add(buttonHolder);

    }


    private Button createGameChoiceButton(PuzzleSetSelectorImages puzzleSetSelectorImage) {
        Button btn = new Button();
        ImageView unselectedImage = puzzleSetSelectorImage.getUnselectedImage();
        ImageView selectedImage = puzzleSetSelectorImage.getSelectedImage();
        btn.setGraphic(unselectedImage);
        btn.setBackground(null);
        btn.setPadding(Insets.EMPTY);

        // choose appropriate action when the button is clicked:
        btn.setOnAction((event) -> SceneManager.switchToPuzzleMode(puzzleSetSelectorImage.getPuzzleGroupIndex()));

        // Change the button graphic when it is hovered over:
        btn.addEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> btn.setGraphic(selectedImage));
        btn.addEventHandler(MouseEvent.MOUSE_EXITED, (event) -> btn.setGraphic(unselectedImage));

        return btn;
    }

    private Button createButton(ButtonImages buttonEnum){
        Button btn = new Button();
        ImageView unselectedImage = buttonEnum.getUnselectedImage();
        ImageView selectedImage = buttonEnum.getSelectedImage();
        if(buttonEnum==ButtonImages.MUTE && SoundManager.isMuted()){ // The mute button's graphic is affected by whether the music is muted
            btn.setGraphic(ButtonImages.MUTED.getUnselectedImage());
        }
        else btn.setGraphic(unselectedImage);
        btn.setBackground(null);
        btn.setPadding(Insets.EMPTY);


        // choose appropriate action when the button is clicked:
        btn.setOnAction((event) -> {
            switch (buttonEnum) {
                case RETURN_TO_MAIN_MENU_HOST:
                    System.out.println("pressed Exit!");
                    SceneManager.switchToMainMenu();
                    break;
                case MUTE:
                    if(SoundManager.isMuted()){
                        SoundManager.unMuteMusic();
                        btn.setGraphic(selectedImage);
                    }
                    else{
                        SoundManager.muteMusic();
                        btn.setGraphic(ButtonImages.MUTED.getSelectedImage());
                    }
                    break;
            }
        });

        // Change the button graphic when it is hovered over:
        btn.addEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> btn.setGraphic(selectedImage));
        btn.addEventHandler(MouseEvent.MOUSE_EXITED, (event) -> btn.setGraphic(unselectedImage));

        return btn;
    }
}
