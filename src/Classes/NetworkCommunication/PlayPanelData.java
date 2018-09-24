package Classes.NetworkCommunication;

import Classes.Animation.AnimationData;
import Classes.Animation.OrbColor;
import Classes.OrbData;
import Classes.PlayPanelUtility;
import Classes.PointInt;

import java.io.*;
import java.util.*;

import static Classes.OrbData.NULL;

/**
 * Contains Orb data relevant to a particular PlayPanel.
 * Note: ammunition orbs are considered specific to a player, and are therefore kept in PlayerData.
 */
public class PlayPanelData implements Serializable {
    public static final int ARRAY_HEIGHT = 20;
    public static final int ARRAY_WIDTH_PER_CHARACTER = 30;
    private static final int NUM_FRAMES_ERROR_TOLERANCE = 24; // the number of frames for which orbArray data that is inconsistent with the host is tolerated. After this many frames, the orbArray is overwritten with the host's data.
    public static final int SHOTS_BETWEEN_DROPS = 15*ARRAY_WIDTH_PER_CHARACTER; // After the player shoots this many times, a new row of orbs appears at the top.

    private Random randomPuzzleGenerator;
    private PlayPanelUtility playPanelUtility = new PlayPanelUtility();

    private final int team;
    private final int numPlayers;
    private int shotsUntilNewRow;
    private OrbData orbArray[][];
    private OrbData deathOrbs[]; // orbs below the line of death. If these are not immediately cleared in 1 frame, then this team has lost.
    private List<OrbData> shootingOrbs = new LinkedList<>();
    private List<OrbData> burstingOrbs = new LinkedList<>();
    private List<OrbData> droppingOrbs = new LinkedList<>();
    private List<OrbData> transferOutOrbs = new LinkedList<>(); // orbs to be transferred to other players
    private Set<OrbData> transferInOrbs = new HashSet<>(); // orbs to be transferred from other players
    private List<OrbData> thunderOrbs = new LinkedList<>(); // orbs that have dropped off the PlayPanel explode in thunder.
    private List<AnimationData> visualFlourishes = new LinkedList<>();

    //Todo: make deathOrbs an array with width=PLAYPANLE_WIDTH_PER_PLAYER*numPlayers, for easier lookup by (i,j) coordinates
    private boolean victorious = false;

    // cumulative data for this PlayPanel, for end-of-game statistics:
    private int cumulativeShotsFired = 0; // involving all players of this PlayPanel.
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

        orbArray = new OrbData[ARRAY_HEIGHT][ARRAY_WIDTH_PER_CHARACTER*numPlayers];
        initializeOrbArray(puzzleUrl);

        deathOrbs = new OrbData[ARRAY_WIDTH_PER_CHARACTER*numPlayers];
        Arrays.fill(deathOrbs,NULL);

        shotsUntilNewRow = SHOTS_BETWEEN_DROPS*numPlayers;
    }

    // The Copy Constructor is used to make a copy of all the data before sending it out over the network.
    public PlayPanelData(PlayPanelData other){
        team = other.getTeam();
        numPlayers = other.getNumPlayers();
        shotsUntilNewRow = other.getShotsUntilNewRow();

        orbArray = playPanelUtility.deepCopyOrbArray(other.getOrbArray());

        //todo: don't copy NULL. Just set the reference to NULL.
        deathOrbs = new OrbData[ARRAY_WIDTH_PER_CHARACTER*numPlayers];
        OrbData[] otherDeathOrbs = other.getDeathOrbs();
        // System.arraycopy(other.getDeathOrbs(),0,deathOrbs,0,deathOrbs.length); // Unfortunately, System.arraycopy doesn't work because it only copies references to the orbs. Hence, the orbs might get modified before they are sent over the socket connection.
        for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
            deathOrbs[j] = new OrbData(otherDeathOrbs[j]);
        }

        shootingOrbs = playPanelUtility.deepCopyOrbList(other.getShootingOrbs());
        burstingOrbs = playPanelUtility.deepCopyOrbList(other.getBurstingOrbs());
        droppingOrbs = playPanelUtility.deepCopyOrbList(other.getDroppingOrbs());
        transferInOrbs = playPanelUtility.deepCopyOrbSet(other.getTransferInOrbs());
        transferOutOrbs = playPanelUtility.deepCopyOrbList(other.getTransferOutOrbs());
        victorious = other.getVictorious();

        orbArrayChanged = other.isOrbArrayChanged();
        shootingOrbsChanged = other.isShootingOrbsChanged();
        burstingOrbsChanged = other.isBurstingOrbsChanged();
        droppingOrbsChanged = other.isDroppingOrbsChanged();
        transferInOrbsChanged = other.isTransferInOrbsChanged();
        transferOutOrbsChanged = other.isTransferOutOrbsChanged();
        victoriousChanged = other.isVictoriousChanged();
        deathOrbsChanged = other.isDeathOrbsChanged();

        visualFlourishes = playPanelUtility.deepCopyVisualFlourishesList(other.getVisualFlourishes());
    }

    /* Changers: These are called when the host wants to notify the client that something has changed in the official
     * data (e.g. Orbs burst, dropped or fired). */

    /* Specialized Changers */
    public void changeAddShootingOrbs(Queue<OrbData> newShootingOrbs){
        shootingOrbs.addAll(newShootingOrbs);
        cumulativeShotsFired +=newShootingOrbs.size();
        shootingOrbsChanged = true;
        shotsFiredChanged = true;
    }
    public void changeAddTransferOutOrbs(List<OrbData> newTransferOrbs){
        for(OrbData orbData : newTransferOrbs){
            orbData.setOrbAnimationState(OrbData.OrbAnimationState.TRANSFERRING);
        }
        transferOutOrbs.addAll(newTransferOrbs);
        cumulativeOrbsTransferred += newTransferOrbs.size();
        transferOutOrbsChanged = true;
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
    public void setAddShootingOrbs(Queue<OrbData> newShootingOrbs){
        shootingOrbs.addAll(newShootingOrbs);
        cumulativeShotsFired +=newShootingOrbs.size();
    }
    public void setAddDroppingOrb(OrbData newOrb){
        droppingOrbs.add(newOrb);
    }
    public void setAddThunderOrbs(List<OrbData> newThunderOrbs){
        for(OrbData orbData : newThunderOrbs){
            orbData.setOrbAnimationState(OrbData.OrbAnimationState.THUNDERING);
        }
        thunderOrbs.addAll(newThunderOrbs);
    }
    public void setOrbArray(OrbData[][] newOrbArray){
        for (int i=0; i<ARRAY_HEIGHT; i++){
            //System.arraycopy(newOrbArray[i],0,orbArray[i],0,orbArray[i].length); // System.arraycopy is faster, but it might cost more memory because it causes the game to retain a reference to the packet from the host. I'm not sure which option is better... Either way, I'd have to replace all the NULL orbs with references to the local NULL orb anyways.
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                OrbData otherOrbData = newOrbArray[i][j];
                if(otherOrbData.equals(NULL)) orbArray[i][j] = NULL; // note: The host's Orb.NULL is different from our own. This is why we must use .equals instead of == and replace all these instances with a reference to our own Orb.NULL.
                else orbArray[i][j] = new OrbData(otherOrbData);
            }
        }
    }
    public void setDeathOrbs(OrbData[] newDeathOrbs){
        for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
            OrbData otherOrbData = newDeathOrbs[j];
            if(otherOrbData.equals(NULL)) deathOrbs[j] = NULL; // note: The host's Orb.NULL is different from our own. This is why we must use .equals instead of == and replace all these instances with a reference to our own Orb.NULL.
            else deathOrbs[j] = new OrbData(otherOrbData);
        }
    }
    public void setShotsUntilNewRow(int newVal){
        shotsUntilNewRow = newVal;
    }
    public <E extends Collection<OrbData>> void setOrbCollection(E collectionToOverwrite, E collectionToCopy){
        System.out.println("setting an orb collection");
        collectionToOverwrite.clear();
        // Using addAll() would be faster and cleaner, but may use more memory because the game would have to keep a reference to the entire PlayPanelData from the host.
        for(OrbData orbData : collectionToCopy) collectionToOverwrite.add(new OrbData(orbData));
    }
    public void setAddDeathOrbs(List<OrbData> newDeathOrbs){
        for(OrbData newDeathOrb : newDeathOrbs){
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
    public OrbData[][] getOrbArray(){
        return orbArray;
    }
    public List<OrbData> getShootingOrbs(){
        return shootingOrbs;
    }
    public List<OrbData> getBurstingOrbs(){
        return burstingOrbs;
    }
    public List<OrbData> getDroppingOrbs(){
        return droppingOrbs;
    }
    public List<OrbData> getTransferOutOrbs(){
        return transferOutOrbs;
    }
    public Set<OrbData> getTransferInOrbs(){
        return transferInOrbs;
    }
    public List<OrbData> getThunderOrbs(){
        return thunderOrbs;
    }
    public OrbData[] getDeathOrbs(){
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
    public List<AnimationData> getVisualFlourishes(){
        return visualFlourishes;
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

    // Utility function for debugging
    public void printOrbArray(){
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                if(orbArray[i][j]==null) System.out.print('X');
                if(orbArray[i][j]==NULL) System.out.print(' ');
                else System.out.print(orbArray[i][j].getOrbColor().ordinal());
            }
            System.out.print('\n');
        }
    }

    public void checkForConsistency(PlayPanelData other){
        // Check that the orbArray is consistent between the two sets of data:
        boolean inconsistent = false;
        OrbData[][] otherArray = other.getOrbArray();
        for(int i=0; i<ARRAY_HEIGHT; i++) {
            for (int j=0; j < ARRAY_WIDTH_PER_CHARACTER * numPlayers; j++) {
                if (otherArray[i][j].equals(NULL) && !(orbArray[i][j] == NULL)){ // note: cannot use == in the first clause because the host has a different instance of Orb.NULL than we do.
                    inconsistent = true;
                }
                else if (orbArray[i][j].getOrbColor() != otherArray[i][j].getOrbColor()){
                    inconsistent = true;
                }
            }
        }

        // Check that the deathOrbs array is consistent between the two sets of data:
        OrbData[] otherDeathOrbs = other.getDeathOrbs();
        for (int j=0; j < ARRAY_WIDTH_PER_CHARACTER * numPlayers; j++) {
            if (otherDeathOrbs[j].equals(NULL) && !(deathOrbs[j] == NULL)){ // note: cannot use == in the first clause because the host has a different instance of Orb.NULL than we do.
                inconsistent = true;
            }
            else if (otherDeathOrbs[j].getOrbColor() != deathOrbs[j].getOrbColor()){
                inconsistent = true;
            }
        }

        // Check that the number of shots remaining until the next row appears is consistent:
        if(other.getShotsUntilNewRow()!=getShotsUntilNewRow()){
            inconsistent = true;
            System.out.println("number of shots until new row appears is inconsistent between host and client");
        }

        // Check that the number of shooting Orbs are consistent:
        if(other.getShootingOrbs().size() != shootingOrbs.size()) inconsistent = true;

        // Check that the transferInOrbs list is consistent:
        if(!other.getTransferInOrbs().equals(transferInOrbs)){
            inconsistent = true;
            System.out.println("transferInOrbs are detected as inconsistent");
        }

        if(inconsistent) inconsistencyCounter++;
        else inconsistencyCounter = 0;

        if(inconsistencyCounter > NUM_FRAMES_ERROR_TOLERANCE){
            System.err.println("Client data is inconsistent with the host! overriding client data...");
            // Override the important data:
            setOrbArray(other.getOrbArray());
            setDeathOrbs(other.getDeathOrbs());
            setShotsUntilNewRow(other.getShotsUntilNewRow());
            setOrbCollection(shootingOrbs, other.getShootingOrbs());
            setOrbCollection(transferInOrbs, other.getTransferInOrbs());

            // Less important data:
            setCumulativeShotsFired(other.getCumulativeShotsFired());
            setCumulativeOrbsBurst(other.getCumulativeOrbsBurst());
            setCumulativeOrbsDropped(other.getCumulativeOrbsDropped());
            setCumulativeOrbsTransferred(other.getCumulativeOrbsTransferred());
            setLargestGroupExplosion(other.getLargestGroupExplosion());

            inconsistencyCounter = 0;
        }
    }

    public boolean isDeathOrbsEmpty(){
        for (OrbData orbData : deathOrbs){
            if(orbData != NULL) return false;
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
            int orbEnumBound = OrbColor.values().length;
            OrbColor[] orbImages = OrbColor.values();

            for(int i=0; i<rows; ++i){
                for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                    if(j%2==i%2){
                        int randomOrdinal = randomPuzzleGenerator.nextInt(orbEnumBound);
                        OrbColor orbImage = orbImages[randomOrdinal];
                        orbArray[i][j] = new OrbData(orbImage,i,j, OrbData.OrbAnimationState.STATIC);
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
                    //System.out.println("line is " + line);
                    for (j=0; j<line.length() && j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                        char orbSymbol = line.charAt(j);
                        OrbColor orbEnum = OrbColor.lookupOrbImageBySymbol(orbSymbol);
                        if(orbEnum==null) orbArray[i][j] = NULL;
                        else orbArray[i][j] = new OrbData(orbEnum,i,j, OrbData.OrbAnimationState.STATIC);
                    }
                    // if the input line was too short to fill the entire puzzle line, fill in the rest of the line with NULL orbs:
                    for(/*j is already set*/; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                        orbArray[i][j] = NULL;
                    }
                }
                // fill the rest of the orb array with NULL orbs:
                for(/*i is already set*/; i<ARRAY_HEIGHT; i++){
                    for(j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
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

        // todo: use System.arrayCopy
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
        for(OrbData transferOrb : transferInOrbs){
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
        int orbEnumBound = OrbColor.values().length;
        OrbColor[] orbImages = OrbColor.values();
        for(int j=0; j<orbArray[i].length; j++){
            if(j%2==newRowOffset){
                int randomOrdinal = randomPuzzleGenerator.nextInt(orbEnumBound);
                OrbColor orbImage = orbImages[randomOrdinal];
                orbArray[i][j] = new OrbData(orbImage,i,j, OrbData.OrbAnimationState.STATIC);
            }
            else orbArray[i][j] = NULL;
        }
    }

    public int getLowestOccupiedRow(OrbData[][] orbArray, OrbData[] deathOrbs) {
        for(OrbData orbData : deathOrbs){
            if (orbData != NULL) return ARRAY_HEIGHT;
        }

        for (int i = ARRAY_HEIGHT-1; i>=0; i--) {
            OrbData[] row = orbArray[i];
            for (int j = ARRAY_WIDTH_PER_CHARACTER * numPlayers - 1; j >= 0; j--) {
                if (row[j] != NULL) {
                    return i;
                }
            }
        }

        // otherwise, there are no orbs on the board, so return -1
        return -1;
    }
}
