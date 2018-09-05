package Classes.NetworkCommunication;

import Classes.Images.CannonImages;
import Classes.Animation.CharacterAnimations;
import Classes.Animation.OrbColor;
import Classes.OrbData;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import static Classes.OrbData.ORB_RADIUS;
import static Classes.PlayPanel.CANNON_X_POS;
import static Classes.PlayPanel.CANNON_Y_POS;
import static Classes.PlayPanel.PLAYPANEL_WIDTH_PER_PLAYER;

/**
 * Having separate "changers" and "setters" prevents an undesirable feedback loop that would undo changes.
 */
public class PlayerData implements Serializable {
    private static final int NUM_FRAMES_ERROR_TOLERANCE = 5; // the number of frames for which ammunitionOrbs data that is inconsistent with the host is tolerated. After this many frames, the ammunitionOrbs list is overwritten with the host's data.

    private double cannonAngle;
    private int cannonAnimationFrame;
    private final long playerID;
    private int playerPos; // The position index of this player in his/her playpanel (0 or greater)
    private String username;
    private CharacterAnimations characterEnum;
    private int characterAnimationFrame;
    private CannonImages cannonEnum;
    private int team;
    private boolean defeated;
    private long latency;
    private List<OrbData> ammunitionOrbs = new LinkedList<>();
    private Queue<OrbData> firedOrbs = new LinkedList<>();
    private boolean frozen = false; // Todo: to be superseded with CharacterAnimationState.DEFEAT/DISCONNECTED
    private boolean cannonDisabled = false; // Todo: to be superseded with CharacterAnimationState.DEFEAT/DISCONNECTED
    private CharacterAnimations.CharacterAnimationState characterAnimationState = CharacterAnimations.CharacterAnimationState.CONTENT;

    // Flags indicating changes to playerData:
    private boolean bubbleDataChanged = false;
    private boolean firing = false;
    private boolean usernameChanged = false;
    private boolean characterChanged = false;
    private boolean cannonChanged = false;
    private boolean teamChanged = false; // Also used to indicate a playerslot that is unclaimed (team 0);
    private boolean defeatedChanged = false;
    private boolean ammunitionOrbsChanged = false;
    private boolean frozenChanged = false;
    private boolean cannonDisabledChanged = false;

    // Counter for how many frames the local ammunitionOrbs list has been inconsistent with data from the host:
    private int inconsistencyCounter = 0; // hopefully 0 most of the time!

    // When the server first initializes a PlayerData object, it will only know the player's username and userID. Use
    // default values for everything else.
    public PlayerData(String username, long playerID){
        //BubbleData = new BubbleData();
        cannonAngle = -80.0; // recall that JavaFx rotates things clockwise instead of counterclockwise
        cannonAnimationFrame = 0;
        this.username = username;
        this.playerID = playerID;
        if(playerID == -1){ // playerID =- 1 indicates that the Player is of type UnclaimedPlayer (corresponds to an open slot in the MultiplayerSelectionScene)
            characterEnum = CharacterAnimations.UNKNOWN_CHARACTER;
            cannonEnum = CannonImages.UNKNOWN_CANNON;
            team = 0;
        }
        else{ // Otherwise, assign the player the default character and cannon:
            characterEnum = CharacterAnimations.BLITZ;
            cannonEnum = CannonImages.BASIC_CANNON;
            team = 1;
        }
        characterAnimationFrame = 0;
        defeated = false;
    }

    // Copy constructor
    public PlayerData(PlayerData other){
        cannonAngle = other.getCannonAngle();
        cannonAnimationFrame = other.getCannonAnimationFrame();
        username = other.getUsername();
        playerID = other.getPlayerID();
        playerPos = other.getPlayerPos();
        characterEnum = other.getCharacterEnum();
        characterAnimationFrame = other.getCharacterAnimationFrame();
        cannonEnum = other.getCannonEnum();
        team = other.getTeam();
        defeated = other.getDefeated();
        frozen = other.getFrozen();
        cannonDisabled = other.getCannonDisabled();

        ammunitionOrbs = deepCopyOrbList(other.getAmmunition());
        firedOrbs = deepCopyOrbQueue(other.getFiredOrbs());

        bubbleDataChanged = other.isBubbleDataChanged();
        firing = other.isFiring();
        usernameChanged = other.isUsernameChanged();
        characterChanged = other.isCharacterChanged();
        cannonChanged = other.isCannonChanged();
        teamChanged = other.isTeamChanged();
        defeatedChanged = other.isDefeatedChanged();
        ammunitionOrbsChanged = other.isAmmunitionChanged();
        frozenChanged = other.isFrozenChanged();
        cannonDisabledChanged = other.isCannonDisabledChanged();
    }

    // todo: this method is copied from PlayPanelData. Can I put this in some utility class or make it static?
    public List<OrbData> deepCopyOrbList(List<OrbData> other){
        List<OrbData> copiedList = new LinkedList<>();
        for(OrbData orbData : other){
            copiedList.add(new OrbData(orbData));
        }
        return copiedList;
    }

    public Queue<OrbData> deepCopyOrbQueue(Queue<OrbData> other){
        Queue<OrbData> copiedQueue = new LinkedList<>();
        for(OrbData orbData : other){
            copiedQueue.add(new OrbData(orbData));
        }
        return copiedQueue;
    }

    // Called by the PlayPanel constructor
    public void initializePlayerPos(int playerPos){
        this.playerPos = playerPos;
        positionAmmunitionOrbs();
    }


    /* Changers: These are called when a client wants to notify the host that he/she is actively changing something
     * (e.g. Changing character, team, username, etc). The host will then notify all clients that the data has changed. */
    /*public void changeFiring(boolean firing){
        this.firing = firing;
        bubbleDataChanged = true;
    }*/
    public void changeUsername(String username){
        this.username = username;
        usernameChanged = true;
    }
    public void changeCharacter(CharacterAnimations characterEnum){
        this.characterEnum = characterEnum;
        characterChanged = true;
    }
    public void changeCannon(CannonImages cannonEnum){
        this.cannonEnum = cannonEnum;
        cannonChanged = true;
    }
    public void changeTeam(int team){
        this.team = team;
        teamChanged = true;
    }
    public void changeDefeated(boolean defeated){
        this.defeated = defeated;
        defeatedChanged = true;
    }
    public void changeCannonAngle(double cannonAngle){
        this.cannonAngle = cannonAngle;
        // no change flag is needed for cannon angle
    }
    public void changeLatency(long latency){
        this.latency = latency;
        // no change flag is needed for latency
    }
    public void changeAddAmunitionOrb(OrbData newOrb){
        ammunitionOrbs.add(newOrb);
        ammunitionOrbsChanged = true;
    }
    public OrbData changeFire(double angle, OrbColor newEnum){
        // Remove the first ammunition orb and fire it
        OrbData firedOrb = ammunitionOrbs.remove(0);
        firedOrb.setRawTimestamp(System.nanoTime());
        firedOrb.setAngle(angle);
        firedOrb.setSpeed(firedOrb.getOrbColor().getOrbSpeed());
        firedOrbs.add(firedOrb);
        firing = true;

        // Add a new ammunition orb to the end of the list
        if(ammunitionOrbs.size()<2) ammunitionOrbs.add(new OrbData(newEnum,0,0, OrbData.OrbAnimationState.STATIC)); // Updates model

        positionAmmunitionOrbs();

        return firedOrb;
    }
    public void changeFrozen(boolean newVal){
        frozen = newVal;
        frozenChanged = true;
    }
    public void changeCannonDisabled(boolean newVal){
        cannonDisabled = newVal;
        cannonDisabledChanged = true;
    }

    // set the positions of the 1st and second shooting orbs for this player
    public void positionAmmunitionOrbs(){
        // update the positions of the next 2 ammunition orbs
        ammunitionOrbs.get(0).relocate(ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2 + PLAYPANEL_WIDTH_PER_PLAYER*playerPos, CANNON_Y_POS);
        ammunitionOrbs.get(1).relocate(CANNON_X_POS + getCannonEnum().getAmmunitionRelativeX() + PLAYPANEL_WIDTH_PER_PLAYER*playerPos, CANNON_Y_POS + getCannonEnum().getAmmunitionRelativeY());
    }

    public void changeFiringFlag(boolean firing){
        this.firing = firing;
    }
    public void changeAmmunitionFlag(boolean ammunitionOrbsChanged){
        this.ammunitionOrbsChanged = ammunitionOrbsChanged;
    }

    public void setFire(OrbColor newEnum){
        ammunitionOrbs.remove(0);

        // Add a new ammunition orb to the end of the list
        if(ammunitionOrbs.size()<2) ammunitionOrbs.add(new OrbData(newEnum,0,0, OrbData.OrbAnimationState.STATIC)); // Updates model

        positionAmmunitionOrbs();
    }

    /* Setters: These are called when a client simply wants to update locally-stored player information without
     * notifying the host. */
    public void setUsername(String username){
        this.username = username;
    }
    public void setCharacter(CharacterAnimations characterEnum){
        this.characterEnum = characterEnum;
    }
    public void setCannon(CannonImages cannonEnum){
        this.cannonEnum = cannonEnum;
    }
    public void setTeam(int team){
        this.team = team;
    }
    public void setDefeated(boolean defeated){
        this.defeated = defeated;
    }
    public void setCannonAngle(double cannonAngle){
        this.cannonAngle = cannonAngle;
    }
    public void setLatency(long latency){
        this.latency = latency;
    }
    public void setAmmunitionOrbs(List<OrbData> ammunitionOrbs){
        this.ammunitionOrbs.clear();
        for(OrbData orbData : ammunitionOrbs){
            this.ammunitionOrbs.add(new OrbData(orbData));
        }
    }
    public void setFrozen(boolean newVal){
        frozen = newVal;
    }
    public void setCannonDisabled(boolean newVal){
        cannonDisabled = newVal;
    }

    public void resetFlags(){
        bubbleDataChanged = false;
        firing = false;
        usernameChanged = false;
        characterChanged = false;
        cannonChanged = false;
        teamChanged = false;
        defeatedChanged = false;
        firedOrbs.clear();
        frozenChanged = false;
        cannonDisabledChanged = false;
    }

    /* Change Getters: These are called to see whether the sending party has changed the data. They are always
     * called before retrieving their corresponding, actual player data (with the exception of isFiring(), which returns
     * the actual player data). */
    public boolean isBubbleDataChanged(){
        return bubbleDataChanged;
    }
    public boolean isFiring(){
        return firing;
    }
    public boolean isUsernameChanged(){
        return usernameChanged;
    }
    public boolean isCharacterChanged(){
        return characterChanged;
    }
    public boolean isCannonChanged(){
        return cannonChanged;
    }
    public boolean isTeamChanged(){
        return teamChanged;
    }
    public boolean isDefeatedChanged(){
        return defeatedChanged;
    }
    boolean isAmmunitionChanged(){
        return ammunitionOrbsChanged;
    }
    public boolean isFrozenChanged(){
        return frozenChanged;
    }
    public boolean isCannonDisabledChanged(){
        return cannonDisabledChanged;
    }

    /* Direct Getters: These are called to get the actual player data*/
    public double getCannonAngle(){
        return cannonAngle;
    }
    public int getCannonAnimationFrame(){
        return cannonAnimationFrame;
    }
    public String getUsername(){
        return username;
    }
    public long getPlayerID(){
        return playerID;
    }
    public int getPlayerPos() {
        return playerPos;
    }
    public CharacterAnimations getCharacterEnum(){
        return characterEnum;
    }
    public int getCharacterAnimationFrame(){
        return characterAnimationFrame;
    }
    public CannonImages getCannonEnum(){
        return cannonEnum;
    }
    public int getTeam(){
        return team;
    }
    public boolean getDefeated(){
        return defeated;
    }
    public long getLatency(){
        return latency;
    }
    public List<OrbData> getAmmunition(){
        return ammunitionOrbs;
    }
    public Queue<OrbData> getFiredOrbs(){
        return firedOrbs;
    }
    public boolean getFrozen(){
        return frozen;
    }
    public boolean getCannonDisabled(){
        return cannonDisabled;
    }

    public void checkForConsistency(PlayerData other){
        boolean inconsistent = false;

        // check for consistency in the ammunition Orbs:
        List<OrbData> hostAmmunition = other.getAmmunition();
        if(ammunitionOrbs.size() != hostAmmunition.size()) inconsistent = true;
        else{
            for(int i=0; i<ammunitionOrbs.size(); i++){
                if(ammunitionOrbs.get(i).getOrbColor() != hostAmmunition.get(i).getOrbColor()) inconsistent = true;
            }
        }

        // check for consistency in disabling flags:
        if(frozen != other.getFrozen()) inconsistent = true;
        if(cannonDisabled != other.getCannonDisabled()) inconsistent = true;
        if(defeated != other.getDefeated()) inconsistent = true;

        if(inconsistent) inconsistencyCounter++;
        else inconsistencyCounter = 0;

        if(inconsistencyCounter > NUM_FRAMES_ERROR_TOLERANCE) {
            System.err.println("Client playerData is inconsistent with the host! overriding client's data...");

            // Make the ammunition Orbs consistent:
            setAmmunitionOrbs(hostAmmunition);

            // Make the disabling flags consistent:
            setFrozen(other.getFrozen());
            setCannonDisabled(other.getCannonDisabled());
            setDefeated(other.getDefeated());

            inconsistencyCounter = 0;
        }
    }

    // lowestRow is the index of the lowest Orb on this Player's PlayPanel this frame.
    //    Lowest possible value: -1 if there aren't any Orbs
    //    Highest possible value: ARRAY_HEIGHT , if there's an orb on the deathOrbs list.
    public void tick(int lowestRow, Random miscRandomGenerator){
        // determine which animation state we should be in, and increment animationFrame:
        if(characterAnimationState.inRange(lowestRow)){
            characterAnimationFrame++;
            int[] bounds = characterEnum.getAnimationBounds(characterAnimationState);
            if(bounds[1] == characterAnimationFrame) characterAnimationFrame = bounds[0];
        }
        else{
            for(CharacterAnimations.CharacterAnimationState state : CharacterAnimations.CharacterAnimationState.values()){
                if(state.inRange(lowestRow)){
                    characterAnimationState = state;
                    int[] bounds = characterEnum.getAnimationBounds(characterAnimationState);
                    characterAnimationFrame = miscRandomGenerator.nextInt(bounds[1]-bounds[0]) + bounds[0];
                    //System.out.println("new state: " + state + " animation bounds: " + bounds[0] + ", " + bounds[1]);
                    //System.out.println("new animationFrame: " + animationFrame);
                    break;
                }
            }
        }
        //System.out.println("current animation frame: " + animationFrame);
        //Note to self: DO NOT update the character view here. Remember that we're not in the JavaFX application thread.
    }
}
