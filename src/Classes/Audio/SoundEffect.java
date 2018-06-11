package Classes.Audio;

import javafx.scene.media.MediaPlayer;

public interface SoundEffect {

    // should return a new, unique instance of media player so that the same sound effect can be played multiple times simultaneously.
    MediaPlayer getMediaPlayer();
}
