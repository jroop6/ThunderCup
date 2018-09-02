package Classes;

import Classes.Animation.Sprite;
import Classes.Images.CannonImages;
import Classes.NetworkCommunication.PlayerData;
import javafx.scene.effect.ColorAdjust;

/**
 * For displaying a cannon
 */
public class Cannon {
    private Sprite staticBackground;
    private Sprite movingPart;
    private Sprite staticForeground;

    public Cannon(PlayerData playerData){
        staticBackground = new Sprite(playerData.getCannonEnum().getBackgroundImageSpriteSheet());
        movingPart = new Sprite(playerData.getCannonEnum().getMovingPartSpriteSheet());
        staticForeground = new Sprite(playerData.getCannonEnum().getForegroundImageSpriteSheet());
        setAngle(playerData.getCannonAngle(), playerData.getCannonAnimationFrame());
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
        staticBackground.setSpriteSheet(cannonEnum.getBackgroundImageSpriteSheet());
        movingPart.setSpriteSheet(cannonEnum.getMovingPartSpriteSheet());
        staticForeground.setSpriteSheet(cannonEnum.getForegroundImageSpriteSheet());
    }

    public void relocate(double x, double y, int currentFrame){
        staticBackground.relocate(x,y,currentFrame);
        movingPart.relocate(x,y,currentFrame);
        staticForeground.relocate(x,y,currentFrame);
    }

    public void setAngle(double angle, int currentFrame){
        movingPart.rotate(angle, currentFrame);
    }

    public void setScale(double scaleFactor, int currentFrame){
        staticBackground.scale(scaleFactor,currentFrame);
        movingPart.scale(scaleFactor, currentFrame);
        staticForeground.scale(scaleFactor,currentFrame);
    }

    public double getPosX(){
        return movingPart.getxPos();
    }
    public double getPosY(){
        return movingPart.getyPos();
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
