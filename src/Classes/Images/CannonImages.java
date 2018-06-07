package Classes.Images;

import javafx.scene.image.Image;

/**
 * Unlike other enum, this class contains getters for *Images* instead of *ImageViews*. ImageViews are constructed
 * within the Cannon class using Images. This makes it easier to change the cannon type by simply calling
 * movingPart.setImage(cannonEnum.getMovingImage()) without needing to re-attach any event handlers to the ImageView
 * movingPart.
 */
public enum CannonImages {
    STANDARD_CANNON("res/images/cannons/movingCannonPart.png","res/images/cannons/BasicCannon_Background.png","res/images/cannons/BasicCannon_Foreground.png",92.0, 123.0, 1050.0, 135.0, 32.0, true),
    UNKNOWN_CANNON("res/images/cannons/unknownCannon.png","res/images/cannons/BasicCannon_Background.png","res/images/cannons/BasicCannon_Foreground.png",92.0, 123.0, 1050.0, 135.0, 32.0, true),
    BOT_CANNON("res/images/cannons/BotCannon.png","res/images/cannons/BasicCannon_Background.png","res/images/cannons/BasicCannon_Foreground.png",92.0, 123.0, 1050.0, 135.0, 32.0, true);

    Image movingPart;
    Image stationaryBackground;
    Image stationaryForeground;
    private double stationaryHeightToAxis;
    private double characterX; // The x-position of the center of a character's hooves in a 1-player playpanel.
    private double characterY; // The y-position of the center of a character's hooves in a 1-player playpanel.
    private double ammunitionRelativeX; // The x-position of the (center of the) reserve ammunition Orb, relative to the center of the cannon.
    private double ammunitionRelativeY; // The y-position of the (center of the) reserve ammunition Orb, relative to the center of the cannon.
    private boolean selectable;

    CannonImages(String movingPartUrl, String stationaryBackgroundUrl, String stationaryForegroundUrl, double stationaryHeightToAxis, double characterX, double characterY, double ammunitionRelativeX, double ammunitionRelativeY, boolean selectable){
        this.movingPart = new Image(movingPartUrl);
        this.stationaryBackground = new Image(stationaryBackgroundUrl);
        this.stationaryForeground = new Image(stationaryForegroundUrl);
        this.stationaryHeightToAxis = stationaryHeightToAxis;
        this.characterX = characterX;
        this.characterY = characterY;
        this.ammunitionRelativeX = ammunitionRelativeX;
        this. ammunitionRelativeY = ammunitionRelativeY;
        this.selectable = selectable;
    }

    public Image getBackgroundImage(){
        return stationaryBackground;
    }
    public Image getMovingImage(){
        return movingPart;
    }
    public Image getForegroundImage(){
        return stationaryForeground;
    }

    // Retrieves the next Cannon in the enumeration.
    public CannonImages next(){
        return values()[(this.ordinal()+1) % values().length];
    }

    // Is this cannon type a playable cannon?
    public boolean isSelectable(){
        return selectable;
    }

    public double getRotationRadius(){
        return movingPart.getWidth()/2;
    }
    public double getCannonLength(){
        return movingPart.getHeight();
    }
    public double getStationaryWidth(){
        return stationaryBackground.getWidth();
    }
    public double getStationaryHeightToAxis(){
        return stationaryHeightToAxis;
    }
    public double getCharacterX(){
        return characterX;
    }
    public double getCharacterY(){
        return characterY;
    }
    public double getAmmunitionRelativeX(){
        return ammunitionRelativeX;
    }
    public double getAmmunitionRelativeY(){
        return ammunitionRelativeY;
    }

}
