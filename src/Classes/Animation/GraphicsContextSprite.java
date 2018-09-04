package Classes.Animation;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;

public class GraphicsContextSprite extends AnimationView {
    GraphicsContext graphicsContext;
    public GraphicsContextSprite(GraphicsContext graphicsContext){
        this.graphicsContext = graphicsContext;
    }

    // todo: take scale into account
    public int drawFrame(AnimationData animationData){
        if(animationData.getVisibility() == AnimationData.Visibility.INVISIBLE) return 0;

        // Save the existing effect:
        Effect previousEffect = graphicsContext.getEffect(new ColorAdjust());
        double previousAlpha = graphicsContext.getGlobalAlpha();

        // Determine which effect to apply for this sprite:
        switch(animationData.getVisibility()){
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
        animationData.currentAnimation.getSpriteSheet().drawFrame(graphicsContext, animationData.getAnchorX(), animationData.getAnchorY(), animationData.getFrame());

        // Restore the previous effect:
        graphicsContext.setEffect(previousEffect);
        graphicsContext.setGlobalAlpha(previousAlpha);

        return 0;
    }

    public void setScale(double scale){
        // nothing to do here for this implementation of AnimationView.
    }
}
