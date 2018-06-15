package Classes.Audio;

import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Jonathan D. Roop on 7/21/2017.
 * manages the playing of sound effects and music
 * Everything in here is static in order to make accessing the SoundManager from anywhere easy; no instance of
 * SoundManager is needed to call its member functions.
 *
 * Sounds are the accessed using the Music and SoundEffect enumerations. This makes it easy to identify a particular
 * song or sound, because useful enum names can be used instead of cryptic indices, like playSong(Music.GO_TAKE_FLIGHT)
 * instead of playSong(4). Songs and sounds can also be rearranged within their enumerations and an unused sound
 * can be removed from an enumeration without breaking code.
 */
public class SoundManager {
    public static double MUSIC_VOLUME = 0.5;
    public static double SOUND_EFFECTS_VOLUME  = 1.0;

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
        Music randomSong;
        do{
            randomSong = Music.values()[rand.nextInt(Music.values().length)];
        } while (!randomSong.isRandomBgMusic() && randomSong!=notThisSong);
        playSong(randomSong);
        final Music randomSongCopy = randomSong; // need a final instance for the lambda expression.
        randomSong.getMediaPlayer().setOnEndOfMedia(()-> SoundManager.playRandomSongs(randomSongCopy));
    }

    // Overloaded method for playing any random song:
    public static void playRandomSongs(){
        playRandomSongs(null);
    }

    public static void playSong(Music song){
        stopMusic(); // stop any music that's already playing
        currentMusic = song;
        if(!muted){
            currentMusic.getMediaPlayer().setVolume(MUSIC_VOLUME);
            currentMusic.getMediaPlayer().play();
        }
    }

    public static MediaPlayer playSoundEffect(SoundEffect soundEffect){
        MediaPlayer newSoundEffect = soundEffect.getMediaPlayer();
        if(muted) return null;
        currentSoundEffects.add(newSoundEffect);
        newSoundEffect.setOnEndOfMedia(()->{
            currentSoundEffects.remove(newSoundEffect);
            System.out.println("removing sound effect");
        });
        System.out.println("playing sound effect");
        newSoundEffect.setVolume(SOUND_EFFECTS_VOLUME);
        newSoundEffect.play();
        return newSoundEffect;
    }

    // Note: The caller should use stopLoopingSoundEffect to stop the soundEffect. If stopLoopingSoundEffect is never
    // called, however, the loop is forcibly stopped after 1 minute unless the caller sets the loop forever flag.
    public static MediaPlayer loopSoundEffect(SoundEffect soundEffect, boolean loopForever){
        long startTime = System.currentTimeMillis();
        MediaPlayer mediaPlayer = playSoundEffect(soundEffect);
        if(mediaPlayer == null) return null;
        mediaPlayer.setOnEndOfMedia(()-> {
            System.out.println("media has ended. loopForever = " + loopForever + " passed time(millis) = " +  (System.currentTimeMillis()-startTime));
            if(!loopForever && System.currentTimeMillis()-startTime > 60000){
                System.err.println("IN HERE");
                stopLoopingSoundEffect(mediaPlayer);
            }
            else mediaPlayer.seek(Duration.ZERO);
        });
        return mediaPlayer;
    }

    // An overloaded method that sets the loop forever flag to false, purely for convenience.
    public static MediaPlayer loopSoundEffect(SoundEffect soundEffect){
        return loopSoundEffect(soundEffect, false);
    }

    public static void stopLoopingSoundEffect(MediaPlayer mediaPlayerToStop){
        if(mediaPlayerToStop==null) return;
        mediaPlayerToStop.stop();
        currentSoundEffects.remove(mediaPlayerToStop);
        System.out.println("removing looping sound effect");
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

    public static void setMusicVolume(double newVolume){
        MUSIC_VOLUME = newVolume;
        currentMusic.getMediaPlayer().setVolume(newVolume);
    }

    public static void setSoundEffectsVolume(double newVolume){
        SOUND_EFFECTS_VOLUME = newVolume;
        for(MediaPlayer soundEffect : currentSoundEffects){
            soundEffect.setVolume(newVolume);
        }
    }

}