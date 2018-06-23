package Classes.Images;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Created by HydrusBeta on 8/1/2017.
 */
public enum StaticBgImages {
    SKY("res/images/backgrounds/gameChoiceBackground.png"),
    NIGHT_SKY("res/images/backgrounds/nightSky.png"),
    NIGHT_SKY_CLOUDS("res/images/backgrounds/nightSkyMidgroundClouds.png"),
    CANNON_STATIONARY_PART("res/images/backgrounds/cannon_stationary_part.png"),
    FOREGROUND_CLOUD_1("res/images/backgrounds/foregroundCloud1.png"),
    FOREGROUND_CLOUD_2("res/images/backgrounds/foregroundCloud2.png"),
    CHARACTER_1("res/images/backgrounds/static character1.png"),
    CHARACTER_2("res/images/backgrounds/static character2.png"),
    STATIONARY_CANNON_PARTS_1("res/images/backgrounds/stationaryCannonParts1.png"),
    STATIONARY_CANNON_PARTS_2("res/images/backgrounds/stationaryCannonParts2.png"),
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
    PLAYERSLOT_SPACER("res/images/backgrounds/playerSlotSpacer.png"),
    DAY_SKY("res/images/backgrounds/daySky.png"),
    CHATBOX_SCROLLPANE_BACKGROUND("res/images/backgrounds/chatBoxScrollPaneBackground.png"),
    CHATBOX_TEXTFIELD_BACKGROUND("res/images/backgrounds/chatBoxTextFieldBackground.png"),
    MSS_BUTTONS_BACKGROUND("res/images/backgrounds/MSSButtonsBackground.png"),
    PLAYPANEL_NIGHTSKY_DROPCLOUD("res/images/backgrounds/PlayPanelNightSkyDropCloud.png"),
    PLAYPANEL_NIGHTSKY_BACKGROUND_CLOUDS("res/images/backgrounds/PlayPanelNightSkyBackgroundClouds.png"),
    PLAYPANEL_NIGHTSKY_FOREGROUND_CLOUDS("res/images/backgrounds/PlayPanelNightSkyForegroundClouds.png"),
    PLAYPANEL_NIGHTSKY_SEPARATOR("res/images/backgrounds/PlayPanelNightSkySeparator.png"),
    GAME_PAUSED("res/images/backgrounds/GamePaused.png"),
    PUZZLE_COMPLETE("res/images/backgrounds/GamePaused.png"); // todo: update this last one.

    Image image;

    StaticBgImages(String url){
        image = new Image(url);
    }

    public ImageView getImageView(){
        ImageView newImageView = new ImageView(image);
        newImageView.setPreserveRatio(true);
        return newImageView;
    }

    public double getHeight(){
        return image.getHeight();
    }
}
