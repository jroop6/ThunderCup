package Classes.Animation;

import Classes.GameSettings;

public enum OrbExplosion {
    EXPLOSION_1("res/images/orbs/highRes/OrbExplosion_spritesheet.png", "res/images/orbs/lowRes/null_spritesheet.png");

    private SpriteSheet spriteSheet;

    OrbExplosion(String spriteSheetURL_highRes, String spriteSheetURL_lowRes){
        if(GameSettings.getImageResolution()==GameSettings.ImageResolution.HIGH) spriteSheet = new SpriteSheet(spriteSheetURL_highRes);
        else spriteSheet = new SpriteSheet(spriteSheetURL_lowRes);
    }

    public SpriteSheet getSpriteSheet(){
        return spriteSheet;
    }

}
