package Classes;

import Classes.Animation.Animation;
import Classes.Animation.StatusOption;
import Classes.Animation.VisibilityOption;
import Classes.Images.CannonType;
import Classes.NetworkCommunication.Mode;
import Classes.NetworkCommunication.SynchronizedComparable;
import Classes.NetworkCommunication.SynchronizedData;
import Classes.NetworkCommunication.Synchronizer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;

import java.io.Serializable;

/**
 * For displaying a cannon
 */
public class Cannon implements Serializable {

    private SynchronizedComparable<CannonType> cannonType;
    private Animation backgroundAnimation;
    private Animation movingPartAnimation;
    private Animation foregroundAnimation;
    private CannonType.CannonAnimationState cannonAnimationState;
    private SynchronizedComparable<Double> cannonAngle;
    private final Synchronizer synchronizer;

    public Cannon(CannonType cannonType, long parentID, Synchronizer synchronizer){
        this.synchronizer = synchronizer;
        cannonAnimationState = CannonType.CannonAnimationState.AIMING;
        backgroundAnimation = new Animation(cannonType.getBackgroundAnimation(cannonAnimationState));
        movingPartAnimation = new Animation(cannonType.getMovingPartAnimation(cannonAnimationState));
        foregroundAnimation = new Animation(cannonType.getForegroundAnimation(cannonAnimationState));
        synchronized (this.synchronizer){
            this.cannonType = new SynchronizedComparable<>("cannonType", cannonType,
                    (CannonType newVal, Mode mode, int i, int j)->{
                        backgroundAnimation.setAnimationName(newVal.getBackgroundAnimation(cannonAnimationState));
                        movingPartAnimation.setAnimationName(newVal.getMovingPartAnimation(cannonAnimationState));
                        foregroundAnimation.setAnimationName(newVal.getForegroundAnimation(cannonAnimationState));
                    },
                    SynchronizedData.Precedence.CLIENT,parentID,synchronizer,24);
            cannonAngle = new SynchronizedComparable<>("cannonAngle",-80.0,(Double newVal, Mode mode, int i, int j)->movingPartAnimation.setRotation(newVal), SynchronizedData.Precedence.CLIENT,parentID,synchronizer,Integer.MAX_VALUE);
            cannonAngle.setTo(-80.0);
        }
    }

    public SynchronizedComparable<CannonType> getCannonType(){
        return cannonType;
    }

    public void setCannonAnimationState(CannonType.CannonAnimationState cannonAnimationState){
        this.cannonAnimationState = cannonAnimationState;
        backgroundAnimation.setAnimationName(cannonType.getData().getBackgroundAnimation(cannonAnimationState));
        movingPartAnimation.setAnimationName(cannonType.getData().getMovingPartAnimation(cannonAnimationState));
        foregroundAnimation.setAnimationName(cannonType.getData().getForegroundAnimation(cannonAnimationState));
        switch(cannonAnimationState){
            case AIMING:
                backgroundAnimation.setRandomFrame();
                movingPartAnimation.setRandomFrame();
                foregroundAnimation.setRandomFrame();
            case FIRING:
            case VICTORIOUS:
            case DEFEATED:
                backgroundAnimation.setVisibility(VisibilityOption.NORMAL);
                movingPartAnimation.setVisibility(VisibilityOption.NORMAL);
                foregroundAnimation.setVisibility(VisibilityOption.NORMAL);
                backgroundAnimation.setStatus(StatusOption.PLAYING);
                movingPartAnimation.setStatus(StatusOption.PLAYING);
                foregroundAnimation.setStatus(StatusOption.PLAYING);
                break;
            case DISCONNECTED:
                backgroundAnimation.setVisibility(VisibilityOption.GREYSCALE);
                movingPartAnimation.setVisibility(VisibilityOption.GREYSCALE_TRANSPARENT);
                foregroundAnimation.setVisibility(VisibilityOption.GREYSCALE);
                backgroundAnimation.setStatus(StatusOption.PAUSED);
                movingPartAnimation.setStatus(StatusOption.PAUSED);
                foregroundAnimation.setStatus(StatusOption.PAUSED);
                break;
        }
    }

    public void relocate(double x, double y){
        backgroundAnimation.relocate(x,y);
        movingPartAnimation.relocate(x,y);
        foregroundAnimation.relocate(x,y);
    }

    public void setScale(double scaleFactor){
        backgroundAnimation.setScale(scaleFactor);
        movingPartAnimation.setScale(scaleFactor);
        foregroundAnimation.setScale(scaleFactor);
    }

    public double getPosX(){
        return movingPartAnimation.getAnchorX();
    }
    public double getPosY(){
        return movingPartAnimation.getAnchorY();
    }

    public SynchronizedComparable<Double> getCannonAngle(){
        return cannonAngle;
    }

    // todo: consider having separate drawBackgroundImage, drawCannon, and drawForegroundImage methods.
    public void drawSelf(GraphicsContext graphicsContext){
        backgroundAnimation.drawSelf(graphicsContext);
        movingPartAnimation.drawSelf(graphicsContext);
        foregroundAnimation.drawSelf(graphicsContext);
    }

    // Draws only the moving part
    public void drawSelf(ImageView imageView){
        movingPartAnimation.drawSelf(imageView);
    }

    // Draws everything, on 3 different ImageViews.
    public void drawSelf(ImageView backgroundImageView, ImageView movingPartImageVew, ImageView foregroundImageView){
        backgroundAnimation.drawSelf(backgroundImageView);
        movingPartAnimation.drawSelf(movingPartImageVew);
        foregroundAnimation.drawSelf(foregroundImageView);
    }

    public void tick(){
        synchronized (synchronizer){ // due to setCannonAnimationState(), it is possible for characterAnimationState to change while we're working with it. So, we must synchronize.
            // determine which animation state we should be in, and increment animationFrame:
            // todo: review this for correctness. In particular, I've been assuming that the lengths of the firing animations are the same for the background, moving part, and foreground; is this necessarily true?
            switch (cannonAnimationState){
                case FIRING:
                    if(movingPartAnimation.tick()) setCannonAnimationState(CannonType.CannonAnimationState.AIMING);
                    else {
                        backgroundAnimation.tick();
                        foregroundAnimation.tick();
                    }
                    break;
                case DISCONNECTED:
                case DEFEATED:
                case VICTORIOUS:
                case AIMING:
                    backgroundAnimation.tick();
                    movingPartAnimation.tick();
                    foregroundAnimation.tick();
                    break;
            }
        }
    }
}
