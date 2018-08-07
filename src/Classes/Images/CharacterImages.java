package Classes.Images;
import Classes.PlayerTypes.BotPlayer;
import Classes.SpriteSheet;

/**
 * Created by Jonathan Roop on 8/1/2017.
 */
public enum CharacterImages {

    BLITZ("res/animations/characters/blitz/blitz_spritesheet.png", true, null),
    UNKNOWN_CHARACTER("res/animations/characters/unknownCharacter/unknownCharacter_spritesheet.png", false, null),
    GEARSHIFT("res/animations/characters/gearshift/gearshift_spritesheet.png", true, null),
    CLOUDESTINE("res/animations/characters/cloudestine/cloudestine_spritesheet.png", true, null),
    CHARCOAL("res/animations/characters/charcoal/charcoal_spritesheet.png", true, null),
    FILLY_BOT_EASY("res/animations/characters/botPony/botPonyEasy_spritesheet.png", true, BotPlayer.Difficulty.EASY),
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
