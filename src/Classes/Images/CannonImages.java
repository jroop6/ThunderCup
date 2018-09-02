package Classes.Images;

import Classes.Animation.SpriteSheet;

public enum CannonImages {
    BASIC_CANNON("res/animations/cannons/BasicCannon_spritesheet.png","res/animations/cannons/BasicCannonBackground_spritesheet.png","res/animations/cannons/BasicCannonForeground_spritesheet.png", 123.0, 1050.0, 135.0, 32.0, true),
    UNKNOWN_CANNON("res/animations/cannons/UnknownCannon_spritesheet.png","res/animations/cannons/BasicCannonBackground_spritesheet.png","res/animations/cannons/BasicCannonForeground_spritesheet.png", 123.0, 1050.0, 135.0, 32.0, true),
    BOT_CANNON("res/animations/cannons/BotCannon_spritesheet.png","res/animations/cannons/BasicCannonBackground_spritesheet.png","res/animations/cannons/BasicCannonForeground_spritesheet.png", 123.0, 1050.0, 135.0, 32.0, true);

    SpriteSheet movingPartSpriteSheet;
    SpriteSheet stationaryBackgroundSpriteSheet;
    SpriteSheet stationaryForegroundSpriteSheet;
    private double characterX; // The x-position of the center of a character's hooves in a 1-player playpanel, relative to the ammunition Orb.
    private double characterY; // The y-position of the center of a character's hooves in a 1-player playpanel, relative to the ammunition Orb.
    private double ammunitionRelativeX; // The x-position of the (center of the) reserve ammunition Orb, relative to the ammunition Orb.
    private double ammunitionRelativeY; // The y-position of the (center of the) reserve ammunition Orb, relative to the ammunition Orb.
    private boolean selectable;

    CannonImages(String movingPartUrl, String stationaryBackgroundUrl, String stationaryForegroundUrl, double characterX, double characterY, double ammunitionRelativeX, double ammunitionRelativeY, boolean selectable){
        this.movingPartSpriteSheet = new SpriteSheet(movingPartUrl);
        this.stationaryBackgroundSpriteSheet = new SpriteSheet(stationaryBackgroundUrl);
        this.stationaryForegroundSpriteSheet = new SpriteSheet(stationaryForegroundUrl);
        this.characterX = characterX;
        this.characterY = characterY;
        this.ammunitionRelativeX = ammunitionRelativeX;
        this. ammunitionRelativeY = ammunitionRelativeY;
        this.selectable = selectable;
    }

    public SpriteSheet getBackgroundImageSpriteSheet(){
        return stationaryBackgroundSpriteSheet;
    }
    public SpriteSheet getMovingPartSpriteSheet(){
        return movingPartSpriteSheet;
    }
    public SpriteSheet getForegroundImageSpriteSheet(){
        return stationaryForegroundSpriteSheet;
    }

    // Retrieves the next Cannon in the enumeration.
    public CannonImages next(){
        return values()[(this.ordinal()+1) % values().length];
    }

    // Is this cannon type a playable cannon?
    public boolean isSelectable(){
        return selectable;
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
