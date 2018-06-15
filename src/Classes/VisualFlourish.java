package Classes;

import Classes.Animation.MiscAnimations;
import javafx.scene.canvas.GraphicsContext;

// For miscellaneous animations displayed on the PlayPanel
// Todo: make all animation controllers (except perhaps Orb) instances of this class or extend this class.
public class VisualFlourish {
    private int currentFrame = 0;
    private double xPos;
    private double yPos;
    private MiscAnimations animationEnum;

    public VisualFlourish(MiscAnimations animationEnum, double xPos, double yPos){
        this.animationEnum = animationEnum;
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public boolean animationTick(){
        currentFrame++;
        return currentFrame > animationEnum.getSpriteSheet().getMaxFrameIndex();
    }

    public void drawSelf(GraphicsContext orbDrawer){
        animationEnum.getSpriteSheet().drawSprite(orbDrawer, xPos, yPos, currentFrame);
    }
}
