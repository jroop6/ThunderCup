package Classes;

import javafx.scene.image.ImageView;

import javafx.scene.transform.Translate;

/**
 * A class that provides an ImageView wrapper for a SpriteSheet and convenience methods for updating which frame of the
 * spritesheet is displayed in the ImageView.
 */
public class Sprite extends ImageView{
    // note to future self: DON'T store currentFrame in this class. Use classes like Orb, Character, and Cannon to do that.
    // why? recall that Orb does not have a Sprite - only a SpriteSheet. To keep things consistent, put frame management
    // into those classes. Also, objects might have multiple animations that will be easier to manage from the Character class.
    private SpriteSheet spriteSheet;
    Translate anchorPointCorrector = new Translate();

    public Sprite(SpriteSheet spriteSheet){
        this.spriteSheet = spriteSheet;
        getTransforms().add(anchorPointCorrector);
        setImage(spriteSheet);
        setFrame(0);
    }

    public void setFrame(int index){
        SpriteSheet.FrameBound frameBound = spriteSheet.getFrameBound(index);
        setViewport(frameBound.getPosAndDim());
        anchorPointCorrector.setX(-frameBound.getAnchorPoint().getX());
        anchorPointCorrector.setY(-frameBound.getAnchorPoint().getY());
    }

    // Note: whenever something calls this method, it should also immediately call setFrame to set the Frame appropriately.
    public void setSpriteSheet(SpriteSheet spriteSheet){
        this.spriteSheet = spriteSheet;
        setImage(spriteSheet);
        setFrame(0);
    }

    // Move frame index management to Orb, Character, Cannon, etc.
    /*private LoopBehavior loopBehavior = LoopBehavior.LOOP;
    // animation will loop by default
    public void incrementFrame(){
        currentFrame++;
        if(currentFrame >= frameBounds.size()){
            if(loopBehavior == LoopBehavior.LOOP) currentFrame = 0;
            else currentFrame = frameBounds.size()-1;
        }
        setViewport(frameBounds.get(currentFrame).posAndDim);
    }*/
}
