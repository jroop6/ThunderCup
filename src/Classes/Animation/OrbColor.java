package Classes.Animation;

/**
 * Note: Orbs are assumed to be 46 pixels in diameter. This cannot easily be changed.
 */
public enum OrbColor {
    RED(Animation.RED_ORB_IMPLODING, 720.0, 'R'),
    GREEN(Animation.GREEN_ORB_IMPLODING, 720.0, 'G'),
    BLUE(Animation.BLUE_ORB_IMPLODING, 720.0, 'B'),
    YELLOW(Animation.YELLOW_ORB_IMPLODING, 720.0, 'Y'),
    BLACK(Animation.BLACK_ORB_IMPLODING, 720.0, 'K'),
    WHITE(Animation.WHITE_ORB_IMPLODING, 720.0, 'W');

    private Animation implodeAnimation;
    private double orbSpeed; // pixels per second
    private char symbol; // Symbol used to specify this type of Orb in a puzzle file or ammunition file.

    OrbColor(Animation implodeAnimation, double orbSpeed, char symbol){
        this.implodeAnimation = implodeAnimation;
        this.orbSpeed = orbSpeed;
        this.symbol = symbol;
    }

    public Animation getImplodeAnimation(){
        return implodeAnimation;
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
