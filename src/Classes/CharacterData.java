package Classes;

import Classes.Animation.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;

/**
 * For managing the state and display of an animated character
 */
public class CharacterData {
    private CharacterAnimations characterAnimations; // basically, a collection of all of this character's possible animations.
    private AnimationData currentAnimation; // The currently playing animation from among the collection.
    private CharacterAnimations.CharacterAnimationState characterAnimationState; // i.e. Content, Worried, Defeated, etc.

    // Initialize with a content display of the character:
    public CharacterData(CharacterAnimations characterAnimations){
        this.characterAnimations = characterAnimations;
        characterAnimationState = CharacterAnimations.CharacterAnimationState.CONTENT;
        currentAnimation = new AnimationData(characterAnimations.getAnimation(characterAnimationState));
    }

    public void relocate(double x, double y){
        currentAnimation.relocate(x,y);
    }

    public void setScale(double scaleFactor){
        currentAnimation.setScale(scaleFactor);
    }

    public void setCharacterEnum(CharacterAnimations characterAnimations){
        this.characterAnimations = characterAnimations;
        currentAnimation.setAnimation(characterAnimations.getAnimation(characterAnimationState));
    }

    public void freeze(){
        currentAnimation.setVisibility(VisibilityOption.GREYSCALE);
        currentAnimation.setStatus(StatusOption.PAUSED);
    }


    //todo: implement this
    public void drawSelf(GraphicsContext graphicsContext){

    }

    //todo: finish implementing this
    public void drawSelf(ImageView imageView){
        currentAnimation.drawSelf(imageView);
    }

    //todo: finish implementing this
    public void tick(){
        currentAnimation.tick();
    }

    // older versions. Keeping them around for reference.
    /*public void setScale(double scaleFactor, CharacterAnimations characterEnum, int currentFrame){
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
    }*/
}


