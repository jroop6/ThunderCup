package Classes;

import Classes.Images.CharacterImages;
import Classes.NetworkCommunication.PlayerData;
import javafx.geometry.Point2D;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.transform.Scale;

/**
 * For displaying a character
 */
public class Character {
    private CharacterImages characterEnum;
    private Sprite sprite;
    private AnimationType animationType;
    private int currentFrame = 0;
    private Scale scaler = new Scale();

    // Initialize with a static display of the character:
    public Character(PlayerData playerData){
        characterEnum = playerData.getCharacterEnum();
        sprite = new Sprite(characterEnum.getSpriteSheet());
        sprite.getTransforms().add(scaler);
        animationType = AnimationType.STATIC_DISPLAY;
    }

    public void relocate(double x, double y){
        sprite.relocate(x,y,currentFrame);
    }

    public void setScale(double scaleFactor){
        Point2D anchorPoint = characterEnum.getSpriteSheet().getFrameBound(currentFrame).getAnchorPoint();
        scaler.setPivotX(anchorPoint.getX());
        scaler.setPivotY(anchorPoint.getY());
        scaler.setX(scaleFactor);
        scaler.setY(scaleFactor);
    }

    public void setCharacterEnum(CharacterImages characterEnum){
        this.characterEnum = characterEnum;
        double scale = scaler.getX();
        setScale(1.0); // undo any scaling
        sprite.setSpriteSheet(characterEnum.getSpriteSheet());
        sprite.setFrame(0); // todo: choose correct frame.
        setScale(scale); // restore scaling, using new anchor point
    }

    public CharacterImages getCharacterEnum(){
        return characterEnum;
    }

    public Sprite getSprite(){
        return sprite;
    }

    public void freeze(){
        ColorAdjust desaturator = new ColorAdjust(0,-1,0,0);
        sprite.setEffect(desaturator);
    }
}

enum AnimationType{
    STATIC_DISPLAY,
    IDLE,
    CHEERING,
    JUMPING,
    BUCKING;
}

