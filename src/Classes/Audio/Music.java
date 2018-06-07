package Classes.Audio;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URISyntaxException;

/**
 * Holds the url of music files and some information about them.
 */
public enum Music {
    THE_PERFECT_STALLION("res/sound/music/The Perfect Stallion (Instrumental Remix).mp3",false),
    FLUTTERSHYS_LAMENT("res/sound/music/Fluttershy's Lament (Instrumental).mp3",true),
    BBBFF("res/sound/music/BBBFF (The Pony Way) (Instrumental).mp3",true),
    PONY_SHOULD_PONY_PONY("res/sound/music/Pony Should Pony Pony (Instrumental).mp3",true),
    GO_TAKE_FLIGHT("res/sound/music/Go Take Flight.mp3",false),
    CHANGELING("res/sound/music/Changeling (Instrumental).mp3",true);

    private String url;
    private boolean randomBgMusic; // indicates whether the song is OK to play as random background music during a game.
    private MediaPlayer mediaPlayer;

    Music(String url, boolean randomBgMusic){
        this.url = url;
        this.randomBgMusic = randomBgMusic;

        try {
            String path = getClass().getClassLoader().getResource(url).toURI().toString();
            mediaPlayer = new MediaPlayer(new Media(path));
        } catch(URISyntaxException e){
            e.printStackTrace();
        }
    }

    public boolean isRandomBgMusic(){
        return randomBgMusic;
    }

    public MediaPlayer getMediaPlayer(){
        return mediaPlayer;
    }
}
