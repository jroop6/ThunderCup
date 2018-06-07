package Classes.Animation;
import Classes.MiscAnimations;
import Classes.Sprite;

public class PngSequencePlayer extends Sprite {

    int currentFrame = 0;
    LoopBehavior loopBehavior = LoopBehavior.PLAY_ONCE;
    int maxFrame;

    public PngSequencePlayer(MiscAnimations animationEnum){
        super(animationEnum.getSpriteSheet());
        maxFrame = animationEnum.getSpriteSheet().getMaxFrameIndex();
    }

    public void incrementFrame(){
        currentFrame++;
        if(currentFrame>maxFrame){
            switch (loopBehavior){
                case LOOP:
                    currentFrame = 0;
                    break;
                case PLAY_ONCE:
                    currentFrame = maxFrame;
                    return;
            }
        }
        setFrame(currentFrame);
    }

    private enum LoopBehavior {LOOP, PLAY_ONCE}

}
