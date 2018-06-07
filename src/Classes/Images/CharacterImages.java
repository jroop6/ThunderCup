package Classes.Images;
import Classes.SpriteSheet;

/**
 * Created by HydrusBeta on 8/1/2017.
 */
public enum CharacterImages {

    PINK_FILLY("res/animations/characters/pinkFilly/pinkFilly_spritesheet.png", true, false),
    UNKNOWN_CHARACTER("res/animations/characters/unknown/unknownCharacter_spritesheet.png", false, false),
    BROWN_COLT("res/animations/characters/colt/colt_spritesheet.png", true, false),
    FILLY_BOT("res/animations/characters/botPony/botPony_spritesheet.png", false, true);

    private SpriteSheet spriteSheet;
    private boolean playable;
    private boolean bot;

    CharacterImages(String url, boolean playable, boolean bot){
        spriteSheet = new SpriteSheet(url);
        this.playable = playable;
        this.bot = bot;
    }

    public SpriteSheet getSpriteSheet(){
        return spriteSheet;
    }

    // Retrieves the next Character in the enumeration.
    public CharacterImages next(){
        return values()[(this.ordinal()+1) % values().length];
    }

    // Is this character type a playable character?
    public boolean isPlayable(){
        return playable;
    }

    // IS this character a bot type?
    public boolean isBot(){
        return bot;
    }
}
