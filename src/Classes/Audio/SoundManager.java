package Classes.Audio;

import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Jonathan D. Roop on 7/21/2017.
 * Manages the playing of sound effects and music
 * Everything in here is static in order to make accessing the SoundManager from anywhere easy; no instance of
 * SoundManager is needed to call its member functions. (I figure that audio is a very global feature of most programs
 * anyways because you only have 1 set of speakers and you aren't going to be instantiating multiple SoundManagers on
 * the same computer).
 *
 * Sounds are the accessed using the Music and SoundEffect enumerations. This makes it easy to identify a particular
 * song or sound, because useful enum names can be used instead of cryptic indices, like playSong(Music.GO_TAKE_FLIGHT)
 * instead of playSong(4). Songs and sounds can also be rearranged within their enumerations and an unused sound
 * can be removed from an enumeration without breaking code.
 */
public class SoundManager {
    private static double MUSIC_VOLUME = 0.5;
    private static double SOUND_EFFECTS_VOLUME  = 1.0;

    // Random number generator used for selecting random music:
    private static Random rand = new Random();

    // variables used in muting and resuming sound:
    private static boolean muted = false;
    private static Music currentMusic;
    private static List<MediaPlayer> currentSoundEffects = new LinkedList<>();

    // A cached instance of the Victory sound effect

    //TODO: handle audio files that did not load for some reason
    // Play any random background music except the one specified in the argument.
    private static void playRandomSongs(Music notThisSong){
        if(muted) return;
        Music randomSong;
        do{
            randomSong = Music.values()[rand.nextInt(Music.values().length)];
        } while (!randomSong.isRandomBgMusic() || randomSong==notThisSong);
        playSong(randomSong, false);

        // loop random songs
        final Music randomSongCopy = randomSong; // need a final instance for the lambda expression.
        randomSong.getMediaPlayer().setOnEndOfMedia(()-> SoundManager.playRandomSongs(randomSongCopy));
    }

    // Overloaded method for playing any random song:
    public static void playRandomSongs(){
        playRandomSongs(null);
    }

    public static void playSong(Music song, boolean loop){
        if (currentMusic == song && Math.abs(currentMusic.getMediaPlayer().getVolume()-MUSIC_VOLUME)<0.001){
            return; // don't restart a song that's already playing
        }
        silenceMusic(); // stop any other music that's already playing
        currentMusic = song;
        if(!muted){
            currentMusic.getMediaPlayer().seek(Duration.ZERO); // go back to the beginning of the song, in case we're not already there.
            currentMusic.getMediaPlayer().setVolume(MUSIC_VOLUME);
            currentMusic.getMediaPlayer().play();
        }

        // loop the song
        if(loop){
            final Music songCopy = song; // need a final instance for the lambda expression.
            songCopy.getMediaPlayer().setOnEndOfMedia(()-> {
                songCopy.getMediaPlayer().seek(Duration.ZERO);
                songCopy.getMediaPlayer().play();
            });
        }
    }

    public static MediaPlayer playSoundEffect(SoundEffect soundEffect){
        MediaPlayer newSoundEffect = soundEffect.getMediaPlayer();
        if(muted) return null;
        currentSoundEffects.add(newSoundEffect);
        newSoundEffect.setOnEndOfMedia(()->{
            currentSoundEffects.remove(newSoundEffect);
        });
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
            if(!loopForever && System.currentTimeMillis()-startTime > 60000){
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
        mediaPlayerToStop.setVolume(0.0);
        currentSoundEffects.remove(mediaPlayerToStop);
    }

    public static void stopMusic(){
        if(currentMusic != null){
            currentMusic.getMediaPlayer().setOnEndOfMedia(null); // remove any eventHandler
            currentMusic.getMediaPlayer().stop();
        }
    }

    // A quicker way to "stop" the music. It doesn't actually stop playing, but it's volume is set to 0. If the track is
    // ever played again, playSong() will set the volume back to MUSIC_VOLUME.
    public static void silenceMusic(){
        if(currentMusic != null){
            currentMusic.getMediaPlayer().setVolume(0.0);
            currentMusic.getMediaPlayer().setOnEndOfMedia(null); //
        }
    }

    // Sets the volume of all current sound effects to zero. The sound effects are not removed from the
    // currentSoundEffects list right away, but will eventually be removed via their setOnEndOfMedia listeners (see
    // playSoundEffect()). Note: the original solution used soundEffect.stop() on all sounds followed by
    // currentSoundEffects.clear(), but this caused a few frames to be dropped (I guess stopping a MediaPlayer is a lot
    // of work?).
    public static void silenceAllSoundEffects(){
        for(MediaPlayer soundEffect : currentSoundEffects){
            soundEffect.setVolume(0.0);
        }
    }

    public static void muteMusic(){
        currentMusic.getMediaPlayer().pause();
        muted = true;
    }

    public static void unMuteMusic(){
        currentMusic.getMediaPlayer().setVolume(MUSIC_VOLUME);
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