package Classes;

import Classes.Images.CannonImages;
import Classes.NetworkCommunication.PlayerData;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

/**
 * For displaying a cannon
 */
public class Cannon {
    private CannonImages cannonEnum;
    private ImageView staticBackground = new ImageView();
    private ImageView movingPart = new ImageView();
    private ImageView staticForeground = new ImageView();
    private Rotate rotater = new Rotate();
    private double posX;
    private double posY;
    private Scale movingPartsScaler = new Scale();
    private Scale staticPartsScaler = new Scale();

    public Cannon(PlayerData playerData){
        this.cannonEnum = playerData.getCannonEnum();
        movingPart.getTransforms().add(rotater);
        movingPart.getTransforms().add(movingPartsScaler);
        staticBackground.getTransforms().add(staticPartsScaler);
        staticForeground.getTransforms().add(staticPartsScaler);
        setCannonEnum(cannonEnum);
        setAngle(playerData.getCannonAngle());
    }

    public ImageView getMovingPart(){
        return movingPart;
    }
    public ImageView getStaticBackground(){
        return staticBackground;
    }
    public ImageView getStaticForeground(){
        return staticForeground;
    }

    public void setCannonEnum(CannonImages cannonEnum){
        this.cannonEnum = cannonEnum;
        staticBackground.setImage(cannonEnum.getBackgroundImage());
        movingPart.setImage(cannonEnum.getMovingImage());
        staticForeground.setImage(cannonEnum.getForegroundImage());
        rotater.setPivotX(cannonEnum.getRotationRadius());
        rotater.setPivotY(cannonEnum.getCannonLength()-cannonEnum.getRotationRadius());
        movingPartsScaler.setPivotX(cannonEnum.getRotationRadius());
        movingPartsScaler.setPivotY(cannonEnum.getCannonLength()-cannonEnum.getRotationRadius());
        staticPartsScaler.setPivotX(cannonEnum.getBackgroundImage().getWidth()/2);
        staticPartsScaler.setPivotY(cannonEnum.getStationaryHeightToAxis());
    }

    public void relocate(double x, double y){
        posX = x;
        posY = y;
        staticBackground.relocate(x-cannonEnum.getStationaryWidth()/2,y-cannonEnum.getStationaryHeightToAxis());
        movingPart.relocate(x-cannonEnum.getRotationRadius(),y-cannonEnum.getCannonLength()+cannonEnum.getRotationRadius());
        staticForeground.relocate(x-cannonEnum.getStationaryWidth()/2,y-cannonEnum.getStationaryHeightToAxis());
    }

    public void setAngle(double angle){
        rotater.setAngle(angle);
    }

    public void setScale(double scaleFactor){
        movingPartsScaler.setX(scaleFactor);
        movingPartsScaler.setY(scaleFactor);
        staticPartsScaler.setX(scaleFactor);
        staticPartsScaler.setY(scaleFactor);
    }

    public double getPosX(){
        return posX;
    }
    public double getPosY(){
        return posY;
    }

    public void freeze(){
        ColorAdjust desaturator = new ColorAdjust(0,-1,0,0);
        staticBackground.setEffect(desaturator);
        movingPart.setEffect(desaturator);
        movingPart.setOpacity(0.5); // So that the cannon won't obscure the view of Orbs for remaining allies.
        staticForeground.setEffect(desaturator);
    }
}
