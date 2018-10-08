package Classes.NetworkCommunication;

import Classes.CannonData;
import Classes.CharacterData;
import Classes.Images.CannonType;
import Classes.Animation.CharacterType;
import Classes.Animation.OrbColor;
import Classes.OrbData;
import Classes.PlayPanel;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;

import java.io.*;
import java.util.*;

public class PlayerData implements Serializable {
    private static final int NUM_FRAMES_ERROR_TOLERANCE = 5; // the number of frames for which ammunitionOrbs data that is inconsistent with the host is tolerated. After this many frames, the ammunitionOrbs list is overwritten with the host's data.
    public static final long HOST_ID = -1L;
    public static final long GAME_ID = 0L;

    private final long playerID;

    private int playerPos;
    private double ammunitionOrb1X;
    private double ammunitionOrb1Y;
    private double ammunitionOrb2X;
    private double ammunitionOrb2Y;
    //private Point2D ammunitionOrb1Position; // temporarily replaced with ammunitionOrb1X and ammunitionOrb1Y because Point2D is not Serializable
    //private Point2D ammunitionOrb2Position; // temporarily replaced with ammunitionOrb2X and ammunitionOrb2Y because Point2D is not Serializable

    private SynchronizedComparable<PlayerType> playerType;
    protected SynchronizedComparable<String> username;
    protected SynchronizedComparable<Integer> team;
    private SynchronizedComparable<State> state;
    private SynchronizedList<Message> messagesOut;
    private transient long latency;
    private List<OrbData> ammunitionOrbs = new LinkedList<>();
    //private Queue<OrbData> firedOrbs = new LinkedList<>();
    private SynchronizedList<OrbData> firedOrbs;

    // An enum indicating a special state of player:
    public enum State {NORMAL, DEFEATED, DISCONNECTED, VICTORIOUS}

    // An enum indicating whether the player is local, remote, a bot, etc.
    public enum PlayerType{LOCAL, REMOTE_HOSTVIEW, REMOTE_CLIENTVIEW, BOT, UNCLAIMED}

    // Flags indicating changes to playerData:
    private boolean bubbleDataChanged = false;
    private boolean characterChanged = false;
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

    private final Synchronizer synchronizer;

    public PlayerData(String username, PlayerType playerType, long playerID, Synchronizer synchronizer){
        this.playerID = playerID;
        this.synchronizer = synchronizer;

        CharacterType myCharacterEnum;
        CannonType myCannonType;
        int myTeam;
        switch(playerType){
            case UNCLAIMED:
                // corresponds to an open slot in the MultiplayerSelectionScene
                myCharacterEnum = CharacterType.UNKNOWN_CHARACTER;
                myCannonType = CannonType.UNKNOWN_CANNON;
                myTeam = 0;
                break;
            case BOT:
                myCharacterEnum = CharacterType.FILLY_BOT_MEDIUM;
                myCannonType = CannonType.BOT_CANNON;
                myTeam = 1;
                break;
            default:
                // Otherwise, assign the player the default character and cannon:
                myCharacterEnum = CharacterType.BLITZ;
                myCannonType = CannonType.BASIC_CANNON;
                myTeam = 1;
                break;
        }

        synchronized (this.synchronizer){
            this.username = new SynchronizedComparable<>("username", username, SynchronizedData.Precedence.CLIENT, this.playerID, synchronizer);
            this.playerType = new SynchronizedComparable<>("playerType", playerType, SynchronizedData.Precedence.INFORMATIONAL, this.playerID, synchronizer);
            messagesOut = new SynchronizedList<Message>("messagesOut", new LinkedList<>(), SynchronizedData.Precedence.CLIENT, playerID, synchronizer, Integer.MAX_VALUE);
            team = new SynchronizedComparable<>("team",myTeam, SynchronizedData.Precedence.CLIENT,this.playerID,synchronizer);
        }

        this.cannonData = new CannonData(myCannonType, this.playerID, synchronizer);
        this.characterData = new CharacterData(myCharacterEnum, this.playerID, synchronizer);

        synchronized(this.synchronizer){
            state = new SynchronizedComparable<>("state", State.NORMAL,
                    (State newVal, Mode mode, int i, int j)->{
                        switch (newVal){
                            case NORMAL:
                                characterData.setCharacterAnimationState(CharacterType.CharacterAnimationState.CONTENT);
                                cannonData.setCannonAnimationState(CannonType.CannonAnimationState.AIMING);
                                break;
                            case DISCONNECTED:
                                characterData.setCharacterAnimationState(CharacterType.CharacterAnimationState.DISCONNECTED);
                                cannonData.setCannonAnimationState(CannonType.CannonAnimationState.DISCONNECTED);
                                break;
                            case DEFEATED:
                                characterData.setCharacterAnimationState(CharacterType.CharacterAnimationState.DEFEATED);
                                cannonData.setCannonAnimationState(CannonType.CannonAnimationState.DEFEATED);
                                break;
                            case VICTORIOUS:
                                characterData.setCharacterAnimationState(CharacterType.CharacterAnimationState.VICTORIOUS);
                                cannonData.setCannonAnimationState(CannonType.CannonAnimationState.VICTORIOUS);
                                break;
                        }
                    },
                    (State newVal, Mode mode, int i, int j)->{
                        switch (newVal) {
                            case NORMAL:
                                characterData.setCharacterAnimationState(CharacterType.CharacterAnimationState.CONTENT);
                                cannonData.setCannonAnimationState(CannonType.CannonAnimationState.AIMING);
                                break;
                            case DISCONNECTED:
                                characterData.setCharacterAnimationState(CharacterType.CharacterAnimationState.DISCONNECTED);
                                cannonData.setCannonAnimationState(CannonType.CannonAnimationState.DISCONNECTED);
                                break;
                            case DEFEATED:
                                characterData.setCharacterAnimationState(CharacterType.CharacterAnimationState.DEFEATED);
                                cannonData.setCannonAnimationState(CannonType.CannonAnimationState.DEFEATED);
                                break;
                            case VICTORIOUS:
                                characterData.setCharacterAnimationState(CharacterType.CharacterAnimationState.VICTORIOUS);
                                cannonData.setCannonAnimationState(CannonType.CannonAnimationState.VICTORIOUS);
                                break;
                        }
                    },
                    SynchronizedData.Precedence.CLIENT, playerID, synchronizer, 0);
            firedOrbs = new SynchronizedList<>("firedOrbs",new LinkedList<OrbData>(),
                    (LinkedList<OrbData> newVal, Mode mode, int i, int j)->{

                    },
                    (LinkedList<OrbData> newVal, Mode mode, int i, int j)->{

                    },
                    SynchronizedData.Precedence.CLIENT,this.playerID,this.synchronizer, 24);

            // Adjust precedences on the networked data:
            switch(playerType){
                case REMOTE_CLIENTVIEW:
                    this.username.setPrecedence(SynchronizedData.Precedence.HOST);
                    this.team.setPrecedence(SynchronizedData.Precedence.HOST);
                    this.characterData.getCharacterType().setPrecedence(SynchronizedData.Precedence.HOST);
                    this.cannonData.getCannonType().setPrecedence(SynchronizedData.Precedence.HOST);
                    this.cannonData.getCannonAngle().setPrecedence(SynchronizedData.Precedence.HOST);
                    this.messagesOut.setPrecedence(SynchronizedData.Precedence.HOST);
                    this.state.setPrecedence(SynchronizedData.Precedence.HOST);
                    this.firedOrbs.setPrecedence(SynchronizedData.Precedence.HOST);
                    break;
            }
        }
    }

    public PlayerData(String username, PlayerType playerType, Synchronizer synchronizer){
        this(username, playerType, createID(), synchronizer);
    }

    // Copy constructor
    public PlayerData(PlayerData other){
        synchronizer = new Synchronizer();
        playerID = other.playerID;
        username = new SynchronizedComparable<>("username", other.username.getData(), other.username.getPrecedence(), playerID, synchronizer);
        team = new SynchronizedComparable<>("team",other.team.getData(), other.team.getPrecedence(), playerID, synchronizer);
        playerType = new SynchronizedComparable<>("playerType", other.playerType.getData(), other.playerType.getPrecedence(), playerID, synchronizer);
        messagesOut = new SynchronizedList<Message>("messagesOut", new LinkedList<>(), other.getMessagesOut().getPrecedence(), playerID, synchronizer, Integer.MAX_VALUE);
        state = new SynchronizedComparable<>("resigned", other.state.getData(), other.state.getPrecedence(), playerID, synchronizer);
        firedOrbs = new SynchronizedList<>("firedOrbs", new LinkedList<OrbData>(), other.firedOrbs.getPrecedence(), playerID, synchronizer);
        playerPos = other.playerPos;
        ammunitionOrb1X = other.ammunitionOrb1X;
        ammunitionOrb1Y = other.ammunitionOrb1Y;
        ammunitionOrb2X = other.ammunitionOrb2X;
        ammunitionOrb2Y = other.ammunitionOrb2Y;
        //ammunitionOrb1Position = other.ammunitionOrb1Position;
        //ammunitionOrb2Position = other.ammunitionOrb2Position;

        ammunitionOrbs = deepCopyOrbList(other.getAmmunition());

        bubbleDataChanged = other.isBubbleDataChanged();
        characterChanged = other.isCharacterChanged();
        defeatedChanged = other.isDefeatedChanged();
        ammunitionOrbsChanged = other.isAmmunitionChanged();
        frozenChanged = other.isFrozenChanged();
        cannonDisabledChanged = other.isCannonDisabledChanged();

        characterData = new CharacterData(other.characterData, other.playerID, synchronizer);
        cannonData = new CannonData(other.getCannonData(), other.playerID, synchronizer);

    }

    // For LOCAL and BOT PlayerTypes:
    // create a (probably) unique player ID
    private static long createID(){
        long playerID;
        do{
            playerID = (new Random()).nextLong();
            if(playerID>0) playerID = -playerID;
        } while (playerID == HOST_ID || playerID == GAME_ID);
        return playerID;
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

    public void setAmmunitionOrbPositions(Point2D ammunitionOrb1Position, Point2D ammunitionOrb2Position){
        this.ammunitionOrb1X = ammunitionOrb1Position.getX();
        this.ammunitionOrb1Y = ammunitionOrb1Position.getY();
        this.ammunitionOrb2X = ammunitionOrb2Position.getX();
        this.ammunitionOrb2Y = ammunitionOrb2Position.getY();
        //this.ammunitionOrb1Position = ammunitionOrb1Position;
        //this.ammunitionOrb2Position = ammunitionOrb2Position;
    }

    public Synchronizer getSynchronizer(){
        return synchronizer;
    }

    /* Changers: These are called when a client wants to notify the host that he/she is actively changing something
     * (e.g. Changing character, team, username, etc). The host will then notify all clients that the data has changed. */

    public void changeLatency(long latency){
        this.latency = latency;
        // no change flag is needed for latency
    }
    public void changeAddAmunitionOrb(OrbData newOrb){
        ammunitionOrbs.add(newOrb);
        ammunitionOrbsChanged = true;
    }
    public OrbData changeFire(double angle, OrbColor newEnum){
        synchronized (synchronizer){
            // Remove the first ammunition orb and fire it
            OrbData firedOrb = ammunitionOrbs.remove(0);
            firedOrb.setRawTimestamp(System.nanoTime());
            firedOrb.setAngle(angle);
            firedOrb.setSpeed(firedOrb.getOrbColor().getOrbSpeed());
            firedOrbs.changeAdd(firedOrb);

            // Add a new ammunition orb to the end of the list
            if(ammunitionOrbs.size()<2) ammunitionOrbs.add(new OrbData(newEnum,0,0, OrbData.OrbAnimationState.STATIC)); // Updates model

            changeAmmunitionFlag(true);

            positionAmmunitionOrbs();
            return firedOrb;
        }
    }

    public void initializePlayerPos(int i){
        this.playerPos = i;
    }

    // set the positions of the 1st and second shooting orbs for this player
    public void positionAmmunitionOrbs(){
        // update the positions of the next 2 ammunition orbs
        ammunitionOrbs.get(0).relocate(ammunitionOrb1X, ammunitionOrb1Y);
        ammunitionOrbs.get(1).relocate(ammunitionOrb2X, ammunitionOrb2Y);
        //ammunitionOrbs.get(0).relocate(ammunitionOrb1Position.getX(), ammunitionOrb1Position.getY());
        //ammunitionOrbs.get(1).relocate(ammunitionOrb2Position.getX(), ammunitionOrb2Position.getY());
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
    /*public void setTeam(int team){
        this.team = team;
    }*/
    public void setLatency(long latency){
        this.latency = latency;
    }
    public void setAmmunitionOrbs(List<OrbData> ammunitionOrbs){
        this.ammunitionOrbs.clear();
        for(OrbData orbData : ammunitionOrbs){
            this.ammunitionOrbs.add(new OrbData(orbData));
        }
    }

    public void resetFlags(){
        bubbleDataChanged = false;
        characterChanged = false;
        defeatedChanged = false;
        frozenChanged = false;
        cannonDisabledChanged = false;
    }

    /* Change Getters: These are called to see whether the sending party has changed the data. They are always
     * called before retrieving their corresponding, actual player data (with the exception of isFiring(), which returns
     * the actual player data). */
    public boolean isBubbleDataChanged(){
        return bubbleDataChanged;
    }
    public boolean isCharacterChanged(){
        return characterChanged;
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
    public SynchronizedComparable<String> getUsername(){
        return username;
    }
    public long getPlayerID(){
        return playerID;
    }
    public SynchronizedComparable<PlayerType> getPlayerType(){
        return playerType;
    }
    public int getPlayerPos() {
        return playerPos;
    }
    public CharacterData getCharacterData(){
        return characterData;
    }
    public SynchronizedComparable<Integer> getTeam(){
        return team;
    }
    public SynchronizedList<Message> getMessagesOut(){
        return messagesOut;
    }
    public SynchronizedComparable<State>  getState(){
        return state;
    }
    public long getLatency(){
        return latency;
    }
    public List<OrbData> getAmmunition(){
        return ammunitionOrbs;
    }
    public SynchronizedList<OrbData> getFiredOrbs(){
        return firedOrbs;
    }
    public CannonData getCannonData(){
        return cannonData;
    }
    public PlayPanel getPlayPanel(){
        return playPanel;
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

        if(inconsistent) inconsistencyCounter++;
        else inconsistencyCounter = 0;

        if(inconsistencyCounter > NUM_FRAMES_ERROR_TOLERANCE) {
            System.err.println("Client playerData is inconsistent with the host! overriding client's data...");

            // Make the ammunition Orbs consistent:
            setAmmunitionOrbs(hostAmmunition);

            inconsistencyCounter = 0;
        }
    }













    /* More methods from the old Player class*/


    public void incrementCharacterEnum(){
        synchronized (synchronizer){
            CharacterType nextType = characterData.getCharacterType().getData().next();
            if (playerType.getData() == PlayerType.LOCAL){
                while(!nextType.isPlayable()){
                    nextType = nextType.next();
                }
            }
            else if(playerType.getData() == PlayerType.BOT){
                while(nextType.getBotDifficulty()==null){
                    nextType = nextType.next();
                }
                username.changeTo("fillyBot [" + nextType.getBotDifficulty() +"]");
            }
            else{ // this should never happen, but just in case...
                System.err.println("The user has attempted to change a characterType they down not own.");
                return;
            }
            characterData.getCharacterType().changeTo(nextType);
        }
    }

    public void incrementCannonEnum(){
        synchronized (synchronizer){
            CannonType nextType = cannonData.getCannonType().getData().next();
            while(!nextType.isSelectable()){
                nextType = nextType.next();
            }
            cannonData.getCannonType().changeTo(nextType);
        }
    }

    // Points the cannon at a position given in scene coordinates.
    public void pointCannon(double sceneX, double sceneY){
        Point2D localLoc = playPanel.sceneToLocal(sceneX, sceneY);
        double mouseRelativeX = localLoc.getX() - cannonData.getPosX();
        double mouseRelativeY = localLoc.getY() - cannonData.getPosY(); // recall that the y-axis points down.
        double newAngleRad = Math.atan2(mouseRelativeY,mouseRelativeX);
        double newAngleDeg = newAngleRad*180.0/Math.PI; // 0 degrees points to the right, 90 degrees points straight up.
        pointCannon(newAngleDeg); // updates model and view.
    }

    // Points the cannon at a given angle, in degrees (0 degrees points to the right)
    public void pointCannon(double angle){
        if(state.getData() == State.DEFEATED) return;
        cannonData.getCannonAngle().changeTo(angle);
    }

    private void setFireCannon(){
        // Check whether this player is allowed to fire:
        State currentState = state.getData();
        if(currentState==State.DEFEATED || currentState==State.VICTORIOUS) return;
        OrbColor newShooterOrbEnum = playPanel.getNextShooterOrbEnum(ammunitionGenerator.nextDouble());
        setFire(newShooterOrbEnum);
        // View is updated in the PlayPanel repaint() method, which paints the first two ammunitionOrbs on the canvas.
        // Note: The PlayPanel model was already updated via the updatePlayer() method in the PlayPanel class.
    }

    public void changeFireCannon(){
        // Check whether this player is allowed to fire:
        State currentState = state.getData();
        if(currentState==State.DEFEATED || currentState==State.VICTORIOUS) return;

        // Remove an orb from ammunitionOrbs and add it to this player's firedOrbs. Also add a new orb to ammunitionOrbs
        OrbColor newShooterOrbEnum = playPanel.getNextShooterOrbEnum(ammunitionGenerator.nextDouble());
        OrbData firedOrb = changeFire(cannonData.getCannonAngle().getData()*(Math.PI/180), newShooterOrbEnum);

        // Add the firedOrb to the shootingOrbs list as well, in the PlayPanel
        Queue<OrbData> firedOrbList = new LinkedList<>();
        firedOrbList.add(firedOrb);
        playPanel.changeAddShootingOrbs(firedOrbList); // updates PlayPanel model
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
    }


    // Called every frame by a client to update a player's data according to a packet received over the network. The
    // data is *not* updated if the locally-stored flags indicate that the player has manually changed something
    // since the last packet was sent. This is to prevent the host from overwriting the client's change request with
    // old data.
    public void updateWithSetters(PlayerData newPlayerData, PlayerType playerType){
        boolean isLocalPlayer = playerType==PlayerType.LOCAL;

        // remote players' cannon angles are always updated:
        /*if(!isLocalPlayer){
            cannonData.setAngle(newPlayerData.getCannonData().getCannonAngle());
        }*/

        // players' latencies are always updated:
        setLatency(newPlayerData.getLatency()); // updates model

        // check for consistency between this player's ammunitionOrbs and the host's data for ammunitionOrbs. If they
        // are different for too long, then override the client's data with the host's data.
        checkForConsistency(newPlayerData);

    }

    // Attempt to load Orbs from the file first. If the file doesn't exist or if "RANDOM" is specified or if there are
    // only a few orbs in the file, then add random Orbs to the ammunition until we have 10.
    public void readAmmunitionOrbs(String filename, int seed){
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
                for(int i=0; i<playerPos; i++){
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
        synchronized (synchronizer){ // Due to the incrementCharacterEnum and incrementCannonEnum methods, it is possible that the animationDatas in characterData and cannonData could change while we're trying to access them. Hence, we must synchronize here.
            if(characterImageView!=null) characterData.drawSelf(characterImageView);
            if(cannonImageView!=null) cannonData.drawSelf(cannonImageView);
        }
    }

    //todo: implement this
    public void drawSelf(GraphicsContext graphicsContext){
        // Note: I would call synchronize(synchronizer) here, but this method is only ever called within a synchronized
        // block of PlayPanel.repaint(). Due to PlayerData.incrementCharacterEnum() PlayerData.incrementCannonEnum,
        // CharacterData().setCharacterAnimationState() and CannonData.setCannonAnimationState(), it is possible that
        // the animationDatas in characterData and cannonData could change while we're trying to access them. Hence,
        // we must make sure this method is synchronized.
        characterData.drawSelf(graphicsContext);
        cannonData.drawSelf(graphicsContext);
    }
}
