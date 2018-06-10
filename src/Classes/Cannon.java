package Classes;

import Classes.Images.CannonImages;
import Classes.NetworkCommunication.PlayerData;
import javafx.scene.effect.ColorAdjust;

/**
 * For displaying a cannon
 */
public class Cannon {
    private CannonImages cannonEnum;
    private Sprite staticBackground;
    private Sprite movingPart;
    private Sprite staticForeground;
    private int currentFrame = 0;

    public Cannon(PlayerData playerData){
        this.cannonEnum = playerData.getCannonEnum();
        staticBackground = new Sprite(cannonEnum.getBackgroundImageSpriteSheet());
        movingPart = new Sprite(cannonEnum.getMovingPartSpriteSheet());
        staticForeground = new Sprite(cannonEnum.getForegroundImageSpriteSheet());
        setCannonEnum(cannonEnum);
        setAngle(playerData.getCannonAngle());
    }

    public Sprite getMovingPart(){
        return movingPart;
    }
    public Sprite getStaticBackground(){
        return staticBackground;
    }
    public Sprite getStaticForeground(){
        return staticForeground;
    }

    public void setCannonEnum(CannonImages cannonEnum){
        this.cannonEnum = cannonEnum;
        staticBackground.setSpriteSheet(cannonEnum.getBackgroundImageSpriteSheet());
        movingPart.setSpriteSheet(cannonEnum.getMovingPartSpriteSheet());
        staticForeground.setSpriteSheet(cannonEnum.getForegroundImageSpriteSheet());
    }

    public void relocate(double x, double y){
        staticBackground.relocate(x,y);
        movingPart.relocate(x,y);
        staticForeground.relocate(x,y);
    }

    public void setAngle(double angle){
        movingPart.rotate(angle, currentFrame);
    }

    public void setScale(double scaleFactor){
        staticBackground.scale(scaleFactor,currentFrame);
        movingPart.scale(scaleFactor, currentFrame);
        staticForeground.scale(scaleFactor,currentFrame);
    }

    public double getPosX(){
        return movingPart.getLayoutX();
    }
    public double getPosY(){
        return movingPart.getLayoutY();
    }
    public double getAngle(){
        return movingPart.getAngle();
    }

    public void freeze(){
        ColorAdjust desaturator = new ColorAdjust(0,-1,0,0);
        staticBackground.setEffect(desaturator);
        movingPart.setEffect(desaturator);
        staticForeground.setEffect(desaturator);
        movingPart.setOpacity(0.5); // So that the cannon won't obscure the view of Orbs for remaining allies.
    }
}
