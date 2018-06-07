package Classes.Images;

import Classes.SpriteSheet;

/**
 * Note: Orbs are assumed to be 46 pixels in diameter. This cannot easily be changed.
 */
public enum OrbImages {
    //RED_ORB("res/images/orbs/highRes/redOrb_spritesheet.png", new int[]{1,23}, new int[]{24,57}, 720.0, 'R'),
    RED_ORB("res/images/orbs/highRes/redOrb_spritesheet.png", 720.0, 'R'),
    GREEN_ORB("res/images/orbs/highRes/greenOrb_spritesheet.png", 720.0, 'G'),
    BLUE_ORB("res/images/orbs/highRes/blueOrb_spritesheet.png", 720.0, 'B'),
    YELLOW_ORB("res/images/orbs/highRes/yellowOrb_spritesheet.png", 720.0, 'Y'),
    BLACK_ORB("res/images/orbs/highRes/blackOrb_spritesheet.png", 720.0, 'K'),
    WHITE_ORB("res/images/orbs/highRes/whiteOrb_spritesheet.png", 720.0, 'W');

    private SpriteSheet spriteSheet;
    private double orbSpeed; // pixels per second
    private char symbol; // Symbol used to specify this type of Orb in a puzzle file or ammunition file.

    OrbImages(String spriteSheetURL, double orbSpeed, char symbol){
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
    private char getSymbol(){
        return symbol;
    }

    public static OrbImages lookupOrbImageBySymbol(char symbol){
        for(OrbImages orbImage: values()){
            if(orbImage.getSymbol()==symbol) return orbImage;
        }
        return null;
    }
}
