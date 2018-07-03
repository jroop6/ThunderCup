package Classes;

import Classes.Audio.SoundEffect;
import Classes.Animation.OrbElectrification;
import Classes.Animation.OrbExplosion;
import Classes.Animation.OrbImages;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import static Classes.GameScene.ANIMATION_FRAME_RATE;

/**
 * Created by HydrusBeta on 8/16/2017.
 */
public class Orb implements Serializable{
    private OrbImages orbEnum;
    private OrbElectrification orbElectrification; // enum used for displaying electrification animation.
    private OrbExplosion orbExplosion; // enum used for displaying burst animation.
    private SoundEffect orbThunder; // enum used for displaying thunder animation.
    private BubbleAnimationType animationEnum;
    public static final double ORB_RADIUS = 23.0;
    public static final double TIME_TO_TRANSFER = 3; // how much time it takes for a transfer orb to materialize.
    public static final double TIME_TO_THUNDER = 1; // how much time it takes for a dropped orb to thunder.

    // Special orbs that are used to identify walls and the ceiling as collision objects. Used in collision
    // detection logic within the PlayPanel class.
    public static final Orb WALL = new Orb(OrbImages.BLACK_ORB,0,0, BubbleAnimationType.STATIC);
    public static final Orb CEILING = new Orb(OrbImages.BLACK_ORB,0,0, BubbleAnimationType.STATIC);

    // Special orb that indicates an unoccupied space on the orb array
    // Note to self: This is better memory-wise than creating an NULL OrbImage because we only have 1 instance of this orb instead of literally hundreds.
    public static final Orb NULL = new Orb(OrbImages.BLACK_ORB, 0, 0, BubbleAnimationType.STATIC);

    // Cached constant for better performance:
    private static final double ROOT3 = Math.sqrt(3.0);

    // The Orb knows its physical location in the PlayPanel (note: these are NOT row/column indices, but pixel positions):
    private double xPos;
    private double yPos;
    private double angle; // angle of travel, radians
    private double speed; // current speed, pixels per second

    // If the orb is on the array, it also knows its index:
    private int iPos;
    private int jPos;

    // The Orb's current animation frames:
    private int currentFrame = 0;
    private int electrificationAnimationFrame = 0; // electrification frame index is maintained separately.

    // Timestamps so that the host and client will know when the orbs were fired:
    private long rawTimestamp; // simply the client's System.nanotime()
    private long timeStamp; // how many nanoseconds have passed since the previous packet was sent from the client.


    public Orb(OrbImages orbEnum, int iPos, int jPos, BubbleAnimationType animationEnum){
        this.orbEnum = orbEnum;
        setIJ(iPos, jPos);
        setAnimationEnum(animationEnum);
    }

    /* Copy Constructor */
    public Orb(Orb other){
        orbEnum = other.getOrbEnum();
        orbElectrification = other.getOrbElectrification();
        orbExplosion = other.getOrbExplosion();
        orbThunder = other.getOrbThunder();
        animationEnum = other.getAnimationEnum();

        xPos = other.getXPos();
        yPos = other.getYPos();
        angle = other.getAngle();
        speed = other.getSpeed();
        iPos = other.getI();
        jPos = other.getJ();

        currentFrame = other.getCurrentFrame();
        electrificationAnimationFrame = other.getElectrificationAnimationFrame();

        rawTimestamp = other.getRawTimestamp();
        timeStamp = other.getTimestamp();
    }

    public void setXPos(double xPos){
        this.xPos = xPos;
    }
    public void setYPos(double yPos){
        this.yPos = yPos;
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
    public void setAnimationEnum(BubbleAnimationType animationEnum){
        this.animationEnum = animationEnum;
        switch(animationEnum){
            case BURSTING:
                // Get a bursting animation Todo: randomize this once I have more than 1 type of animation
                orbExplosion = OrbExplosion.EXPLOSION_1;
                currentFrame = 0;
            case IMPLODING:
                currentFrame = 0;
            case DROPPING:
                currentFrame = 0;
            case STATIC:
                currentFrame = 0;
                break;
            case ELECTRIFYING:
                // Get an electrification animation Todo: randomize this once I have more than 1 type of animation
                orbElectrification = OrbElectrification.ELECTRIFICATION_1;
                electrificationAnimationFrame = 0;
                break;
            case TRANSFERRING:
                speed = 0.0;
                currentFrame = 0;
            case THUNDERING:
                // Get a thunder enumeration todo: randomize this once I have more than 1 type of animation/sound effect
                // orbThunder = SoundEffect.THUNDER_1;
                currentFrame = 0;
        }
    }
    public void setIJ(int i, int j){
        iPos = i;
        jPos = j;
        xPos = ORB_RADIUS + ORB_RADIUS *(jPos);
        yPos = ORB_RADIUS + iPos*PlayPanel.ROW_HEIGHT;
    }
    public void setCurrentFrame(int newIndex){
        currentFrame = newIndex;
    }
    public void computeTimeStamp(long timeLastPacketSent){
        timeStamp = rawTimestamp - timeLastPacketSent;
    }
    public void setOrbEnum(OrbImages orbEnum){
        this.orbEnum = orbEnum;
    }

    public double getXPos(){
        return xPos;
    }
    public double getYPos(){
        return yPos;
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
    public BubbleAnimationType getAnimationEnum(){
        return animationEnum;
    }
    public int getI(){
        return iPos;
    }
    public int getJ(){
        return jPos;
    }
    public OrbImages getOrbEnum(){
        return orbEnum;
    }
    public OrbElectrification getOrbElectrification(){
        return orbElectrification;
    }
    public OrbExplosion getOrbExplosion(){
        return orbExplosion;
    }
    public SoundEffect getOrbThunder(){
        return orbThunder;
    }
    public int getCurrentFrame(){
        return currentFrame;
    }
    public int getElectrificationAnimationFrame(){
        return electrificationAnimationFrame;
    }



    // called 24 times per second.
    // A return of "true" means that the animation sequence has ended.
    public boolean animationTick(){
        switch(animationEnum){
            case STATIC:
                break;
            case DROPPING:
                break; // note: The Orb's physical location is changed in PlayPanel because we don't know the size of the playpanel here in the Orb class (so we wouldn't know when it goes off the bottom edge of the PlayPanel).
            case IMPLODING:
                currentFrame++;
                if(currentFrame > orbEnum.getSpriteSheet().getMaxFrameIndex()){
                    setAnimationEnum(BubbleAnimationType.BURSTING);
                }
                break;
            case BURSTING:
                currentFrame++;
                if(currentFrame > orbExplosion.getSpriteSheet().getMaxFrameIndex()){
                    return true;
                }
                break;
            case ELECTRIFYING:
                electrificationAnimationFrame++;
                if(electrificationAnimationFrame>orbElectrification.getSpriteSheet().getMaxFrameIndex()){
                    setAnimationEnum(BubbleAnimationType.STATIC);
                    return true;
                }
                break;
            case TRANSFERRING:
                currentFrame++;
                if(currentFrame >TIME_TO_TRANSFER*ANIMATION_FRAME_RATE){
                    setAnimationEnum(BubbleAnimationType.STATIC);
                    return true;
                }
                break;
            case THUNDERING:
                currentFrame++;
                if(currentFrame > TIME_TO_THUNDER*ANIMATION_FRAME_RATE){
                    // Todo: maybe play a more subtle sound effect or show some visual effect???
                    //SoundManager.playSoundEffect(orbThunder);
                    return true;
                }
        }

        return false;
    }

    public void drawSelf(GraphicsContext orbDrawer, double vibrationOffset){
        if(this == NULL) return; // null orbs are not drawn.
        int err;
        switch(animationEnum){
            case STATIC:
            case DROPPING:
            case IMPLODING:
                err = orbEnum.getSpriteSheet().drawSprite(orbDrawer, xPos, yPos + vibrationOffset, currentFrame);
                if(err!=0) System.err.println("imploding");
                break;
            case BURSTING:
                err = orbExplosion.getSpriteSheet().drawSprite(orbDrawer, xPos, yPos + vibrationOffset, currentFrame);
                if(err!=0) System.err.println("bursting");
                break;
            case ELECTRIFYING:
                err = orbEnum.getSpriteSheet().drawSprite(orbDrawer, xPos, yPos + vibrationOffset, currentFrame);
                if(err!=0) System.err.println("electrifying - orbEnum");
                err = orbElectrification.getSpriteSheet().drawSprite(orbDrawer, xPos, yPos + vibrationOffset, electrificationAnimationFrame);
                if(err!=0) System.err.println("electrifying - orbElectrification");
                break;
            case TRANSFERRING:
                // draw the orb at 50% transparency:
                orbDrawer.setGlobalAlpha(0.5);
                err = orbEnum.getSpriteSheet().drawSprite(orbDrawer, xPos, yPos + vibrationOffset, 0);
                if(err!=0) System.err.println("transferring");
                orbDrawer.setGlobalAlpha(1.0);
                // draw an outline that shows how much time is left before the transfer is complete:
                orbDrawer.setStroke(Color.rgb(0,255,255));
                orbDrawer.setLineWidth(2.0);
                orbDrawer.strokeArc(xPos-ORB_RADIUS,yPos-ORB_RADIUS + vibrationOffset,ORB_RADIUS*2,ORB_RADIUS*2,90,-360* currentFrame /(TIME_TO_TRANSFER*ANIMATION_FRAME_RATE),ArcType.OPEN);
        }
    }

    // Convert Orb coordinates from (x, y) space (position on canvas in pixels) to (i, j) space (indices in orb array)
    // results will be doubles, not round integers.
    // Todo: Uh... why did I do this way, again? Seems like this should take 2 doubles as arguments and arrayOrbs should already be keeping track of their (i,j) coordinates. Consider a refactor.
    public Point2D xyToIj() {
        double iCoordinate = (yPos-ORB_RADIUS)/(ORB_RADIUS*ROOT3);
        double jCoordinate = (xPos-ORB_RADIUS)/(ORB_RADIUS);
        Point2D ijCoordinates = new Point2D(iCoordinate, jCoordinate);
        return ijCoordinates;
    }

    // returns the four closest Orb snap locations centers in (i, j) space. The coordinates returned are NOT guaranteed to be valid!
    // (i.e. they may be negative or too large).
    public List<PointInt> findClosestOrbSnapsIJ() {
        Point2D sourceIJ = this.xyToIj();
        int floorI = (int)Math.round(Math.floor(sourceIJ.getX()));
        int floorJ = (int)Math.round(Math.floor(sourceIJ.getY()));
        int ceilI = (int)Math.round(Math.ceil(sourceIJ.getX()));
        int ceilJ = (int)Math.round(Math.ceil(sourceIJ.getY()));

        PointInt orbSnap;
        List<PointInt> closestOrbSnaps = new LinkedList<>();
        orbSnap = new PointInt(floorI, floorJ);
        closestOrbSnaps.add(orbSnap);
        orbSnap = new PointInt(floorI, ceilJ);
        closestOrbSnaps.add(orbSnap);
        orbSnap = new PointInt(ceilI, ceilJ);
        closestOrbSnaps.add(orbSnap);
        orbSnap = new PointInt(ceilI, floorJ);
        closestOrbSnaps.add(orbSnap);

        return closestOrbSnaps;
    }

    public double computeSquareDistance(Orb otherOrb)
    {
         double xDifference = this.getXPos() - otherOrb.getXPos();
         double yDifference = this.getYPos() - otherOrb.getYPos();
         return xDifference*xDifference + yDifference*yDifference;
    }

    /*public static Point2D.Double findClosestOrbSnapsXY(Point2D.Double sourceXY)
    {
        List<Point> closestOrbSnapsIJ = findClosestOrbSnapsIJ(sourceXY);

        for (Point orbSnap: closestOrbSnapsIJ) {

        }
    }*/

    // Two orbs are considered the same if their positions and angles are nearly the same and they have the same enumeration ordinal.
    @Override
    public boolean equals(Object other){
        if (!(other instanceof Orb)) return false;
        else{
            Orb otherOrb = (Orb) other;
            double tolerance = 1;
            if(Math.abs(otherOrb.xPos-xPos)<tolerance
                    && Math.abs(otherOrb.yPos-yPos)<tolerance
                    && otherOrb.getOrbEnum() == orbEnum
                    && Math.abs(otherOrb.getAngle()-angle)<tolerance/180) return true;
            else return false;
        }
    }

    // The hashCode is the sum of two cantor pairings.
    @Override
    public int hashCode(){
        int xPosInt = (int)xPos/5;
        int yPosInt = (int)yPos/5;
        int angleInt = (int)angle;
        int enumInt = orbEnum.ordinal();
        int cantor1 = ((xPosInt+yPosInt)*(xPosInt+yPosInt+1))/2 + yPosInt;
        int cantor2 = ((angleInt+enumInt)*(angleInt+enumInt+1))/2 + enumInt;
        return cantor1 + cantor2;
    }

    public enum BubbleAnimationType{
        STATIC,
        ELECTRIFYING,
        IMPLODING,
        BURSTING,
        DROPPING,
        TRANSFERRING,
        THUNDERING
    }
}



