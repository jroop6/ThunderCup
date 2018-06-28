package Classes.PlayerTypes;

import Classes.Cannon;
import Classes.Character;
import Classes.GameScene;
import Classes.Images.CannonImages;
import Classes.Images.CharacterImages;
import Classes.NetworkCommunication.PlayerData;
import Classes.Orb;
import jdk.internal.org.objectweb.asm.util.CheckAnnotationAdapter;

import java.util.Random;

public class BotPlayer extends Player {

    private Difficulty difficulty;

    // Targeting
    private double startingAngle;
    private double target;
    private double broadMovementOffset;
    private double fineMovementOffset;
    private Random offsetGenerator = new Random();

    // Animation control
    private Phase currentPhase = Phase.THINKING;
    private int currentFrame = 0;
    private int transitionFrame;

    public BotPlayer(Difficulty difficulty){
        // create a (probably) unique player ID:
        long playerID;
        do{
            playerID = (new Random()).nextLong();
            System.out.println("player ID is: " + playerID);
        } while (playerID == 0 || playerID == -1); // A non-host player absolutely cannot have an ID of 0 or -1. These are reserved for the host and unclaimed player slots, respectively.

        // initialize model:
        this.playerData = new PlayerData("FillyBot [easy]",playerID);
        playerData.setCannon(CannonImages.BOT_CANNON);
        playerData.setCharacter(CharacterImages.FILLY_BOT);
        this.difficulty = difficulty;

        // initialize views:
        this.cannon = new Cannon(playerData);
        this.character = new Character(playerData);
        character.setCharacterEnum(CharacterImages.FILLY_BOT); // character initializes with a default player, so change it to a bot, here.
        usernameButton.setText(playerData.getUsername());
        teamChoice.getSelectionModel().select(playerData.getTeam()-1);
    }

    public void tick(){
        if(playerData.getCannonDisabled()) return;

        // Handle the current frame:
        switch(currentPhase){
            case THINKING:
                if(currentFrame == transitionFrame-1) retarget();
                break;
            case PRE_MOVEMENT:
                break;
            case BROAD_MOVEMENT:
                if(currentFrame == 0){
                    startingAngle = cannon.getAngle();
                }
                pointCannon(getSmoothedAngle(broadMovementOffset));
                if(currentFrame==transitionFrame-1) cannon.setAngle(target+broadMovementOffset);
                break;
            case INTERCESSION:
                break;
            case FINE_MOVEMENT:
                if(currentFrame == 0){
                    startingAngle = cannon.getAngle();
                }
                pointCannon(getSmoothedAngle(fineMovementOffset));
                if(currentFrame==transitionFrame-1) cannon.setAngle(target + fineMovementOffset);
                break;
            case FIRING:
                if(currentFrame==transitionFrame-1) changeFireCannon();
        }

        // Increment currentFrame. If the time has come, transition to the next phase
        currentFrame++;
        if(currentFrame>=transitionFrame){
            currentFrame = 0;
            switch(currentPhase){
                case THINKING:
                    currentPhase = Phase.PRE_MOVEMENT;
                    transitionFrame = difficulty.getPreMovementFrames();
                    break;
                case PRE_MOVEMENT:
                    currentPhase = Phase.BROAD_MOVEMENT;
                    transitionFrame = difficulty.getBroadMovementFrames();
                    break;
                case BROAD_MOVEMENT:
                    currentPhase = Phase.INTERCESSION;
                    transitionFrame = difficulty.getIntercessionFrames();
                case INTERCESSION:
                    currentPhase = Phase.FINE_MOVEMENT;
                    transitionFrame = difficulty.getFineMovementFrames();
                    break;
                case FINE_MOVEMENT:
                    currentPhase = Phase.FIRING;
                    transitionFrame = difficulty.getFiringFrames();
                    break;
                case FIRING:
                    currentPhase = Phase.THINKING;
                    transitionFrame = difficulty.getThinkingFrames();
                    break;
            }
        }
    }

    public double computeInitialDistance(Orb orb){
        return 0.0;
    }

    private void retarget(){
        target = -180.0*(new Random().nextDouble()); // recall that JavaFx rotates clockwise instead of counterclockwise
        broadMovementOffset = difficulty.getBroadMovementOffset()*(2*offsetGenerator.nextDouble()-1.0);
        fineMovementOffset = difficulty.getFineMovementOffset()*(2*offsetGenerator.nextDouble()-1.0);
    }

    private double getSmoothedAngle(double offset){
        // Make the motion smooth using a sigmoid function:
        double x = 12*(((double)currentFrame/(double)transitionFrame)-0.5);
        double smoothedProgress = 1/(1+Math.exp(-x));
        double smoothedAngle = startingAngle + smoothedProgress*(target+offset-startingAngle);
        return smoothedAngle;
    }

    public enum Difficulty
    {
        EASY(2.0, 0.25, 1.0, 0.25, 1.0, 0.5, 10.0, 5.0),
        MEDIUM(1.5, 0.25, 1.0, 0.25, 1.0, 0.5, 10.0, 2.5),
        HARD(1.0, 0.25, 1.0, 0.25, 1.0, 0.5, 10.0, 0.5);

        private int thinkingFrames;
        private int preMovementFrames;
        private int broadMovementFrames;
        private int intercessionFrames;
        private int fineMovementFrames;
        private int firingFrames;
        private double broadMovementOffset; // degrees
        private double fineMovementOffset; // degrees

        Difficulty(double thinkingTime, double preMovementTime, double broadMovementTime, double intercessionTime, double fineMovementTime, double firingTime, double broadMovementOffset, double fineMovementOffset){
            thinkingFrames = (int)Math.round(thinkingTime*GameScene.ANIMATION_FRAME_RATE);
            preMovementFrames = (int)Math.round(preMovementTime*GameScene.ANIMATION_FRAME_RATE);
            broadMovementFrames = (int)Math.round(broadMovementTime*GameScene.ANIMATION_FRAME_RATE);
            intercessionFrames = (int)Math.round(intercessionTime*GameScene.ANIMATION_FRAME_RATE);
            fineMovementFrames = (int)Math.round(fineMovementTime*GameScene.ANIMATION_FRAME_RATE);
            firingFrames = (int)Math.round(firingTime*GameScene.ANIMATION_FRAME_RATE);
            this.broadMovementOffset = broadMovementOffset;
            this.fineMovementOffset = fineMovementOffset;
        }

        public int getThinkingFrames(){
            return thinkingFrames;
        }
        public int getPreMovementFrames(){
            return preMovementFrames;
        }
        public int getBroadMovementFrames(){
            return broadMovementFrames;
        }
        public int getIntercessionFrames(){
            return intercessionFrames;
        }
        public int getFineMovementFrames(){
            return fineMovementFrames;
        }
        public int getFiringFrames(){
            return firingFrames;
        }
        public double getBroadMovementOffset(){
            return broadMovementOffset;
        }
        private double getFineMovementOffset(){
            return fineMovementOffset;
        }
    }
    private enum Phase{ THINKING, PRE_MOVEMENT, BROAD_MOVEMENT, INTERCESSION, FINE_MOVEMENT, FIRING}
}

