package Classes.Images;
import Classes.PlayerTypes.BotPlayer;
import Classes.SpriteSheet;

/**
 * Created by Jonathan Roop on 8/1/2017.
 */
public enum CharacterImages {

    PINK_FILLY("res/animations/characters/pinkFilly/pinkFilly_spritesheet.png", true, null),
    UNKNOWN_CHARACTER("res/animations/characters/unknown/unknownCharacter_spritesheet.png", false, null),
    BROWN_COLT("res/animations/characters/colt/colt_spritesheet.png", true, null),
    FILLY_BOT_EASY("res/animations/characters/botPony/botPonyEasy_spritesheet.png", false, BotPlayer.Difficulty.EASY),
    FILLY_BOT_MEDIUM("res/animations/characters/botPony/botPonyMedium_spritesheet.png", false, BotPlayer.Difficulty.MEDIUM),
    FILLY_BOT_HARD("res/animations/characters/botPony/botPonyHard_spritesheet.png", false, BotPlayer.Difficulty.HARD);

    private SpriteSheet spriteSheet;
    private boolean playable;
    private BotPlayer.Difficulty botDifficulty;

    CharacterImages(String url, boolean playable, BotPlayer.Difficulty botDifficulty){
        spriteSheet = new SpriteSheet(url);
        this.playable = playable;
        this.botDifficulty = botDifficulty;
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

    public BotPlayer.Difficulty getBotDifficulty() {
        return botDifficulty;
    }
}
