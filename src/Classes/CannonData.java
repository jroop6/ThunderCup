package Classes;

import Classes.Animation.AnimationData;
import Classes.Animation.VisibilityOption;
import Classes.Images.CannonType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;

import java.io.Serializable;

/**
 * For displaying a cannon
 */
public class CannonData implements Serializable {

    private CannonType cannonType;
    private AnimationData backgroundAnimation;
    private AnimationData movingPartAnimation;
    private AnimationData foregroundAnimation;
    private CannonType.CannonAnimationState cannonAnimationState;

    //todo: I need an AnimationData-like class for the Background and foreground images. Maybe just use an AnimationData.

    public CannonData(CannonType cannonType){
        this.cannonType = cannonType;
        cannonAnimationState = CannonType.CannonAnimationState.AIMING;
        backgroundAnimation = new AnimationData(cannonType.getBackgroundAnimation(cannonAnimationState));
        movingPartAnimation = new AnimationData(cannonType.getMovingPartAnimation(cannonAnimationState));
        foregroundAnimation = new AnimationData(cannonType.getForegroundAnimation(cannonAnimationState));
        setAngle(-80);
    }

    public CannonData(CannonData other){
        this.cannonType = other.getCannonType();
        this.cannonAnimationState = other.getCannonAnimationState();
        backgroundAnimation = new AnimationData(other.getBackgroundAnimationData());
        movingPartAnimation = new AnimationData(other.getMovingPartAnimationData());
        foregroundAnimation = new AnimationData(other.getForegroundAnimationData());
    }

    public CannonType getCannonType(){
        return cannonType;
    }
    public void setCannonType(CannonType cannonType){
        this.cannonType = cannonType;
        backgroundAnimation.setAnimation(cannonType.getBackgroundAnimation(cannonAnimationState));
        movingPartAnimation.setAnimation(cannonType.getMovingPartAnimation(cannonAnimationState));
        foregroundAnimation.setAnimation(cannonType.getForegroundAnimation(cannonAnimationState));
    }

    public CannonType.CannonAnimationState getCannonAnimationState(){
        return cannonAnimationState;
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
        movingPartAnimation.setRotation(angle);
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
