package Classes;

import Classes.Animation.AnimationData;
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

    public CannonData(CannonType cannonType, long parentID, Synchronizer synchronizer){
        cannonAnimationState = CannonType.CannonAnimationState.AIMING;
        backgroundAnimation = new AnimationData(cannonType.getBackgroundAnimation(cannonAnimationState));
        movingPartAnimation = new AnimationData(cannonType.getMovingPartAnimation(cannonAnimationState));
        foregroundAnimation = new AnimationData(cannonType.getForegroundAnimation(cannonAnimationState));
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

    public CannonData(CannonData other, long parentID, Synchronizer synchronizer){
        this.cannonAnimationState = other.getCannonAnimationState();
        backgroundAnimation = new AnimationData(other.getBackgroundAnimationData());
        movingPartAnimation = new AnimationData(other.getMovingPartAnimationData());
        foregroundAnimation = new AnimationData(other.getForegroundAnimationData());
        this.cannonType = new SynchronizedComparable<>("cannonType", other.getCannonType().getData(), other.getCannonType().getPrecedence(), parentID, synchronizer);
        cannonAngle = new SynchronizedComparable<>("cannonAngle", -80.0, SynchronizedData.Precedence.CLIENT, parentID, synchronizer);
        cannonAngle.setTo(other.getAngle());
    }

    public SynchronizedComparable<CannonType> getCannonType(){
        return cannonType;
    }

    public CannonType.CannonAnimationState getCannonAnimationState(){
        return cannonAnimationState;
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
            case DISCONNECTED:
        }
    }

    public AnimationData getBackgroundAnimationData(){
        return backgroundAnimation;
    }
    public AnimationData getMovingPartAnimationData(){
        return movingPartAnimation;
    }
    public AnimationData getForegroundAnimationData(){
        return foregroundAnimation;
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

    public double getAngle(){
        return movingPartAnimation.getRotation();
    }
    public void setAngle(double angle){
        cannonAngle.setTo(angle);
    }

    public void freeze(){
        backgroundAnimation.setVisibility(VisibilityOption.GREYSCALE);
        movingPartAnimation.setVisibility(VisibilityOption.GREYSCALE_TRANSPARENT);
        foregroundAnimation.setVisibility(VisibilityOption.GREYSCALE);
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
