package Classes.Animation;

import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;

//todo: setScale() only affects the data but not the view (with GraphicsContextSprite).
//todo: need to implement ImageViewSprite
public class AnimationManager {
    AnimationData animationData;
    PlayOption currentPlayOption = PlayOption.LOOP;
    Status currentStatus = Status.PLAYING;
    AnimationView animationView;

    public AnimationManager(Animation animation, GraphicsContext gc){
        animationData = new AnimationData(animation);
        animationView = new GraphicsContextSprite(gc);
    }
    public AnimationManager(Animation animation, ImageView imageView){
        animationData = new AnimationData(animation);
        animationView = new ImageViewSprite(imageView);
    }

    void setAnimation(Animation newAnimation){
        animationData.setAnimation(newAnimation);
    }

    enum PlayOption{LOOP, PLAY_ONCE_THEN_VANISH, PLAY_ONCE_THEN_PAUSE, REVERSE}
    void setPlayOption(PlayOption playOption){
        currentPlayOption = playOption;
    }

    enum Status{PLAYING, PAUSED}
    void setStatus(Status status){
        currentStatus = status;
    }

    void setVisibility(AnimationData.Visibility visibility){
        animationData.setVisibility(visibility);
    }

    void setScale(double newScale){
        animationData.setScale(newScale);
        animationView.setScale(newScale);
    }

    // This method should only ever be called by the JavaFX thread.
    void updateView(){
        if (!Platform.isFxApplicationThread()){
            System.err.println("ERROR! A non-JavaFX thread is attempting to call AnimationManager.updateView(). " +
                    "That method updates UI components, so it must be called only from the JavaFX Application thread.");
        }
        animationView.drawFrame(animationData);
    }

    // return true if the currentAnimation is finishing.
    boolean tick(){
        boolean finished = false;
        switch(currentStatus){
            case PLAYING:
                switch(currentPlayOption){
                    case LOOP:
                    case PLAY_ONCE_THEN_VANISH:
                    case PLAY_ONCE_THEN_PAUSE:
                        finished = animationData.incrementFrame();
                        break;
                    case REVERSE:
                        finished = animationData.decrementFrame();
                }
                break;
            case PAUSED:
                break;
        }

        if(finished){
            switch (currentPlayOption){
                case LOOP:
                    animationData.setFrame(0);
                    break;
                case PLAY_ONCE_THEN_VANISH:
                    setVisibility(AnimationData.Visibility.INVISIBLE);
                    animationData.setFrame(0); // todo: should I pause the animation here? (such a status change would be nonobvious to the user...).
                    break;
                case PLAY_ONCE_THEN_PAUSE:
                    setStatus(Status.PAUSED);
                    break;
                case REVERSE:
                    animationData.setFrame(animationData.currentAnimation.getSpriteSheet().getMaxFrameIndex());
                    break;
            }
        }

        return finished;
    }
}
