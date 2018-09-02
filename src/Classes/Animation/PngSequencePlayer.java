package Classes.Animation;

public class PngSequencePlayer extends Sprite {

    int currentFrame = 0;
    LoopBehavior loopBehavior;
    int maxFrame;

    public PngSequencePlayer(MiscAnimations animationEnum, LoopBehavior loopBehavior){
        super(animationEnum.getSpriteSheet());
        this.loopBehavior = loopBehavior;
        maxFrame = animationEnum.getSpriteSheet().getMaxFrameIndex();
    }


    public PngSequencePlayer(MiscAnimations animationEnum){
        this(animationEnum, LoopBehavior.PLAY_ONCE);
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
