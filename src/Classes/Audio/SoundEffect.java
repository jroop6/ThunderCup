package Classes.Audio;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URISyntaxException;
import java.net.URL;

public enum SoundEffect{

    THUNDERCLAP("res/sound/soundEffects/ThunderClap.wav"),
    CHINK("res/sound/soundEffects/chink.wav"),
    MAGIC_TINKLE("res/sound/soundEffects/magicTinkle.wav"),
    PLACEMENT("res/sound/soundEffects/placement.wav"),
    DROP("res/sound/soundEffects/drop.wav"),
    EXPLOSION("res/sound/soundEffects/explosion.wav"),
    ALMOST_NEW_ROW("res/sound/soundEffects/AlmostNewRow.wav"),
    NEW_ROW("res/sound/soundEffects/NewRow.wav"),
    VICTORY_FLOURISH("res/sound/soundEffects/victoryFlourish.wav");

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
