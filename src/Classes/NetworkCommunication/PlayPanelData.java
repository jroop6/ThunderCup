package Classes.NetworkCommunication;

import Classes.Animation.OrbImages;
import Classes.Audio.SoundEffect;
import Classes.Audio.SoundManager;
import Classes.Orb;
import Classes.PointInt;

import java.io.*;
import java.util.*;

import static Classes.Orb.NULL;

/**
 * Contains Orb data relevant to a particular PlayPanel.
 * Note: ammunition orbs are considered specific to a player, and are therefore kept in PlayerData.
 */
public class PlayPanelData implements Serializable {
    public static final int ARRAY_HEIGHT = 20;
    public static final int ARRAY_WIDTH_PER_CHARACTER = 30;
    private static final int NUM_FRAMES_ERROR_TOLERANCE = 12; // the number of frames for which orbArray data that is inconsistent with the host is tolerated. After this many frames, the orbArray is overwritten with the host's data.
    public static final int SHOTS_BETWEEN_DROPS = 15*ARRAY_WIDTH_PER_CHARACTER; // After the player shoots this many times, a new row of orbs appears at the top.

    private Random randomPuzzleGenerator;

    private int team;
    private int numPlayers;
    private int shotsUntilNewRow;
    private Orb orbArray[][];
    private Orb deathOrbs[]; // orbs below the line of death. If these are not immediately cleared in 1 frame, then this team has lost.
    private List<Orb> shootingOrbs = new LinkedList<>();
    private List<Orb> burstingOrbs = new LinkedList<>();
    private List<Orb> droppingOrbs = new LinkedList<>();
    private List<Orb> transferOutOrbs = new LinkedList<>(); // orbs to be transferred to other players
    private List<Orb> transferInOrbs = new LinkedList<>(); // orbs to be transferred from other players
    private List<Orb> thunderOrbs = new LinkedList<>(); // orbs that have dropped off the PlayPanel explode in thunder.
    //Todo: make deathOrbs an array with width=PLAYPANLE_WIDTH_PER_PLAYER*numPlayers, for easier lookup by (i,j) coordinates
    private boolean victorious = false;

    // cumulative data for this playpanel, for end-of-game statistics:
    private int cumulativeShotsFired = 0; // involving all players of this playpanel.
    private int cumulativeOrbsBurst = 0;
    private int cumulativeOrbsTransferred = 0;
    private int cumulativeOrbsDropped = 0;
    private int largestGroupExplosion = 0;

    // Flags indicating a change in the data:
    private boolean orbArrayChanged = false;
    private boolean shootingOrbsChanged = false;
    private boolean burstingOrbsChanged = false;
    private boolean droppingOrbsChanged = false;
    private boolean transferOutOrbsChanged = false;
    private boolean transferInOrbsChanged = false;
    private boolean deathOrbsChanged = false;
    private boolean victoriousChanged = false;
    private boolean shotsFiredChanged = false;

    // Counter for how many frames the local orbArray data has been inconsistent with data from the host:
    private int inconsistencyCounter = 0; // hopefully 0 most of the time!

    // Options available for depth-first search
    public enum FilterOption {ALL, SAME_COLOR}

    // The size of an PlayPanelData's orbArray depends on the number of players associated with the PlayPanel:
    public PlayPanelData(int team, int numPlayers, int seed, String puzzleUrl){
        this.team = team;
        this.numPlayers = numPlayers;
        this.randomPuzzleGenerator = new Random(seed);

        orbArray = new Orb[ARRAY_HEIGHT][ARRAY_WIDTH_PER_CHARACTER*numPlayers];
        initializeOrbArray(puzzleUrl);

        deathOrbs = new Orb[ARRAY_WIDTH_PER_CHARACTER*numPlayers];
        Arrays.fill(deathOrbs,NULL);

        shotsUntilNewRow = SHOTS_BETWEEN_DROPS*numPlayers;
    }

    // The Copy Constructor is used to make a copy of all the data before sending it out over the network.
    public PlayPanelData(PlayPanelData other){
        team = other.getTeam();
        numPlayers = other.getNumPlayers();
        shotsUntilNewRow = other.getShotsUntilNewRow();

        orbArray = deepCopyOrbArray(other.getOrbArray());

        //todo: don't copy NULL. Just set the reference to NULL.
        deathOrbs = new Orb[ARRAY_WIDTH_PER_CHARACTER*numPlayers];
        Orb[] otherDeathOrbs = other.getDeathOrbs();
        // System.arraycopy(other.getDeathOrbs(),0,deathOrbs,0,deathOrbs.length); // Unfortunately, System.arraycopy doesn't work because it only copies references to the orbs. Hence, the orbs might get modified before they are sent over the socket connection.
        for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
            deathOrbs[j] = new Orb(otherDeathOrbs[j]);
        }

        shootingOrbs = deepCopyOrbList(other.getShootingOrbs());
        burstingOrbs = deepCopyOrbList(other.getBurstingOrbs());
        droppingOrbs = deepCopyOrbList(other.getDroppingOrbs());
        transferInOrbs = deepCopyOrbList(other.getTransferInOrbs());
        transferOutOrbs = deepCopyOrbList(other.getTransferOutOrbs());
        victorious = other.getVictorious();

        orbArrayChanged = other.isOrbArrayChanged();
        shootingOrbsChanged = other.isShootingOrbsChanged();
        burstingOrbsChanged = other.isBurstingOrbsChanged();
        droppingOrbsChanged = other.isDroppingOrbsChanged();
        transferInOrbsChanged = other.isTransferInOrbsChanged();
        transferOutOrbsChanged = other.isTransferOutOrbsChanged();
        victoriousChanged = other.isVictoriousChanged();
        deathOrbsChanged = other.isDeathOrbsChanged();
    }

    public Orb[] deepCopyOrbArray(Orb[] other){
        Orb[] copiedArray = new Orb[other.length];
        for(int j=0; j<other.length; j++){
            if(other[j].equals(NULL)) copiedArray[j] = NULL;
            else other[j] = new Orb(other[j]);
        }
        return copiedArray;
    }

    // todo: This doesn't work and I can't figure out why. An alternative implementation that does work is given below.
    /*public Orb[][] deepCopyOrbArray(Orb[][] other){
        Orb[][] copiedArray = new Orb[other.length][];
        // deep copy one row at a time:
        for(int i=0; i<other.length; i++){
            copiedArray[i] = deepCopyOrbArray(other[i]);
        }
        return copiedArray;
    }*/

    public Orb[][] deepCopyOrbArray(Orb[][] other){
        Orb[][] copiedArray = new Orb[other.length][other[0].length];
        // deep copy one row at a time:
        for(int i=0; i<other.length; i++){
            for(int j=0; j<other[0].length; j++){
                if(other[i][j].equals(NULL)) copiedArray[i][j] = NULL;
                else copiedArray[i][j] = new Orb(other[i][j]);
            }
        }
        return copiedArray;
    }

    public List<Orb> deepCopyOrbList(List<Orb> other){
        List<Orb> copiedList = new LinkedList<>();
        for(Orb orb : other){
            copiedList.add(new Orb(orb));
        }
        return copiedList;
    }

    /* Changers: These are called when the host wants to notify the client that something has changed in the official
     * data (e.g. Orbs burst, dropped or fired). */

    /* Specialized Changers */
    public void changeAddShootingOrbs(Queue<Orb> newShootingOrbs){
        shootingOrbs.addAll(newShootingOrbs);
        cumulativeShotsFired +=newShootingOrbs.size();
        shootingOrbsChanged = true;
        shotsFiredChanged = true;
    }
    public void changeAddTransferOutOrbs(List<Orb> newTransferOrbs){
        transferOutOrbs.addAll(newTransferOrbs);
        cumulativeOrbsTransferred += newTransferOrbs.size();
        transferOutOrbsChanged = true;
    }
    public void changeAddTransferInOrbs(List<Orb> transferOutOrbs, Random miscRandomGenerator){
        // Make a deep copy of the orbs to be transferred. We can't place the same orb instance in 2 PlayPanels
        List<Orb> newTransferOrbs = deepCopyOrbList(transferOutOrbs);

        // The new transfer orbs need to be placed appropriately. Find open, connected spots:
        int offset = 0;
        for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
            if(orbArray[0][j]!=NULL){
                offset = j%2;
                break;
            }
        }
        List<PointInt> openSpots = new LinkedList<>();
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=offset + i%2 - 2*offset*i%2; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j+=2){
                if(orbArray[i][j]==NULL && (!getNeighbors(new PointInt(i,j), orbArray).isEmpty() || i==0) && !isTransferInOrbOccupyingPosition(i,j)){
                    openSpots.add(new PointInt(i,j));
                }
            }
        }

        // Now pick one spot for each orb:
        //todo: if the orbArray is full, put transfer orbs in the deathOrbs list
        //todo: if there are otherwise not enough open, connected spots, then place transferOrbs in secondary and tertiary locations.
        for(Orb orb : newTransferOrbs){
            if(openSpots.isEmpty()) break; //todo: temporary fix to avoid calling nextInt(0). In the future, place transferOrbs in secondary and tertiary locations.
            int index = miscRandomGenerator.nextInt(openSpots.size());
            PointInt openSpot = openSpots.get(index);
            orb.setIJ(openSpot.i,openSpot.j);
            orb.setAnimationEnum(Orb.BubbleAnimationType.TRANSFERRING);
            System.out.println("size of openSpots is " + openSpots.size());
            openSpots.remove(index);
        }

        // now, finally add them to the appropriate Orb list:
        transferInOrbs.addAll(newTransferOrbs);
        transferInOrbsChanged = true;
    }
    public void changeBurstShootingOrbs(List<Orb> newBurstingOrbs, List<Orb> shootingOrbs, List<Orb> burstingOrbs){
        shootingOrbs.removeAll(newBurstingOrbs);
        burstingOrbs.addAll(newBurstingOrbs);
        for(Orb orb : newBurstingOrbs){
            orb.setAnimationEnum(Orb.BubbleAnimationType.IMPLODING);
        }
        if(burstingOrbs == this.burstingOrbs) burstingOrbsChanged = true; // to prevent a simulated shot from setting this value to true
        if(shootingOrbs == this.shootingOrbs) shootingOrbsChanged = true; // to prevent a simulated shot from setting this value to true
    }

    public void changeBurstArrayOrbs(List<PointInt> newBurstingOrbs, Orb[][] orbArray, Orb[] deathOrbs, List<Orb> burstingOrbs){
        for(PointInt point : newBurstingOrbs){
            Orb orb;
            if(validOrbArrayCoordinates(point)){
                orb = orbArray[point.i][point.j];
                orbArray[point.i][point.j] = NULL;
                if(orbArray == this.orbArray) orbArrayChanged = true; // to prevent a simulated shot from setting this value to true
            }
            else if(validDeathOrbsCoordinates(point)){
                orb = deathOrbs[point.j];
                deathOrbs[point.j] = NULL;
                if(deathOrbs == this.deathOrbs) deathOrbsChanged = true; // to prevent a simulated shot from setting this value to true
            }
            // We've already made sure that invalid snap coordinates are discarded and that offending orbs are burst
            // (see snapOrbs() method in PlayPanel) But, it doesn't hurt much to double-check.
            else{
                System.err.println("Error. Attempted to burst an orb at an invalid location. (" + point.i + ", "+point.j + ")");
                orb = new Orb(OrbImages.RED_ORB,0,0,Orb.BubbleAnimationType.IMPLODING); // to have something to call setAnimationEnum on.
            }
            orb.setAnimationEnum(Orb.BubbleAnimationType.IMPLODING);
            burstingOrbs.add(orb);
        }
        if(burstingOrbs == this.burstingOrbs){ // to prevent a simulated shot from setting these values.
            cumulativeOrbsBurst += newBurstingOrbs.size();
            burstingOrbsChanged = true;
        }
    }
    public void changeDropArrayOrbs(List<PointInt> newDroppingOrbs, List<Orb> droppingOrbs, Orb[][] orbArray){
        if(newDroppingOrbs.isEmpty()) return; // quick getaway
        for(PointInt point : newDroppingOrbs){
            Orb orb = orbArray[point.i][point.j];
            droppingOrbs.add(orb);
            orbArray[point.i][point.j] = NULL;
        }
        if(orbArray == this.orbArray){ // to prevent a simulated shot from setting this value
            orbArrayChanged = true;
        }
        if(droppingOrbs == this.droppingOrbs){ // to prevent a simulated shot from setting these values.
            cumulativeOrbsDropped += newDroppingOrbs.size();
            droppingOrbsChanged = true;
        }
    }
    public void changeAddDeathOrb(Orb orb){
        deathOrbs[orb.getJ()] = orb;
        deathOrbsChanged = true;
    }
    public void changeDeclareVictory(){
        if(!victorious){ // Don't do anything if we've already declared victory
            this.victorious = true;
            this.victoriousChanged = true;
        }
    }

    public void decrementShotsUntilNewRow(int amountToDecrement){
        shotsUntilNewRow-=amountToDecrement;
    }

    /* Setters: These are called by clients when they are updating their data according to data from the host*/
    //ToDo: Do I really need these, or should I just use the copy constructor?
    public void setAddShootingOrbs(Queue<Orb> newShootingOrbs){
        shootingOrbs.addAll(newShootingOrbs);
        cumulativeShotsFired +=newShootingOrbs.size();
    }
    public void setAddDroppingOrb(Orb newOrb){
        droppingOrbs.add(newOrb);
    }
    public void setAddThunderOrbs(List<Orb> newThunderOrbs){
        thunderOrbs.addAll(newThunderOrbs);
    }
    public void setOrbArray(Orb[][] newOrbArray){
        for (int i=0; i<ARRAY_HEIGHT; i++){
            //System.arraycopy(newOrbArray[i],0,orbArray[i],0,orbArray[i].length); // System.arraycopy is faster, but it might cost more memory because it causes the game to retain a reference to the packet from the host. I'm not sure which option is better... Either way, I'd have to replace all the NULL orbs with references to the local NULL orb anyways.
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                Orb otherOrb = newOrbArray[i][j];
                if(otherOrb.equals(NULL)) orbArray[i][j] = NULL; // note: The host's Orb.NULL is different from our own. This is why we must use .equals instead of == and replace all these instances with a reference to our own Orb.NULL.
                else orbArray[i][j] = new Orb(otherOrb);
            }
        }
    }
    public void setDeathOrbs(Orb[] newDeathOrbs){
        for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
            Orb otherOrb = newDeathOrbs[j];
            if(otherOrb.equals(NULL)) deathOrbs[j] = NULL; // note: The host's Orb.NULL is different from our own. This is why we must use .equals instead of == and replace all these instances with a reference to our own Orb.NULL.
            else deathOrbs[j] = new Orb(otherOrb);
        }
    }
    public void setShotsUntilNewRow(int newVal){
        shotsUntilNewRow = newVal;
    }
    public void setOrbList(List<Orb> listToOverwrite, List<Orb> listToCopy){
        listToOverwrite.clear();
        // Using addAll() would be faster and cleaner, but may use more memory because the game would have to keep a reference to the entire PlayPanelData from the host.
        for(Orb orb : listToCopy) listToOverwrite.add(new Orb(orb));
    }
    public void setAddDeathOrbs(List<Orb> newDeathOrbs){
        for(Orb newDeathOrb : newDeathOrbs){
            deathOrbs[newDeathOrb.getJ()] = newDeathOrb;
        }
    }
    public void setVictorious(boolean newVal){
        victorious = newVal;
    }
    public void setCumulativeShotsFired(int newVal){
        cumulativeShotsFired = newVal;
    }
    public void setCumulativeOrbsBurst(int newVal){
        cumulativeOrbsBurst = newVal;
    }
    public void setCumulativeOrbsTransferred(int newVal){
        cumulativeOrbsTransferred = newVal;
    }
    public void setCumulativeOrbsDropped(int newVal){
        cumulativeOrbsDropped = newVal;
    }
    public void setLargestGroupExplosion(int newVal){
        largestGroupExplosion = newVal;
    }

    /* Change Getters: These are called to see whether the host has changed the data. They are always
     * called before retrieving their corresponding, actual Orb data. */
    //ToDo: Do I really need these, considering that the clients will *always* be checking for consistency?
    public boolean isOrbArrayChanged(){
        return orbArrayChanged;
    }
    public boolean isShootingOrbsChanged(){
        return shootingOrbsChanged;
    }
    public boolean isBurstingOrbsChanged(){
        return burstingOrbsChanged;
    }
    public boolean isDroppingOrbsChanged(){
        return droppingOrbsChanged;
    }
    public boolean isTransferOutOrbsChanged(){
        return transferOutOrbsChanged;
    }
    public boolean isTransferInOrbsChanged(){
        return transferInOrbsChanged;
    }
    public boolean isDeathOrbsChanged(){
        return deathOrbsChanged;
    }
    public boolean isVictoriousChanged() {
        return victoriousChanged;
    }

    /* Direct Getters: These are called to get the actual player data*/
    public int getTeam(){
        return team;
    }
    public int getNumPlayers(){
        return numPlayers;
    }
    public Orb[][] getOrbArray(){
        return orbArray;
    }
    public List<Orb> getShootingOrbs(){
        return shootingOrbs;
    }
    public List<Orb> getBurstingOrbs(){
        return burstingOrbs;
    }
    public List<Orb> getDroppingOrbs(){
        return droppingOrbs;
    }
    public List<Orb> getTransferOutOrbs(){
        return transferOutOrbs;
    }
    public List<Orb> getTransferInOrbs(){
        return transferInOrbs;
    }
    public List<Orb> getThunderOrbs(){
        return thunderOrbs;
    }
    public Orb[] getDeathOrbs(){
        return deathOrbs;
    }
    public int getShotsUntilNewRow(){
        return shotsUntilNewRow;
    }
    public boolean getVictorious(){
        return victorious;
    }
    public int getCumulativeShotsFired(){
        return cumulativeShotsFired;
    }

    public int getCumulativeOrbsBurst(){
        return cumulativeOrbsBurst;
    }
    public int getCumulativeOrbsTransferred(){
        return cumulativeOrbsTransferred;
    }
    public int getCumulativeOrbsDropped(){
        return cumulativeOrbsDropped;
    }
    public int getLargestGroupExplosion(){
        return largestGroupExplosion;
    }

    public void resetFlags(){
        orbArrayChanged = false;
        shootingOrbsChanged = false;
        burstingOrbsChanged = false;
        droppingOrbsChanged = false;
        transferOutOrbsChanged = false;
        transferInOrbsChanged = false;
        deathOrbsChanged = false;
        victoriousChanged = false;
        shotsFiredChanged = false;
    }

    // In one game tick, the following happens here:
    //   1. Todo: Array bubble electrification animations are advanced; new electrified bubbles are spawned; the ones that have finished their animations are returned to a normal state.
    //   2. Todo: Dropping bubbles are advanced. The ones that have fallen off the edge of the screen are removed from the droppingBubbles list.
    //   3. Todo: Bursting bubble animations are advanced. The ones that are done are removed from the burstingOrbs list.
    //   4. Todo: Shooter bubbles are advanced
    void tick(){

    }

    // Todo: to be deprecated in favor of getNeighbors(Orb sourceOrb)
    public List<PointInt> getNeighbors(PointInt source, Orb[][] orbArray){
        int i = source.i;
        int j = source.j;
        List<PointInt> neighbors = new LinkedList<>();
        //test all possible neighbors for valid coordinates
        int[] iTests = {i-1, i-1, i, i, i+1, i+1};
        int[] jTests = {j-1, j+1, j-2, j+2, j-1, j+1};
        for(int k=0; k<iTests.length; k++){
            if(validOrbArrayCoordinates(new PointInt(iTests[k],jTests[k])) && orbArray[iTests[k]][jTests[k]]!=NULL){
                neighbors.add(new PointInt(iTests[k],jTests[k]));
            }
        }
        return neighbors;
    }

    public List<Orb> getNeighbors(Orb sourceOrb){
        int i = sourceOrb.getI();
        int j = sourceOrb.getJ();
        List<Orb> neighbors = new LinkedList<>();
        //test all possible neighbors for valid coordinates
        int[] iTests = {i-1, i-1, i, i, i+1, i+1};
        int[] jTests = {j-1, j+1, j-2, j+2, j-1, j+1};
        for(int k=0; k<iTests.length; k++){
            if(validOrbArrayCoordinates(new PointInt(iTests[k],jTests[k])) && orbArray[iTests[k]][jTests[k]]!=NULL){
                neighbors.add(orbArray[iTests[k]][jTests[k]]);
            }
        }
        return neighbors;
    }

    // todo: to be deprecated in favor of depthFirstSearch(Orb sourceOrb, FilterOption filter)
    public List<PointInt> depthFirstSearch(PointInt source, Orb[][] orbArray, FilterOption filter) {

        List<PointInt> matches = new LinkedList<>();

        // A boolean array that has the same size as the orbArray, to mark orbs as "examined"
        boolean examined[][] = new boolean[PlayPanelData.ARRAY_HEIGHT][PlayPanelData.ARRAY_WIDTH_PER_CHARACTER * numPlayers];

        // A stack containing the "active" elements to be examined next
        Deque<PointInt> active = new LinkedList<>();

        // Add the collision's shooter orb to the active list and mark it as "examined"
        active.push(source);
        if(validOrbArrayCoordinates(source)) examined[source.i][source.j] = true; // deathOrbs have "invalid" coordinates.

        // Do a depth-first search
        while (!active.isEmpty()) {
            PointInt activeOrb = active.pop();
            matches.add(new PointInt(activeOrb.i, activeOrb.j));
            List<PointInt> neighbors = getNeighbors(activeOrb, orbArray);
            for (PointInt neighbor : neighbors) {
                if (!examined[neighbor.i][neighbor.j]) {
                    // apply the filter option
                    boolean passesFilter = false;
                    switch(filter){
                        case ALL:
                            if(validOrbArrayCoordinates(source) || validDeathOrbsCoordinates(source)) passesFilter = true;
                            else{// This shouldn't ever happen, but checking anyways. // Todo: consider eliminating the previous check for performance.
                                System.err.println("depth-first search somehow encountered a PointInt with invalid coordinates");
                                passesFilter = false;
                            }
                            break;
                        case SAME_COLOR:
                            Orb sourceOrb;
                            if(validDeathOrbsCoordinates(source)){
                                sourceOrb = deathOrbs[source.j];
                            }
                            else if(validOrbArrayCoordinates(source)) sourceOrb = orbArray[source.i][source.j];
                            else{ // This shouldn't ever happen, but checking anyways. // Todo: consider turning the previous "else if" into an "else" for performance.
                                System.err.println("depth-first search somehow encountered a PointInt with invalid coordinates");
                                passesFilter = false;
                                break;
                            }
                            if(orbArray[neighbor.i][neighbor.j].getOrbEnum() == sourceOrb.getOrbEnum()) passesFilter = true;
                            break;
                    }
                    if(passesFilter){
                        active.add(neighbor);
                        examined[neighbor.i][neighbor.j] = true;
                    }
                }
            }
        }
        return matches;
    }

    public List<PointInt> depthFirstSearch(Orb sourceOrb, FilterOption filter) {
        List<PointInt> matches = new LinkedList<>();

        // A boolean array that has the same size as the orbArray, to mark orbs as "examined"
        boolean examined[][] = new boolean[PlayPanelData.ARRAY_HEIGHT][PlayPanelData.ARRAY_WIDTH_PER_CHARACTER * numPlayers];

        // A stack containing the "active" elements to be examined next
        Deque<Orb> active = new LinkedList<>();

        // Add the collision's shooter orb to the active list and mark it as "examined"
        active.push(sourceOrb);
        if(validOrbArrayCoordinates(new PointInt(sourceOrb.getI(),sourceOrb.getJ()))){
            examined[sourceOrb.getI()][sourceOrb.getJ()] = true; // deathOrbs have "invalid" coordinates.
        }

        // Do a depth-first search
        while (!active.isEmpty()) {
            Orb activeOrb = active.pop();
            matches.add(new PointInt(activeOrb.getI(), activeOrb.getJ()));
            List<Orb> neighbors = getNeighbors(activeOrb);
            for (Orb neighbor : neighbors) {
                if (!examined[neighbor.getI()][neighbor.getJ()]) {
                    // apply the filter option
                    boolean passesFilter = false;
                    switch(filter){
                        case ALL:
                            if(validOrbArrayCoordinates(new PointInt(neighbor.getI(),neighbor.getJ())) || validDeathOrbsCoordinates(new PointInt(neighbor.getI(),neighbor.getJ()))) passesFilter = true;
                            else{// This shouldn't ever happen, but checking anyways. // Todo: consider eliminating the previous check for performance.
                                System.err.println("depth-first search somehow encountered a PointInt with invalid coordinates");
                                passesFilter = false;
                            }
                            break;
                        case SAME_COLOR:
                            if(orbArray[neighbor.getI()][neighbor.getJ()].getOrbEnum() == sourceOrb.getOrbEnum()) passesFilter = true;
                            break;
                    }
                    if(passesFilter){
                        active.add(neighbor);
                        examined[neighbor.getI()][neighbor.getJ()] = true;
                    }
                }
            }
        }
        return matches;
    }

    public Set<PointInt> findConnectedOrbs(Orb[][] orbArray){
        Set<PointInt> connectedOrbs = new HashSet<>();

        // find all orbs connected to the ceiling:
        for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
            PointInt sourcePoint = new PointInt(0, j);
            if(orbArray[0][j]==NULL) continue;
            connectedOrbs.addAll(depthFirstSearch(sourcePoint, orbArray, FilterOption.ALL));
        }

        return connectedOrbs;
    }

    // Finds floating orbs and drops them. Also checks for victory conditions
    public List<PointInt> findFloatingOrbs(Set<PointInt> connectedOrbs, Orb[][] orbArray){
        List<PointInt> orbsToDrop = new LinkedList<>();

        // any remaining orbs in the array should be dropped.
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                if(orbArray[i][j]!=NULL){ // We only care about locations that have actual Orbs.
                    PointInt orb = new PointInt(i,j);
                    if(!connectedOrbs.contains(orb)){
                        orbsToDrop.add(orb);
                    }
                }
            }
        }
        return orbsToDrop;
    }


    // Todo: overload with validOrbArrayCoordinates(Orb sourceOrb)
    public boolean validOrbArrayCoordinates(PointInt testPoint)
    {
        return (testPoint.i>=0 && testPoint.i< ARRAY_HEIGHT && testPoint.j>=0 && testPoint.j <ARRAY_WIDTH_PER_CHARACTER*numPlayers);
    }

    public boolean validDeathOrbsCoordinates(PointInt testPoint){
        return (testPoint.i==ARRAY_HEIGHT && testPoint.j>=0 && testPoint.j<ARRAY_WIDTH_PER_CHARACTER*numPlayers);
    }

    // Utility function for debugging
    public void printOrbArray(){
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                if(orbArray[i][j]==null) System.out.print('X');
                if(orbArray[i][j]==NULL) System.out.print(' ');
                else System.out.print(orbArray[i][j].getOrbEnum().ordinal());
            }
            System.out.print('\n');
        }
    }

    public void checkForConsistency(PlayPanelData other){
        // Check that the orbArray is consistent between the two sets of data:
        boolean inconsistent = false;
        Orb[][] otherArray = other.getOrbArray();
        for(int i=0; i<ARRAY_HEIGHT; i++) {
            for (int j=0; j < ARRAY_WIDTH_PER_CHARACTER * numPlayers; j++) {
                if (otherArray[i][j].equals(NULL) && !(orbArray[i][j] == NULL)){ // note: cannot use == in the first clause because the host has a different instance of Orb.NULL than we do.
                    inconsistent = true;
                    System.out.println("orbArray["+i+"]["+j+"] is null but otherArray[i][j] is not null");
                }
                else if (orbArray[i][j].getOrbEnum() != otherArray[i][j].getOrbEnum()){
                    inconsistent = true;
                    System.out.println("array enums at ["+i+"]["+j+"] do not match");
                }
            }
        }

        // Check that the deathOrbs array is consistent between the two sets of data:
        inconsistent = false;
        Orb[] otherDeathOrbs = other.getDeathOrbs();
        for (int j=0; j < ARRAY_WIDTH_PER_CHARACTER * numPlayers; j++) {
            if (otherDeathOrbs[j].equals(NULL) && !(deathOrbs[j] == NULL)){ // note: cannot use == in the first clause because the host has a different instance of Orb.NULL than we do.
                inconsistent = true;
                System.out.println("deathOrbs["+j+"] is null but otherDeathOrbs[j] is not null");
            }
            else if (otherDeathOrbs[j].getOrbEnum() != deathOrbs[j].getOrbEnum()){
                inconsistent = true;
                System.out.println("deathOrb enums at ["+j+"] do not match");
            }
        }

        // Check that the number of shots remaining until the next row appears is consistent:
        if(other.getShotsUntilNewRow()!=getShotsUntilNewRow()){
            inconsistent = true;
            System.out.println("number of shots until new row appears is inconsistent between host and client");
        }

        // Check that the number of shooting, bursting, dropping, thunder, and transferOut orbs are consistent:
        if(other.getShootingOrbs().size() != shootingOrbs.size()) inconsistent = true;
        if(other.getBurstingOrbs().size() != burstingOrbs.size()) inconsistent = true;
        if(other.getDroppingOrbs().size() != droppingOrbs.size()) inconsistent = true;
        if(other.getThunderOrbs().size() != thunderOrbs.size()) inconsistent = true;
        if(other.getTransferOutOrbs().size() != transferOutOrbs.size()) inconsistent = true;

        // Check that the transferInOrbs list is consistent:
        if(other.getTransferInOrbs().size() != transferInOrbs.size()) inconsistent=true;
        else{
            for(Orb transferInOrb : other.getTransferInOrbs()){
                boolean matchFound = false;
                for(Orb transferInOrb2 : transferInOrbs){
                    if(transferInOrb.equals(transferInOrb2)){
                        matchFound = true;
                        break;
                    }
                }
                if(!matchFound){
                    inconsistent = true;
                    System.out.println("the transferInOrbs lists are inconsistent");
                }
            }
        }

        if(inconsistent) inconsistencyCounter++;
        else inconsistencyCounter = 0;

        if(inconsistencyCounter > NUM_FRAMES_ERROR_TOLERANCE){
            System.err.println("Client data is inconsistent with the host! overriding client data...");
            setOrbArray(other.getOrbArray());
            setDeathOrbs(other.getDeathOrbs());
            setShotsUntilNewRow(other.getShotsUntilNewRow());
            setOrbList(transferInOrbs, other.getTransferInOrbs());
            setOrbList(shootingOrbs, other.getShootingOrbs());
            setOrbList(burstingOrbs, other.getBurstingOrbs());
            setOrbList(droppingOrbs, other.getDroppingOrbs());
            setOrbList(thunderOrbs, other.getThunderOrbs());
            setOrbList(transferOutOrbs, other.getTransferOutOrbs());
            setCumulativeShotsFired(other.getCumulativeShotsFired());
            setCumulativeOrbsBurst(other.getCumulativeOrbsBurst());
            setCumulativeOrbsDropped(other.getCumulativeOrbsDropped());
            setCumulativeOrbsTransferred(other.getCumulativeOrbsTransferred());
            setLargestGroupExplosion(other.getLargestGroupExplosion());
            inconsistencyCounter = 0;
        }
    }

    private boolean isTransferInOrbOccupyingPosition(int i, int j){
        for(Orb orb : transferInOrbs){
            if(orb.getI()==i && orb.getJ()==j) return true;
        }
        return false;
    }

    public boolean isDeathOrbsEmpty(){
        for (Orb orb : deathOrbs){
            if(orb != NULL) return false;
        }
        return true;
    }


    /*
     * Puzzles are organized into groups. Naming convention for puzzles:
     *    puzzle_XX_YY_ZZ, where XX = number of players on the playpanel, YY = group index, and ZZ = individual index. Examples:
     *    puzzle_01_01_01 - single-player PlayPanel, puzzle 1-1
     *    puzzle_01_01_02 - single-player PlayPanel, puzzle 1-2
     *    puzzle_03_01_01 - three-player PlayPanel, puzzle 1-1
     * After the puzzle, shooter orbs are specified for each player on a separate line.
     *
     * Alternatively, a random puzzle can be specified with the "url" RANDOM_#, where # = the desired number of rows in the puzzle. Examples:
     *    RANDOM_5 - a random puzzle with 5 rows
     *    RANDOM_17 - a random puzzle with 17 rows
     */
    public boolean initializeOrbArray(String puzzleUrl){
        if(puzzleUrl.substring(0,6).equals("RANDOM")){
            int rows = Integer.parseInt(puzzleUrl.substring(7));
            if(rows>19) rows = 19;
            int orbEnumBound = OrbImages.values().length;
            OrbImages[] orbImages = OrbImages.values();

            for(int i=0; i<rows; ++i){
                for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                    if(j%2==i%2){
                        int randomOrdinal = randomPuzzleGenerator.nextInt(orbEnumBound);
                        OrbImages orbImage = orbImages[randomOrdinal];
                        orbArray[i][j] = new Orb(orbImage,i,j,Orb.BubbleAnimationType.STATIC);
                    }
                    else orbArray[i][j] = NULL;
                }
            }
            for(int i=rows; i<ARRAY_HEIGHT; i++){
                for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                    orbArray[i][j] = NULL;
                }
            }
        }
        else{
            String line;
            try{
                InputStream stream = getClass().getClassLoader().getResourceAsStream(puzzleUrl);
                if(stream == null) return false;
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                // Discard the first line, which is just there for human-readability
                while(!(line = reader.readLine()).equals("***PUZZLE***"));

                int i;
                int j;
                for(i=0; !((line = reader.readLine()).trim().isEmpty()) && i<ARRAY_HEIGHT; i++){
                    System.out.println("line is " + line);
                    for (j=0; j<line.length() && j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                        char orbSymbol = line.charAt(j);
                        OrbImages orbEnum = OrbImages.lookupOrbImageBySymbol(orbSymbol);
                        if(orbEnum==null) orbArray[i][j] = NULL;
                        else orbArray[i][j] = new Orb(orbEnum,i,j,Orb.BubbleAnimationType.STATIC);
                    }
                    // if the input line was too short to fill the entire puzzle line, fill in the rest of the line with NULL orbs:
                    for(/*j is already set*/; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                        orbArray[i][j] = NULL;
                    }
                }
                // fill the rest of the orb array with NULL orbs:
                for(/*i is already set*/; i<ARRAY_HEIGHT; i++){
                    for(j=0; j<ARRAY_WIDTH_PER_CHARACTER; j++){
                        orbArray[i][j] = NULL;
                    }
                }
                reader.close();

            } catch(IOException e){
                e.printStackTrace();
            }
        }
        shotsUntilNewRow = SHOTS_BETWEEN_DROPS;
        return true;
    }

    public void addNewRow(){
        System.out.println("ADDING NEW ROW");

        // Move the existing array down 1 index:
        int i = orbArray.length-1;
        for(int j=0; j<orbArray[i].length; j++){
            if(orbArray[i][j]!=NULL){
                System.out.println("This team has lost");
                orbArray[i][j].setIJ(i+1,j);
                deathOrbs[j] = orbArray[i][j];
                orbArray[i][j] = NULL;
            }
        }
        for(i=orbArray.length-2; i>=0; i--){
            for(int j=0; j<orbArray[i].length; j++){
                if(orbArray[i][j]!=NULL) orbArray[i][j].setIJ(i+1, j);
                orbArray[i+1][j] = orbArray[i][j];
            }
        }

        // Move all transferring orbs down 1 index:
        for(Orb transferOrb : transferInOrbs){
            transferOrb.setIJ(transferOrb.getI()+1,transferOrb.getJ());
        }

        // Determine whether the new row has "odd" or "even" placement:
        i = 1;
        int newRowOffset = 0;
        for(int j=0; j<orbArray[i].length; j++){
            if(orbArray[i][j] != NULL){
                newRowOffset = 1-j%2;
                break;
            }
        }

        // finally, add the new row
        i = 0;
        int orbEnumBound = OrbImages.values().length;
        OrbImages[] orbImages = OrbImages.values();
        for(int j=0; j<orbArray[i].length; j++){
            if(j%2==newRowOffset){
                int randomOrdinal = randomPuzzleGenerator.nextInt(orbEnumBound);
                OrbImages orbImage = orbImages[randomOrdinal];
                orbArray[i][j] = new Orb(orbImage,i,j,Orb.BubbleAnimationType.STATIC);
            }
            else orbArray[i][j] = NULL;
        }
    }
}
