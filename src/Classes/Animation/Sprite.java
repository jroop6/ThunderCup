package Classes.Animation;

import javafx.scene.image.ImageView;

import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

/**
 * A class that provides an ImageView wrapper for a SpriteSheet and convenience methods for updating which frame of the
 * spritesheet is displayed in the ImageView.
 */
public class Sprite extends ImageView{
    // note to future self: DON'T store currentFrame in this class. Use classes like Orb, Character, and Cannon to do that.
    // Why? recall that Orb does not have a Sprite - only a SpriteSheet. To keep things consistent, put frame management
    // into those classes. Also, objects might have multiple animations that will be easier to manage from those higher-
    // level classes. For example, Orbs have separate electrification and explosion animations.
    private SpriteSheet spriteSheet;
    private Rotate rotater = new Rotate();
    private Scale scaler = new Scale();
    private double xPos;
    private double yPos;

    public Sprite(SpriteSheet spriteSheet){
        this.spriteSheet = spriteSheet;
        getTransforms().add(rotater);
        getTransforms().add(scaler);
        setImage(spriteSheet);
        setFrame(0);
    }

    public void setFrame(int frameIndex){
        SpriteSheet.FrameBound frameBound = spriteSheet.getFrameBound(frameIndex);
        setViewport(frameBound.getPosAndDim());
        centerTheAnchorPoint(frameIndex);

        //Todo: take into account rotation and scaling. This will only become important once I start animating the cannons.
    }

    // Note: whenever something calls this method, it should also immediately call setFrame to set the Frame appropriately.
    public void setSpriteSheet(SpriteSheet spriteSheet){
        this.spriteSheet = spriteSheet;
        setImage(spriteSheet);
        setFrame(0);
    }

    public void rotate(double angle, int index){
        SpriteSheet.FrameBound frameBound = spriteSheet.getFrameBound(index);
        rotater.setPivotX(frameBound.getAnchorPoint().getX());
        rotater.setPivotY(frameBound.getAnchorPoint().getY());
        rotater.setAngle(angle);
    }

    public void scale(double scaleFactor, int index){
        SpriteSheet.FrameBound frameBound = spriteSheet.getFrameBound(index);
        scaler.setPivotX(frameBound.getAnchorPoint().getX());
        scaler.setPivotY(frameBound.getAnchorPoint().getY());
        scaler.setX(scaleFactor);
        scaler.setY(scaleFactor);
    }

    public void relocate(double x, double y, int frameIndex){
        xPos = x;
        yPos = y;
        centerTheAnchorPoint(frameIndex);
    }

    private void centerTheAnchorPoint(int frameIndex){
        SpriteSheet.FrameBound frameBound = spriteSheet.getFrameBound(frameIndex);
        double layoutX = xPos-frameBound.getAnchorPoint().getX();
        double layoutY = yPos-frameBound.getAnchorPoint().getY();
        relocate(layoutX,layoutY);
    }

    public double getxPos(){
        return xPos;
    }
    public double getyPos() {
        return yPos;
    }

    public double getAngle(){
        return rotater.getAngle();
    }

    public double getScale(){
        return scaler.getX();
    }
}
