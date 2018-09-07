package Classes.Animation;
import Classes.PlayerTypes.BotPlayer;

/**
 * Created by Jonathan Roop on 8/1/2017.
 */
public enum CharacterType {

    // Note: the order of animations in the Animation array matters. It must match the order of the enums in CharacterAnimationState, accordingly.
    BLITZ(new Animation[] {Animation.BLITZ_VICTORY, Animation.BLITZ_CONTENT, Animation.BLITZ_WORRIED, Animation.BLITZ_DEFEAT, Animation.BLITZ_DISCONNECTED}, true, null),
    GEARSHIFT(new Animation[] {Animation.GEARSHIFT_VICTORY, Animation.GEARSHIFT_CONTENT, Animation.GEARSHIFT_WORRIED, Animation.GEARSHIFT_DEFEAT, Animation.GEARSHIFT_DISCONNECTED}, true, null),
    CLOUDESTINE(new Animation[] {Animation.CLOUDESTINE_VICTORY, Animation.CLOUDESTINE_CONTENT, Animation.CLOUDESTINE_WORRIED, Animation.CLOUDESTINE_DEFEAT, Animation.CLOUDESTINE_DISCONNECTED}, true, null),
    CHARCOAL(new Animation[] {Animation.CHARCOAL_VICTORY, Animation.CHARCOAL_CONTENT, Animation.CHARCOAL_WORRIED, Animation.CHARCOAL_DEFEAT, Animation.CHARCOAL_DISCONNECTED}, true, null),
    FILLY_BOT_EASY(new Animation[] {Animation.FILLY_BOT_EASY_VICTORY, Animation.FILLY_BOT_EASY_CONTENT, Animation.FILLY_BOT_EASY_WORRIED, Animation.FILLY_BOT_EASY_DEFEAT, Animation.FILLY_BOT_EASY_DISCONNECTED}, true, BotPlayer.Difficulty.EASY),
    FILLY_BOT_MEDIUM(new Animation[] {Animation.FILLY_BOT_MEDIUM_VICTORY, Animation.FILLY_BOT_MEDIUM_CONTENT, Animation.FILLY_BOT_MEDIUM_WORRIED, Animation.FILLY_BOT_MEDIUM_DEFEAT, Animation.FILLY_BOT_MEDIUM_DISCONNECTED}, false, BotPlayer.Difficulty.MEDIUM),
    FILLY_BOT_HARD(new Animation[] {Animation.FILLY_BOT_HARD_VICTORY, Animation.FILLY_BOT_HARD_CONTENT, Animation.FILLY_BOT_HARD_WORRIED, Animation.FILLY_BOT_HARD_DEFEAT, Animation.FILLY_BOT_HARD_DISCONNECTED}, false, BotPlayer.Difficulty.HARD),
    UNKNOWN_CHARACTER(new Animation[] {Animation.UNKNOWN_CHARACTER, Animation.UNKNOWN_CHARACTER, Animation.UNKNOWN_CHARACTER, Animation.UNKNOWN_CHARACTER, Animation.UNKNOWN_CHARACTER}, false, null);

    private Animation[] animations;
    private boolean playable; // Can a human player use this character?
    private BotPlayer.Difficulty botDifficulty;

    public enum CharacterAnimationState{VICTORY(-1,-1), CONTENT(0,12), WORRIED(13,19), DEFEAT(20,20), DISCONNECTED(Integer.MAX_VALUE,Integer.MAX_VALUE);
        private int upperTriggerHeight;
        private int lowerTriggerHeight;
        CharacterAnimationState(int upperTriggerHeight, int lowerTriggerHeight){
            this.upperTriggerHeight = upperTriggerHeight;
            this.lowerTriggerHeight = lowerTriggerHeight;
        }
        public boolean inRange(int index){
            return index >=upperTriggerHeight && index <= lowerTriggerHeight;
        }
    }

    CharacterType(Animation[] animations, boolean playable, BotPlayer.Difficulty botDifficulty){
        this.animations = animations;
        this.playable = playable;
        this.botDifficulty = botDifficulty;
    }

    public Animation getAnimation(CharacterAnimationState characterAnimationType){
        return animations[characterAnimationType.ordinal()];
    }

    // Retrieves the next Character in the enumeration.
    public CharacterType next(){
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
