package Classes;

import Classes.Animation.*;
import Classes.Audio.SoundEffect;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

import java.io.Serializable;
import java.util.Random;

import static Classes.GameScene.DATA_FRAME_RATE;
import static Classes.PlayPanel.ORB_RADIUS;

public class Orb extends PointInt implements Serializable, Comparable<Orb>{
    private static final double TIME_TO_TRANSFER = 3; // how many seconds it takes for a transfer orb to materialize.
    private static final double TIME_TO_THUNDER = 1; // how many seconds it takes for a dropped orb to thunder.
    private static final double ELECTRIFICATION_PROBABILITY = .004;

    // Special orbs that are used to identify walls and the ceiling as collision objects. Used in collision
    // detection logic within the PlayPanel class.
    static final Orb WALL = new Orb(OrbColor.RED,-1,-1, OrbAnimationState.STATIC);
    static final Orb CEILING = new Orb(OrbColor.YELLOW,-1,-1, OrbAnimationState.STATIC);

    // Special orb that indicates an unoccupied space on the orb array
    // Note to self: This is better memory-wise than creating an NULL OrbType because we only have 1 instance of this
    // orb instead of literally hundreds for all the unusable locations on the orbArray.
    public static final Orb NULL = new Orb(OrbColor.BLACK, -2, -2, OrbAnimationState.STATIC);

    private OrbColor orbColor;
    private Animation orbAnimation; // The animation currently selected for the Orb.
    private Animation orbElectrification; // the animation selected for displaying the electrification animation.
    private Animation orbExplosion; // the animation selected for displaying the burst animation.
    private SoundEffect orbThunder; // enum used for displaying thunder animation.
    private OrbAnimationState orbAnimationState;

    private Random miscRandomGenerator = new Random();

    private double angle; // angle of travel, radians
    private double speed; // current speed, pixels per second

    // A frame count for the "transferring" and "thundering" animations, which are handled in a special way (without using an Animation)
    private int currentFrame = 0;

    // Timestamps so that the host and client will know when the orbs were fired:
    private long rawTimestamp; // simply the client's System.nanotime()
    private long timeStamp; // how many nanoseconds have passed since the previous packet was sent from the client.

    public Orb(OrbColor orbColor, int iPos, int jPos, OrbAnimationState orbAnimationState){
        super(iPos, jPos);
        this.orbColor = orbColor;
        orbAnimation = new Animation(orbColor.getImplodeAnimationName());
        orbAnimation.setStatus(StatusOption.PAUSED);
        setIJ(iPos, jPos);
        setOrbAnimationState(orbAnimationState);
    }

    /* Copy Constructor */
    public Orb(Orb other){
        super(other.i, other.j);
        orbColor = other.orbColor;
        orbAnimation = new Animation(other.orbAnimation);
        if(other.orbElectrification!=null) orbElectrification = new Animation(other.orbElectrification);
        if(other.orbExplosion!=null) orbExplosion = new Animation(other.orbExplosion);
        orbThunder = other.orbThunder;
        setOrbAnimationState(other.orbAnimationState);

        angle = other.angle;
        speed = other.speed;

        currentFrame = other.currentFrame;

        rawTimestamp = other.rawTimestamp;
        timeStamp = other.timeStamp;
    }

    public void relocate(double xPos, double yPos){
        orbAnimation.relocate(xPos, yPos);
    }
    public void setAngle(double angle){
        this.angle = angle;
    }
    public void setSpeed(double speed){
        this.speed = speed;
    }
    public void setRawTimestamp(long rawTimestamp){
        this.rawTimestamp = rawTimestamp;
    }
    public void setOrbAnimationState(OrbAnimationState orbAnimationState){
        this.orbAnimationState = orbAnimationState;
        switch(orbAnimationState){
            case BURSTING:
                // Get a bursting animation Todo: randomize this once I have more than 1 type of animation
                orbExplosion = new Animation(AnimationName.EXPLOSION_1, orbAnimation.getAnchorX(), orbAnimation.getAnchorY(), PlayOption.PLAY_ONCE_THEN_VANISH);
                break;
            case IMPLODING:
                orbAnimation.setStatus(StatusOption.PLAYING);
                break;
            case DROPPING:
            case STATIC:
                orbExplosion = null;
                orbElectrification = null;
                orbAnimation.setVisibility(VisibilityOption.NORMAL);
                break;
            case ELECTRIFYING:
                // Get an electrification animation Todo: randomize this once I have more than 1 type of animation
                orbElectrification = new Animation(AnimationName.ELECTRIFICATION_1, orbAnimation.getAnchorX(), orbAnimation.getAnchorY(), PlayOption.PLAY_ONCE_THEN_VANISH);
                break;
            case TRANSFERRING:
                speed = 0.0;
                currentFrame = 0;
                orbAnimation.setVisibility(VisibilityOption.TRANSPARENT);
                break;
            case THUNDERING:
                // Get a thunder enumeration todo: randomize this once I have more than 1 type of animation/sound effect
                // orbThunder = SoundEffect.THUNDER_1;
                currentFrame = 0;
        }
    }
    @Override
    public void setIJ(int i, int j){
        this.i = i;
        this.j = j;
        double xPos = ORB_RADIUS + ORB_RADIUS *(j);
        double yPos = ORB_RADIUS + i*PlayPanel.ROW_HEIGHT;
        orbAnimation.relocate(xPos, yPos);
    }
    public void computeTimeStamp(long timeLastPacketSent){
        timeStamp = rawTimestamp - timeLastPacketSent;
    }
    public void setOrbColor(OrbColor orbColor){
        this.orbColor = orbColor;
        orbAnimation.setAnimationName(orbColor.getImplodeAnimationName());
    }

    public double getXPos(){
        return orbAnimation.getAnchorX();
    }
    public double getYPos(){
        return orbAnimation.getAnchorY();
    }
    public double getAngle(){
        return angle;
    }
    public double getSpeed(){
        return speed;
    }
    public OrbAnimationState getOrbAnimationState(){
        return orbAnimationState;
    }
    public OrbColor getOrbColor(){
        return orbColor;
    }

    // called 24 times per second.
    // A return of "true" means that an animation sequence has ended.
    public boolean tick(){
        switch(orbAnimationState){
            case STATIC:
                if(miscRandomGenerator.nextDouble()< ELECTRIFICATION_PROBABILITY){
                    setOrbAnimationState(Orb.OrbAnimationState.ELECTRIFYING);
                    return true;
                }
                break;
            case DROPPING:
                break; // note: The Orb's physical location is changed in PlayPanel because we don't know the size of the playpanel here in the Orb class (so we wouldn't know when it goes off the bottom edge of the PlayPanel).
            case IMPLODING:
                if(orbAnimation.tick()) {
                    setOrbAnimationState(OrbAnimationState.BURSTING);
                }
                break;
            case BURSTING:
                if(orbExplosion.tick()){
                    return true;
                }
                break;
            case ELECTRIFYING:
                if(orbElectrification.tick()){
                    setOrbAnimationState(OrbAnimationState.STATIC);
                    return true;
                }
                break;
            case TRANSFERRING:
                currentFrame++;
                if(currentFrame >TIME_TO_TRANSFER* DATA_FRAME_RATE){
                    setOrbAnimationState(OrbAnimationState.STATIC);
                    return true;
                }
                break;
            case THUNDERING:
                currentFrame++;
                if(currentFrame > TIME_TO_THUNDER* DATA_FRAME_RATE){
                    // Todo: maybe play a more subtle sound effect or show some visual effect???
                    // todo: also, don't invoke the SoundManager here. This isn't the Application thread. Instead, add the sound to the soundEffectsToPlay set.
                    //SoundManager.playSoundEffect(orbThunder);
                    return true;
                }
        }

        return false;
    }

    //todo: incorporate vibrationOffset with the new animation framework (probably just include an offset parameter in Animation.drawSelf()).
    public void drawSelf(GraphicsContext orbDrawer, double vibrationOffset){
        if(this.equals(NULL)) return; // NULL orbs are not drawn.
        int err;
        switch(orbAnimationState){
            case STATIC:
            case DROPPING:
            case IMPLODING:
                err = orbAnimation.drawSelf(orbDrawer);
                if(err!=0) System.err.println("imploding");
                break;
            case BURSTING:
                err = orbExplosion.drawSelf(orbDrawer);
                if(err!=0) System.err.println("bursting");
                break;
            case ELECTRIFYING:
                err = orbAnimation.drawSelf(orbDrawer);
                if(err!=0) System.err.println("electrifying - orbAnimation");
                err = orbElectrification.drawSelf(orbDrawer);
                if(err!=0) System.err.println("electrifying - orbElectrification");
                break;
            case TRANSFERRING:
                err = orbAnimation.drawSelf(orbDrawer);
                if(err!=0) System.err.println("transferring");
                // draw an outline that shows how much time is left before the transfer is complete:
                orbDrawer.setStroke(Color.rgb(0,255,255));
                orbDrawer.setLineWidth(2.0);
                orbDrawer.strokeArc(orbAnimation.getAnchorX()-ORB_RADIUS,orbAnimation.getAnchorY()-ORB_RADIUS + vibrationOffset,ORB_RADIUS*2,ORB_RADIUS*2,90,-360* currentFrame /(TIME_TO_TRANSFER* DATA_FRAME_RATE),ArcType.OPEN);
        }
    }

    @Override
    public boolean equals(Object other){
        Orb otherOrb;
        if (!(other instanceof Orb)) return false;
        else otherOrb = (Orb) other;

        if(i==-2){ // This orb is a NULL orb
            return otherOrb.i==-2;
        }
        else if(i==-1){ // This orb is a shooting Orb
            double tolerance = 1;
            return otherOrb.i==-1
                    && otherOrb.orbColor == orbColor
                    /*&& Math.abs(otherOrb.getXPos()-getXPos())<tolerance
                    && Math.abs(otherOrb.getYPos()-getYPos())<tolerance*/
                    && Math.abs(Math.toDegrees(otherOrb.angle-angle))<tolerance;
        }
        else{ // The orbs are array Orbs, so we don't need to check locations. Note: the (i,j) coordinates don't need to be checked because equals() is only ever called on an arrayOrb by deepEquals(), which is already going element-by-element.
            return otherOrb.i >= 0
                    && otherOrb.orbColor == orbColor;
        }
    }

    @Override
    public int compareTo(Orb other){
        if(i==-1){ // This orb is a shooting Orb
            double tolerance = 1;
            if(other.i==-1
                    && other.orbColor == orbColor
                    /*&& Math.abs(other.getXPos()-getXPos())<tolerance
                    && Math.abs(other.getYPos()-getYPos())<tolerance*/
                    && Math.abs(Math.toDegrees(other.angle-angle))<tolerance) return 0;
            else return -1;
        }
        else{ // The orbs are array Orbs, so we don't need to check locations. Note: the (i,j) coordinates don't need to be checked because equals() is only ever called by deepEquals(), which is already going element-by-element.
            if(other.i!=-1 && other.orbColor == orbColor) return 0;
            else return -1;
        }
    }

    // The hashCode is the sum of two cantor pairings.
    @Override
    public int hashCode(){
        int xPosInt = (int)getXPos()/5;
        int yPosInt = (int)getYPos()/5;
        int angleInt = (int)angle;
        int enumInt = orbColor.ordinal();
        int cantor1 = ((xPosInt+yPosInt)*(xPosInt+yPosInt+1))/2 + yPosInt;
        int cantor2 = ((angleInt+enumInt)*(angleInt+enumInt+1))/2 + enumInt;
        return cantor1 + cantor2;
    }

    public enum OrbAnimationState {
        STATIC,
        ELECTRIFYING,
        IMPLODING,
        BURSTING,
        DROPPING,
        TRANSFERRING,
        THUNDERING
    }
}



