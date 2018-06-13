package Classes.Audio;

import javafx.scene.media.MediaPlayer;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by HydrusBeta on 7/21/2017.
 * manages the playing of sound effects and music
 * Everything in here is static in order to make accessing the SoundManager from anywhere easy; no instance of
 * SoundManager is needed to call its member functions.
 *
 * Sounds are the accessed using the Music and Sound enumerations. This makes it easy to identify a particular song or
 * sound, because useful enum names can be used instead of cryptic indices, like playSong(Music.GO_TAKE_FLIGHT)
 * instead of playSong(4). Songs and sounds can also be rearranged within their enumerations and an unused sound
 * can be removed from an enumeration without breaking code.
 */
public class SoundManager {

    // Random number generator used for selecting random music:
    private static Random rand = new Random();

    // variables used in muting and resuming sound:
    private static boolean muted = false;
    private static Music currentMusic;
    private static List<MediaPlayer> currentSoundEffects = new LinkedList<>();

    //TODO: handle audio files that did not load for some reason
    // Play any random background music except the one specified in the argument.
    public static void playRandomSongs(Music notThisSong){
        if(muted) return;
        stopMusic(); // stop any music that's already playing
        Music randomSong;
        do{
            randomSong = Music.values()[rand.nextInt(Music.values().length)];
        } while (!randomSong.isRandomBgMusic() && randomSong!=notThisSong);
        currentMusic = randomSong;
        currentMusic.getMediaPlayer().setOnEndOfMedia(()-> SoundManager.playRandomSongs(currentMusic));
        currentMusic.getMediaPlayer().play();
    }

    // Overloaded method for playing any random song:
    public static void playRandomSongs(){
        playRandomSongs(null);
    }

    public static void playSong(Music song){
        stopMusic(); // stop any music that's already playing
        currentMusic = song;
        if(!muted) currentMusic.getMediaPlayer().play();
    }

    public static void playSoundEffect(SoundEffect soundEffect){
        MediaPlayer newSoundEffect = soundEffect.getMediaPlayer();
        if(newSoundEffect==null) return; // avoid null pointer exception if media couldn't be loaded.
        currentSoundEffects.add(newSoundEffect);
        newSoundEffect.setOnEndOfMedia(()->{
            currentSoundEffects.remove(newSoundEffect);
            System.out.println("removing sound effect");
        });
        System.out.println("playing sound effect");
        newSoundEffect.play();
    }

    public static void stopMusic(){
        if(currentMusic != null){
            currentMusic.getMediaPlayer().setOnEndOfMedia(null); // remove any eventHandler
            currentMusic.getMediaPlayer().stop();
        }
    }

    public static void muteMusic(){
        currentMusic.getMediaPlayer().pause();
        muted = true;
    }

    public static void unMuteMusic(){
        currentMusic.getMediaPlayer().play();
        muted = false;
    }

    public static boolean isMuted(){
        return muted;
    }
}