package Classes.Images;

import Classes.SpriteSheet;

public enum OrbElectrification {

    ELECTRIFICATION_1("res/images/orbs/highRes/OrbElectrification_spritesheet.png");

    private SpriteSheet spriteSheet;

    OrbElectrification(String spriteSheetURL){
        spriteSheet = new SpriteSheet(spriteSheetURL);
    }

    public SpriteSheet getSpriteSheet(){
        return spriteSheet;
    }
}
