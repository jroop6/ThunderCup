package Classes;

import Classes.Animation.AnimationData;
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
public class CannonData implements Serializable {

    private SynchronizedComparable<CannonType> cannonType;
    private AnimationData backgroundAnimation;
    private AnimationData movingPartAnimation;
    private AnimationData foregroundAnimation;
    private CannonType.CannonAnimationState cannonAnimationState;
    private SynchronizedComparable<Double> cannonAngle;
    private final Synchronizer synchronizer;

    public CannonData(CannonType cannonType, long parentID, Synchronizer synchronizer){
        this.synchronizer = synchronizer;
        cannonAnimationState = CannonType.CannonAnimationState.AIMING;
        backgroundAnimation = new AnimationData(cannonType.getBackgroundAnimation(cannonAnimationState));
        movingPartAnimation = new AnimationData(cannonType.getMovingPartAnimation(cannonAnimationState));
        foregroundAnimation = new AnimationData(cannonType.getForegroundAnimation(cannonAnimationState));
        synchronized (this.synchronizer){
            this.cannonType = new SynchronizedComparable<>("cannonType", cannonType,
                    (CannonType newVal, Mode mode, int i, int j)->{
                        backgroundAnimation.setAnimation(newVal.getBackgroundAnimation(cannonAnimationState));
                        movingPartAnimation.setAnimation(newVal.getMovingPartAnimation(cannonAnimationState));
                        foregroundAnimation.setAnimation(newVal.getForegroundAnimation(cannonAnimationState));
                    },
                    (CannonType newVal, Mode mode, int i, int j)->{
                        backgroundAnimation.setAnimation(newVal.getBackgroundAnimation(cannonAnimationState));
                        movingPartAnimation.setAnimation(newVal.getMovingPartAnimation(cannonAnimationState));
                        foregroundAnimation.setAnimation(newVal.getForegroundAnimation(cannonAnimationState));
                    },
                    SynchronizedData.Precedence.CLIENT,parentID,synchronizer,24);
            cannonAngle = new SynchronizedComparable<>("cannonAngle",-80.0,(Double newVal, Mode mode, int i, int j)->movingPartAnimation.setRotation(newVal), (Double newVal, Mode mode, int i, int j)->movingPartAnimation.setRotation(newVal), SynchronizedData.Precedence.CLIENT,parentID,synchronizer,0);
            cannonAngle.setTo(-80.0);
        }
    }

    public CannonData(CannonData other, long parentID, Synchronizer synchronizer){
        this.synchronizer = synchronizer;
        this.cannonAnimationState = other.cannonAnimationState;
        backgroundAnimation = new AnimationData(other.backgroundAnimation);
        movingPartAnimation = new AnimationData(other.movingPartAnimation);
        foregroundAnimation = new AnimationData(other.foregroundAnimation);
        this.cannonType = new SynchronizedComparable<>("cannonType", other.cannonType.getData(), other.cannonType.getPrecedence(), parentID, synchronizer);
        cannonAngle = new SynchronizedComparable<>("cannonAngle", -80.0, SynchronizedData.Precedence.CLIENT, parentID, synchronizer);
        cannonAngle.setTo(other.getCannonAngle().getData());
    }

    public SynchronizedComparable<CannonType> getCannonType(){
        return cannonType;
    }

    public void setCannonAnimationState(CannonType.CannonAnimationState cannonAnimationState){
        this.cannonAnimationState = cannonAnimationState;
        backgroundAnimation.setAnimation(cannonType.getData().getBackgroundAnimation(cannonAnimationState));
        movingPartAnimation.setAnimation(cannonType.getData().getMovingPartAnimation(cannonAnimationState));
        foregroundAnimation.setAnimation(cannonType.getData().getForegroundAnimation(cannonAnimationState));
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
}
