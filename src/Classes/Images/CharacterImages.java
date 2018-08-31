package Classes.Images;
import Classes.PlayerTypes.BotPlayer;
import Classes.SpriteSheet;

/**
 * Created by Jonathan Roop on 8/1/2017.
 */
public enum CharacterImages {

    // About the animationIndexBound array:
    // Each index is an "exclusive" bound of one of the character's animations. The index of the last frame of the
    // corresponding animation is 1 less. The animations correspond to the CharacterImages.CharacterAnimationState enums
    // (see below). Therefore, the length of the array must match Player.CharacterAnimationState.values().length. The
    // numbers must be increasing, due to assumptions made in getAnimationBounds().

    BLITZ("res/animations/characters/blitz/blitz_spritesheet.png", true, null, new int[] {1, 3, 5, 7, 9}),
    UNKNOWN_CHARACTER("res/animations/characters/unknownCharacter/unknownCharacter_spritesheet.png", false, null, new int[] {1, 2, 3, 4, 5}),
    GEARSHIFT("res/animations/characters/gearshift/gearshift_spritesheet.png", true, null, new int[] {1, 2, 3, 4, 5}),
    CLOUDESTINE("res/animations/characters/cloudestine/cloudestine_spritesheet.png", true, null, new int[] {1, 2, 3, 4, 5}),
    CHARCOAL("res/animations/characters/charcoal/charcoal_spritesheet.png", true, null, new int[] {1, 2, 3, 4, 5}),
    FILLY_BOT_EASY("res/animations/characters/botPony/botPonyEasy_spritesheet.png", true, BotPlayer.Difficulty.EASY, new int[] {1, 2, 3, 4, 5}),
    FILLY_BOT_MEDIUM("res/animations/characters/botPony/botPonyMedium_spritesheet.png", false, BotPlayer.Difficulty.MEDIUM, new int[] {1, 2, 3, 4, 5}),
    FILLY_BOT_HARD("res/animations/characters/botPony/botPonyHard_spritesheet.png", false, BotPlayer.Difficulty.HARD, new int[] {1, 2, 3, 4, 5});

    private SpriteSheet spriteSheet;
    private boolean playable; // Can a human player use this character?
    private BotPlayer.Difficulty botDifficulty;
    private int[] animationIndexBound;

    CharacterImages(String url, boolean playable, BotPlayer.Difficulty botDifficulty, int[] animationIndexBound){
        spriteSheet = new SpriteSheet(url);
        this.playable = playable;
        this.botDifficulty = botDifficulty;
        this.animationIndexBound = animationIndexBound;

        // Some sanity checks:
        if(animationIndexBound.length != CharacterAnimationState.values().length){
            System.err.println("Error! The animationIndexBound array for " + this + " is not of the correct length! The game will probably crash if you use this character.");
        }
        int previousBound = animationIndexBound[0];
        for(int i = 1; i< animationIndexBound.length; i++){
            if(previousBound >= animationIndexBound[i]){
                System.err.println("Error! The animationIndexBound array for " + this + " is not strictly increasing. Animations might not behave as expected for this character.");
                break;
            }
            previousBound = animationIndexBound[i];
        }
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

    public int[] getAnimationBounds(CharacterAnimationState characterAnimationState){
        // find the (inclusive) lower bound:
        int lowerBound;
        if(characterAnimationState.ordinal()==0) lowerBound = 0;
        else lowerBound = animationIndexBound[characterAnimationState.ordinal()-1];

        // find the (exclusive) upper bound:
        int upperBound = animationIndexBound[characterAnimationState.ordinal()];

        return new int[] {lowerBound, upperBound};
    }

    // Character animation control:
    public enum CharacterAnimationState{VICTORY(-1,-1), CONTENT(0,12), WORRIED(13,19), DEFEAT(20, 20), DISCONNECTED(Integer.MAX_VALUE,Integer.MAX_VALUE);
        private int upperTriggerHeight;
        private int lowerTriggerHeight;
        CharacterAnimationState(int upperTriggerHeight, int lowerTriggerHeight){
            this.upperTriggerHeight = upperTriggerHeight;
            this.lowerTriggerHeight = lowerTriggerHeight;
        }
        public boolean inRange(int index){
            return index>=upperTriggerHeight && index <= lowerTriggerHeight;
        }
    }
}
