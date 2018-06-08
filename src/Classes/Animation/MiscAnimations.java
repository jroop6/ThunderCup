package Classes.Animation;

import Classes.GameSettings;
import Classes.SpriteSheet;

/**
 * An enumeration containing all miscellaneous animations, in the form of png spritesheets. Numerous instances of
 * Sprite can be created by calling getSpriteSheet.
 */
public enum MiscAnimations {
    TITLE("res/animations/misc/highRes/title_spritesheet.png", "res/animations/misc/lowRes/null_spritesheet.png");

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
