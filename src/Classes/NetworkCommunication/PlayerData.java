package Classes.NetworkCommunication;

import Classes.CannonData;
import Classes.CharacterData;
import Classes.Images.CannonType;
import Classes.Animation.CharacterType;
import Classes.Animation.OrbColor;
import Classes.OrbData;
import Classes.PlayPanel;
import Classes.PlayerTypes.LocalPlayer;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;

import java.io.*;
import java.util.*;

import static Classes.OrbData.ORB_RADIUS;
import static Classes.PlayPanel.CANNON_X_POS;
import static Classes.PlayPanel.CANNON_Y_POS;
import static Classes.PlayPanel.PLAYPANEL_WIDTH_PER_PLAYER;

/**
 * Having separate "changers" and "setters" prevents an undesirable feedback loop in network communications that would undo changes.
 */
public class PlayerData implements Serializable {
    private static final int NUM_FRAMES_ERROR_TOLERANCE = 5; // the number of frames for which ammunitionOrbs data that is inconsistent with the host is tolerated. After this many frames, the ammunitionOrbs list is overwritten with the host's data.
    public static final long HOST_ID = -1L;
    public static final long UNCLAIMED_PLAYER_ID = -2L;
    public static final long GAME_ID = 0L;

    private final long playerID;
    private int playerPos; // The position index of this player in his/her playpanel (0 or greater)
    private boolean frozen = false; // Todo: to be superseded with CharacterAnimationState.DEFEAT/DISCONNECTED

    //private String username;
    private SynchronizedComparable<String> username;
    private int team;
    private long latency;
    private List<OrbData> ammunitionOrbs = new LinkedList<>();
    private Queue<OrbData> firedOrbs = new LinkedList<>();
    private boolean defeated; // Todo: to be superseded with CharacterAnimationState.DEFEAT/DISCONNECTED
    private boolean cannonDisabled = false; // Todo: to be superseded with CharacterAnimationState.DEFEAT/DISCONNECTED

    // Flags indicating changes to playerData:
    private boolean bubbleDataChanged = false;
    private boolean firing = false;
    //private boolean usernameChanged = false;
    private boolean characterChanged = false;
    private boolean cannonChanged = false;
    private boolean teamChanged = false; // Also used to indicate a playerslot that is unclaimed (team 0);
    private boolean defeatedChanged = false;
    private boolean ammunitionOrbsChanged = false;
    private boolean frozenChanged = false;
    private boolean cannonDisabledChanged = false;

    protected CharacterData characterData;
    protected CannonData cannonData;

    // The PlayPanel associated with this player (needed for firing new shootingOrbs):
    protected PlayPanel playPanel;

    // Data needed for constructing the shooter Orbs. These values are determined *once* by the host at the start
    // of the game and are never changed. Host and client maintain their own copies separately.
    private int seed;
    private Random ammunitionGenerator;

    // Counter for how many frames the local ammunitionOrbs list has been inconsistent with data from the host:
    private int inconsistencyCounter = 0; // hopefully 0 most of the time!

    private Synchronizer synchronizer;

    // When the server first initializes a PlayerData object, it will only know the player's username and userID. Use
    // default values for everything else.
    public PlayerData(String username, long playerID, Synchronizer synchronizer){
        //BubbleData = new BubbleData();
        //this.username = username;
        this.username = new SynchronizedComparable<>("username", username, SynchronizedData.Precedence.CLIENT, playerID, synchronizer);
        this.playerID = playerID;
        this.synchronizer = synchronizer;
        CharacterType characterEnum;
        CannonType cannonType;
        if(playerID == UNCLAIMED_PLAYER_ID){ // corresponds to an open slot in the MultiplayerSelectionScene
            characterEnum = CharacterType.UNKNOWN_CHARACTER;
            cannonType = CannonType.UNKNOWN_CANNON;
            team = 0;
        }
        else{ // Otherwise, assign the player the default character and cannon:
            characterEnum = CharacterType.BLITZ;
            cannonType = CannonType.BASIC_CANNON;
            team = 1;
        }
        defeated = false;

        this.cannonData = new CannonData(cannonType);
        this.characterData = new CharacterData(characterEnum, playerID);
    }

    // Copy constructor
    public PlayerData(PlayerData other){
        //username = other.getUsername();
        playerID = other.getPlayerID();
        playerPos = other.getPlayerPos();
        team = other.getTeam();
        defeated = other.getDefeated();
        frozen = other.getFrozen();
        cannonDisabled = other.getCannonDisabled();

        ammunitionOrbs = deepCopyOrbList(other.getAmmunition());
        firedOrbs = deepCopyOrbQueue(other.getFiredOrbs());

        bubbleDataChanged = other.isBubbleDataChanged();
        firing = other.isFiring();
        //usernameChanged = other.isUsernameChanged();
        characterChanged = other.isCharacterChanged();
        cannonChanged = other.isCannonChanged();
        teamChanged = other.isTeamChanged();
        defeatedChanged = other.isDefeatedChanged();
        ammunitionOrbsChanged = other.isAmmunitionChanged();
        frozenChanged = other.isFrozenChanged();
        cannonDisabledChanged = other.isCannonDisabledChanged();

        characterData = new CharacterData(other.getCharacterData(), playerID);
        cannonData = new CannonData(other.getCannonData());
    }

    /* Concrete methods from old Player class: */
    public void registerToPlayPanel(PlayPanel playPanel){
        this.playPanel = playPanel;
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

    public Synchronizer getSynchronizer(){
        return synchronizer;
    }

    /* Changers: These are called when a client wants to notify the host that he/she is actively changing something
     * (e.g. Changing character, team, username, etc). The host will then notify all clients that the data has changed. */
    /*public void changeFiring(boolean firing){
        this.firing = firing;
        bubbleDataChanged = true;
    }*/
    /*public void changeUsername(String username){
        this.username = username;
        usernameChanged = true;
    }*/
    public void changeCharacter(CharacterType characterEnum){
        characterData.setCharacterType(characterEnum);
        characterChanged = true;
    }
    public void changeCannon(CannonType cannonType){
        cannonData.setCannonType(cannonType);
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
    public void changeCannonAngle(double cannonAngle) {
        cannonData.setAngle(cannonAngle);
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
        ammunitionOrbs.get(1).relocate(CANNON_X_POS + getCannonType().getAmmunitionRelativeX() + PLAYPANEL_WIDTH_PER_PLAYER*playerPos, CANNON_Y_POS + getCannonType().getAmmunitionRelativeY());
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
    /*public void setUsername(String username){
        this.username = username;
    }*/
    public void setTeam(int team){
        this.team = team;
    }
    public void setDefeated(boolean defeated){
        this.defeated = defeated;
    }
    public void setCannonAngle(double cannonAngle){
        cannonData.setAngle(cannonAngle);
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
        //usernameChanged = false;
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
    /*public boolean isUsernameChanged(){
        return usernameChanged;
    }*/
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
        return cannonData.getAngle();
    }
    public SynchronizedComparable<String> getUsername(){
        return username;
    }
    public long getPlayerID(){
        return playerID;
    }
    public int getPlayerPos() {
        return playerPos;
    }
    private CharacterData getCharacterData(){
        return characterData;
    }
    public CannonType getCannonType(){
        return cannonData.getCannonType();
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
    public CannonData getCannonData(){
        return cannonData;
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













    /* More methods from the old Player class*/


    public void incrementCharacterEnum(){
        CharacterType nextType = characterData.getCharacterType().next();
        if (this instanceof LocalPlayer){
            while(!nextType.isPlayable()){
                nextType = nextType.next();
            }
        }
        else{ // player must be an instance of BotPlayer
            while(nextType.getBotDifficulty()==null){
                nextType = nextType.next();
            }
            //changeUsername("fillyBot [" + nextType.getBotDifficulty() +"]");
        }
        changeCharacter(nextType);
        characterData.setCharacterType(nextType);
    }
    public void incrementCannonEnum(){
        CannonType nextType = getCannonType().next();
        while(!nextType.isSelectable()){
            nextType = nextType.next();
        }
        changeCannon(nextType); // updates model
        cannonData.setCannonType(nextType); // updates view
    }

    public void changeResignPlayer(){
        changeDefeated(true); // updates model
        changeFrozen(true); // updates model
        changeCannonDisabled(true); // updates model
        // freezePlayer(); // updates view (GameScene) // This ought to be done in updateView(), to prevent a race condition.
        // in the MultiplayerSceneSelection, the view is updated in either the deleteRemovedPlayer() or
        // processPacketAsClient() methods, for host and clients respectively.
    }

    public void changeDisableCannon(){
        changeCannonDisabled(true); // updates model
        // There is no change to the view, other than the fact that the player is no longer able to fire.
    }

    public void freezePlayer(){
        characterData.freeze(); // updates view (GameScene)
        cannonData.freeze();// updates view (GameScene)
    }

    // Points the cannon at a position given in scene coordinates.
    public void pointCannon(double sceneX, double sceneY){
        if(getDefeated()) return;
        Point2D localLoc = playPanel.sceneToLocal(sceneX, sceneY);
        double mouseRelativeX = localLoc.getX() - cannonData.getPosX();
        double mouseRelativeY = localLoc.getY() - cannonData.getPosY(); // recall that the y-axis points down.
        double newAngleRad = Math.atan2(mouseRelativeY,mouseRelativeX);
        double newAngleDeg = newAngleRad*180.0/Math.PI; // 0 degrees points to the right, 90 degrees points straight up.
        pointCannon(newAngleDeg); // updates model and view.
    }

    // Points the cannon at a given angle, in degrees (0 degrees points to the right)
    public void pointCannon(double angle){
        if(getDefeated()) return;
        changeCannonAngle(angle); // updates model
    }

    private void setFireCannon(){
        if(getDefeated()) return;
        OrbColor newShooterOrbEnum = playPanel.getNextShooterOrbEnum(ammunitionGenerator.nextDouble());
        setFire(newShooterOrbEnum); // updates Player model
        // View is updated in the PlayPanel repaint() method, which paints the first two ammunitionOrbs on the canvas.
        // Note: The PlayPanel model was already updated via the updatePlayer() method in the PlayPanel class.
    }

    public void changeFireCannon(){
        if(getCannonDisabled()) return;
        OrbColor newShooterOrbEnum = playPanel.getNextShooterOrbEnum(ammunitionGenerator.nextDouble());
        OrbData firedOrb = changeFire(cannonData.getAngle()*(Math.PI/180), newShooterOrbEnum); // updates Player model
        Queue<OrbData> firedOrbList = new LinkedList<>();
        firedOrbList.add(firedOrb);
        playPanel.getPlayPanelData().changeAddShootingOrbs(firedOrbList); // updates PlayPanel model
        // View is updated in the PlayPanel repaint() method, which paints the first two ammunitionOrbs on the canvas.
    }

    public void relocateCannon(double x, double y){
        cannonData.relocate(x,y);
    }
    public void relocateCharacter(double x, double y){
        characterData.relocate(x,y);
    }
    public void setScale(double scaleFactor){
        cannonData.setScale(scaleFactor);
        characterData.setScale(scaleFactor);
    }

    // Called every frame by the host to update a player's data according to a packet received over the network
    public void updateWithChangers(PlayerData newPlayerData, Map<Long, Long> latencies){
        if(newPlayerData.isFiring()){
            getFiredOrbs().addAll(newPlayerData.getFiredOrbs()); // updates model
            changeFiringFlag(true); // marks data as updated
            for(int i=0; i<newPlayerData.getFiredOrbs().size(); i++) setFireCannon(); // updates model
            changeAmmunitionFlag(true); // marks data as updated
            // note: view will be updated in the PlayPanel's repaint() method.
        }
        if(newPlayerData.isCannonChanged()){
            changeCannon(newPlayerData.getCannonType()); // updates model
        }
        if(newPlayerData.isCharacterChanged()){
            changeCharacter(newPlayerData.getCharacterData().getCharacterType()); //updates model
        }
        if(newPlayerData.isDefeatedChanged()){
            changeDefeated(newPlayerData.getDefeated()); //updates model
            // Note: In the MultiplayerSceneSelection, this player's corresponding PlayerSlot will be removed in either
            // the deleteRemovedPlayer() or processPacketAsClient() methods, for host and clients respectively.
            // In the GameScene, the player should also have been frozen. Updating the frozen state updates the view.
        }
        if(newPlayerData.isFrozenChanged()){
            changeFrozen(newPlayerData.getFrozen()); // updates model
        }
        if(newPlayerData.isTeamChanged()){
            changeTeam(newPlayerData.getTeam()); // updates model
        }
        /*if(newPlayerData.isUsernameChanged()){
            changeUsername(newPlayerData.getUsername()); // updates model
        }*/

        changeCannonAngle(newPlayerData.getCannonAngle()); //updates model

    }


    // Called every frame by a client to update a player's data according to a packet received over the network. The
    // data is *not* updated if the locally-stored flags indicate that the player has manually changed something
    // since the last packet was sent. This is to prevent the host from overwriting the client's change request with
    // old data.
    public void updateWithSetters(PlayerData newPlayerData, boolean isLocalPlayer){
        if(newPlayerData.isFiring() && !isLocalPlayer){
            System.out.println("CLIENT: Another player has fired. Incrementing their ammunitionOrbs");
            for (int i=0; i<newPlayerData.getFiredOrbs().size(); i++) setFireCannon();
            // note: view will be updated in the PlayPanel's repaint() method.
        }
        if(newPlayerData.isCannonChanged() && !isCannonChanged()){
            cannonData.setCannonType(newPlayerData.getCannonType());
        }
        if(newPlayerData.isCharacterChanged() && !isCharacterChanged()){
            characterData.setCharacterType(newPlayerData.getCharacterData().getCharacterType());
        }
        if(newPlayerData.isDefeatedChanged() && !isDefeatedChanged()){
            setDefeated(newPlayerData.getDefeated()); //updates model
        }
        if(newPlayerData.isFrozenChanged() && !isFrozenChanged()){
            setFrozen(newPlayerData.getFrozen()); // updates model
        }
        if(newPlayerData.isTeamChanged() && !isTeamChanged()){
            setTeam(newPlayerData.getTeam()); // updates model
        }
        /*if(newPlayerData.isUsernameChanged() && !isUsernameChanged()){
            setUsername(newPlayerData.getUsername()); // updates model
        }*/

        // remote players' cannon angles are always updated:
        if(!isLocalPlayer){
            setCannonAngle(newPlayerData.getCannonAngle()); //updates model
        }

        // players' latencies are always updated:
        setLatency(newPlayerData.getLatency()); // updates model

        // check for consistency between this player's ammunitionOrbs and the host's data for ammunitionOrbs. If they
        // are different for too long, then override the client's data with the host's data.
        checkForConsistency(newPlayerData);

    }

    // Attempt to load Orbs from the file first. If the file doesn't exist or if "RANDOM" is specified or if there are
    // only a few orbs in the file, then add random Orbs to the ammunition until we have 10.
    public void readAmmunitionOrbs(String filename, int seed, int positionIndex){
        this.seed = seed;
        if(ammunitionGenerator==null) ammunitionGenerator = new Random(seed);
        List<OrbData> ammunitionOrbs = getAmmunition();
        ammunitionOrbs.clear();
        if(!filename.substring(0,6).equals("RANDOM")){
            InputStream stream = getClass().getClassLoader().getResourceAsStream(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            try{
                // skip ahead to where the shooter orbs are:
                String line;
                while(!(line = reader.readLine()).equals("***SHOOTER_ORBS***"));

                // skip ahead to the line for this player's positionIndex:
                for(int i=0; i<positionIndex; i++){
                    reader.readLine();
                }

                // Add the Orbs specified in the file to the ammunitionOrbs Queue:
                int nextOrbSymbol;
                int temp = 0;
                while((nextOrbSymbol = reader.read())!=-1 && nextOrbSymbol!='\n' && nextOrbSymbol!='\r'){
                    OrbColor orbEnum = OrbColor.lookupOrbImageBySymbol((char)nextOrbSymbol);
                    if(orbEnum==null){
                        System.err.println("Unparseable character \"" + nextOrbSymbol + "\" in ammunitionOrbs file. Skipping that one...");
                        continue;
                    }
                    ammunitionOrbs.add(new OrbData(orbEnum,0,0, OrbData.OrbAnimationState.STATIC)); // Updates model
                    temp++;
                    // Note: view gets updated 24 times per second in the repaint() method of the PlayPanel.
                }
                // System.out.println("read " + temp + "orbs from file");
            } catch(IOException e){
                e.printStackTrace();
            }
        }
        // Add random Orbs to the ammunitionOrbs Queue after that:
        while(ammunitionOrbs.size()<2){
            System.out.println();
            int randomOrdinal = ammunitionGenerator.nextInt(OrbColor.values().length);
            OrbColor orbImage = OrbColor.values()[randomOrdinal];
            ammunitionOrbs.add(new OrbData(orbImage,0,0, OrbData.OrbAnimationState.STATIC)); // Updates model
            // Note: view gets updated 24 times per second in the repaint() method of the PlayPanel.
        }
    }

    //todo: implement this
    public void tick(int lowestRow){
        characterData.tick(lowestRow);
        //cannonData.tick();
    }

    //todo: implement this
    public void drawSelf(ImageView characterImageView, ImageView cannonImageView){
        if(isFrozenChanged()){
            freezePlayer(); // updates view
        }
        if(characterImageView!=null) characterData.drawSelf(characterImageView);
        if(cannonImageView!=null) cannonData.drawSelf(cannonImageView);

    }

    //todo: implement this
    public void drawSelf(GraphicsContext graphicsContext){
        if(isFrozenChanged()){
            freezePlayer(); // updates view
        }
        characterData.drawSelf(graphicsContext);
        cannonData.drawSelf(graphicsContext);
    }
}
