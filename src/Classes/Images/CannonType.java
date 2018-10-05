package Classes.Images;

import Classes.Animation.Animation;

public enum CannonType {
    BASIC_CANNON(new Animation[]{Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND}, new Animation[] {Animation.BASIC_CANNON_STATIC, Animation.BASIC_CANNON_STATIC, Animation.BASIC_CANNON_STATIC, Animation.BASIC_CANNON_STATIC, Animation.BASIC_CANNON_STATIC}, new Animation[]{Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND}, 123.0, 1050.0, 135.0, 32.0, true),
    UNKNOWN_CANNON(new Animation[]{Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND}, new Animation[] {Animation.UNKNOWN_CANNON_STATIC, Animation.UNKNOWN_CANNON_STATIC, Animation.UNKNOWN_CANNON_STATIC, Animation.UNKNOWN_CANNON_STATIC, Animation.UNKNOWN_CANNON_STATIC}, new Animation[]{Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND}, 123.0, 1050.0, 135.0, 32.0, true),
    BOT_CANNON(new Animation[]{Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND, Animation.BASIC_CANNON_BACKGROUND}, new Animation[] {Animation.BOT_CANNON_STATIC, Animation.BOT_CANNON_STATIC, Animation.BOT_CANNON_STATIC, Animation.BOT_CANNON_STATIC, Animation.BOT_CANNON_STATIC}, new Animation[]{Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND, Animation.BASIC_CANNON_FOREGROUND}, 123.0, 1050.0, 135.0, 32.0, true);

    Animation[] backgroundAnimations;
    Animation[] movingPartAnimations;
    Animation[] foregroundAnimations;
    private double characterX; // The x-position of the center of a character's hooves in a 1-player playpanel, relative to the ammunition Orb.
    private double characterY; // The y-position of the center of a character's hooves in a 1-player playpanel, relative to the ammunition Orb.
    private double ammunitionRelativeX; // The x-position of the (center of the) reserve ammunition Orb, relative to the ammunition Orb.
    private double ammunitionRelativeY; // The y-position of the (center of the) reserve ammunition Orb, relative to the ammunition Orb.
    private boolean selectable;

    CannonType(Animation[] backgroundAnimations, Animation[] movingPartAnimations, Animation[] foregroundAnimations, double characterX, double characterY, double ammunitionRelativeX, double ammunitionRelativeY, boolean selectable){
        this.backgroundAnimations = backgroundAnimations;
        this.movingPartAnimations = movingPartAnimations;
        this.foregroundAnimations = foregroundAnimations;
        this.characterX = characterX;
        this.characterY = characterY;
        this.ammunitionRelativeX = ammunitionRelativeX;
        this. ammunitionRelativeY = ammunitionRelativeY;
        this.selectable = selectable;
    }

    public enum CannonAnimationState{AIMING, FIRING, DISCONNECTED, DEFEATED, VICTORIOUS}


    public Animation getBackgroundAnimation(CannonAnimationState cannonAnimationState){
        return backgroundAnimations[cannonAnimationState.ordinal()];
    }
    public Animation getMovingPartAnimation(CannonAnimationState cannonAnimationState){
        return movingPartAnimations[cannonAnimationState.ordinal()];
    }
    public Animation getForegroundAnimation(CannonAnimationState cannonAnimationState){
        return foregroundAnimations[cannonAnimationState.ordinal()];
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
    public boolean isSelectable(){
        return selectable;
    }


    // Retrieves the next Cannon in the enumeration.
    public CannonType next(){
        return values()[(this.ordinal()+1) % values().length];
    }
}
