package Classes.Animation;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.io.Serializable;
import java.util.Random;

public class AnimationData implements Serializable {
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
        frame = 0;
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
    public void setRotation(double amount){
        rotation = amount;
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
    private void setFrame(int index){
        frame = index;
    }
    public void setRandomFrame(){
        frame = (new Random()).nextInt(animation.getSpriteSheet().getMaxFrameIndex()+1);
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

    // This method should only ever be called by the JavaFX application thread.
    public int drawSelf(GraphicsContext graphicsContext){
        if (!Platform.isFxApplicationThread()){
            System.err.println("Warning! A non-JavaFX thread is attempting to call AnimationData.drawSelf(). " +
                    "That method updates UI components, so it must be called only from the JavaFX Application thread.");
            return 1;
        }

        // Save the existing effect:
        graphicsContext.save();

        // Determine which visual effect, if any, to apply for this sprite:
        switch(visibility){
            case INVISIBLE:
                graphicsContext.setGlobalAlpha(0);
            case NORMAL:
                graphicsContext.setGlobalAlpha(1);
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

        // Apply translation, rotation, and scale transforms. Note: Order matters! Contrary to logic, translation should occur before rotation:
        Point2D anchorPoint = animation.getSpriteSheet().getFrameBound(frame).getAnchorPoint();
        Affine transform = new Affine();
        transform.appendTranslation(anchorX-anchorPoint.getX(),anchorY-anchorPoint.getY());
        transform.appendRotation(rotation, anchorPoint);
        transform.appendScale(scale,scale,anchorPoint);
        graphicsContext.setTransform(transform);

        // Draw the sprite:
        animation.getSpriteSheet().drawFrame(graphicsContext, frame);

        // Restore the previous effect:
        graphicsContext.restore();

        return 0;
    }

    // This method should only ever be called by the JavaFX application thread.
    // Note: I recommend using drawSelf(GraphicsContext) instead, in most cases. Why? You usually only have to keep
    // track of a single GraphicsContext and pass it to many AnimationDatas. If you used ImageViews, on the other hand,
    // you would have to keep track of a unique ImageView for each and every instance of AnimationData. Having many
    // instances of ImageView also takes more memory than having a single instance of GraphicsContext. Also, this
    // implementation of drawSelf() is clearly suboptimal, seeing as how it clears all transforms and creates new ones
    // each frame. A more efficient implementation might cache the transforms somehow. I did not do this because I
    // wanted this method to take *any* ImageView. Since the ImageView can theoretically change from one frame to the
    // next, we can't cache pointers to its transforms.
    public int drawSelf(ImageView imageView){
        if (!Platform.isFxApplicationThread()){
            System.err.println("ERROR! A non-JavaFX thread is attempting to call AnimationData.drawSelf(). " +
                    "That method updates UI components, so it must be called only from the JavaFX Application thread.");
            return 1;
        }

        switch(visibility){
            case INVISIBLE:
                imageView.setVisible(false);
            case NORMAL:
                imageView.setVisible(true);
                imageView.setEffect(null);
                break;
            case GREYSCALE:
                imageView.setVisible(true);
                imageView.setEffect(new ColorAdjust(0,-1,0,0));
                break;
            case GREYSCALE_TRANSPARENT:
                imageView.setEffect(new ColorAdjust(0,-1,0,0));
            case TRANSPARENT:
                imageView.setVisible(true);
                imageView.setOpacity(0.5);
                break;
        }

        // Set the viewport to display only the current frame:
        SpriteSheet.FrameBound frameBound = animation.getSpriteSheet().getFrameBound(frame);
        imageView.setImage(animation.getSpriteSheet());
        imageView.setViewport(frameBound.getPosAndDim());

        // clear the existing transforms on this object:
        imageView.getTransforms().clear();

        // Apply translation, rotation, and scale transforms:
        Point2D anchorPoint = frameBound.getAnchorPoint();
        Translate translator = new Translate(anchorX-anchorPoint.getX(), anchorY-anchorPoint.getY());
        Rotate rotater = new Rotate(rotation, anchorPoint.getX(), anchorPoint.getY());
        Scale scaler = new Scale(scale, scale, anchorPoint.getX(), anchorPoint.getY());
        imageView.getTransforms().addAll(translator, rotater, scaler);

        return 0;
    }
}
