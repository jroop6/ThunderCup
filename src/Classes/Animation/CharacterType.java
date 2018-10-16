package Classes.Animation;
import Classes.PlayerTypes.BotPlayer;

/**
 * Created by Jonathan Roop on 8/1/2017.
 */
public enum CharacterType {

    // Note: the order of animationNames in the AnimationName array matters. It must match the order of the enums in CharacterAnimationState, accordingly.
    BLITZ(new AnimationName[] {AnimationName.BLITZ_VICTORY, AnimationName.BLITZ_CONTENT, AnimationName.BLITZ_WORRIED, AnimationName.BLITZ_DEFEAT, AnimationName.BLITZ_DISCONNECTED}, true, null),
    GEARSHIFT(new AnimationName[] {AnimationName.GEARSHIFT_VICTORY, AnimationName.GEARSHIFT_CONTENT, AnimationName.GEARSHIFT_WORRIED, AnimationName.GEARSHIFT_DEFEAT, AnimationName.GEARSHIFT_DISCONNECTED}, true, null),
    CLOUDESTINE(new AnimationName[] {AnimationName.CLOUDESTINE_VICTORY, AnimationName.CLOUDESTINE_CONTENT, AnimationName.CLOUDESTINE_WORRIED, AnimationName.CLOUDESTINE_DEFEAT, AnimationName.CLOUDESTINE_DISCONNECTED}, true, null),
    CHARCOAL(new AnimationName[] {AnimationName.CHARCOAL_VICTORY, AnimationName.CHARCOAL_CONTENT, AnimationName.CHARCOAL_WORRIED, AnimationName.CHARCOAL_DEFEAT, AnimationName.CHARCOAL_DISCONNECTED}, true, null),
    FILLY_BOT_EASY(new AnimationName[] {AnimationName.FILLY_BOT_EASY_VICTORY, AnimationName.FILLY_BOT_EASY_CONTENT, AnimationName.FILLY_BOT_EASY_WORRIED, AnimationName.FILLY_BOT_EASY_DEFEAT, AnimationName.FILLY_BOT_EASY_DISCONNECTED}, true, BotPlayer.Difficulty.EASY),
    FILLY_BOT_MEDIUM(new AnimationName[] {AnimationName.FILLY_BOT_MEDIUM_VICTORY, AnimationName.FILLY_BOT_MEDIUM_CONTENT, AnimationName.FILLY_BOT_MEDIUM_WORRIED, AnimationName.FILLY_BOT_MEDIUM_DEFEAT, AnimationName.FILLY_BOT_MEDIUM_DISCONNECTED}, false, BotPlayer.Difficulty.MEDIUM),
    FILLY_BOT_HARD(new AnimationName[] {AnimationName.FILLY_BOT_HARD_VICTORY, AnimationName.FILLY_BOT_HARD_CONTENT, AnimationName.FILLY_BOT_HARD_WORRIED, AnimationName.FILLY_BOT_HARD_DEFEAT, AnimationName.FILLY_BOT_HARD_DISCONNECTED}, false, BotPlayer.Difficulty.HARD),
    UNKNOWN_CHARACTER(new AnimationName[] {AnimationName.UNKNOWN_CHARACTER, AnimationName.UNKNOWN_CHARACTER, AnimationName.UNKNOWN_CHARACTER, AnimationName.UNKNOWN_CHARACTER, AnimationName.UNKNOWN_CHARACTER}, false, null);

    private AnimationName[] animationNames;
    private boolean playable; // Can a human player use this character?
    private BotPlayer.Difficulty botDifficulty;

    public enum CharacterAnimationState{
        VICTORIOUS(-1,-1), CONTENT(0,12), WORRIED(13,19), DEFEATED(20,Integer.MAX_VALUE), DISCONNECTED(Integer.MAX_VALUE,Integer.MAX_VALUE);
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

    CharacterType(AnimationName[] animationNames, boolean playable, BotPlayer.Difficulty botDifficulty){
        this.animationNames = animationNames;
        this.playable = playable;
        this.botDifficulty = botDifficulty;
    }

    public AnimationName getAnimationName(CharacterAnimationState characterAnimationType){
        return animationNames[characterAnimationType.ordinal()];
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
