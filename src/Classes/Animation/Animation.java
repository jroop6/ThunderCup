package Classes.Animation;

public enum Animation {
    /* CHARACTERS */
    BLITZ_VICTORY("res/animations/characters/blitz/blitz_spritesheet.png"),
    BLITZ_CONTENT("res/animations/characters/blitz/blitz_spritesheet.png"),
    BLITZ_WORRIED("res/animations/characters/blitz/blitz_spritesheet.png"),
    BLITZ_DEFEAT("res/animations/characters/blitz/blitz_spritesheet.png"),
    BLITZ_DISCONNECTED("res/animations/characters/blitz/blitz_spritesheet.png"),

    GEARSHIFT_VICTORY("res/animations/characters/gearshift/gearshift_spritesheet.png"),
    GEARSHIFT_CONTENT("res/animations/characters/gearshift/gearshift_spritesheet.png"),
    GEARSHIFT_WORRIED("res/animations/characters/gearshift/gearshift_spritesheet.png"),
    GEARSHIFT_DEFEAT("res/animations/characters/gearshift/gearshift_spritesheet.png"),
    GEARSHIFT_DISCONNECTED("res/animations/characters/gearshift/gearshift_spritesheet.png"),

    CLOUDESTINE_VICTORY("res/animations/characters/cloudestine/cloudestine_spritesheet.png"),
    CLOUDESTINE_CONTENT("res/animations/characters/cloudestine/cloudestine_spritesheet.png"),
    CLOUDESTINE_WORRIED("res/animations/characters/cloudestine/cloudestine_spritesheet.png"),
    CLOUDESTINE_DEFEAT("res/animations/characters/cloudestine/cloudestine_spritesheet.png"),
    CLOUDESTINE_DISCONNECTED("res/animations/characters/cloudestine/cloudestine_spritesheet.png"),

    CHARCOAL_VICTORY("res/animations/characters/charcoal/charcoal_spritesheet.png"),
    CHARCOAL_CONTENT("res/animations/characters/charcoal/charcoal_spritesheet.png"),
    CHARCOAL_WORRIED("res/animations/characters/charcoal/charcoal_spritesheet.png"),
    CHARCOAL_DEFEAT("res/animations/characters/charcoal/charcoal_spritesheet.png"),
    CHARCOAL_DISCONNECTED("res/animations/characters/charcoal/charcoal_spritesheet.png"),

    FILLY_BOT_EASY_VICTORY("res/animations/characters/botPony/botPonyEasy_spritesheet.png"),
    FILLY_BOT_EASY_CONTENT("res/animations/characters/botPony/botPonyEasy_spritesheet.png"),
    FILLY_BOT_EASY_WORRIED("res/animations/characters/botPony/botPonyEasy_spritesheet.png"),
    FILLY_BOT_EASY_DEFEAT("res/animations/characters/botPony/botPonyEasy_spritesheet.png"),
    FILLY_BOT_EASY_DISCONNECTED("res/animations/characters/botPony/botPonyEasy_spritesheet.png"),

    FILLY_BOT_MEDIUM_VICTORY("res/animations/characters/botPony/botPonyMedium_spritesheet.png"),
    FILLY_BOT_MEDIUM_CONTENT("res/animations/characters/botPony/botPonyMedium_spritesheet.png"),
    FILLY_BOT_MEDIUM_WORRIED("res/animations/characters/botPony/botPonyMedium_spritesheet.png"),
    FILLY_BOT_MEDIUM_DEFEAT("res/animations/characters/botPony/botPonyMedium_spritesheet.png"),
    FILLY_BOT_MEDIUM_DISCONNECTED("res/animations/characters/botPony/botPonyMedium_spritesheet.png"),

    FILLY_BOT_HARD_VICTORY("res/animations/characters/botPony/botPonyHard_spritesheet.png"),
    FILLY_BOT_HARD_CONTENT("res/animations/characters/botPony/botPonyHard_spritesheet.png"),
    FILLY_BOT_HARD_WORRIED("res/animations/characters/botPony/botPonyHard_spritesheet.png"),
    FILLY_BOT_HARD_DEFEAT("res/animations/characters/botPony/botPonyHard_spritesheet.png"),
    FILLY_BOT_HARD_DISCONNECTED("res/animations/characters/botPony/botPonyHard_spritesheet.png"),


    /* ORBS */
    RED_ORB("res/images/orbs/highRes/redOrb_spritesheet.png"),
    GREEN_ORB("res/images/orbs/highRes/greenOrb_spritesheet.png"),
    BLUE_ORB("res/images/orbs/highRes/blueOrb_spritesheet.png"),
    YELLOW_ORB("res/images/orbs/highRes/yellowOrb_spritesheet.png"),
    BLACK_ORB("res/images/orbs/highRes/blackOrb_spritesheet.png"),
    WHITE_ORB("res/images/orbs/highRes/whiteOrb_spritesheet.png"),

    /* MISC */
    UNKNOWN_CHARACTER("res/animations/characters/unknownCharacter/unknownCharacter_spritesheet.png"),
    TITLE("res/animations/misc/highRes/title_spritesheet.png"),
    EXCLAMATION_MARK("res/animations/misc/highRes/exclamationMark_spritesheet.png"),
    MAGIC_TELEPORTATION("res/animations/misc/highRes/magicTeleportation_spritesheet.png"),
    WIN_SCREEN("res/animations/misc/highRes/winScreen_spritesheet.png"),
    LOSE_SCREEN("res/animations/misc/highRes/loseScreen_spritesheet.png"),
    CLEAR_SCREEN("res/animations/misc/highRes/clearScreen_spritesheet.png"),
    ELECTRIFICATION_1("res/images/orbs/highRes/OrbElectrification_spritesheet.png"),
    EXPLOSION_1("res/images/orbs/highRes/OrbExplosion_spritesheet.png");

    private SpriteSheet spriteSheet;

    Animation(String url){
        spriteSheet = new SpriteSheet(url);
    }

    public SpriteSheet getSpriteSheet(){
        return spriteSheet;
    }
}
