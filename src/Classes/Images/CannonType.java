package Classes.Images;

import Classes.Animation.AnimationName;

public enum CannonType {
    BASIC_CANNON(new AnimationName[]{AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND}, new AnimationName[] {AnimationName.BASIC_CANNON_STATIC, AnimationName.BASIC_CANNON_STATIC, AnimationName.BASIC_CANNON_STATIC, AnimationName.BASIC_CANNON_STATIC, AnimationName.BASIC_CANNON_STATIC}, new AnimationName[]{AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND}, 123.0, 1050.0, 135.0, 32.0, true),
    UNKNOWN_CANNON(new AnimationName[]{AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND}, new AnimationName[] {AnimationName.UNKNOWN_CANNON_STATIC, AnimationName.UNKNOWN_CANNON_STATIC, AnimationName.UNKNOWN_CANNON_STATIC, AnimationName.UNKNOWN_CANNON_STATIC, AnimationName.UNKNOWN_CANNON_STATIC}, new AnimationName[]{AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND}, 123.0, 1050.0, 135.0, 32.0, true),
    BOT_CANNON(new AnimationName[]{AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND, AnimationName.BASIC_CANNON_BACKGROUND}, new AnimationName[] {AnimationName.BOT_CANNON_STATIC, AnimationName.BOT_CANNON_STATIC, AnimationName.BOT_CANNON_STATIC, AnimationName.BOT_CANNON_STATIC, AnimationName.BOT_CANNON_STATIC}, new AnimationName[]{AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND, AnimationName.BASIC_CANNON_FOREGROUND}, 123.0, 1050.0, 135.0, 32.0, true);

    AnimationName[] backgroundAnimationNames;
    AnimationName[] movingPartAnimationNames;
    AnimationName[] foregroundAnimationNames;
    private double characterX; // The x-position of the center of a character's hooves in a 1-player playpanel, relative to the ammunition Orb.
    private double characterY; // The y-position of the center of a character's hooves in a 1-player playpanel, relative to the ammunition Orb.
    private double ammunitionRelativeX; // The x-position of the (center of the) reserve ammunition Orb, relative to the ammunition Orb.
    private double ammunitionRelativeY; // The y-position of the (center of the) reserve ammunition Orb, relative to the ammunition Orb.
    private boolean selectable;

    CannonType(AnimationName[] backgroundAnimationNames, AnimationName[] movingPartAnimationNames, AnimationName[] foregroundAnimationNames, double characterX, double characterY, double ammunitionRelativeX, double ammunitionRelativeY, boolean selectable){
        this.backgroundAnimationNames = backgroundAnimationNames;
        this.movingPartAnimationNames = movingPartAnimationNames;
        this.foregroundAnimationNames = foregroundAnimationNames;
        this.characterX = characterX;
        this.characterY = characterY;
        this.ammunitionRelativeX = ammunitionRelativeX;
        this. ammunitionRelativeY = ammunitionRelativeY;
        this.selectable = selectable;
    }

    public enum CannonAnimationState{AIMING, FIRING, DISCONNECTED, DEFEATED, VICTORIOUS}


    public AnimationName getBackgroundAnimation(CannonAnimationState cannonAnimationState){
        return backgroundAnimationNames[cannonAnimationState.ordinal()];
    }
    public AnimationName getMovingPartAnimation(CannonAnimationState cannonAnimationState){
        return movingPartAnimationNames[cannonAnimationState.ordinal()];
    }
    public AnimationName getForegroundAnimation(CannonAnimationState cannonAnimationState){
        return foregroundAnimationNames[cannonAnimationState.ordinal()];
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
