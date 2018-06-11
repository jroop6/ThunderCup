package Classes.Audio;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URISyntaxException;

public enum OrbThunder implements SoundEffect{

    THUNDER_1("res/sound/soundEffects/RollingThunder1.aiff");

    private String url;

    OrbThunder(String url){
        this.url = url;
    }

    @Override
    public MediaPlayer getMediaPlayer(){
        MediaPlayer mediaPlayer = null;
        try {
            String path = getClass().getClassLoader().getResource(url).toURI().toString();
            mediaPlayer = new MediaPlayer(new Media(path));
        } catch(URISyntaxException e){
            e.printStackTrace();
        }
        return mediaPlayer;
    }

}
