package Classes.NetworkCommunication;

import Classes.Cannon;
import Classes.Character;
import Classes.Images.CannonType;
import Classes.Animation.CharacterType;
import Classes.Animation.OrbColor;
import Classes.Orb;
import Classes.PlayPanel;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;

import java.io.*;
import java.util.*;

public class Player implements Serializable {
    private static final int NUM_FRAMES_ERROR_TOLERANCE = 5; // the number of frames for which ammunitionOrbs data that is inconsistent with the host is tolerated. After this many frames, the ammunitionOrbs list is overwritten with the host's data.
    public static final long HOST_ID = -1L;
    public static final long GAME_ID = 0L;

    private final long playerID;

    private int playerPos;
    private Point2D ammunitionOrb1Position;
    private Point2D ammunitionOrb2Position;

    private SynchronizedComparable<PlayerType> playerType;
    protected SynchronizedComparable<String> username;
    protected SynchronizedComparable<Integer> team;
    private SynchronizedComparable<State> state;
    private SynchronizedList<Message> messagesOut;
    private SynchronizedList<Orb> firedOrbs;
    private transient long latency;
    private List<Orb> ammunitionOrbs = new LinkedList<>();

    // An enum indicating a special state of player:
    public enum State {NORMAL, DEFEATED, DISCONNECTED, VICTORIOUS}

    // An enum indicating whether the player is local, remote, a bot, etc.
    public enum PlayerType{LOCAL, REMOTE_HOSTVIEW, REMOTE_CLIENTVIEW, BOT, UNCLAIMED}

    protected Character character;
    protected Cannon cannon;

    // The PlayPanel associated with this player (needed for firing new shootingOrbs):
    protected PlayPanel playPanel;

    // Data needed for constructing the shooter Orbs. These values are determined *once* by the host at the start
    // of the game and are never changed. Host and client maintain their own copies separately.
    private int seed;
    private Random ammunitionGenerator;

    // Counter for how many frames the local ammunitionOrbs list has been inconsistent with data from the host:
    private int inconsistencyCounter = 0; // hopefully 0 most of the time!

    private final Synchronizer synchronizer;

    public Player(String username, PlayerType playerType, long id, Synchronizer synchronizer){
        this.playerID = id;
        this.synchronizer = synchronizer;

        CharacterType myCharacterEnum;
        CannonType myCannonType;
        int myTeam;
        switch(playerType){
            case UNCLAIMED:
                // corresponds to an open slot in the LobbyScene
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
            messagesOut = new SynchronizedList<Message>("messagesOut", new LinkedList<>(), SynchronizedData.Precedence.CLIENT, playerID, synchronizer, SynchronizedList.SynchronizationType.SEND_ONCE);
            team = new SynchronizedComparable<>("team",myTeam, SynchronizedData.Precedence.CLIENT,this.playerID,synchronizer);
        }

        this.cannon = new Cannon(myCannonType, this.playerID, synchronizer);
        this.character = new Character(myCharacterEnum, this.playerID, synchronizer);

        synchronized(this.synchronizer){
            state = new SynchronizedComparable<>("state", State.NORMAL,
                    (State newVal, Mode mode, int i, int j)->{
                        switch (newVal){
                            case NORMAL:
                                character.setCharacterAnimationState(CharacterType.CharacterAnimationState.CONTENT);
                                cannon.setCannonAnimationState(CannonType.CannonAnimationState.AIMING);
                                break;
                            case DISCONNECTED:
                                character.setCharacterAnimationState(CharacterType.CharacterAnimationState.DISCONNECTED);
                                cannon.setCannonAnimationState(CannonType.CannonAnimationState.DISCONNECTED);
                                break;
                            case DEFEATED:
                                character.setCharacterAnimationState(CharacterType.CharacterAnimationState.DEFEATED);
                                cannon.setCannonAnimationState(CannonType.CannonAnimationState.DEFEATED);
                                break;
                            case VICTORIOUS:
                                character.setCharacterAnimationState(CharacterType.CharacterAnimationState.VICTORIOUS);
                                cannon.setCannonAnimationState(CannonType.CannonAnimationState.VICTORIOUS);
                                break;
                        }
                    },
                    (State newVal, Mode mode, int i, int j)->{
                        switch (newVal) {
                            case NORMAL:
                                character.setCharacterAnimationState(CharacterType.CharacterAnimationState.CONTENT);
                                cannon.setCannonAnimationState(CannonType.CannonAnimationState.AIMING);
                                break;
                            case DISCONNECTED:
                                character.setCharacterAnimationState(CharacterType.CharacterAnimationState.DISCONNECTED);
                                cannon.setCannonAnimationState(CannonType.CannonAnimationState.DISCONNECTED);
                                break;
                            case DEFEATED:
                                character.setCharacterAnimationState(CharacterType.CharacterAnimationState.DEFEATED);
                                cannon.setCannonAnimationState(CannonType.CannonAnimationState.DEFEATED);
                                break;
                            case VICTORIOUS:
                                character.setCharacterAnimationState(CharacterType.CharacterAnimationState.VICTORIOUS);
                                cannon.setCannonAnimationState(CannonType.CannonAnimationState.VICTORIOUS);
                                break;
                        }
                    },
                    SynchronizedData.Precedence.CLIENT, playerID, synchronizer, 0);
            firedOrbs = new SynchronizedList<>("firedOrbs",new LinkedList<>(),
                    (LinkedList<Orb> newVal, Mode mode, int i, int j)->{
                        synchronized (synchronizer){
                            switch (mode){
                                case ADD:
                                    // Add a new ammunition orb to the end of the list
                                    if(ammunitionOrbs.size()<2){
                                        OrbColor newEnum = playPanel.getNextShooterOrbEnum(ammunitionGenerator.nextDouble());
                                        ammunitionOrbs.add(new Orb(newEnum,0,0, Orb.OrbAnimationState.STATIC));
                                    }
                                    positionAmmunitionOrbs();
                                    // Add the firedOrb to the shootingOrbs list as well, in the PlayPanel
                                    playPanel.setAddShootingOrbs(newVal);
                                    break;
                                case REMOVE:
                                case SET:
                            }
                        }
                    },
                    (LinkedList<Orb> newVal, Mode mode, int i, int j)->{
                        synchronized (synchronizer){
                            switch (mode){
                                case ADD:
                                    // Add a new ammunition orb to the end of the list
                                    if(ammunitionOrbs.size()<2){
                                        OrbColor newEnum = playPanel.getNextShooterOrbEnum(ammunitionGenerator.nextDouble());
                                        ammunitionOrbs.add(new Orb(newEnum,0,0, Orb.OrbAnimationState.STATIC));
                                    }
                                    positionAmmunitionOrbs();
                                    // Add the firedOrb to the shootingOrbs list as well, in the PlayPanel
                                    playPanel.setAddShootingOrbs(newVal);
                                    break;
                                case REMOVE:
                                case SET:
                            }
                        }
                    },
                    SynchronizedData.Precedence.CLIENT,this.playerID,this.synchronizer, SynchronizedList.SynchronizationType.SEND_ONCE, 24);

            // Adjust precedences on the networked data:
            switch(playerType){
                case REMOTE_CLIENTVIEW:
                    this.username.setPrecedence(SynchronizedData.Precedence.HOST);
                    this.team.setPrecedence(SynchronizedData.Precedence.HOST);
                    this.character.getCharacterType().setPrecedence(SynchronizedData.Precedence.HOST);
                    this.cannon.getCannonType().setPrecedence(SynchronizedData.Precedence.HOST);
                    this.cannon.getCannonAngle().setPrecedence(SynchronizedData.Precedence.HOST);
                    this.messagesOut.setPrecedence(SynchronizedData.Precedence.HOST);
                    this.state.setPrecedence(SynchronizedData.Precedence.HOST);
                    this.firedOrbs.setPrecedence(SynchronizedData.Precedence.HOST);
                    break;
            }
        }
    }

    // Intended For LOCAL, BOT, and UNCLAIMED PlayerTypes:
    // create a (probably) unique player ID
    public static long createID(){
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

    public void setAmmunitionOrbPositions(Point2D ammunitionOrb1Position, Point2D ammunitionOrb2Position){
        this.ammunitionOrb1Position = ammunitionOrb1Position;
        this.ammunitionOrb2Position = ammunitionOrb2Position;
    }

    public Synchronizer getSynchronizer(){
        return synchronizer;
    }

    public void changeLatency(long latency){
        this.latency = latency;
        // no change flag is needed for latency
    }

    public void initializePlayerPos(int i){
        this.playerPos = i;
    }

    // set the positions of the 1st and second shooting orbs for this player
    public void positionAmmunitionOrbs(){
        // update the positions of the next 2 ammunition orbs
        ammunitionOrbs.get(0).relocate(ammunitionOrb1Position.getX(), ammunitionOrb1Position.getY());
        ammunitionOrbs.get(1).relocate(ammunitionOrb2Position.getX(), ammunitionOrb2Position.getY());
    }

    /* Setters: These are called when a client simply wants to update locally-stored player information without
     * notifying the host. */
    public void setLatency(long latency){
        this.latency = latency;
    }
    public void setAmmunitionOrbs(List<Orb> ammunitionOrbs){
        this.ammunitionOrbs.clear();
        for(Orb orb : ammunitionOrbs){
            this.ammunitionOrbs.add(new Orb(orb));
        }
    }

    public SynchronizedComparable<String> getUsername(){
        return username;
    }
    public SynchronizedComparable<PlayerType> getPlayerType(){
        return playerType;
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
    public long getPlayerID(){
        return playerID;
    }
    public int getPlayerPos() {
        return playerPos;
    }
    public Character getCharacter(){
        return character;
    }
    public Cannon getCannon(){
        return cannon;
    }
    public long getLatency(){
        return latency;
    }
    public List<Orb> getAmmunition(){
        return ammunitionOrbs;
    }

    public void incrementCharacterEnum(){
        synchronized (synchronizer){
            CharacterType nextType = character.getCharacterType().getData().next();
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
            character.getCharacterType().changeTo(nextType);
        }
    }

    public void incrementCannonEnum(){
        synchronized (synchronizer){
            CannonType nextType = cannon.getCannonType().getData().next();
            while(!nextType.isSelectable()){
                nextType = nextType.next();
            }
            cannon.getCannonType().changeTo(nextType);
        }
    }

    // Points the cannon at a position given in scene coordinates.
    public void pointCannon(double sceneX, double sceneY){
        Point2D localLoc = playPanel.sceneToLocal(sceneX, sceneY);
        double mouseRelativeX = localLoc.getX() - cannon.getPosX();
        double mouseRelativeY = localLoc.getY() - cannon.getPosY(); // recall that the y-axis points down.
        double newAngleRad = Math.atan2(mouseRelativeY,mouseRelativeX);
        double newAngleDeg = newAngleRad*180.0/Math.PI; // 0 degrees points to the right, 90 degrees points straight up.
        pointCannon(newAngleDeg); // updates model and view.
    }

    // Points the cannon at a given angle, in degrees (0 degrees points to the right)
    public void pointCannon(double angle){
        if(state.getData() == State.DEFEATED) return;
        cannon.getCannonAngle().changeTo(angle);
    }

    public void changeFireCannon(){
        synchronized (synchronizer){
            // Check whether this player is allowed to fire:
            State currentState = state.getData();
            if(currentState==State.DEFEATED || currentState==State.VICTORIOUS) return;

            // Remove an orb from ammunitionOrbs and add it to this player's firedOrbs. Also add a new orb to ammunitionOrbs
            Orb firedOrb = ammunitionOrbs.remove(0);
            firedOrb.setRawTimestamp(System.nanoTime());
            firedOrb.setAngle(Math.toRadians(cannon.getCannonAngle().getData()));
            firedOrb.setSpeed(firedOrb.getOrbColor().getOrbSpeed());
            firedOrbs.changeAdd(firedOrb);
        }
    }

    public void relocateCannon(double x, double y){
        cannon.relocate(x,y);
    }
    public void relocateCharacter(double x, double y){
        character.relocate(x,y);
    }
    public void setScale(double scaleFactor){
        cannon.setScale(scaleFactor);
        character.setScale(scaleFactor);
    }

    // Attempt to load Orbs from the file first. If the file doesn't exist or if "RANDOM" is specified or if there are
    // only a few orbs in the file, then add random Orbs to the ammunition until we have 10.
    public void readAmmunitionOrbs(String filename, int seed){
        this.seed = seed;
        if(ammunitionGenerator==null) ammunitionGenerator = new Random(seed);
        List<Orb> ammunitionOrbs = getAmmunition();
        ammunitionOrbs.clear();
        if(!filename.substring(0,6).equals("RANDOM")){
            InputStream stream = getClass().getClassLoader().getResourceAsStream(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            try{
                // skip ahead to where the shooter orbs are:
                while(!(reader.readLine()).equals("***SHOOTER_ORBS***"));

                // skip ahead to the line for this player's positionIndex:
                for(int i=0; i<playerPos; i++){
                    reader.readLine();
                }

                // Add the Orbs specified in the file to the ammunitionOrbs Queue:
                int nextOrbSymbol;
                while((nextOrbSymbol = reader.read())!=-1 && nextOrbSymbol!='\n' && nextOrbSymbol!='\r'){
                    OrbColor orbEnum = OrbColor.lookupOrbImageBySymbol((char)nextOrbSymbol);
                    if(orbEnum==null){
                        System.err.println("Unparseable character \"" + nextOrbSymbol + "\" in ammunitionOrbs file. Skipping that one...");
                        continue;
                    }
                    ammunitionOrbs.add(new Orb(orbEnum,0,0, Orb.OrbAnimationState.STATIC));
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
        // Add random Orbs to the ammunitionOrbs Queue after that:
        while(ammunitionOrbs.size()<2){
            System.out.println();
            int randomOrdinal = ammunitionGenerator.nextInt(OrbColor.values().length);
            OrbColor orbImage = OrbColor.values()[randomOrdinal];
            ammunitionOrbs.add(new Orb(orbImage,0,0, Orb.OrbAnimationState.STATIC)); // Updates model
            // Note: view gets updated 24 times per second in the repaint() method of the PlayPanel.
        }
    }

    public void tick(int lowestRow){
        character.tick(lowestRow);
        cannon.tick();
    }

    public void drawSelf(ImageView characterImageView, ImageView cannonImageView){
        synchronized (synchronizer){ // Due to the incrementCharacterEnum and incrementCannonEnum methods, it is possible that the animationDatas in character and cannon could change while we're trying to access them. Hence, we must synchronize here.
            if(characterImageView!=null) character.drawSelf(characterImageView);
            if(cannonImageView!=null) cannon.drawSelf(cannonImageView);
        }
    }

    public void drawSelf(GraphicsContext graphicsContext){
        // Note: I would call synchronize(synchronizer) here, but this method is only ever called within a synchronized
        // block of PlayPanel.repaint(). Due to Player.incrementCharacterEnum() Player.incrementCannonEnum,
        // Character().setCharacterAnimationState() and Cannon.setCannonAnimationState(), it is possible that
        // the animationDatas in character and cannon could change while we're trying to access them. Hence,
        // we must make sure this method is synchronized.
        character.drawSelf(graphicsContext);
        cannon.drawSelf(graphicsContext);
    }
}
