package Classes.Animation;

import Classes.GameSettings;
import Classes.SpriteSheet;

/**
 * An enumeration containing all miscellaneous animations, in the form of png spritesheets.
 */
public enum MiscAnimations {
    TITLE("res/animations/misc/highRes/title_spritesheet.png", "res/animations/misc/lowRes/null_spritesheet.png"),
    EXCLAMATION_MARK("res/animations/misc/highRes/exclamationMark_spritesheet.png", "res/animations/misc/lowRes/null_spritesheet.png"),
    MAGIC_TELEPORTATION("res/animations/misc/highRes/magicTeleportation_spritesheet.png", "res/animations/misc/lowRes/null_spritesheet.png"),
    WIN_SCREEN("res/animations/misc/highRes/winScreen_spritesheet.png", "res/animations/misc/lowRes/null_spritesheet.png"),
    LOSE_SCREEN("res/animations/misc/highRes/loseScreen_spritesheet.png", "res/animations/misc/lowRes/null_spritesheet.png"),
    CLEAR_SCREEN("res/animations/misc/highRes/clearScreen_spritesheet.png", "res/animations/misc/lowRes/null_spritesheet.png");

    private SpriteSheet spriteSheet;

    MiscAnimations(String imageURL_highRes, String imageURL_lowRes){
        if(GameSettings.getImageResolution()==GameSettings.ImageResolution.HIGH){
            spriteSheet = new SpriteSheet(imageURL_highRes);
        }
        else spriteSheet = new SpriteSheet(imageURL_lowRes);
    }

    public SpriteSheet getSpriteSheet(){
        return spriteSheet;
    }
}
