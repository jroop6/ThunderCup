package Classes.Animation;

/**
 * Note: Orbs are assumed to be 46 pixels in diameter. This cannot easily be changed.
 */
public enum OrbAnimations {
    RED_ORB("res/images/orbs/highRes/redOrb_spritesheet.png", 720.0, 'R'),
    GREEN_ORB("res/images/orbs/highRes/greenOrb_spritesheet.png", 720.0, 'G'),
    BLUE_ORB("res/images/orbs/highRes/blueOrb_spritesheet.png", 720.0, 'B'),
    YELLOW_ORB("res/images/orbs/highRes/yellowOrb_spritesheet.png", 720.0, 'Y'),
    BLACK_ORB("res/images/orbs/highRes/blackOrb_spritesheet.png", 720.0, 'K'),
    WHITE_ORB("res/images/orbs/highRes/whiteOrb_spritesheet.png", 720.0, 'W');

    private SpriteSheet spriteSheet;
    private double orbSpeed; // pixels per second
    private char symbol; // Symbol used to specify this type of Orb in a puzzle file or ammunition file.

    OrbAnimations(String spriteSheetURL, double orbSpeed, char symbol){
        spriteSheet = new SpriteSheet(spriteSheetURL);
        this.orbSpeed = orbSpeed;
        this.symbol = symbol;
    }

    public SpriteSheet getSpriteSheet(){
        return spriteSheet;
    }
    public double getOrbSpeed(){
        return orbSpeed;
    }
    public char getSymbol(){
        return symbol;
    }

    public static OrbAnimations lookupOrbImageBySymbol(char symbol){
        for(OrbAnimations orbImage: values()){
            if(orbImage.getSymbol()==symbol) return orbImage;
        }
        return null;
    }

    // Retrieves the next orb in the enumeration, or null if the end of the enumeration has been reached.
    public OrbAnimations next(){
        if(this.ordinal()+1==values().length) return null;
        else return values()[(this.ordinal()+1)];
    }

    // Retrieves the previous orb in the enumeration, or null if the beginning of the enumeration has been reached.
    public OrbAnimations previous(){
        if(this.ordinal()==0) return null;
        else return values()[(this.ordinal()-1)];
    }
}
