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
    private boolean sticky; // Should the last frame continue to be displayed when the animation is over?

    public VisualFlourish(MiscAnimations animationEnum, double xPos, double yPos, boolean sticky){
        this.animationEnum = animationEnum;
        this.xPos = xPos;
        this.yPos = yPos;
        this.sticky = sticky;
    }

    public boolean animationTick(){
        currentFrame++;
        if(currentFrame > animationEnum.getSpriteSheet().getMaxFrameIndex()){
            if(sticky){
                currentFrame = animationEnum.getSpriteSheet().getMaxFrameIndex();
                return false;
            }
            else return true;
        }
        return false;
    }

    public void drawSelf(GraphicsContext orbDrawer){
        animationEnum.getSpriteSheet().drawSprite(orbDrawer, xPos, yPos, currentFrame);
    }

    public void relocate(double xPos, double yPos){
        this.xPos = xPos;
        this.yPos = yPos;
    }
}
