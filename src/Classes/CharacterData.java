package Classes;

import Classes.Animation.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;

import java.io.Serializable;

/**
 * For managing the state and display of an animated character
 */
public class CharacterData implements Serializable {
    private CharacterType characterType; // basically, a collection of all of this character's possible animations.
    private AnimationData currentAnimation; // The currently playing animation from among the collection.
    private CharacterType.CharacterAnimationState characterAnimationState; // i.e. Content, Worried, Defeated, etc.

    // Initialize with a content display of the character:
    public CharacterData(CharacterType characterType){
        this.characterType = characterType;
        characterAnimationState = CharacterType.CharacterAnimationState.CONTENT;
        currentAnimation = new AnimationData(characterType.getAnimation(characterAnimationState));
    }

    public CharacterData(CharacterData other){
        characterType = other.getCharacterType();
        characterAnimationState = other.getCharacterAnimationState();
        currentAnimation = new AnimationData(other.getAnimationData());
    }

    public void relocate(double x, double y){
        currentAnimation.relocate(x,y);
    }

    public void setScale(double scaleFactor){
        currentAnimation.setScale(scaleFactor);
    }

    public CharacterType getCharacterType(){
        return characterType;
    }
    public void setCharacterType(CharacterType characterType){
        this.characterType = characterType;
        currentAnimation.setAnimation(characterType.getAnimation(characterAnimationState));
    }

    private AnimationData getAnimationData(){
        return currentAnimation;
    }

    public CharacterType.CharacterAnimationState getCharacterAnimationState(){
        return characterAnimationState;
    }
    public void setCharacterAnimationState(CharacterType.CharacterAnimationState characterAnimationState){
        this.characterAnimationState = characterAnimationState;
        currentAnimation.setAnimation(characterType.getAnimation(characterAnimationState));
        switch(characterAnimationState){
            case CONTENT:
            case WORRIED:
                currentAnimation.setRandomFrame();
            case VICTORY:
            case DEFEAT:
            case DISCONNECTED:
        }
    }

    public void freeze(){
        currentAnimation.setVisibility(VisibilityOption.GREYSCALE);
        currentAnimation.setStatus(StatusOption.PAUSED);
    }

    public void drawSelf(GraphicsContext graphicsContext){
        currentAnimation.drawSelf(graphicsContext);
    }

    public void drawSelf(ImageView imageView){
        currentAnimation.drawSelf(imageView);
    }

    public void tick(int lowestRow){
        // determine which animation state we should be in, and increment animationFrame:
        if(characterAnimationState.inRange(lowestRow)){
            currentAnimation.tick();
        }
        else{
            for(CharacterType.CharacterAnimationState state : CharacterType.CharacterAnimationState.values()){
                if(state.inRange(lowestRow)){
                    setCharacterAnimationState(state);
                    //System.out.println("new state: " + state + " animation bounds: " + bounds[0] + ", " + bounds[1]);
                    //System.out.println("new animationFrame: " + animationFrame);
                    break;
                }
            }
        }
        //System.out.println("current animation frame: " + currentAnimation.getFrame());
    }
}


