package Classes.Animation;

public class AnimationData {
    Animation currentAnimation;
    private double anchorX = 0.0;
    private double anchorY = 0.0;
    private double scale = 1.0;
    private double rotation = 0.0;
    private Visibility currentVisibility = Visibility.NORMAL;
    private int frame = 0;

    AnimationData(Animation startingAnimation){
        currentAnimation = startingAnimation;
    }

    public void setAnimation(Animation newAnimation){
        currentAnimation = newAnimation;
    }

    public void relocate(double x, double y){
        anchorX = x;
        anchorY = y;
    }
    public double getAnchorX(){
        return anchorX;
    }
    public double getAnchorY(){
        return anchorY;
    }

    public void setScale(double amount){
        scale = amount;
    }

    public void rotate(double amount){
        rotation += amount;
    }

    public void setVisibility(Visibility visibility){
        currentVisibility = visibility;
    }
    enum Visibility{NORMAL, GREYSCALE, TRANSPARENT, GREYSCALE_TRANSPARENT, INVISIBLE}
    public Visibility getVisibility(){
        return currentVisibility;
    }

    public void setFrame(int index){
        frame = index;
    }
    public boolean incrementFrame(){
        frame++;
        if(frame > currentAnimation.getSpriteSheet().getMaxFrameIndex()){
            frame = currentAnimation.getSpriteSheet().getMaxFrameIndex(); // to make sure we stay within bounds.
            return true;
        }
        else return false;
    }
    public int getFrame(){
        return frame;
    }

    public boolean decrementFrame(){
        frame--;
        if(frame < 0){
            frame = 0; // to make sure we stay within bounds.
            return true;
        }
        else return false;
    }
}
