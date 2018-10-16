package Classes.Images;

import javafx.scene.image.ImageView;

/**
 * Created by HydrusBeta on 8/1/2017.
 */
public enum ButtonType {
    PUZZLE(DrawingName.PUZZLE, DrawingName.PUZZLE_SEL),
    TUTORIAL(DrawingName.TUTORIAL, DrawingName.TUTORIAL_SEL),
    ADVENTURE(DrawingName.ADVENTURE, DrawingName.ADVENTURE_SEL),
    RANDOM_PUZZLE(DrawingName.RANDOM_PUZZLE, DrawingName.RANDOM_PUZZLE_SEL),
    MULTIPLAYER(DrawingName.MULTIPLAYER, DrawingName.MULTIPLAYER_SEL),
    VS_COMPUTER(DrawingName.VS_COMPUTER, DrawingName.VS_COMPUTER_SEL),
    EXIT(DrawingName.EXIT, DrawingName.EXIT_SEL),
    MUTE(DrawingName.MUTE, DrawingName.MUTE_SEL),
    MUTED(DrawingName.MUTED, DrawingName.MUTED_SEL),
    SCROLL(DrawingName.SCROLL, DrawingName.SCROLL_SEL),
    ADD_PLAYER(DrawingName.ADD_PLAYER, DrawingName.ADD_PLAYER_SEL),
    ADD_BOT(DrawingName.ADD_BOT, DrawingName.ADD_BOT_SEL),
    HOST_AND_PORT(DrawingName.HOST_AND_PORT, DrawingName.HOST_AND_PORT_SEL),
    START(DrawingName.START, DrawingName.START_SEL),
    REJECT(DrawingName.REJECT, DrawingName.REJECT_SEL),
    REMOVE_PLAYER(DrawingName.REMOVE_PLAYER, DrawingName.REMOVE_PLAYER_SEL),
    USERNAME(DrawingName.USERNAME, DrawingName.USERNAME_SEL),
    RETURN_TO_MAIN_MENU_HOST(DrawingName.RETURN_TO_MAIN_MENU_HOST, DrawingName.RETURN_TO_MAIN_MENU_HOST_SEL),
    RETURN_TO_MAIN_MENU_CLIENT(DrawingName.RETURN_TO_MAIN_MENU_CLIENT, DrawingName.RETURN_TO_MAIN_MENU_CLIENT_SEL),
    CREDITS(DrawingName.CREDITS, DrawingName.CREDITS_SEL),
    UNPAUSE(DrawingName.UNPAUSE, DrawingName.UNPAUSE_SEL),
    RESIGN(DrawingName.RESIGN, DrawingName.RESIGN_SEL),
    PUZZLE_SET_1(DrawingName.PUZZLE_SET_1, DrawingName.PUZZLE_SET_1_SEL),
    PUZZLE_SET_2(DrawingName.PUZZLE_SET_2, DrawingName.PUZZLE_SET_2_SEL);

    private DrawingName unselectedDrawing;
    private DrawingName selectedDrawing;

    ButtonType(DrawingName unselectedDrawing, DrawingName selectedDrawing){
        this.unselectedDrawing = unselectedDrawing;
        this.selectedDrawing = selectedDrawing;
    }

    public ImageView getUnselectedImageView(){
        ImageView newImageView = new ImageView(unselectedDrawing.getImage());
        newImageView.setPreserveRatio(true);
        return newImageView;
    }

    public ImageView getSelectedImageView(){
        ImageView newImageView = new ImageView(selectedDrawing.getImage());
        newImageView.setPreserveRatio(true);
        return newImageView;
    }
}
