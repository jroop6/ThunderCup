package Classes.Images;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public enum Drawing {

    /* BUTTONS */
    TUTORIAL("res/images/buttons/TUTORIAL_MODE.png"),
    TUTORIAL_SEL("res/images/buttons/TUTORIAL_MODE_SEL.png"),
    ADVENTURE("res/images/buttons/ADVENTURE_MODE.png"),
    ADVENTURE_SEL("res/images/buttons/ADVENTURE_MODE_SEL.png"),
    PUZZLE("res/images/buttons/PUZZLE_MODE.png"),
    PUZZLE_SEL("res/images/buttons/PUZZLE_MODE_SEL.png"),
    RANDOM_PUZZLE("res/images/buttons/RANDOM_PUZZLE_MODE.png"),
    RANDOM_PUZZLE_SEL("res/images/buttons/RANDOM_PUZZLE_MODE_SEL.png"),
    MULTIPLAYER("res/images/buttons/TWO_PLAYER_MODE.png"),
    MULTIPLAYER_SEL("res/images/buttons/TWO_PLAYER_MODE_SEL.png"),
    VS_COMPUTER("res/images/buttons/VS_COMPUTER_MODE.png"),
    VS_COMPUTER_SEL("res/images/buttons/VS_COMPUTER_MODE_SEL.png"),
    EXIT("res/images/buttons/EXIT_GAME.png"),
    EXIT_SEL("res/images/buttons/EXIT_GAME_SEL.png"),
    MUTE("res/images/buttons/MUTE.png"),
    MUTE_SEL("res/images/buttons/MUTE_SEL.png"),
    MUTED("res/images/buttons/MUTE_MARKED.png"),
    MUTED_SEL("res/images/buttons/MUTE_SEL_MARKED.png"),
    SCROLL("res/images/buttons/scrollIcon.png"),
    SCROLL_SEL("res/images/buttons/scrollIcon_sel.png"),
    ADD_PLAYER("res/images/buttons/ADDPLAYER.png"),
    ADD_PLAYER_SEL("res/images/buttons/ADDPLAYER_SEL.png"),
    ADD_BOT("res/images/buttons/ADDBOT.png"),
    ADD_BOT_SEL("res/images/buttons/ADDBOT_SEL.png"),
    HOST_AND_PORT("res/images/buttons/HOST_AND_PORT.png"),
    HOST_AND_PORT_SEL("res/images/buttons/HOST_AND_PORT_SEL.png"),
    START("res/images/buttons/START.png"),
    START_SEL("res/images/buttons/START_SEL.png"),
    REJECT("res/images/buttons/REJECT.png"),
    REJECT_SEL("res/images/buttons/REJECT.png"),
    REMOVE_PLAYER("res/images/buttons/REMOVE_PLAYER.png"),
    REMOVE_PLAYER_SEL("res/images/buttons/REMOVE_PLAYER_SEL.png"),
    USERNAME("res/images/buttons/USERNAME.png"),
    USERNAME_SEL("res/images/buttons/USERNAME_SEL.png"),
    RETURN_TO_MAIN_MENU_HOST("res/images/buttons/RETURN_TO_MAIN_MENU_HOST.png"),
    RETURN_TO_MAIN_MENU_HOST_SEL("res/images/buttons/RETURN_TO_MAIN_MENU_HOST_SEL.png"),
    RETURN_TO_MAIN_MENU_CLIENT("res/images/buttons/RETURN_TO_MAIN_MENU_CLIENT.png"),
    RETURN_TO_MAIN_MENU_CLIENT_SEL("res/images/buttons/RETURN_TO_MAIN_MENU_CLIENT_SEL.png"),
    CREDITS("res/images/buttons/CREDITS.png"),
    CREDITS_SEL("res/images/buttons/CREDITS_SEL.png"),
    UNPAUSE("res/images/buttons/UNPAUSE.png"),
    UNPAUSE_SEL("res/images/buttons/UNPAUSE_SEL.png"),
    RESIGN("res/images/buttons/RESIGN.png"),
    RESIGN_SEL("res/images/buttons/RESIGN_SEL.png"),
    PUZZLE_SET_1("res/images/buttons/PUZZLE_SET_1.png"),
    PUZZLE_SET_1_SEL("res/images/buttons/PUZZLE_SET_1_SEL.png"),
    PUZZLE_SET_2("res/images/buttons/PUZZLE_SET_2.png"),
    PUZZLE_SET_2_SEL("res/images/buttons/PUZZLE_SET_2_SEL.png"),

    /* SPACERS AND BORDERS */
    SCROLL_SPACER("res/images/misc/ScrollBarSpacer.png"),
    PLAYPANEL_SPACER("res/images/misc/PlayPanelSpacer.png"),
    PLAYPANEL_LIVE_BOUNDARY("res/images/misc/PlayPanelLiveBoundary.png"),
    //PLAYPANEL_SEPARATOR_1("res/images/misc/PlayPanelSeparator1.png"),
    PLAYERSLOT_SPACER("res/images/backgrounds/playerSlotSpacer.png"),
    PLAYPANEL_NIGHTSKY_SEPARATOR("res/images/backgrounds/PlayPanelNightSkySeparator.png"),

    /* BACKGROUND IMAGES*/
    SKY("res/images/backgrounds/gameChoiceBackground.png"),
    NIGHT_SKY("res/images/backgrounds/nightSky.png"),
    NIGHT_SKY_CLOUDS("res/images/backgrounds/nightSkyMidgroundClouds.png"),
    FOREGROUND_CLOUD_1("res/images/backgrounds/foregroundCloud1.png"),
    FOREGROUND_CLOUD_2("res/images/backgrounds/foregroundCloud2.png"),
    STATIONARY_CLOUD_1("res/images/backgrounds/stationaryCloud1.png"),
    STATIONARY_CLOUD_2("res/images/backgrounds/stationaryCloud2.png"),
    ADD_PLAYER_BACKROUND("res/images/backgrounds/AddPlayerBackground.png"),
    PLAYER_SLOT_BACKGROUND("res/images/backgrounds/playerSlotBackground.png"),
    PLAYER_SLOT_FOREGROUND("res/images/backgrounds/playerSlotForeground.png"),
    FAR_CLOUD_1("res/images/backgrounds/farCloud1.png"),
    FAR_CLOUD_2("res/images/backgrounds/farCloud2.png"),
    FAR_CLOUD_3("res/images/backgrounds/farCloud3.png"),
    FAR_CLOUD_WITH_BUBBLES_1("res/images/backgrounds/farCloudWithBubbles1.png"),
    FAR_CLOUD_WITH_BUBBLES_2("res/images/backgrounds/farCloudWithBubbles2.png"),
    NEAR_CLOUD_1("res/images/backgrounds/nearCloud1.png"),
    DAY_SKY("res/images/backgrounds/daySky.png"),
    PLAYPANEL_NIGHTSKY_DROPCLOUD("res/images/backgrounds/PlayPanelNightSkyDropCloud.png"),
    PLAYPANEL_NIGHTSKY_BACKGROUND_CLOUDS("res/images/backgrounds/PlayPanelNightSkyBackgroundClouds.png"),
    PLAYPANEL_NIGHTSKY_FOREGROUND_CLOUDS("res/images/backgrounds/PlayPanelNightSkyForegroundClouds.png"),

    /* OTHER UI IMAGES */
    CHATBOX_SCROLLPANE_BACKGROUND("res/images/backgrounds/chatBoxScrollPaneBackground.png"),
    CHATBOX_TEXTFIELD_BACKGROUND("res/images/backgrounds/chatBoxTextFieldBackground.png"),
    MSS_BUTTONS_BACKDROP("res/images/backgrounds/MSSButtonsBackground.png"),
    PAUSE_MENU_BACKDROP("res/images/backgrounds/GamePaused.png");

    private Image image;

    Drawing(String url){
        image = new Image(url);
    }

    public Image getImage(){
        return image;
    }

    public ImageView getImageView(){
        ImageView newImageView = new ImageView(image);
        newImageView.setPreserveRatio(true);
        return newImageView;
    }
}
