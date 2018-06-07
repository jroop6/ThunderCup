package Classes.Animation;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

/**
 * Deprecated, because Java no longer supports .flv videos, and I need to play something with an alpha channel.
 * An enumeration containing all miscellaneous animations. A single instance of Media is initialized for each resource,
 * and multiple MediaViews (each with their own MediaPlayer) can be retrieved.
 */
public enum MiscAnimationsOld {
    TITLE("res/animations/misc/titleSpriteSheet.png");

    private Media media;

    MiscAnimationsOld(String url){
        String urlWithScheme = getClass().getClassLoader().getResource(url).toExternalForm();
        media = new Media(urlWithScheme);
    }

    public MediaView getMediaView(){
        MediaView mediaView = new MediaView(new MediaPlayer(media));
        return mediaView;
    }
}
