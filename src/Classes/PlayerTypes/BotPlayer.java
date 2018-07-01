package Classes.PlayerTypes;

import Classes.*;
import Classes.Animation.MiscAnimations;
import Classes.Animation.OrbImages;
import Classes.Audio.SoundEffect;
import Classes.Audio.SoundManager;
import Classes.Character;
import Classes.Images.CannonImages;
import Classes.Images.CharacterImages;
import Classes.NetworkCommunication.PlayPanelData;
import Classes.NetworkCommunication.PlayerData;
import jdk.internal.org.objectweb.asm.util.CheckAnnotationAdapter;

import java.util.*;

import static Classes.GameScene.ANIMATION_FRAME_RATE;
import static Classes.Orb.NULL;
import static Classes.Orb.ORB_RADIUS;
import static Classes.PlayPanel.CANNON_Y_POS;
import static Classes.PlayPanel.PLAYPANEL_HEIGHT;
import static Classes.PlayPanel.PLAYPANEL_WIDTH_PER_PLAYER;
import static java.lang.Math.PI;

public class BotPlayer extends Player {

    private Difficulty difficulty;

    // Targeting
    private final double ANGLE_INCREMENT = 0.5; // The resolution of the bot's simulated shots. Smaller == higher resolution but more computation.
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
                    if(target<0){
                        currentPhase = Phase.PRE_MOVEMENT;
                        transitionFrame = difficulty.getPreMovementFrames();
                    }
                    else{
                        // The bot does not see any orbs on the orb array. Perhaps the next puzzle is still being loaded. Wait a bit.
                        System.out.println("No Orbs detected. Re-running thinking phase...");
                    }
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

        // determine the outcome for a variety of shooting angles:
        LinkedList<Outcome> choices = new LinkedList<>();
        for(double angle = -40.0; angle>-135.0; angle-=ANGLE_INCREMENT){
            if (Math.abs(angle + 90)<0.0001) angle+=0.001;
            /*-- Simulate the outcome if we were to fire at this angle --*/
            // First, create a hypothetical shooter orb, fired at this angle:
            OrbImages currentShooterOrbEnum = playerData.getAmmunition().get(0).getOrbEnum();
            Orb hypotheticalOrb = new Orb(currentShooterOrbEnum,0,0,Orb.BubbleAnimationType.STATIC);
            hypotheticalOrb.setXPos(ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2 + PLAYPANEL_WIDTH_PER_PLAYER*playerData.getPlayerPos());
            hypotheticalOrb.setYPos(CANNON_Y_POS);
            hypotheticalOrb.setAngle(Math.toRadians(angle));
            hypotheticalOrb.setSpeed(hypotheticalOrb.getOrbEnum().getOrbSpeed());

            // Now create copies of the existing shooting orbs, orbArray and deathOrbs:
            List<Orb> shootingOrbsCopy = playPanel.getPlayPanelData().deepCopyOrbList(playPanel.getPlayPanelData().getShootingOrbs());
            Orb[][] orbArrayCopy = playPanel.getPlayPanelData().deepCopyOrbArray(playPanel.getPlayPanelData().getOrbArray());
            Orb[] deathOrbsCopy = playPanel.getPlayPanelData().deepCopyOrbArray(playPanel.getPlayPanelData().getDeathOrbs());
            // todo: may not need copies of the following two. Empty lists should do:
            List<Orb> droppingOrbsCopy = playPanel.getPlayPanelData().deepCopyOrbList(playPanel.getPlayPanelData().getDroppingOrbs());
            List<Orb> burstingOrbsCopy = playPanel.getPlayPanelData().deepCopyOrbList(playPanel.getPlayPanelData().getBurstingOrbs());

            // create an EnumSet that must be passed to the methods:
            Set<SoundEffect> soundEffectsToPlay = EnumSet.noneOf(SoundEffect.class);

            // Determine the maximum time over which the hypothetical Orb could possibly travel:
            double maxYDistance = hypotheticalOrb.getYPos(); // it's actually a little less than this, but I want to overestimate a little anyways.
            double maxXDistance = maxYDistance/Math.tan(Math.toRadians(angle));
            double maxDistanceSquared = Math.pow(maxXDistance, 2.0) + Math.pow(maxYDistance, 2.20);
            double maxDistance = Math.sqrt(maxDistanceSquared);
            double maxTime = maxDistance/hypotheticalOrb.getOrbEnum().getOrbSpeed();

            //**** Simulate the shot in 2 steps ****//

            // Create placeholders for important things
            List<PointInt> arrayOrbsToBurst = new LinkedList<>();
            List<PointInt> orbsToDrop = new LinkedList<>();
            List<Orb> orbsToTransfer = new LinkedList<>();

            // The first step advances all the existing shooter orbs
            {
                // Advance shooting orbs and deal with all their collisions:
                List<PlayPanel.Collision> orbsToSnap = playPanel.advanceShootingOrbs(shootingOrbsCopy, orbArrayCopy, maxTime, soundEffectsToPlay);

                // Snap any landed shooting orbs into place:
                List<Orb> shootingOrbsToBurst = playPanel.snapOrbs(orbsToSnap, orbArrayCopy, deathOrbsCopy, soundEffectsToPlay);

                // Remove the snapped shooting orbs from the shootingOrbs list
                for(PlayPanel.Collision collision : orbsToSnap) shootingOrbsCopy.remove(collision.shooterOrb);

                // Determine whether any of the snapped orbs cause any orbs to burst:
                List<Orb> newOrbsToTransfer = new LinkedList<>(); // findPatternCompletions will fill this list as appropriate
                arrayOrbsToBurst.addAll(playPanel.findPatternCompletions(orbsToSnap, orbArrayCopy, shootingOrbsToBurst, soundEffectsToPlay, newOrbsToTransfer));
                orbsToTransfer.addAll(newOrbsToTransfer);

                // Burst new orbs:
                if(!shootingOrbsToBurst.isEmpty() || !arrayOrbsToBurst.isEmpty()){
                    soundEffectsToPlay.add(SoundEffect.EXPLOSION);
                    playPanel.getPlayPanelData().changeBurstShootingOrbs(shootingOrbsToBurst, shootingOrbsCopy, burstingOrbsCopy);
                    playPanel.getPlayPanelData().changeBurstArrayOrbs(arrayOrbsToBurst,orbArrayCopy,deathOrbsCopy, burstingOrbsCopy);
                }

                // Find floating orbs and drop them:
                Set<PointInt> connectedOrbs = playPanel.getPlayPanelData().findConnectedOrbs(orbArrayCopy); // orbs that are connected to the ceiling.
                orbsToDrop.addAll(playPanel.getPlayPanelData().findFloatingOrbs(connectedOrbs, orbArrayCopy));
                playPanel.getPlayPanelData().changeDropArrayOrbs(orbsToDrop, droppingOrbsCopy, orbArrayCopy);

            }

            // If there are no Orbs in the orbArray, then return a positive angle to indicate that the bot should wait.
            boolean empty = true;
            for (Orb[] row: orbArrayCopy) {
                for (Orb orb : row){
                    if(orb!=NULL){
                        empty = false;
                        break;
                    }
                }
                if(!empty) break;
            }
            if(empty){
                target = 1;
                return;
            }

            // The second step determines the outcome of the hypothetical orb
            {
                arrayOrbsToBurst.clear();
                orbsToDrop.clear();
                orbsToTransfer.clear();

                // Advance the hypothetical Orb and deal with all of its collisions:
                shootingOrbsCopy.clear();
                shootingOrbsCopy.add(hypotheticalOrb);
                List<PlayPanel.Collision> orbsToSnap = playPanel.advanceShootingOrbs(shootingOrbsCopy, orbArrayCopy, maxTime, soundEffectsToPlay);

                // Snap the hypothetical Orb into place:
                List<Orb> shootingOrbsToBurst = playPanel.snapOrbs(orbsToSnap, orbArrayCopy, deathOrbsCopy, soundEffectsToPlay);

                // Remove the snapped shooting orbs from the shootingOrbs list
                for(PlayPanel.Collision collision : orbsToSnap) shootingOrbsCopy.remove(collision.shooterOrb);

                // Determine whether any of the snapped orbs cause any orbs to burst:
                List<Orb> newOrbsToTransfer = new LinkedList<>(); // findPatternCompletions will fill this list as appropriate
                arrayOrbsToBurst.addAll(playPanel.findPatternCompletions(orbsToSnap, orbArrayCopy, shootingOrbsToBurst, soundEffectsToPlay, newOrbsToTransfer));
                orbsToTransfer.addAll(newOrbsToTransfer);

                // Find floating orbs and drop them:
                Set<PointInt> connectedOrbs = playPanel.getPlayPanelData().findConnectedOrbs(orbArrayCopy); // orbs that are connected to the ceiling.
                orbsToDrop.addAll(playPanel.getPlayPanelData().findFloatingOrbs(connectedOrbs, orbArrayCopy));
            }


            // Assign a score to the outcome:
            int score = assignScore(hypotheticalOrb, angle, orbsToTransfer, orbsToDrop, arrayOrbsToBurst, orbArrayCopy);

            // Add the Outcome to the list of possible choices:
            choices.add(new Outcome(angle,score));
        }

        // sort the possible choices by score, and choose one of the better ones (tempered by stupidity level):
        // Todo: sort the choices into bins of choices with the same score. Or just use a <Score, Choice> treemap
        LinkedList<OutcomeBin> choiceBins = binSort(choices);
        OutcomeBin chosenBin = choiceBins.get((int)Math.round(offsetGenerator.nextDouble()*difficulty.getStupidity()));
        Outcome choice = chosenBin.selectChoice(difficulty.getStupidity());

        // temporary, for debugging
        /*OrbImages currentShooterOrbEnum = playerData.getAmmunition().get(0).getOrbEnum();
        Orb hypotheticalOrb = new Orb(currentShooterOrbEnum,0,0,Orb.BubbleAnimationType.STATIC);
        hypotheticalOrb.setXPos(ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2 + PLAYPANEL_WIDTH_PER_PLAYER*playerData.getPlayerPos());
        hypotheticalOrb.setYPos(CANNON_Y_POS);
        hypotheticalOrb.setAngle(Math.toRadians(choice.angle));
        PointInt landingPoint = playPanel.predictLandingPoint(hypotheticalOrb);
        System.out.println("Predicted landing point is ("+landingPoint.i+","+landingPoint.j+")");*/

        //target = -180.0*(new Random().nextDouble()); // recall that JavaFx rotates clockwise instead of counterclockwise
        target = choice.angle;
        broadMovementOffset = difficulty.getBroadMovementOffset()*(2*offsetGenerator.nextDouble()-1.0);
        fineMovementOffset = difficulty.getFineMovementOffset()*(2*offsetGenerator.nextDouble()-1.0);
    }

    private LinkedList<OutcomeBin> binSort(List<Outcome> choices){
        Map<Integer,OutcomeBin> lookupMap = new HashMap<>();

        // Sort the choices into bins:
        OutcomeBin binChoices;
        for(Outcome choice : choices){
            binChoices = lookupMap.get(choice.score);
            if(binChoices==null) lookupMap.put(choice.score, new OutcomeBin(choice));
            else binChoices.add(choice);
        }

        // Sort the list of bins:
        LinkedList<OutcomeBin> bins = new LinkedList<>(lookupMap.values());
        bins.sort(Comparator.comparingInt(OutcomeBin::getNegativeScore));
        return bins;
    }

    private int assignScore(Orb hypotheticalOrb, double angle, List<Orb> orbsToTransfer, List<PointInt> orbsToDrop, List<PointInt> arrayOrbsToBurst, Orb[][] orbArray){
        int score = 0;

        // transferring Orbs is a very good thing:
        score += 3*(orbsToTransfer.size() + orbsToDrop.size());

        // bursting Orbs is also great:
        score += 2*arrayOrbsToBurst.size();

        // Otherwise, it is good if the orb is placed next to another Orb of the same color:
        List<PointInt> neighbors = playPanel.getPlayPanelData().getNeighbors(new PointInt(hypotheticalOrb.getI(),hypotheticalOrb.getJ()), orbArray);
        int matchesFound = 0;
        for(PointInt point : neighbors){
            if(orbArray[point.i][point.j].getOrbEnum()==hypotheticalOrb.getOrbEnum()) ++matchesFound;
        }
        if(matchesFound==1) ++ score; // note: if matches > 1, the orbs have already been accounted for, in arrayOrbsToBurst.

        // It is undesirable for the orb to hit the ceiling:
        if (hypotheticalOrb.getI()==0) --score;

        // It looks nicer if the computer doesn't keep shooting in the same direction:
        if((cannon.getAngle()<-90 && angle<-90) || (cannon.getAngle()>-90 && angle>-90)) --score;

        // Todo: If the orb brings us closer to the death line, it is unfavorable

        return score;
    }

    private class Outcome{
        double angle;
        int score;
        Outcome(double angle, int score){
            this.angle = angle;
            this.score = score;
        }
        double getAngle(){
            return angle;
        }
    }

    private class OutcomeBin{
        List<Outcome> binChoices = new LinkedList<>();
        OutcomeBin(Outcome choice){
            add(choice);
        }
        void add(Outcome outcome){
            binChoices.add(outcome);
        }
        // for sorting in reverse
        int getNegativeScore(){
            return -binChoices.get(0).score;
        }

        Outcome selectChoice(double stupidity){
            // First, sort the choices by angle:
            binChoices.sort(Comparator.comparingDouble(Outcome::getAngle));

            // Now put the choices into bins, based on angle:
            List<List<Outcome>> bins = new LinkedList<>();
            List<Outcome> currentBin = new LinkedList<>();
            double previousAngle = 90;
            double adjustedAngleIncrement = ANGLE_INCREMENT*3/2;
            for(Outcome choice : binChoices){
                if(Math.abs(choice.angle-previousAngle)>adjustedAngleIncrement) {
                    currentBin = new LinkedList<>();
                    bins.add(currentBin);
                }
                currentBin.add(choice);
            }

            // Pick one of the bins at random and then pick its middlemost choice:
            List<Outcome> chosenBin = bins.get(offsetGenerator.nextInt(bins.size()));
            // todo: the bot doesn't seem to always pick the middlemost choice. investigate this.
            //chosenBin.sort(Comparator.comparingDouble(Outcome::getAngle));
            return chosenBin.get(chosenBin.size()/2);
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
        HARD(0.15, 0.05, .25, 0.05, 0.25, 0.1, 10.0, 0.000, 0.000);

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

