package Classes.PlayerTypes;

import Classes.*;
import Classes.Animation.OrbColor;
import Classes.Audio.SoundEffect;
import Classes.Animation.CharacterType;
import Classes.Player;
import Classes.NetworkCommunication.Synchronizer;

import java.util.*;
import java.util.concurrent.*;

import static Classes.Orb.NULL;
import static Classes.PlayPanel.CANNON_Y_POS;
import static Classes.PlayPanel.ORB_RADIUS;
import static Classes.PlayPanel.PLAYPANEL_WIDTH_PER_PLAYER;

public class BotPlayer extends Player {

    // Targeting
    private Difficulty difficulty;
    private final double ANGLE_INCREMENT = 1.0; // The resolution of the bot's simulated shots. Smaller == higher resolution but more computation.
    private double startingAngle;
    private double target;
    private double broadMovementOffset;
    private double fineMovementOffset;
    private Random offsetGenerator = new Random();

    // Cannon AnimationName control
    private Phase currentPhase = Phase.THINKING;
    private int currentFrame = 0;
    private int transitionFrame;

    // Multithreading
    private int numThreads = 5; // note: by experimentation, around 5 or maaaaybe 6 is optimal on my machine (4th gen i7) when there are 3 hard computer players.
    private ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);

    // Misc, for debugging
    private long[] botRetargetTime = {0,0,Long.MAX_VALUE,0}; // number of times the retarget() method has been called on bots, the cumulative tiem (nanoseconds) for their executions, minimum execution time, maximum execution time

    public BotPlayer(Player player){
        super(player.getUsername().getData(), player.getPlayerType().getData() , player.getPlayerID(), player.getSynchronizer());
        difficulty = player.getCharacter().getCharacterType().getData().getBotDifficulty();
        transitionFrame = difficulty.getThinkingFrames();
    }

    public BotPlayer(CharacterType characterType, Synchronizer synchronizer){
        super("fillyBot [" + characterType.getBotDifficulty() +"]", PlayerType.BOT, createID(), synchronizer);
        difficulty = character.getCharacterType().getData().getBotDifficulty();
        transitionFrame = difficulty.getThinkingFrames();
    }

    public void tick(){
        PlayerStatus currentPlayerStatus = getPlayerStatus().getData();
        if(currentPlayerStatus == PlayerStatus.DEFEATED || currentPlayerStatus == PlayerStatus.VICTORIOUS) return;

        // Handle the current frame of the current phase:
        switch(currentPhase){
            case THINKING:
                if(currentFrame == transitionFrame-1){
                    long time = System.nanoTime();
                    retarget();
                    if(target<0){
                        time = System.nanoTime() - time;
                        botRetargetTime[0]++;
                        botRetargetTime[1]+=time;
                        if(time < botRetargetTime[2]) botRetargetTime[2] = time;
                        if(time > botRetargetTime[3]) botRetargetTime[3] = time;
                    }
                }
                break;
            case PRE_MOVEMENT:
                break;
            case BROAD_MOVEMENT:
                if(currentFrame == 0) startingAngle = cannon.getCannonAngle().getData();
                if(currentFrame==transitionFrame-1) pointCannon(target+broadMovementOffset);
                else pointCannon(getSmoothedAngle(broadMovementOffset));
                break;
            case INTERCESSION:
                break;
            case FINE_MOVEMENT:
                if(currentFrame == 0) startingAngle = cannon.getCannonAngle().getData();
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
                        // System.out.println("No Orbs detected. Re-running thinking phase...");
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
        // Create copies of the existing data:
        Orb[][] orbArrayCopy = playPanel.deepCopyOrbArray(playPanel.getOrbArray().getData());
        Orb[] deathOrbsCopy = playPanel.deepCopyOrbArray(playPanel.getDeathOrbs());

        // New collections that will be affected by side-effects:
        List<Orb> burstingOrbsCopy = new LinkedList<>();
        List<Orb> droppingOrbsCopy = new LinkedList<>();
        Set<SoundEffect> soundEffectsToPlay = EnumSet.noneOf(SoundEffect.class);
        List<Orb> arrayOrbsToBurst = new LinkedList<>();
        List<Orb> orbsToDrop = new LinkedList<>();
        List<Orb> orbsToTransfer = new LinkedList<>();
        List<Collision> collisions = new LinkedList<>();
        Set<Orb> connectedOrbs = new HashSet<>(); // Orbs connected to the ceiling

        // Advance all existing shooter orbs, one at a time in order.
        // Note: They're done one at a time instead of all at once because a previously fired orb might supposed to
        // be clearing the path for the next orb. If all the shooting orbs are computed simultaneously, this fact
        // won't be simulated, and the 2nd shooting orb will get blocked in its simulation. This still isn't perfect
        // (for example, a more recently-fired shot might actually reach its target before a previous shot) but should
        // be pretty good.
        for(Orb shootingOrb : playPanel.getShootingOrbs()){
            // Create a temporary shootingOrbs list that contains a copy of only one of the current shooting orbs:
            List<Orb> shootingOrbCopy = new LinkedList<>();
            shootingOrbCopy.add(new Orb(shootingOrb));

            // Clear the other lists
            arrayOrbsToBurst.clear();
            connectedOrbs.clear();
            collisions.clear();
            burstingOrbsCopy.clear();
            droppingOrbsCopy.clear();

            // Determine the maximum time over which this shooting Orb could possibly travel:
            double maxYDistance = shootingOrb.getYPos(); // it's actually a little less than this, but I want to overestimate a little anyways.
            double maxXDistance = maxYDistance/Math.tan(Math.toRadians(shootingOrb.getAngle()));
            double maxDistanceSquared = Math.pow(maxXDistance, 2.0) + Math.pow(maxYDistance, 2.0);
            double maxDistance = Math.sqrt(maxDistanceSquared);
            double maxTime = maxDistance/shootingOrb.getOrbColor().getOrbSpeed();

            playPanel.simulateOrbs(orbArrayCopy, burstingOrbsCopy, shootingOrbCopy, droppingOrbsCopy, deathOrbsCopy, soundEffectsToPlay, orbsToDrop, orbsToTransfer, collisions, connectedOrbs, arrayOrbsToBurst, maxTime);
        }

        // If there are no Orbs in the orbArray, then return a positive angle to indicate that the bot should wait.
        boolean empty = true;
        for (Orb orb : orbArrayCopy[0]){ // only need to check for Orbs along the ceiling
            if(orb !=NULL){
                empty = false;
                break;
            }
        }
        if(empty){
            target = 1;
            return;
        }

        // Find the lowest occupied row on the array and save that value. This is used later in the assignScore method.
        int lowestRow = playPanel.getLowestOccupiedRow(orbArrayCopy, deathOrbsCopy);

        // ** Let's determine the outcome for a variety of shooting angles ** //

        LinkedList<Outcome> choices = new LinkedList<>();

        // Create tasks to be run concurrently:
        List<HypotheticalOrbSimulator> tasks = new LinkedList<>();
        double angleDivision = (-135-(-40))/numThreads;
        for(int i=0; i<numThreads; i++){
            double startAngle = -40 + angleDivision*i;
            double endAngle = startAngle + angleDivision;
            tasks.add(new HypotheticalOrbSimulator(startAngle, endAngle, orbArrayCopy, deathOrbsCopy, lowestRow));
        }

        // Execute the tasks in a thread pool:
        List<Future<List<Outcome>>> futures = new LinkedList<>();
        try{
            futures = threadPool.invokeAll(tasks);
        } catch(InterruptedException e){
            e.printStackTrace();
        }

        // Consolidate all the results:
        for(Future<List<Outcome>> future : futures){
            try{
                choices.addAll(future.get());
            } catch(InterruptedException | ExecutionException e){ // I believe this exception happens if the Callable threw an exception during its execution.
                e.printStackTrace();
            }
        }

        // sort the possible choices by score, and choose one of the better ones (tempered by stupidity level):
        LinkedList<OutcomeBin> choiceBins = binSort(choices);
        int binChoice;
        do{
            binChoice = (int)Math.round(offsetGenerator.nextDouble()*difficulty.getStupidity());
        } while(binChoice>=choiceBins.size());
        OutcomeBin chosenBin = choiceBins.get(binChoice);
        Outcome choice = chosenBin.selectChoice(difficulty.getStupidity());

        target = choice.angle;
        broadMovementOffset = difficulty.getBroadMovementOffset()*(2*offsetGenerator.nextDouble()-1.0);
        fineMovementOffset = difficulty.getFineMovementOffset()*(2*offsetGenerator.nextDouble()-1.0);
    }

    private class HypotheticalOrbSimulator implements Callable<List<Outcome>>{
        Orb[][] orbArrayCopy;
        Orb[] deathOrbsCopy;
        int lowestRow;
        double startAngle;
        double endAngle;

        HypotheticalOrbSimulator(double startAngle, double endAngle, Orb[][] orbArrayCopy, Orb[] deathOrbsCopy, int lowestRow){
            this.startAngle = startAngle;
            this.endAngle = endAngle;
            this.orbArrayCopy = orbArrayCopy;
            this.deathOrbsCopy = deathOrbsCopy;
            this.lowestRow = lowestRow;
        }

        @Override
        public List<Outcome> call(){
            LinkedList<Outcome> choices = new LinkedList<>();
            List<Orb> arrayOrbsToBurst = new LinkedList<>();
            List<Orb> orbsToDrop = new LinkedList<>();
            List<Orb> orbsToTransfer = new LinkedList<>();
            Set<Orb> connectedOrbs = new HashSet<>(); // Orbs connected to the ceiling
            List<Collision> collisions = new LinkedList<>();
            List<Orb> burstingOrbsCopy = new LinkedList<>();
            List<Orb> droppingOrbsCopy = new LinkedList<>();
            Set<SoundEffect> soundEffectsToPlay = EnumSet.noneOf(SoundEffect.class);
            for(double angle = startAngle; angle>endAngle; angle-=ANGLE_INCREMENT){
                if (Math.abs(angle + 90)<0.0001) angle+=0.001; // todo: if the angle is exactly -90, then weird things happen. Look into this and fix it.

                /*-- Simulate the outcome if we were to fire at this angle --*/

                // Create a hypothetical shooter orb for the simulated shot:
                OrbColor currentShooterOrbEnum = getAmmunition().getData().get(0).getOrbColor();
                Orb hypotheticalOrb = new Orb(currentShooterOrbEnum,0,0, Orb.OrbAnimationState.STATIC);
                hypotheticalOrb.relocate(ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2 + PLAYPANEL_WIDTH_PER_PLAYER*getPlayerPos(),CANNON_Y_POS);
                hypotheticalOrb.setAngle(Math.toRadians(angle));
                hypotheticalOrb.setSpeed(hypotheticalOrb.getOrbColor().getOrbSpeed());
                List<Orb> shootingOrbCopy = new LinkedList<>();
                shootingOrbCopy.add(hypotheticalOrb);

                // Create copies of the simulated orbArray and deathOrbs:
                Orb[][] orbArrayCopyTemp = playPanel.deepCopyOrbArray(orbArrayCopy);
                Orb[] deathOrbsCopyTemp = playPanel.deepCopyOrbArray(deathOrbsCopy);

                // clear the other lists
                arrayOrbsToBurst.clear();
                orbsToDrop.clear();
                orbsToTransfer.clear();
                connectedOrbs.clear();
                collisions.clear();
                burstingOrbsCopy.clear();
                droppingOrbsCopy.clear();

                // Determine the maximum time over which the hypothetical Orb could possibly travel:
                double maxYDistance = hypotheticalOrb.getYPos(); // it's actually a little less than this, but I want to overestimate a little anyways.
                double maxXDistance = maxYDistance/Math.tan(Math.toRadians(angle));
                double maxDistanceSquared = Math.pow(maxXDistance, 2.0) + Math.pow(maxYDistance, 2.0);
                double maxDistance = Math.sqrt(maxDistanceSquared);
                double maxTime = maxDistance/hypotheticalOrb.getOrbColor().getOrbSpeed();

                playPanel.simulateOrbs(orbArrayCopyTemp, burstingOrbsCopy, shootingOrbCopy, droppingOrbsCopy, deathOrbsCopyTemp, soundEffectsToPlay, orbsToDrop, orbsToTransfer, collisions, connectedOrbs, arrayOrbsToBurst, maxTime);

                // Assign a score to the outcome:
                int score = assignScore(hypotheticalOrb, angle, orbsToTransfer, orbsToDrop, arrayOrbsToBurst, orbArrayCopyTemp, lowestRow);

                // Add the Outcome to the list of possible choices:
                choices.add(new Outcome(angle,score));
            }
            return choices;
        }
    }

    public long[] getBotRetargetTime(){
        return botRetargetTime;
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

    private int assignScore(Orb hypotheticalOrb, double angle, List<Orb> orbsToTransfer, List<Orb> orbsToDrop, List<Orb> arrayOrbsToBurst, Orb[][] orbArray, int lowestRow){
        int score = 0;

        // transferring Orbs is a very good thing:
        score += 3*(orbsToTransfer.size() + orbsToDrop.size());

        // bursting Orbs is also great:
        score += 2*arrayOrbsToBurst.size();

        // Otherwise, it is good if the orb is placed next to another Orb of the same color:
        List<PointInt> neighbors = playPanel.getNeighbors(new PointInt(hypotheticalOrb.getI(),hypotheticalOrb.getJ()), orbArray);
        int matchesFound = 0;
        for(PointInt point : neighbors){
            if(orbArray[point.getI()][point.getJ()].getOrbColor()==hypotheticalOrb.getOrbColor()) ++matchesFound;
        }
        if(matchesFound==1) ++ score; // note: if matches > 1, the orbs have already been accounted for, in arrayOrbsToBurst.

        // It is undesirable for the orb to hit the ceiling:
        if (hypotheticalOrb.getI()==0) score-=5;

        // It looks nicer if the computer doesn't keep shooting in the same direction:
        if((cannon.getCannonAngle().getData()<-90 && angle<-90) || (cannon.getCannonAngle().getData()>-90 && angle>-90)) --score;

        // If the orb brings us closer to the death line, it is unfavorable
        if(hypotheticalOrb.getI() > lowestRow) score-=2;

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
                previousAngle = choice.angle;
            }

            // Pick one of the bins at random and then pick its middlemost choice:
            List<Outcome> chosenBin = bins.get(offsetGenerator.nextInt(bins.size()));
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
        EASY(0.75, 0.45, 1.0, 0.25, 0.4, 0.30, 15.0, 2.00, 4.0),
        MEDIUM(0.3, 0.30, 0.5, 0.20, 0.25, 0.15, 10.0, 1.25, 2.0),
        HARD(0.15, 0.15, .25, 0.15, 0.20, 0.15, 10.0, 0.500, 0.0);

        private int thinkingFrames;
        private int preMovementFrames;
        private int broadMovementFrames;
        private int intercessionFrames;
        private int fineMovementFrames;
        private int firingFrames;
        private double broadMovementOffset; // degrees
        private double fineMovementOffset; // degrees
        private double stupidity; // higher number means the computer is more likely to make bad choices.
        private Random rand = new Random();

        Difficulty(double thinkingTime, double preMovementTime, double broadMovementTime, double intercessionTime, double fineMovementTime, double firingTime, double broadMovementOffset, double fineMovementOffset, double stupidity){
            thinkingFrames = (int)Math.round(thinkingTime*GameScene.DATA_FRAME_RATE);
            preMovementFrames = (int)Math.round(preMovementTime*GameScene.DATA_FRAME_RATE);
            broadMovementFrames = (int)Math.round(broadMovementTime*GameScene.DATA_FRAME_RATE);
            intercessionFrames = (int)Math.round(intercessionTime*GameScene.DATA_FRAME_RATE);
            fineMovementFrames = (int)Math.round(fineMovementTime*GameScene.DATA_FRAME_RATE);
            firingFrames = (int)Math.round(firingTime*GameScene.DATA_FRAME_RATE);
            this.broadMovementOffset = broadMovementOffset;
            this.fineMovementOffset = fineMovementOffset;
            this.stupidity = stupidity;
        }

        public int getThinkingFrames(){
            // randomize the amount of thinking time a little:
            return (int)Math.round(thinkingFrames*2*rand.nextDouble() + 1);
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

    public void cleanUp(){
        threadPool.shutdown();
    }
}

