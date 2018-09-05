package Classes.Animation;

import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;

/** Note: I don't believe this class is going to be used after all. Originally, the idea was that this class would be
 * the controller in a model-view-controller pattern. It would manage the tick() method and be responsible for updating
 * the AnimationView according to the information in AnimationData. Unfortunately, I realized at some point that the
 * data has to be completely decoupled from everything so that it could be copied and passed around between threads and
 * between the host and client. Hence, the AnimationManager would lose track of which AnimationData belonged to it.
 * Instead, everything has been moved into the AnimationData class. I made sure that the fields in AnimationData are
 * easily serializable and don't include any JavaFX nodes so that its serialization is unencumbered by the fact that it
 * is now responsible for drawing itself. AnimationData's drawSelf() method must be *passed* either a GraphicsContext or
 * an ImageView to draw itself upon.*/

//todo: setScale() only affects the data but not the view (with GraphicsContextSprite).
//todo: need to implement ImageViewSprite
public class AnimationManager {
    private AnimationData animationData;
    private AnimationView animationView;

    public AnimationManager(Animation animation, GraphicsContext gc){
        animationData = new AnimationData(animation);
        animationView = new GraphicsContextSprite(gc);
    }
    public AnimationManager(Animation animation, ImageView imageView){
        animationData = new AnimationData(animation);
        animationView = new ImageViewSprite(imageView);
    }
    public AnimationManager(AnimationManager other){

    }

    public void setAnimation(Animation newAnimation){
        animationData.setAnimation(newAnimation);
    }

    public void relocate(double anchorX, double anchorY){
        animationData.relocate(anchorX, anchorY);
    }

    public void setScale(double newScale){
        animationData.setScale(newScale);
        animationView.setScale(newScale);
    }

    // This method should only ever be called by the JavaFX thread.
    public void updateView(){
        if (!Platform.isFxApplicationThread()){
            System.err.println("ERROR! A non-JavaFX thread is attempting to call AnimationManager.drawFrame(). " +
                    "That method updates UI components, so it must be called only from the JavaFX Application thread.");
        }
        animationView.drawFrame(animationData);
    }


}
