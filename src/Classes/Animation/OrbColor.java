package Classes.Animation;

/**
 * Note: Orbs are assumed to be 46 pixels in diameter. This cannot easily be changed.
 */
public enum OrbColor {
    RED(AnimationName.RED_ORB_IMPLODING, 720.0, 'R'),
    GREEN(AnimationName.GREEN_ORB_IMPLODING, 720.0, 'G'),
    BLUE(AnimationName.BLUE_ORB_IMPLODING, 720.0, 'B'),
    YELLOW(AnimationName.YELLOW_ORB_IMPLODING, 720.0, 'Y'),
    BLACK(AnimationName.BLACK_ORB_IMPLODING, 720.0, 'K'),
    WHITE(AnimationName.WHITE_ORB_IMPLODING, 720.0, 'W');

    private AnimationName implodeAnimationName;
    private double orbSpeed; // pixels per second
    private char symbol; // Symbol used to specify this type of Orb in a puzzle file or ammunition file.

    OrbColor(AnimationName implodeAnimationName, double orbSpeed, char symbol){
        this.implodeAnimationName = implodeAnimationName;
        this.orbSpeed = orbSpeed;
        this.symbol = symbol;
    }

    public AnimationName getImplodeAnimationName(){
        return implodeAnimationName;
    }
    public double getOrbSpeed(){
        return orbSpeed;
    }
    public char getSymbol(){
        return symbol;
    }

    public static OrbColor lookupOrbImageBySymbol(char symbol){
        for(OrbColor orbImage: values()){
            if(orbImage.getSymbol()==symbol) return orbImage;
        }
        return null;
    }

    // Retrieves the next orb in the enumeration, or null if the end of the enumeration has been reached.
    public OrbColor next(){
        if(this.ordinal()+1==values().length) return null;
        else return values()[(this.ordinal()+1)];
    }

    // Retrieves the previous orb in the enumeration, or null if the beginning of the enumeration has been reached.
    public OrbColor previous(){
        if(this.ordinal()==0) return null;
        else return values()[(this.ordinal()-1)];
    }
}
