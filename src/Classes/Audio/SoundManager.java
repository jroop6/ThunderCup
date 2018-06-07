package Classes.Audio;

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

    //TODO: actually, this should play a random next song when one ends.
    //TODO: handle audio files that did not load for some reason
    public static void playRandomSongs(){
        if(muted) return;
        stopMusic(); // stop any music that's already playing
        Music randomSong;
        do{
            randomSong = Music.values()[rand.nextInt(Music.values().length)];
        } while (!randomSong.isRandomBgMusic());
        currentMusic = randomSong;
        currentMusic.getMediaPlayer().play();
    }

    public static void playSong(Music song){
        stopMusic(); // stop any music that's already playing
        currentMusic = song;
        if(!muted) currentMusic.getMediaPlayer().play();
    }

    public static void stopMusic(){
        if(currentMusic != null){
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