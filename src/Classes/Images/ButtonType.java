package Classes.Images;

import javafx.scene.image.ImageView;

/**
 * Created by HydrusBeta on 8/1/2017.
 */
public enum ButtonType {
    PUZZLE(Drawing.PUZZLE, Drawing.PUZZLE_SEL),
    TUTORIAL(Drawing.TUTORIAL, Drawing.TUTORIAL_SEL),
    ADVENTURE(Drawing.ADVENTURE, Drawing.ADVENTURE_SEL),
    RANDOM_PUZZLE(Drawing.RANDOM_PUZZLE, Drawing.RANDOM_PUZZLE_SEL),
    MULTIPLAYER(Drawing.MULTIPLAYER, Drawing.MULTIPLAYER_SEL),
    VS_COMPUTER(Drawing.VS_COMPUTER, Drawing.VS_COMPUTER_SEL),
    EXIT(Drawing.EXIT, Drawing.EXIT_SEL),
    MUTE(Drawing.MUTE, Drawing.MUTE_SEL),
    MUTED(Drawing.MUTED, Drawing.MUTED_SEL),
    SCROLL(Drawing.SCROLL, Drawing.SCROLL_SEL),
    ADD_PLAYER(Drawing.ADD_PLAYER, Drawing.ADD_PLAYER_SEL),
    ADD_BOT(Drawing.ADD_BOT, Drawing.ADD_BOT_SEL),
    HOST_AND_PORT(Drawing.HOST_AND_PORT, Drawing.HOST_AND_PORT_SEL),
    START(Drawing.START, Drawing.START_SEL),
    REJECT(Drawing.REJECT, Drawing.REJECT_SEL),
    REMOVE_PLAYER(Drawing.REMOVE_PLAYER, Drawing.REMOVE_PLAYER_SEL),
    USERNAME(Drawing.USERNAME, Drawing.USERNAME_SEL),
    RETURN_TO_MAIN_MENU_HOST(Drawing.RETURN_TO_MAIN_MENU_HOST, Drawing.RETURN_TO_MAIN_MENU_HOST_SEL),
    RETURN_TO_MAIN_MENU_CLIENT(Drawing.RETURN_TO_MAIN_MENU_CLIENT, Drawing.RETURN_TO_MAIN_MENU_CLIENT_SEL),
    CREDITS(Drawing.CREDITS, Drawing.CREDITS_SEL),
    UNPAUSE(Drawing.UNPAUSE, Drawing.UNPAUSE_SEL),
    RESIGN(Drawing.RESIGN, Drawing.RESIGN_SEL),
    PUZZLE_SET_1(Drawing.PUZZLE_SET_1, Drawing.PUZZLE_SET_1_SEL),
    PUZZLE_SET_2(Drawing.PUZZLE_SET_2, Drawing.PUZZLE_SET_2_SEL);

    private Drawing unselectedDrawing;
    private Drawing selectedDrawing;

    ButtonType(Drawing unselectedDrawing, Drawing selectedDrawing){
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
