package Classes.PlayerTypes;

import Classes.*;
import Classes.Animation.OrbImages;
import Classes.Character;
import Classes.Images.CannonImages;
import Classes.Images.CharacterImages;
import Classes.NetworkCommunication.PlayPanelData;
import Classes.NetworkCommunication.PlayerData;
import jdk.internal.org.objectweb.asm.util.CheckAnnotationAdapter;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static Classes.Orb.ORB_RADIUS;
import static Classes.PlayPanel.CANNON_Y_POS;
import static Classes.PlayPanel.PLAYPANEL_WIDTH_PER_PLAYER;
import static java.lang.Math.PI;

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
        transitionFrame = difficulty.getThinkingFrames();

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
                if(currentFrame == 0) startingAngle = cannon.getAngle();
                if(currentFrame==transitionFrame-1) pointCannon(target+broadMovementOffset);
                else pointCannon(getSmoothedAngle(broadMovementOffset));
                break;
            case INTERCESSION:
                break;
            case FINE_MOVEMENT:
                if(currentFrame == 0) startingAngle = cannon.getAngle();
                if(currentFrame==transitionFrame-1) pointCannon(target + fineMovementOffset);
                else pointCannon(getSmoothedAngle(fineMovementOffset));
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

    /* This method must set values for the following 3 fields:
     *    target - The actual angle that the bot will try to shoot at
     *    broadMovementOffset - How far off the bot will be from the target angle at the end of the broad movement phase
     *    fineMovementOffset - How far off the bot will be from the target angle at the end of the fine movement phase
     */
    private void retarget(){
        System.out.println("******RETARGETING*****");
        LinkedList<Outcome> choices = new LinkedList<>();

        // determine the outcome for a variety of shooting angles:
        for(double angle = -40.0; angle>-135.0; angle-=0.5){
            // Find out where the orb would land if we shot it at this angle:
            OrbImages currentShooterOrbEnum = playerData.getAmmunition().get(0).getOrbEnum();
            Orb hypotheticalOrb = new Orb(currentShooterOrbEnum,0,0,Orb.BubbleAnimationType.STATIC);
            hypotheticalOrb.setXPos(ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2 + PLAYPANEL_WIDTH_PER_PLAYER*playerData.getPlayerPos());
            hypotheticalOrb.setYPos(CANNON_Y_POS);
            hypotheticalOrb.setAngle(Math.toRadians(angle));
            PointInt landingPoint = playPanel.predictLandingPoint(hypotheticalOrb);

            // Assign a score to the landing point:
            hypotheticalOrb.setIJ(landingPoint.i, landingPoint.j);
            int score = assignScore(hypotheticalOrb, angle);

            // Add the Outcome to the list of possible choices:
            choices.add(new Outcome(angle,score));
        }

        // sort the possible choices by score, and choose one of the better ones (tempered by stupidity level):
        choices.sort(Comparator.comparingInt(Outcome::getNegativeScore));
        System.out.println("best choice: " + choices.get(0).score + " worst choice: " + choices.getLast().score);
        Outcome choice = choices.get((int)Math.round(offsetGenerator.nextDouble()*difficulty.getStupidity()));
        System.out.println("picked " + choice.score);

        // temporary, for debugging
        OrbImages currentShooterOrbEnum = playerData.getAmmunition().get(0).getOrbEnum();
        Orb hypotheticalOrb = new Orb(currentShooterOrbEnum,0,0,Orb.BubbleAnimationType.STATIC);
        hypotheticalOrb.setXPos(ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2 + PLAYPANEL_WIDTH_PER_PLAYER*playerData.getPlayerPos());
        hypotheticalOrb.setYPos(CANNON_Y_POS);
        hypotheticalOrb.setAngle(Math.toRadians(choice.angle));
        PointInt landingPoint = playPanel.predictLandingPoint(hypotheticalOrb);
        System.out.println("Predicted landing point is ("+landingPoint.i+","+landingPoint.j+")");

        //target = -180.0*(new Random().nextDouble()); // recall that JavaFx rotates clockwise instead of counterclockwise
        target = choice.angle;

        broadMovementOffset = difficulty.getBroadMovementOffset()*(2*offsetGenerator.nextDouble()-1.0);
        fineMovementOffset = difficulty.getFineMovementOffset()*(2*offsetGenerator.nextDouble()-1.0);
    }

    private int assignScore(Orb hypotheticalOrb, double angle){
        int score = 0;

        // determine whether the Orb would land next to any Orbs of the same color
        List<PointInt> neighbors = playPanel.getPlayPanelData().depthFirstSearch(hypotheticalOrb, PlayPanelData.FilterOption.SAME_COLOR);
        score += 3*neighbors.size();

        // It is undesirable for the orb to hit the ceiling:
        if (hypotheticalOrb.getI()==0) score-=2;

        // It looks nicer if the computer doesn't keep shooting in the same direction:
        if((cannon.getAngle()<-90 && angle<-90) || (cannon.getAngle()>-90 && angle>-90)) --score;

        // determine whether the Orb would cause other Orbs to drop

        // If the orb brings us closer to death, it is unfavorable

        return score;
    }

    private class Outcome{
        double angle;
        int score;
        Outcome(double angle, int score){
            this.angle = angle;
            this.score = score;
        }
        // for sorting in reverse
        int getNegativeScore(){
            return -score;
        }
    }

    private double getSmoothedAngle(double offset){
        // Make the motion smooth using a sigmoid function:
        double x = 12*(((double)currentFrame/(double)transitionFrame)-0.5);
        double smoothedProgress = 1/(1+Math.exp(-x));
        return startingAngle + smoothedProgress*(target+offset-startingAngle);
    }

    public enum Difficulty
    {
        EASY(2.0, 0.25, 1.0, 0.25, 1.0, 0.5, 10.0, 3.0, 10),
        MEDIUM(1.5, 0.25, 1.0, 0.25, 1.0, 0.5, 10.0, 1.5, 4),
        HARD(1.0, 0.25, 1.0, 0.25, 1.0, 0.5, 10.0, 0.001, 0.001);

        private int thinkingFrames;
        private int preMovementFrames;
        private int broadMovementFrames;
        private int intercessionFrames;
        private int fineMovementFrames;
        private int firingFrames;
        private double broadMovementOffset; // degrees
        private double fineMovementOffset; // degrees
        private double stupidity; // higher number means the computer is more likely to make bad choices.

        Difficulty(double thinkingTime, double preMovementTime, double broadMovementTime, double intercessionTime, double fineMovementTime, double firingTime, double broadMovementOffset, double fineMovementOffset, double stupidity){
            thinkingFrames = (int)Math.round(thinkingTime*GameScene.ANIMATION_FRAME_RATE);
            preMovementFrames = (int)Math.round(preMovementTime*GameScene.ANIMATION_FRAME_RATE);
            broadMovementFrames = (int)Math.round(broadMovementTime*GameScene.ANIMATION_FRAME_RATE);
            intercessionFrames = (int)Math.round(intercessionTime*GameScene.ANIMATION_FRAME_RATE);
            fineMovementFrames = (int)Math.round(fineMovementTime*GameScene.ANIMATION_FRAME_RATE);
            firingFrames = (int)Math.round(firingTime*GameScene.ANIMATION_FRAME_RATE);
            this.broadMovementOffset = broadMovementOffset;
            this.fineMovementOffset = fineMovementOffset;
            this.stupidity = stupidity;
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
        public double getStupidity(){
            return stupidity;
        }
    }
    private enum Phase{ THINKING, PRE_MOVEMENT, BROAD_MOVEMENT, INTERCESSION, FINE_MOVEMENT, FIRING}
}

