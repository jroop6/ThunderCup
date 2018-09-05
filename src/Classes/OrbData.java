package Classes;

import Classes.Animation.*;
import Classes.Audio.SoundEffect;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

import java.io.Serializable;

import static Classes.GameScene.FRAME_RATE;

public class OrbData extends PointInt implements Serializable{
    public static final double ORB_RADIUS = 23.0;
    private static final double TIME_TO_TRANSFER = 3; // how much time it takes for a transfer orb to materialize.
    private static final double TIME_TO_THUNDER = 1; // how much time it takes for a dropped orb to thunder.

    // Special orbs that are used to identify walls and the ceiling as collision objects. Used in collision
    // detection logic within the PlayPanel class.
    static final OrbData WALL = new OrbData(OrbColor.BLACK,0,0, OrbAnimationState.STATIC);
    static final OrbData CEILING = new OrbData(OrbColor.BLACK,0,0, OrbAnimationState.STATIC);

    // Special orb that indicates an unoccupied space on the orb array
    // Note to self: This is better memory-wise than creating an NULL OrbType because we only have 1 instance of this
    // orb instead of literally hundreds for all the unusable locations on the orbArray.
    public static final OrbData NULL = new OrbData(OrbColor.BLACK, -1, -1, OrbAnimationState.STATIC);

    private OrbColor orbColor;
    private AnimationData orbAnimation; // The animation currently selected for the Orb.
    private AnimationData orbElectrification; // the animation selected for displaying the electrification animation.
    private AnimationData orbExplosion; // the animation selected for displaying the burst animation.
    private SoundEffect orbThunder; // enum used for displaying thunder animation.
    private OrbAnimationState orbAnimationState;

    private double angle; // angle of travel, radians
    private double speed; // current speed, pixels per second

    // A frame count for the "transferring" and "thundering" animations, which are handled in a special way (without using an AnimationData)
    private int currentFrame = 0;

    // Timestamps so that the host and client will know when the orbs were fired:
    private long rawTimestamp; // simply the client's System.nanotime()
    private long timeStamp; // how many nanoseconds have passed since the previous packet was sent from the client.

    public OrbData(OrbColor orbColor, int iPos, int jPos, OrbAnimationState orbAnimationState){
        super(iPos, jPos);
        this.orbColor = orbColor;
        orbAnimation = new AnimationData(orbColor.getImplodeAnimation());
        orbAnimation.setStatus(StatusOption.PAUSED);
        setIJ(iPos, jPos);
        setOrbAnimationState(orbAnimationState);
    }

    /* Copy Constructor */
    public OrbData(OrbData other){
        super(other.getI(), other.getJ());
        orbColor = other.getOrbColor();
        orbAnimation = new AnimationData(other.getOrbAnimation());
        if(other.getOrbElectrification()!=null) orbElectrification = new AnimationData(other.getOrbElectrification());
        if(other.getOrbExplosion()!=null) orbExplosion = new AnimationData(other.getOrbExplosion());
        orbThunder = other.getOrbThunder();
        orbAnimationState = other.getOrbAnimationState();

        angle = other.getAngle();
        speed = other.getSpeed();

        currentFrame = other.getCurrentFrame();

        rawTimestamp = other.getRawTimestamp();
        timeStamp = other.getTimestamp();
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
                orbExplosion = new AnimationData(Animation.EXPLOSION_1, orbAnimation.getAnchorX(), orbAnimation.getAnchorY(), PlayOption.PLAY_ONCE_THEN_VANISH);
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
                orbElectrification = new AnimationData(Animation.ELECTRIFICATION_1, orbAnimation.getAnchorX(), orbAnimation.getAnchorY(), PlayOption.PLAY_ONCE_THEN_VANISH);
                orbElectrification.setFrame(0);
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
    public void setCurrentFrame(int newIndex){
        currentFrame = newIndex;
    }
    public void computeTimeStamp(long timeLastPacketSent){
        timeStamp = rawTimestamp - timeLastPacketSent;
    }
    public void setOrbColor(OrbColor orbColor){
        this.orbColor = orbColor;
        orbAnimation.setAnimation(orbColor.getImplodeAnimation());
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
    double getSpeed(){
        return speed;
    }
    long getRawTimestamp(){
        return rawTimestamp;
    }
    long getTimestamp(){
        return timeStamp;
    }
    public OrbAnimationState getOrbAnimationState(){
        return orbAnimationState;
    }
    public OrbColor getOrbColor(){
        return orbColor;
    }
    public AnimationData getOrbAnimation(){
        return orbAnimation;
    }
    public AnimationData getOrbElectrification(){
        return orbElectrification;
    }
    public AnimationData getOrbExplosion(){
        return orbExplosion;
    }
    public SoundEffect getOrbThunder(){
        return orbThunder;
    }
    public int getCurrentFrame(){
        return currentFrame;
    }

    // called 24 times per second.
    // A return of "true" means that an animation sequence has ended.
    public boolean tick(){
        switch(orbAnimationState){
            case STATIC:
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
                if(currentFrame >TIME_TO_TRANSFER* FRAME_RATE){
                    setOrbAnimationState(OrbAnimationState.STATIC);
                    return true;
                }
                break;
            case THUNDERING:
                currentFrame++;
                if(currentFrame > TIME_TO_THUNDER* FRAME_RATE){
                    // Todo: maybe play a more subtle sound effect or show some visual effect???
                    // todo: also, don't invoke the SoundManager here. This isn't the Application thread. Instead, add the sound to the soundEffectsToPlay set.
                    //SoundManager.playSoundEffect(orbThunder);
                    return true;
                }
        }

        return false;
    }

    //todo: incorporate vibrationOffset with the new animation framework (probably just include an offset parameter in AnimationData.drawSelf()).
    public void drawSelf(GraphicsContext orbDrawer, double vibrationOffset){
        if(this == NULL) return; // null orbs are not drawn.
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
                orbDrawer.strokeArc(orbAnimation.getAnchorX()-ORB_RADIUS,orbAnimation.getAnchorY()-ORB_RADIUS + vibrationOffset,ORB_RADIUS*2,ORB_RADIUS*2,90,-360* currentFrame /(TIME_TO_TRANSFER* FRAME_RATE),ArcType.OPEN);
        }
    }

    // Two orbs are considered the same if their positions and angles are nearly the same and they have the same enumeration ordinal.
    @Override
    public boolean equals(Object other){
        if (!(other instanceof OrbData)) return false;
        else{
            OrbData otherOrbData = (OrbData) other;
            double tolerance = 1;
            if(Math.abs(otherOrbData.getXPos()-getXPos())<tolerance
                    && Math.abs(otherOrbData.getYPos()-getYPos())<tolerance
                    && otherOrbData.getOrbColor() == orbColor
                    && Math.abs(otherOrbData.getAngle()-angle)<tolerance/180) return true;
            else return false;
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



