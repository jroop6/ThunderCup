package Classes.Images;

import javafx.scene.image.ImageView;

/**
 * Created by HydrusBeta on 8/1/2017.
 */
public enum ButtonImages {
    TUTORIAL("res/images/buttons/TUTORIAL_MODE.png","res/images/buttons/TUTORIAL_MODE_SEL.png"),
    ADVENTURE("res/images/buttons/ADVENTURE_MODE.png","res/images/buttons/ADVENTURE_MODE_SEL.png"),
    PUZZLE("res/images/buttons/PUZZLE_MODE.png","res/images/buttons/PUZZLE_MODE_SEL.png"),
    RANDOM_PUZZLE("res/images/buttons/RANDOM_PUZZLE_MODE.png","res/images/buttons/RANDOM_PUZZLE_MODE_SEL.png"),
    MULTIPLAYER("res/images/buttons/TWO_PLAYER_MODE.png","res/images/buttons/TWO_PLAYER_MODE_SEL.png"),
    VS_COMPUTER("res/images/buttons/VS_COMPUTER_MODE.png","res/images/buttons/VS_COMPUTER_MODE_SEL.png"),
    EXIT("res/images/buttons/EXIT_GAME.png","res/images/buttons/EXIT_GAME_SEL.png"),
    MUTE("res/images/buttons/MUTE.png","res/images/buttons/MUTE_SEL.png"),
    MUTED("res/images/buttons/MUTE_MARKED.png","res/images/buttons/MUTE_SEL_MARKED.png"),
    SCROLL("res/images/buttons/scrollIcon.png","res/images/buttons/scrollIcon_sel.png"),
    ADD_PLAYER("res/images/buttons/ADDPLAYER.png","res/images/buttons/ADDPLAYER_SEL.png"),
    ADD_BOT("res/images/buttons/ADDBOT.png","res/images/buttons/ADDBOT_SEL.png"),
    HOST_AND_PORT("res/images/buttons/HOST_AND_PORT.png","res/images/buttons/HOST_AND_PORT_SEL.png"),
    START("res/images/buttons/START.png","res/images/buttons/START_SEL.png"),
    REJECT("res/images/buttons/REJECT.png","res/images/buttons/REJECT.png"),
    REMOVE_PLAYER("res/images/buttons/REMOVE_PLAYER.png","res/images/buttons/REMOVE_PLAYER_SEL.png"),
    USERNAME("res/images/buttons/USERNAME.png","res/images/buttons/USERNAME_SEL.png"),
    RETURN_TO_MAIN_MENU_HOST("res/images/buttons/RETURN_TO_MAIN_MENU_HOST.png","res/images/buttons/RETURN_TO_MAIN_MENU_HOST_SEL.png"),
    RETURN_TO_MAIN_MENU_CLIENT("res/images/buttons/RETURN_TO_MAIN_MENU_CLIENT.png","res/images/buttons/RETURN_TO_MAIN_MENU_CLIENT_SEL.png"),
    CREDITS("res/images/buttons/CREDITS.png","res/images/buttons/CREDITS_SEL.png"),
    UNPAUSE("res/images/buttons/UNPAUSE.png","res/images/buttons/UNPAUSE_SEL.png"),
    RESIGN("res/images/buttons/RESIGN.png","res/images/buttons/RESIGN_SEL.png");

    private String unselectedImageUrl;
    private String selectedImageUrl;

    ButtonImages(String unselectedImageUrl, String selectedImageUrl){
        this.unselectedImageUrl = unselectedImageUrl;
        this.selectedImageUrl = selectedImageUrl;
    }

    public ImageView getUnselectedImage(){
        ImageView newImageView = new ImageView(unselectedImageUrl);
        newImageView.setPreserveRatio(true);
        return newImageView;
    }

    public ImageView getSelectedImage(){
        ImageView newImageView = new ImageView(selectedImageUrl);
        newImageView.setPreserveRatio(true);
        return newImageView;
    }
}
