package Classes.Animation;

import javafx.scene.canvas.GraphicsContext;

import java.io.Serializable;

// For miscellaneous animations displayed on the PlayPanel
// Todo: make all animation controllers (except perhaps Orb) instances of this class or extend this class.
public class VisualFlourish implements Serializable {
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

    public VisualFlourish(VisualFlourish other){
        this.animationEnum = other.animationEnum;
        this.xPos = other.xPos;
        this.yPos = other.yPos;
        this.sticky = other.sticky;
        this.currentFrame = other.currentFrame;
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
        int err= animationEnum.getSpriteSheet().drawSprite(orbDrawer, xPos, yPos, currentFrame);
        if(err!=0) System.err.println("visualFlourish");
    }

    public void relocate(double xPos, double yPos){
        this.xPos = xPos;
        this.yPos = yPos;
    }
}
