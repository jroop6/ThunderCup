package Classes.Audio;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URISyntaxException;
import java.net.URL;

public enum SoundEffect{

    THUNDER_1("res/sound/soundEffects/RollingThunder1.aiff"),
    CHINK("res/sound/soundEffects/chink.wav"),
    PLACEMENT("res/sound/soundEffects/placement.wav");

    private String name;

    SoundEffect(String name){
        this.name = name;
    }

    // returns a new, unique instance of MediaPlayer so that many sounds can be played simultaneously.
    public MediaPlayer getMediaPlayer(){
        MediaPlayer mediaPlayer = null;
        try {
            URL url = getClass().getClassLoader().getResource(name);
            if(url==null){
                System.err.println("Could not locate sound effect resource: " + name);
                return null;
            }
            String path = url.toURI().toString();
            mediaPlayer = new MediaPlayer(new Media(path));
        } catch(URISyntaxException e){
            e.printStackTrace();
        }
        return mediaPlayer;
    }

}
