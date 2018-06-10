package Classes;

import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;

import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * A class that provides an ImageView wrapper for a SpriteSheet and convenience methods for updating which frame of the
 * spritesheet is displayed in the ImageView.
 */
public class Sprite extends ImageView{
    // note to future self: DON'T store currentFrame in this class. Use classes like Orb, Character, and Cannon to do that.
    // why? recall that Orb does not have a Sprite - only a SpriteSheet. To keep things consistent, put frame management
    // into those classes. Also, objects might have multiple animations that will be easier to manage from those higher-
    // level classes.
    private SpriteSheet spriteSheet;
    private Translate anchorPointCorrector = new Translate();
    private Rotate rotater = new Rotate();
    private Scale scaler = new Scale();

    public Sprite(SpriteSheet spriteSheet){
        this.spriteSheet = spriteSheet;
        getTransforms().add(anchorPointCorrector);
        getTransforms().add(rotater);
        getTransforms().add(scaler);
        setImage(spriteSheet);
        setFrame(0);
    }

    public void setFrame(int index){
        SpriteSheet.FrameBound frameBound = spriteSheet.getFrameBound(index);
        setViewport(frameBound.getPosAndDim());
        anchorPointCorrector.setX(-frameBound.getAnchorPoint().getX());
        anchorPointCorrector.setY(-frameBound.getAnchorPoint().getY());

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
        rotater.setAngle(-angle); // Angle is flipped because JavaFX rotates clockwise by default instead of counterclockwise. I prefer thinking counterclockwise.
    }

    public void scale(double scaleFactor, int index){
        SpriteSheet.FrameBound frameBound = spriteSheet.getFrameBound(index);
        scaler.setPivotX(frameBound.getAnchorPoint().getX());
        scaler.setPivotY(frameBound.getAnchorPoint().getY());
        scaler.setX(scaleFactor);
        scaler.setY(scaleFactor);
    }

    public double getAngle(){
        return rotater.getAngle();
    }

    public double getScale(){
        return scaler.getX();
    }
}
