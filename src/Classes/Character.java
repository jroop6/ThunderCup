package Classes;

import Classes.Animation.CharacterAnimations;
import Classes.Animation.Sprite;
import javafx.geometry.Point2D;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.transform.Scale;

/**
 * For displaying a character
 */
public class Character {
    private Sprite sprite;
    private Scale scaler = new Scale();

    // Initialize with a static display of the character:
    public Character(CharacterAnimations characterEnum){
        sprite = new Sprite(characterEnum.getSpriteSheet());
        sprite.getTransforms().add(scaler);
    }

    public void relocate(double x, double y, int currentFrame){
        sprite.relocate(x,y, currentFrame);
    }

    public void setScale(double scaleFactor, CharacterAnimations characterEnum, int currentFrame){
        Point2D anchorPoint = characterEnum.getSpriteSheet().getFrameBound(currentFrame).getAnchorPoint();
        scaler.setPivotX(anchorPoint.getX());
        scaler.setPivotY(anchorPoint.getY());
        scaler.setX(scaleFactor);
        scaler.setY(scaleFactor);
    }

    public void setCharacterEnum(CharacterAnimations characterEnum, int currentFrame){
        double scale = scaler.getX();
        setScale(1.0, characterEnum, currentFrame); // undo any scaling
        sprite.setSpriteSheet(characterEnum.getSpriteSheet());
        sprite.setFrame(0); // todo: choose correct frame.
        setScale(scale, characterEnum, currentFrame); // restore scaling, using new anchor point
    }

    public Sprite getSprite(){
        return sprite;
    }

    public void freeze(){
        ColorAdjust desaturator = new ColorAdjust(0,-1,0,0);
        sprite.setEffect(desaturator);
    }
}


