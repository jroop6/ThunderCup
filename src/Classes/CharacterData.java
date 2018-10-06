package Classes;

import Classes.Animation.*;
import Classes.NetworkCommunication.SynchronizedComparable;
import Classes.NetworkCommunication.SynchronizedData;
import Classes.NetworkCommunication.Synchronizer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;

import java.io.Serializable;

/**
 * For managing the state and display of an animated character
 */
public class CharacterData implements Serializable {
    private SynchronizedComparable<CharacterType> characterType; // basically, a collection of all of this character's possible animations.
    private AnimationData currentAnimation; // The currently playing animation from among the collection.
    private CharacterType.CharacterAnimationState characterAnimationState; // i.e. Content, Worried, Defeated, etc.
    private final Synchronizer synchronizer;

    // Initialize with a content display of the character:
    public CharacterData(CharacterType characterType, long parentID, Synchronizer synchronizer){
        this.synchronizer = synchronizer;
        characterAnimationState = CharacterType.CharacterAnimationState.CONTENT;
        currentAnimation = new AnimationData(characterType.getAnimation(characterAnimationState));
        synchronized (synchronizer){
            this.characterType = new SynchronizedComparable<>("characterType", characterType, ((newValue,mode,i,j) -> currentAnimation.setAnimation(newValue.getAnimation(characterAnimationState))), ((newValue,mode,i,j) -> currentAnimation.setAnimation(newValue.getAnimation(characterAnimationState))), SynchronizedData.Precedence.CLIENT,parentID,synchronizer,5);
        }
    }

    // Initialize with a content display of the character:
    public CharacterData(CharacterData other, long parentID, Synchronizer synchronizer){
        this.synchronizer = synchronizer;
        characterAnimationState = other.characterAnimationState;
        currentAnimation = new AnimationData(other.currentAnimation);
        characterType = new SynchronizedComparable<>("characterType", other.characterType.getData(), other.characterType.getPrecedence(), parentID, synchronizer);
    }

    public void relocate(double x, double y){
        currentAnimation.relocate(x,y);
    }

    public void setScale(double scaleFactor){
        currentAnimation.setScale(scaleFactor);
    }

    public SynchronizedComparable<CharacterType> getCharacterType(){
        return characterType;
    }

    public void setCharacterAnimationState(CharacterType.CharacterAnimationState characterAnimationState){
        this.characterAnimationState = characterAnimationState;
        synchronized (synchronizer){ // make sure no one modifies the data while we're working with it
            currentAnimation.setAnimation(characterType.getData().getAnimation(characterAnimationState));
            switch(characterAnimationState){
                case CONTENT:
                case WORRIED:
                    currentAnimation.setRandomFrame();
                case VICTORIOUS:
                case DEFEATED:
                    currentAnimation.setVisibility(VisibilityOption.NORMAL);
                    currentAnimation.setStatus(StatusOption.PLAYING);
                    break;
                case DISCONNECTED:
                    currentAnimation.setVisibility(VisibilityOption.GREYSCALE);
                    currentAnimation.setStatus(StatusOption.PAUSED);
                    break;
            }
        }
    }

    public void drawSelf(GraphicsContext graphicsContext){
        // note: I would use synchronized(synchronizer) here, but this method is only ever called by
        // playerData.drawSelf(), which is already synchronized.
        currentAnimation.drawSelf(graphicsContext);
    }

    public void drawSelf(ImageView imageView){
        // note: I would use synchronized(synchronizer) here, but this method is only ever called by
        // playerData.drawSelf(), which is already synchronized.
        currentAnimation.drawSelf(imageView);
    }

    public void tick(int lowestRow){
        synchronized (synchronizer){ // due to setCharacterAnimationState(), it is possible for characterAnimationState to change while we're working with it. So, we must synchronize.
            // determine which animation state we should be in, and increment animationFrame:
            if(characterAnimationState.inRange(lowestRow) || characterAnimationState== CharacterType.CharacterAnimationState.DISCONNECTED){
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
}


