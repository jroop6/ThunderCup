package Classes.NetworkCommunication;

import Classes.Images.CannonImages;
import Classes.Images.CharacterImages;
import Classes.Images.OrbImages;
import Classes.Orb;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static Classes.Orb.ORB_RADIUS;
import static Classes.PlayPanel.CANNON_X_POS;
import static Classes.PlayPanel.CANNON_Y_POS;
import static Classes.PlayPanel.PLAYPANEL_WIDTH_PER_PLAYER;

/**
 * Having separate "changers" and "setters" prevents an undesirable feedback loop that would undo changes.
 */
public class PlayerData implements Serializable {
    private static final int NUM_FRAMES_ERROR_TOLERANCE = 5; // the number of frames for which ammunitionOrbs data that is inconsistent with the host is tolerated. After this many frames, the ammunitionOrbs list is overwritten with the host's data.

    private double cannonAngle;
    private final long playerID;
    private int playerPos; // The position of this player in his/her playpanel (0 or greater)
    private String username;
    private CharacterImages characterEnum;
    private CannonImages cannonEnum;
    private int team;
    private boolean defeated;
    private long latency;
    private List<Orb> ammunitionOrbs = new LinkedList<>();
    private Queue<Orb> firedOrbs = new LinkedList<>();

    // Flags indicating changes to playerData:
    private boolean bubbleDataChanged = false;
    private boolean firing = false;
    private boolean usernameChanged = false;
    private boolean characterChanged = false;
    private boolean cannonChanged = false;
    private boolean teamChanged = false; // Also used to indicate a playerslot that is unclaimed (team 0);
    private boolean defeatedChanged = false;
    private boolean ammunitionOrbsChanged = false;

    // Counter for how many frames the local ammunitionOrbs list has been inconsistent with data from the host:
    private int inconsistencyCounter = 0; // hopefully 0 most of the time!

    // When the server first initializes a PlayerData object, it will only know the player's username and userID. Use
    // default values for everything else.
    public PlayerData(String username, long playerID){
        //BubbleData = new BubbleData();
        cannonAngle = 20.0;
        this.username = username;
        this.playerID = playerID;
        if(playerID == -1){ // playerID =- 1 indicates that the Player is of type UnclaimedPlayer (corresponds to an open slot in the MultiplayerSelectionScene)
            characterEnum = CharacterImages.UNKNOWN_CHARACTER;
            cannonEnum = CannonImages.UNKNOWN_CANNON;
            team = 0;
        }
        else{ // Otherwise, assign the player the default character and cannon:
            characterEnum = CharacterImages.PINK_FILLY;
            cannonEnum = CannonImages.STANDARD_CANNON;
            team = 1;
        }
        defeated = false;
    }

    // Copy constructor
    public PlayerData(PlayerData other){
        cannonAngle = other.getCannonAngle();
        username = other.getUsername();
        playerID = other.getPlayerID();
        playerPos = other.getPlayerPos();
        characterEnum = other.getCharacterEnum();
        cannonEnum = other.getCannonEnum();
        team = other.getTeam();
        defeated = other.getDefeated();
        ammunitionOrbs.addAll(other.getAmmunition());
        firedOrbs.addAll(other.getFiredOrbs());

        bubbleDataChanged = other.isBubbleDataChanged();
        firing = other.isFiring();
        usernameChanged = other.isUsernameChanged();
        characterChanged = other.isCharacterChanged();
        cannonChanged = other.isCannonChanged();
        teamChanged = other.isTeamChanged();
        defeatedChanged = other.isDefeatedChanged();
        ammunitionOrbsChanged = other.isAmmunitionChanged();
    }

    // Called by the PlayPanel constructor
    public void initializePlayerPos(int playerPos){
        this.playerPos = playerPos;

        // set the positions of the 1st and second shooting orbs for this player
        getAmmunition().get(0).setXPos(ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2 + PLAYPANEL_WIDTH_PER_PLAYER*playerPos);
        ammunitionOrbs.get(0).setYPos(CANNON_Y_POS);
        ammunitionOrbs.get(1).setXPos(CANNON_X_POS + getCannonEnum().getAmmunitionRelativeX() + PLAYPANEL_WIDTH_PER_PLAYER*playerPos);
        ammunitionOrbs.get(1).setYPos(CANNON_Y_POS + getCannonEnum().getAmmunitionRelativeY());
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
    public void changeCharacter(CharacterImages characterEnum){
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
    public void changeAddAmunitionOrb(Orb newOrb){
        ammunitionOrbs.add(newOrb);
        ammunitionOrbsChanged = true;
    }
    public Orb changeFire(double angle, int newOrdinal){
        // Remove the first ammunition orb and fire it
        Orb firedOrb = ammunitionOrbs.remove(0);
        firedOrb.setRawTimestamp(System.nanoTime());
        firedOrb.setAngle(angle);
        firedOrb.setSpeed(firedOrb.getOrbEnum().getOrbSpeed());
        firedOrbs.add(firedOrb);
        firing = true;

        // update the positions of the next 2 ammunition orbs
        ammunitionOrbs.get(0).setXPos(ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2 + PLAYPANEL_WIDTH_PER_PLAYER*playerPos);
        ammunitionOrbs.get(0).setYPos(CANNON_Y_POS);
        ammunitionOrbs.get(1).setXPos(CANNON_X_POS + getCannonEnum().getAmmunitionRelativeX() + PLAYPANEL_WIDTH_PER_PLAYER*playerPos);
        ammunitionOrbs.get(1).setYPos(CANNON_Y_POS + getCannonEnum().getAmmunitionRelativeY());

        // Add a new ammunition orb to the end of the list
        OrbImages orbImage = OrbImages.values()[newOrdinal];
        ammunitionOrbs.add(new Orb(orbImage,0,0,Orb.BubbleAnimationType.STATIC)); // Updates model

        return firedOrb;
    }

    public void changeFiringFlag(boolean firing){
        this.firing = firing;
    }
    public void changeAmmunitionFlag(boolean ammunitionOrbsChanged){
        this.ammunitionOrbsChanged = ammunitionOrbsChanged;
    }

    public void setFire(int newOrdinal){
        ammunitionOrbs.remove(0);

        // update the positions of the next 2 ammunition orbs
        ammunitionOrbs.get(0).setXPos(ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2 + PLAYPANEL_WIDTH_PER_PLAYER*playerPos);
        ammunitionOrbs.get(0).setYPos(CANNON_Y_POS);
        ammunitionOrbs.get(1).setXPos(CANNON_X_POS + getCannonEnum().getAmmunitionRelativeX() + PLAYPANEL_WIDTH_PER_PLAYER*playerPos);
        ammunitionOrbs.get(1).setYPos(CANNON_Y_POS + getCannonEnum().getAmmunitionRelativeY());

        // Add a new ammunition orb to the end of the list
        OrbImages orbImage = OrbImages.values()[newOrdinal];
        ammunitionOrbs.add(new Orb(orbImage,0,0,Orb.BubbleAnimationType.STATIC)); // Updates model
    }

    /* Setters: These are called when a client simply wants to update locally-stored player information without
     * notifying the host. */
    public void setUsername(String username){
        this.username = username;
    }
    public void setCharacter(CharacterImages characterEnum){
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
    public void setAmmunitionOrbs(List<Orb> ammunitionOrbs){
        this.ammunitionOrbs.clear();
        this.ammunitionOrbs.addAll(ammunitionOrbs);
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

    /* Direct Getters: These are called to get the actual player data*/
    public double getCannonAngle(){
        return cannonAngle;
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
    public CharacterImages getCharacterEnum(){
        return characterEnum;
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
    public List<Orb> getAmmunition(){
        return ammunitionOrbs;
    }
    public Queue<Orb> getFiredOrbs(){
        return firedOrbs;
    }

    public void checkForConsistency(PlayerData other){
        List<Orb> hostAmmunition = other.getAmmunition();
        boolean inconsistent = false;
        for(int i=0; i<ammunitionOrbs.size(); i++){
            if(ammunitionOrbs.get(i).getOrbEnum() != hostAmmunition.get(i).getOrbEnum()) inconsistent = true;
        }
        if(inconsistent) inconsistencyCounter++;
        else inconsistencyCounter = 0;

        if(inconsistencyCounter > NUM_FRAMES_ERROR_TOLERANCE) {
            System.err.println("Client ammunitionOrbs list is inconsistent with the host! overriding ammunitionOrbs...");
            setAmmunitionOrbs(hostAmmunition);
            inconsistencyCounter = 0;
        }
    }
}
