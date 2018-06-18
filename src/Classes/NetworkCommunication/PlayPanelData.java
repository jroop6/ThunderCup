package Classes.NetworkCommunication;

import Classes.Orb;
import Classes.PointInt;
import javafx.scene.shape.Arc;

import java.io.Serializable;
import java.util.*;

/**
 * Contains Orb data relevant to a particular PlayPanel.
 * Note: ammunition orbs are considered specific to a player, and are therefore kept in PlayerData.
 */
public class PlayPanelData implements Serializable {
    public static final int ARRAY_HEIGHT = 20;
    public static final int ARRAY_WIDTH_PER_CHARACTER = 30;
    private static final int NUM_FRAMES_ERROR_TOLERANCE = 12; // the number of frames for which orbArray data that is inconsistent with the host is tolerated. After this many frames, the orbArray is overwritten with the host's data.
    public static final int SHOTS_BETWEEN_DROPS = 999; // After the player shoots this many times, a new row of orbs appears at the top.

    private int team;
    private int numPlayers;
    private int shotsUntilNewRow;
    private Orb orbArray[][];
    private List<Orb> shootingOrbs = new LinkedList<>();
    private List<Orb> burstingOrbs = new LinkedList<>();
    private List<Orb> droppingOrbs = new LinkedList<>();
    private List<Orb> transferOutOrbs = new LinkedList<>(); // orbs to be transferred to other players
    private List<Orb> transferInOrbs = new LinkedList<>(); // orbs to be transferred from other players
    private List<Orb> thunderOrbs = new LinkedList<>(); // orbs that have dropped off the PlayPanel explode in thunder.
    //Todo: make deathOrbs an array with width=PLAYPANLE_WIDTH_PER_PLAYER*numPlayers, for easier lookup by (i,j) coordinates
    private List<Orb> deathOrbs = new LinkedList<>(); // orbs below the line of death. If these are not immediately cleared in 1 frame, then this team has lost.
    private boolean victorious = false;

    // Flags indicating a change in the data:
    private boolean orbArrayChanged = false;
    private boolean shootingOrbsChanged = false;
    private boolean burstingOrbsChanged = false;
    private boolean droppingOrbsChanged = false;
    private boolean transferOutOrbsChanged = false;
    private boolean transferInOrbsChanged = false;
    private boolean deathOrbsChanged = false;
    private boolean victoriousChanged = false;

    // Counter for how many frames the local orbArray data has been inconsistent with data from the host:
    private int inconsistencyCounter = 0; // hopefully 0 most of the time!

    // Options available for depth-first search
    public enum FilterOption {ALL, SAME_COLOR}

    // The size of an PlayPanelData's orbArray depends on the number of players associated with the PlayPanel:
    public PlayPanelData(int team, int numPlayers){
        this.team = team;
        this.numPlayers = numPlayers;
        orbArray = new Orb[ARRAY_HEIGHT][ARRAY_WIDTH_PER_CHARACTER*numPlayers];
        shotsUntilNewRow = SHOTS_BETWEEN_DROPS*numPlayers;
    }

    // Copy Constructor:
    public PlayPanelData(PlayPanelData other){
        team = other.getTeam();
        numPlayers = other.getNumPlayers();
        shotsUntilNewRow = other.getShotsUntilNewRow();

        orbArray = new Orb[ARRAY_HEIGHT][ARRAY_WIDTH_PER_CHARACTER*other.getNumPlayers()];
        for(int i=0; i<ARRAY_HEIGHT; i++){
            System.arraycopy(other.getOrbArray()[i],0,orbArray[i],0,orbArray[i].length);
        }
        shootingOrbs.addAll(other.getShootingOrbs());
        burstingOrbs.addAll(other.getBurstingOrbs());
        droppingOrbs.addAll(other.getDroppingOrbs());
        transferInOrbs.addAll(other.getTransferInOrbs());
        transferOutOrbs.addAll(other.getTransferOutOrbs());
        deathOrbs.addAll(other.getDeathOrbs());
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

    /* Changers: These are called when the host wants to notify the client that something has changed in the official
     * data (e.g. Orbs burst, dropped or fired). */

    /* Specialized Changers */
    public void changeAddShootingOrbs(Queue<Orb> newShootingOrbs){
        shootingOrbs.addAll(newShootingOrbs);
        shootingOrbsChanged = true;
    }
    public void changeAddTransferOutOrbs(List<Orb> newTransferOrbs){
        transferOutOrbs.addAll(newTransferOrbs);
        transferOutOrbsChanged = true;
    }
    public void changeAddTransferInOrbs(List<Orb> newTransferOrbs, Random miscRandomGenerator){
        // The new transfer orbs need to be placed appropriately. Find open, connected spots:
        int offset = 0;
        for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
            if(orbArray[0][j]!=Orb.NULL){
                offset = j%2;
                break;
            }
        }
        List<PointInt> openSpots = new LinkedList<>();
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=offset + i%2 - 2*offset*i%2; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j+=2){
                if(orbArray[i][j]==Orb.NULL && (!getNeighbors(new PointInt(i,j)).isEmpty() || i==0) && !isTransferInOrbOccupyingPosition(i,j)){
                    openSpots.add(new PointInt(i,j));
                }
            }
        }

        // Now pick one spot for each orb:
        //todo: if the orbArray is full, put transfer orbs in the deathOrbs list
        //todo: if there are otherwise not enough open, connected spots, then place transferOrbs in secondary and tertiary locations.
        for(Orb orb : newTransferOrbs){
            int index = miscRandomGenerator.nextInt(openSpots.size());
            PointInt openSpot = openSpots.get(index);
            orb.setIJ(openSpot.i,openSpot.j);
            orb.setAnimationEnum(Orb.BubbleAnimationType.TRANSFERRING);
            System.out.println("size of openSpots is " + openSpots.size());
            openSpots.remove(index);
            if(openSpots.isEmpty()) break; //todo: temporary fix to avoid calling nextInt(0). In the future, place transferOrbs in secondary and tertiary locations.
        }

        // now, finally add them to the appropriate Orb list:
        transferInOrbs.addAll(newTransferOrbs);
        transferInOrbsChanged = true;
    }
    public void changeBurstShootingOrbs(List<Orb> newBurstingOrbs){
        shootingOrbs.removeAll(newBurstingOrbs);
        burstingOrbs.addAll(newBurstingOrbs);
        for(Orb orb : newBurstingOrbs){
            orb.setAnimationEnum(Orb.BubbleAnimationType.IMPLODING);
        }
        burstingOrbsChanged = true;
        shootingOrbsChanged = true;
    }
    public void changeBurstArrayOrbs(List<PointInt> newBurstingOrbs){
        for(PointInt point : newBurstingOrbs){
            Orb orb = Orb.NULL;
            if(validCoordinates(point)) orb = orbArray[point.i][point.j];
            else{
                for(Orb deathOrb : deathOrbs){
                    if(deathOrb.getI() == point.i && deathOrb.getJ() == point.j) orb = deathOrb;
                    break;
                }
            }
            orb.setAnimationEnum(Orb.BubbleAnimationType.IMPLODING);
            burstingOrbs.add(orb);
            if(validCoordinates(point)) orbArray[point.i][point.j] = Orb.NULL;
            else deathOrbs.remove(orb);
        }
        burstingOrbsChanged = true;
        orbArrayChanged = true;
    }
    public void changeDropArrayOrbs(List<PointInt> newDroppingOrbs){
        for(PointInt point : newDroppingOrbs){
            Orb orb = orbArray[point.i][point.j];
            droppingOrbs.add(orb);
            orbArray[point.i][point.j] = Orb.NULL;
        }
        droppingOrbsChanged = true;
        orbArrayChanged = true;
    }
    public void changeAddDeathOrb(Orb orb){
        deathOrbs.add(orb);
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
    public void setAddShootingOrb(Orb newOrb){
        shootingOrbs.add(newOrb);
    }
    public void setAddShootingOrbs(Queue<Orb> newShootingOrbs){
        shootingOrbs.addAll(newShootingOrbs);
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
                if(otherOrb.equals(Orb.NULL)) orbArray[i][j] = Orb.NULL; // note: The host's Orb.NULL is different from our own. This is why we must use .equals instead of == and replace all these instances with a reference to our own Orb.NULL.
                else orbArray[i][j] = new Orb(otherOrb.getOrbEnum(),i,j,otherOrb.getAnimationEnum());
            }
        }
    }
    public void setShotsUntilNewRow(int newVal){
        shotsUntilNewRow = newVal;
    }
    public void setTransferOutOrbs(List<Orb> transferOutOrbs){
        this.transferOutOrbs.clear();
        this.transferOutOrbs.addAll(transferOutOrbs);
    }
    public void setTransferInOrbs(List<Orb> transferInOrbs){
        this.transferInOrbs.clear();
        //this.transferInOrbs.addAll(transferInOrbs); // Using addAll() would be faster and cleaner, but may use more memory because the game would have to keep a reference to the entire PlayPanelData from the host.
        for(Orb orb : transferInOrbs){
            this.transferInOrbs.add(new Orb(orb.getOrbEnum(), orb.getI(), orb.getJ(), orb.getAnimationEnum()));
        }
    }
    public void setAddDeathOrbs(List<Orb> newDeathOrbs){
        this.deathOrbs.addAll(newDeathOrbs);
    }
    public void setVictorious(boolean newVal){
        victorious = newVal;
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
    public List<Orb> getDeathOrbs(){
        return deathOrbs;
    }
    public int getShotsUntilNewRow(){
        return shotsUntilNewRow;
    }
    public boolean getVictorious(){
        return victorious;
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
    }

    // In one game tick, the following happens here:
    //   1. Todo: Array bubble electrification animations are advanced; new electrified bubbles are spawned; the ones that have finished their animations are returned to a normal state.
    //   2. Todo: Dropping bubbles are advanced. The ones that have fallen off the edge of the screen are removed from the droppingBubbles list.
    //   3. Todo: Bursting bubble animations are advanced. The ones that are done are removed from the burstingOrbs list.
    //   4. Todo: Shooter bubbles are advanced
    void tick(){

    }

    public List<PointInt> getNeighbors(PointInt source){
        int i = source.i;
        int j = source.j;
        List<PointInt> neighbors = new LinkedList<>();
        //test all possible neighbors for valid coordinates
        int[] iTests = {i-1, i-1, i, i, i+1, i+1};
        int[] jTests = {j-1, j+1, j-2, j+2, j-1, j+1};
        for(int k=0; k<iTests.length; k++){
            if(validCoordinates(new PointInt(iTests[k],jTests[k])) && orbArray[iTests[k]][jTests[k]]!=Orb.NULL){
                neighbors.add(new PointInt(iTests[k],jTests[k]));
            }
        }
        return neighbors;
    }

    public List<PointInt> depthFirstSearch(PointInt source, FilterOption filter) {

        List<PointInt> matches = new LinkedList<>();

        // A boolean array that has the same size as the orbArray, to mark orbs as "examined"
        boolean examined[][] = new boolean[PlayPanelData.ARRAY_HEIGHT][PlayPanelData.ARRAY_WIDTH_PER_CHARACTER * numPlayers];

        // A stack containing the "active" elements to be examined next
        Deque<PointInt> active = new LinkedList<>();

        // Add the collision's shooter orb to the active list and mark it as "examined"
        active.push(source);
        if(validCoordinates(source)) examined[source.i][source.j] = true; // deathOrbs have "invalid" coordinates.

        // Do a depth-first search
        while (!active.isEmpty()) {
            PointInt activeOrb = active.pop();
            matches.add(new PointInt(activeOrb.i, activeOrb.j));
            List<PointInt> neighbors = getNeighbors(activeOrb);
            for (PointInt neighbor : neighbors) {
                if (!examined[neighbor.i][neighbor.j]) {
                    // apply the filter option
                    boolean passesFilter = false;
                    switch(filter){
                        case ALL:
                            passesFilter = true;
                            break;
                        case SAME_COLOR:
                            Orb sourceOrb = Orb.NULL; // needs to be initialized to satisfy compiler.
                            if(!validCoordinates(source)){
                                for(Orb orb : deathOrbs){
                                    if(orb.getI() == source.i && orb.getJ() == source.j) sourceOrb = orb;
                                    break;
                                }
                            }
                            else sourceOrb = orbArray[source.i][source.j];
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

    public Set<PointInt> findConnectedOrbs(){
        Set<PointInt> connectedOrbs = new HashSet<>();

        // find all orbs connected to the ceiling:
        for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
            PointInt sourcePoint = new PointInt(0, j);
            if(orbArray[0][j]==Orb.NULL) continue;
            connectedOrbs.addAll(depthFirstSearch(sourcePoint, FilterOption.ALL));
        }

        return connectedOrbs;
    }

    // Finds floating orbs and drops them. Also checks for victory conditions
    public List<PointInt> findFloatingOrbs(Set<PointInt> connectedOrbs){
        List<PointInt> orbsToDrop = new LinkedList<>();

        // any remaining orbs in the array should be dropped.
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                if(orbArray[i][j]!=Orb.NULL){ // We only care about locations that have actual Orbs.
                    PointInt orb = new PointInt(i,j);
                    if(!connectedOrbs.contains(orb)){
                        orbsToDrop.add(orb);
                    }
                }
            }
        }
        return orbsToDrop;
    }

    public boolean validCoordinates(PointInt testPoint)
    {
        return (testPoint.i>=0 && testPoint.i< ARRAY_HEIGHT && testPoint.j>=0 && testPoint.j <ARRAY_WIDTH_PER_CHARACTER*numPlayers);
    }

    // Utility function for debugging
    public void printOrbArray(){
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                if(orbArray[i][j]==null) System.out.print('X');
                if(orbArray[i][j]==Orb.NULL) System.out.print(' ');
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
                if (otherArray[i][j].equals(Orb.NULL) && !(orbArray[i][j] == Orb.NULL)){ // note: cannot use == in the first clause because the host has a different instance of Orb.NULL than we do.
                    inconsistent = true;
                    System.out.println("orbArray["+i+"]["+j+"] is null but otherArray[i][j] is not null");
                }
                else if (orbArray[i][j].getOrbEnum() != otherArray[i][j].getOrbEnum()){
                    inconsistent = true;
                    System.out.println("array enums at ["+i+"]["+j+"] do not match");
                }
            }
        }

        // Check that the number of shots remaining until the next row appears is consistent:
        if(other.getShotsUntilNewRow()!=getShotsUntilNewRow()){
            inconsistent = true;
            System.out.println("number of shots until new row appears is inconsistent between host and client");
        }

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
            System.err.println("Client data is inconsistent with the host! overriding orbArray, shotsUntilNewRow, and transferInOrbs...");
            setOrbArray(other.getOrbArray());
            setShotsUntilNewRow(other.getShotsUntilNewRow());
            setTransferInOrbs(other.transferInOrbs);
            inconsistencyCounter = 0;
        }
    }

    private boolean isTransferInOrbOccupyingPosition(int i, int j){
        for(Orb orb : transferInOrbs){
            if(orb.getI()==i && orb.getJ()==j) return true;
        }
        return false;
    }
}
