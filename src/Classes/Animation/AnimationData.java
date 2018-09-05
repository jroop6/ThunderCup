package Classes.Animation;

import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.image.ImageView;

public class AnimationData {
    private Animation animation;
    private double anchorX = 0.0;
    private double anchorY = 0.0;
    private double scale = 1.0;
    private double rotation = 0.0;
    private VisibilityOption visibility = VisibilityOption.NORMAL;
    private PlayOption playOption = PlayOption.LOOP;
    private StatusOption status = StatusOption.PLAYING;
    private int frame = 0;

    public AnimationData(Animation startingAnimation){
        animation = startingAnimation;
    }
    public AnimationData(Animation startingAnimation, double anchorX, double anchorY, PlayOption initialPlayOption){
        animation = startingAnimation;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        playOption = initialPlayOption;
    }
    public AnimationData(AnimationData other){
        animation = other.getAnimation();
        anchorX = other.getAnchorX();
        anchorY = other.getAnchorY();
        scale = other.getScale();
        rotation = other.getRotation();
        visibility = other.getVisibility();
        playOption = other.getPlayOption();
        status = other.getStatus();
        frame = other.getFrame();
    }

    public Animation getAnimation(){
        return animation;
    }
    public void setAnimation(Animation newAnimation){
        animation = newAnimation;
    }

    public double getAnchorX(){
        return anchorX;
    }
    public double getAnchorY(){
        return anchorY;
    }
    public void relocate(double x, double y){
        anchorX = x;
        anchorY = y;
    }

    public double getScale(){
        return scale;
    }
    public void setScale(double amount){
        scale = amount;
    }

    public double getRotation(){
        return rotation;
    }
    public void rotate(double amount){
        rotation += amount;
    }

    public VisibilityOption getVisibility(){
        return visibility;
    }
    public void setVisibility(VisibilityOption visibility){
        this.visibility = visibility;
    }

    public PlayOption getPlayOption(){
        return playOption;
    }
    public void setPlayOption(PlayOption playOption){
        this.playOption = playOption;
    }

    public StatusOption getStatus(){
        return status;
    }
    public void setStatus(StatusOption status){
        this.status = status;
    }

    public int getFrame(){
        return frame;
    }
    public void setFrame(int index){
        frame = index;
    }
    private boolean incrementFrame(){
        return ++frame > animation.getSpriteSheet().getMaxFrameIndex();
    }
    private boolean decrementFrame(){
        return --frame < 0;
    }
    // return true if the animation is finished.
    public boolean tick(){
        boolean finished = false;
        switch(status){
            case PLAYING:
                switch(playOption){
                    case LOOP:
                    case PLAY_ONCE_THEN_VANISH:
                    case PLAY_ONCE_THEN_PAUSE:
                        finished = incrementFrame();
                        break;
                    case REVERSE:
                        finished = decrementFrame();
                }
                break;
            case PAUSED:
                break;
        }

        // note: you MUST call setFrame if finished==true.
        if(finished){
            switch (playOption){
                case LOOP:
                    setFrame(0);
                    break;
                case PLAY_ONCE_THEN_VANISH:
                    setVisibility(VisibilityOption.INVISIBLE);
                    setFrame(0); // todo: should I pause the animation here? (such a status change might be nonobvious to the user...).
                    break;
                case PLAY_ONCE_THEN_PAUSE:
                    setFrame(animation.getSpriteSheet().getMaxFrameIndex());
                    setStatus(StatusOption.PAUSED);
                    finished = false; // We want the last frame to continue to display, so don't report that the animation is "finished".
                    break;
                case REVERSE:
                    setFrame(animation.getSpriteSheet().getMaxFrameIndex());
                    break;
            }
        }

        return finished;
    }

    // todo: take scale into account
    // This method should only ever be called by the JavaFX application thread.
    public int drawSelf(GraphicsContext graphicsContext){
        if (!Platform.isFxApplicationThread()){
            System.err.println("Warning! A non-JavaFX thread is attempting to call AnimationData.drawSelf(). " +
                    "That method updates UI components, so it must be called only from the JavaFX Application thread.");
            return 1;
        }

        if(visibility == VisibilityOption.INVISIBLE) return 0;

        // Save the existing effect:
        Effect previousEffect = graphicsContext.getEffect(new ColorAdjust());
        double previousAlpha = graphicsContext.getGlobalAlpha();

        // Determine which effect to apply for this sprite:
        switch(visibility){
            case NORMAL:
                break;
            case GREYSCALE:
                graphicsContext.setEffect(new ColorAdjust(0,-1,0,0));
                break;
            case GREYSCALE_TRANSPARENT:
                graphicsContext.setEffect(new ColorAdjust(0,-1,0,0));
            case TRANSPARENT:
                graphicsContext.setGlobalAlpha(0.5);
                break;
        }

        // Draw the sprite:
        animation.getSpriteSheet().drawFrame(graphicsContext, anchorX, anchorY, frame);

        // Restore the previous effect:
        graphicsContext.setEffect(previousEffect);
        graphicsContext.setGlobalAlpha(previousAlpha);

        return 0;
    }

    // todo: take scale into account
    // This method should only ever be called by the JavaFX application thread.
    // Note: AnimationData may be best suited for use with a GraphicsContext. Why? You usually only have to keep track
    // of a single GraphicsContext and pass it to many AnimationDatas. If you used ImageViews, on the other hand, you
    // would have to keep track of a unique ImageView for each and every instance of AnimationData. Having many
    // instances of ImageView also takes more memory than having a single instance of GraphicsContext.
    public int drawSelf(ImageView imageView){
        if (!Platform.isFxApplicationThread()){
            System.err.println("ERROR! A non-JavaFX thread is attempting to call AnimationData.drawSelf(). " +
                    "That method updates UI components, so it must be called only from the JavaFX Application thread.");
            return 1;
        }

        if(visibility == VisibilityOption.INVISIBLE){
            imageView.setVisible(false);
            return 0;
        }

        imageView.setImage(animation.getSpriteSheet());

        SpriteSheet.FrameBound frameBound = animation.getSpriteSheet().getFrameBound(frame);
        imageView.setViewport(frameBound.getPosAndDim());
        double layoutX = anchorX-frameBound.getAnchorPoint().getX();
        double layoutY = anchorY-frameBound.getAnchorPoint().getY();
        imageView.relocate(layoutX,layoutY);
        return 0;
    }
}
