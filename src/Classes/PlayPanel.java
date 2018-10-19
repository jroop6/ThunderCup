package Classes;

import Classes.Animation.*;
import Classes.Audio.SoundEffect;
import Classes.Images.DrawingName;
import Classes.NetworkCommunication.*;
import Classes.PlayerTypes.BotPlayer;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.io.*;
import java.util.*;
import java.util.List;

import static Classes.GameScene.DATA_FRAME_RATE;
import static Classes.Orb.*;
import static java.lang.Math.PI;

/**
 *
 */
public class PlayPanel extends Pane implements Serializable {
    // Constants determining PlayPanel layout:
    public static final double ORB_RADIUS = 23.0;
    public static final double PLAYPANEL_WIDTH_PER_PLAYER = 690;
    public static final double PLAYPANEL_HEIGHT = 1080;
    public static final double CANNON_X_POS = ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2.0; // The x-position of the cannon's axis of rotation in a 1-player playpanel.
    public static final double CANNON_Y_POS = 975; // The y-position of the cannon's axis of rotation in a 1-player playpanel.
    private static final double[] VIBRATION_FREQUENCIES = {15.2, 10.7, 5.3}; // cycles per second
    private static final double[] VIBRATION_MAGNITUDES = {2.5, 1.5, 1}; // How much the array orbs vibrate before they drop 1 level.

    // Cached constants
    public static final double ROW_HEIGHT = Math.sqrt(Math.pow(2* ORB_RADIUS,2) - Math.pow(ORB_RADIUS,2)); // Vertical distance from one Orb row to the next.
    public static final double FOUR_R_SQUARED = 4 * ORB_RADIUS * ORB_RADIUS;

    // Constants affecting data structure and game logic
    public static final int ARRAY_HEIGHT = 20; // The number of orb rows
    public static final int ARRAY_WIDTH_PER_CHARACTER = 30; // The number of orb columns per player
    private static final int NUM_FRAMES_ERROR_TOLERANCE = 24; // The number of frames for which orbArray data that is inconsistent with the host is tolerated. After this many frames, the orbArray is overwritten with the host's data.
    public static final int SHOTS_BETWEEN_DROPS = 15*ARRAY_WIDTH_PER_CHARACTER; // After the player shoots this many times, a new row of orbs appears at the top.
    private static final double ELECTRIFICATION_PROBABILITY = .004;

    // JavaFX nodes
    private Rectangle liveBoundary;
    private Canvas orbCanvas;
    private GraphicsContext orbDrawer;

    // PlayPanel data
    private final int team;
    private final int arrayWidth;
    private final int seed;
    private SynchronizedComparable<TeamState> teamState;
    private final List<Player> players;
    private int shotsUntilNewRow;
    //private Orb orbArray[][];
    private SynchronizedArray<Orb> orbArray;
    private Orb deathOrbs[]; // orbs below the line of death. If these are not immediately cleared in 1 frame, then this team has lost.
    private List<Orb> shootingOrbs = new LinkedList<>();
    private List<Orb> burstingOrbs = new LinkedList<>();
    private List<Orb> droppingOrbs = new LinkedList<>();
    private List<Orb> transferOutOrbs = new LinkedList<>(); // orbs to be transferred to other players
    private Set<Orb> transferInOrbs = new HashSet<>(); // orbs to be transferred from other players
    private List<Orb> thunderOrbs = new LinkedList<>(); // orbs that have dropped off the PlayPanel explode in thunder.
    private List<Animation> visualFlourishes = new LinkedList<>();

    public enum TeamState {DEFEATED, VICTORIOUS, NORMAL}

    private final Synchronizer synchronizer;

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

    // Counter for how many frames the local orbArray data has been inconsistent with data from the host:
    private int inconsistencyCounter = 0; // hopefully 0 most of the time!

    // Options available for depth-first search
    public enum FilterOption {ALL, SAME_COLOR}

    // For generating the puzzle and ammunition, and determining where transfer orbs appear:
    private Random randomPuzzleGenerator;
    private Random randomTransferOrbGenerator;
    private Random miscRandomGenerator = new Random();
    private String puzzleUrl;

    PlayPanel(int team, List<Player> players, int seed, String puzzleUrl, Synchronizer synchronizer, LocationType locationType){
        // The size of the PlayPanel is determined by the liveBoundary rectangle.
        arrayWidth = ARRAY_WIDTH_PER_CHARACTER*players.size();
        liveBoundary = new Rectangle(PLAYPANEL_WIDTH_PER_PLAYER*players.size() + ORB_RADIUS, PLAYPANEL_HEIGHT, Color.TRANSPARENT);
        getChildren().add(liveBoundary);
        this.synchronizer = synchronizer;
        this.seed = seed;

        // The Orbs are placed on a Canvas. Initialize the Canvas:
        orbCanvas = new Canvas();
        orbCanvas.setWidth(liveBoundary.getWidth());
        orbCanvas.setHeight(PLAYPANEL_HEIGHT);
        orbDrawer = orbCanvas.getGraphicsContext2D();
        getChildren().add(orbCanvas);

        // Add and initialize players to the PlayPanel:
        DrawingName foregroundCloudsEnum = locationType.getForegroundCloudsEnum();
        DrawingName dropCloudEnum = locationType.getDropCloudEnum();
        for(int i=0; i<players.size(); ++i){
            Player player = players.get(i);

            // register the new player, add his/her cannon and character to the UI, and add other background image components:
            player.registerToPlayPanel(this);
            player.relocateCannon(CANNON_X_POS + PLAYPANEL_WIDTH_PER_PLAYER*i, CANNON_Y_POS);
            player.relocateCharacter(player.getCannon().getCannonType().getData().getCharacterX() + (PLAYPANEL_WIDTH_PER_PLAYER)*(i) + ORB_RADIUS,
                    player.getCannon().getCannonType().getData().getCharacterY());
            ImageView foregroundClouds = foregroundCloudsEnum.getImageView();
            foregroundClouds.relocate(PLAYPANEL_WIDTH_PER_PLAYER*i,PLAYPANEL_HEIGHT-foregroundCloudsEnum.getHeight());
            getChildren().add(foregroundClouds);

            // Scale the character and cannon:
            player.setScale(1.0);

            // initialize the positions of the 1st two shooting orbs:
            player.initializePlayerPos(i);
            player.readAmmunitionOrbs(puzzleUrl, seed);
            Point2D ammunitionOrb1Pos = new Point2D(ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2 + PLAYPANEL_WIDTH_PER_PLAYER*i, CANNON_Y_POS);
            Point2D ammunitionOrb2Pos = new Point2D(CANNON_X_POS + player.getCannon().getCannonType().getData().getAmmunitionRelativeX() + PLAYPANEL_WIDTH_PER_PLAYER*i, CANNON_Y_POS + player.getCannon().getCannonType().getData().getAmmunitionRelativeY());
            player.setAmmunitionOrbPositions(ammunitionOrb1Pos, ammunitionOrb2Pos);
            player.positionAmmunitionOrbs();
        }

        this.team = team;
        this.players = players;
        randomTransferOrbGenerator = new Random(seed);
        this.randomPuzzleGenerator = new Random(seed);
        this.puzzleUrl = puzzleUrl;
        this.teamState = new SynchronizedComparable<>("teamState", TeamState.NORMAL,
                (TeamState newVal, Mode mode, int i, int j) ->{
                    Player.PlayerStatus newPlayerStatus;
                    switch(newVal){
                        case VICTORIOUS:
                            newPlayerStatus = Player.PlayerStatus.VICTORIOUS;
                            break;
                        case DEFEATED:
                            newPlayerStatus = Player.PlayerStatus.DEFEATED;
                            break;
                        default:
                            newPlayerStatus = Player.PlayerStatus.NORMAL;
                            break;
                    }
                    for(Player player : players){
                        player.getPlayerStatus().setTo(newPlayerStatus);
                    }
                },
                (TeamState newVal, Mode mode, int i, int j) ->{
                    Player.PlayerStatus newPlayerStatus;
                    switch(newVal){
                        case VICTORIOUS:
                            newPlayerStatus = Player.PlayerStatus.VICTORIOUS;
                            break;
                        case DEFEATED:
                            newPlayerStatus = Player.PlayerStatus.DEFEATED;
                            break;
                        default:
                            newPlayerStatus = Player.PlayerStatus.NORMAL;
                            break;
                    }
                    for(Player player : players){
                        player.getPlayerStatus().setTo(newPlayerStatus);
                    }
                },
                SynchronizedData.Precedence.HOST,team,synchronizer,0);

        // If this playpanel contains the localPlayer and we're not the host, change precedence to CLIENT:
        for(Player player : players){
            if(player.getPlayerType().getData()== Player.PlayerType.LOCAL && player.getPlayerID()!= Player.HOST_ID){
                teamState.setPrecedence(SynchronizedData.Precedence.CLIENT);
            }
        }

        //orbArray = new Orb[ARRAY_HEIGHT][arrayWidth];
        orbArray = new SynchronizedArray<>("orbArray",new Orb[ARRAY_HEIGHT][arrayWidth], SynchronizedData.Precedence.HOST, team, synchronizer);
        initializeOrbArray(this.puzzleUrl);

        deathOrbs = new Orb[arrayWidth];
        Arrays.fill(deathOrbs,NULL);

        shotsUntilNewRow = SHOTS_BETWEEN_DROPS*players.size();
    }

    /*PlayPanel(PlayPanel other){
        this.synchronizer = new Synchronizer(other.synchronizer.getId());
        team = other.team;
        players = new LinkedList<>();
        shotsUntilNewRow = other.shotsUntilNewRow;
        this.arrayWidth = other.players.size()*ARRAY_WIDTH_PER_CHARACTER;
        this.seed = other.seed;
        this.teamState = new SynchronizedComparable<>("teamState", TeamState.NORMAL, other.teamState.getPrecedence(),other.team,synchronizer);

        orbArray = deepCopyOrbArray(other.orbArray);

        //todo: don't copy NULL. Just set the reference to NULL.
        deathOrbs = new Orb[ARRAY_WIDTH_PER_CHARACTER*players.size()];
        Orb[] otherDeathOrbs = other.deathOrbs;
        // System.arraycopy(other.getDeathOrbs(),0,deathOrbs,0,deathOrbs.length); // Unfortunately, System.arraycopy doesn't work because it only copies references to the orbs. Hence, the orbs might get modified before they are sent over the socket connection.
        for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*players.size(); j++){
            deathOrbs[j] = new Orb(otherDeathOrbs[j]);
        }

        shootingOrbs = deepCopyOrbList(other.shootingOrbs);
        burstingOrbs = deepCopyOrbList(other.burstingOrbs);
        droppingOrbs = deepCopyOrbList(other.droppingOrbs);
        transferInOrbs = deepCopyOrbSet(other.transferInOrbs);
        transferOutOrbs = deepCopyOrbList(other.transferOutOrbs);

        orbArrayChanged = other.orbArrayChanged;
        shootingOrbsChanged = other.shootingOrbsChanged;
        burstingOrbsChanged = other.burstingOrbsChanged;
        droppingOrbsChanged = other.droppingOrbsChanged;
        transferInOrbsChanged = other.transferInOrbsChanged;
        transferOutOrbsChanged = other.transferOutOrbsChanged;
        deathOrbsChanged = other.deathOrbsChanged;

        visualFlourishes = deepCopyVisualFlourishesList(other.visualFlourishes);
    }*/

    /* Specialized Changers */
    public void changeAddTransferOutOrbs(List<Orb> newTransferOrbs){
        for(Orb orb : newTransferOrbs){
            orb.setOrbAnimationState(Orb.OrbAnimationState.TRANSFERRING);
        }
        transferOutOrbs.addAll(newTransferOrbs);
        cumulativeOrbsTransferred += newTransferOrbs.size();
        transferOutOrbsChanged = true;
    }

    public void decrementShotsUntilNewRow(int amountToDecrement){
        shotsUntilNewRow-=amountToDecrement;
    }

    /* Setters: These are called by clients when they are updating their data according to data from the host*/
    //ToDo: Do I really need these, or should I just use the copy constructor?
    public void setAddShootingOrb(Orb newShootingOrb){
        shootingOrbs.add(newShootingOrb);
        cumulativeShotsFired ++;
    }
    public void setAddDroppingOrb(Orb newOrb){
        droppingOrbs.add(newOrb);
    }
    public void setAddThunderOrbs(List<Orb> newThunderOrbs){
        for(Orb orb : newThunderOrbs){
            orb.setOrbAnimationState(Orb.OrbAnimationState.THUNDERING);
        }
        thunderOrbs.addAll(newThunderOrbs);
    }
    public void setOrbArray(Orb[][] newOrbArray){
        for (int i=0; i<ARRAY_HEIGHT; i++){
            //System.arraycopy(newOrbArray[i],0,orbArray[i],0,orbArray[i].length); // System.arraycopy is faster, but it might cost more memory because it causes the game to retain a reference to the packet from the host. I'm not sure which option is better... Either way, I'd have to replace all the NULL orbs with references to the local NULL orb anyways.
            for(int j=0; j<arrayWidth; j++){
                Orb otherOrb = newOrbArray[i][j];
                if(otherOrb.equals(NULL)) orbArray.setModify(i, j, NULL); // note: The host's Orb.NULL is different from our own. This is why we must use .equals instead of == and replace all these instances with a reference to our own Orb.NULL.
                else orbArray.setModify(i, j, new Orb(otherOrb));
            }
        }
    }
    public void setDeathOrbs(Orb[] newDeathOrbs){
        for(int j=0; j<arrayWidth; j++){
            Orb otherOrb = newDeathOrbs[j];
            if(otherOrb.equals(NULL)) deathOrbs[j] = NULL; // note: The host's Orb.NULL is different from our own. This is why we must use .equals instead of == and replace all these instances with a reference to our own Orb.NULL.
            else deathOrbs[j] = new Orb(otherOrb);
        }
    }
    public <E extends Collection<Orb>> void setOrbCollection(E collectionToOverwrite, E collectionToCopy){
        System.out.println("setting an orb collection");
        collectionToOverwrite.clear();
        // Using addAll() would be faster and cleaner, but may use more memory because the game would have to keep a reference to the entire PlayPanelData from the host.
        for(Orb orb : collectionToCopy) collectionToOverwrite.add(new Orb(orb));
    }


    /* Direct Getters: These are called to get the actual player data*/
    public int getTeam(){
        return team;
    }
    public SynchronizedArray<Orb> getOrbArray(){
        return orbArray;
    }
    public List<Orb> getShootingOrbs(){
        return shootingOrbs;
    }
    public List<Orb> getTransferOutOrbs(){
        return transferOutOrbs;
    }
    public Set<Orb> getTransferInOrbs(){
        return transferInOrbs;
    }
    public Orb[] getDeathOrbs(){
        return deathOrbs;
    }
    public SynchronizedComparable<TeamState> getTeamState(){
        return teamState;
    }

    public void resetFlags(){
        orbArrayChanged = false;
        shootingOrbsChanged = false;
        burstingOrbsChanged = false;
        droppingOrbsChanged = false;
        transferOutOrbsChanged = false;
        transferInOrbsChanged = false;
        deathOrbsChanged = false;
    }

    // repaints all orbs and Character animations on the PlayPanel.
    public void repaint(){
        // Clear the canvas
        orbDrawer.clearRect(0,0,orbCanvas.getWidth(),orbCanvas.getHeight());
        double vibrationOffset = 0.0;

        // A simple line shows the limit of the orbArray. If any orb is placed below this line, the player loses.
        double deathLineY = ROW_HEIGHT*(ARRAY_HEIGHT-1)+2*ORB_RADIUS;
        orbDrawer.setStroke(Color.PINK);
        orbDrawer.setLineWidth(2.0);
        orbDrawer.strokeLine(0,deathLineY,liveBoundary.getWidth(),deathLineY);

        synchronized (synchronizer){
            // Paint dropping Orbs:
            for(Orb orb : droppingOrbs) orb.drawSelf(orbDrawer, vibrationOffset);

            // If we are close to adding 1 more level to the orbArray, apply a vibration effect to the array orbs:
            if(shotsUntilNewRow<=VIBRATION_FREQUENCIES.length && shotsUntilNewRow>0){
                vibrationOffset = VIBRATION_MAGNITUDES[shotsUntilNewRow-1]*Math.sin(2*PI*System.nanoTime()*VIBRATION_FREQUENCIES[shotsUntilNewRow-1]/1000000000);
            }

            // paint Array orbs:
            for(int i=0; i<ARRAY_HEIGHT; ++i){
                for(int j=0; j<arrayWidth; j++){
                    orbArray.getData()[i][j].drawSelf(orbDrawer, vibrationOffset);
                }
            }

            // paint Death orbs:
            for(Orb orb : deathOrbs) orb.drawSelf(orbDrawer,vibrationOffset);

            // paint VisualFlourishes:
            for(Animation visualFlourish : visualFlourishes) visualFlourish.drawSelf(orbDrawer);

            // paint transferring orbs:
            for(Orb orb : transferInOrbs) orb.drawSelf(orbDrawer, vibrationOffset);

            // remaining orbs should not vibrate:
            vibrationOffset = 0.0;

            // Paint bursting orbs:
            for(Orb orb : burstingOrbs){
                orb.drawSelf(orbDrawer, vibrationOffset);
            }

            // Paint each player and his/her ammunition orbs:
            for(Player player : players){
                if(player.getTeam().getData() == team){
                    player.drawSelf(orbDrawer);
                    List<Orb> ammunitionOrbs = player.getAmmunition().getData();
                    ammunitionOrbs.get(0).drawSelf(orbDrawer, vibrationOffset);
                    ammunitionOrbs.get(1).drawSelf(orbDrawer, vibrationOffset);
                }
            }

            // Paint shooting orbs:
            for(Orb orb : shootingOrbs) orb.drawSelf(orbDrawer, vibrationOffset);
        }
    }

    public Random getRandomTransferOrbGenerator(){
        return randomTransferOrbGenerator;
    }

    // todo: this is horribly inefficient. Can I make it faster? It is somewhat amortized, though...
    public OrbColor getNextShooterOrbEnum(double randomNumber){
        // Count the number of each type of orb in the orbArray.
        LinkedHashMap<OrbColor,Double> counts = new LinkedHashMap<>();
        synchronized (synchronizer){
            for(int i=0; i<ARRAY_HEIGHT; i++){
                for(int j=0; j<arrayWidth; j++){
                    if(orbArray.getData()[i][j]==NULL) continue;
                    else if(counts.containsKey(orbArray.getData()[i][j].getOrbColor())){
                        counts.replace(orbArray.getData()[i][j].getOrbColor(),counts.get(orbArray.getData()[i][j].getOrbColor())+1);
                    }
                    else{
                        counts.put(orbArray.getData()[i][j].getOrbColor(),1.0);
                    }
                }
            }
        }

        // Normalize the counts and make them cumulative
        double total = 0.0;
        for(Double amount : counts.values()){
            total += amount;
        }
        double cumulativeSum = 0.0;
        for(OrbColor orbImage : counts.keySet()){
            cumulativeSum += counts.get(orbImage);
            counts.replace(orbImage, cumulativeSum/total);
        }

        OrbColor chosenEnum = OrbColor.BLACK; // initializing to make the compiler happy.
        for(OrbColor orbImage : counts.keySet()){
            if(randomNumber<counts.get(orbImage)){
                chosenEnum = orbImage;
                break;
            }
        }

        return chosenEnum;
    }

    public List<Player> getPlayerList(){
        return players;
    }

    public void displayVictoryResults(VictoryType victoryType){
        Text statistics;
        Animation visualFlourish;
        String specializedStatistic;

        // Note: we must add getDroppingOrbs().size() to getCumulativeOrbsTransferred() because orbs that are dropping at the very end of the game haven't been added to the transferOutOrbs list, yet.
        switch(victoryType) {
            case VS_WIN:
                visualFlourish = new Animation(AnimationName.WIN_SCREEN, 0, 0, PlayOption.PLAY_ONCE_THEN_PAUSE);
                specializedStatistic = "Orbs transferred to other players: " + (cumulativeOrbsTransferred + droppingOrbs.size()) + "\n";
                break;
            case VS_LOSE:
                visualFlourish = new Animation(AnimationName.LOSE_SCREEN, 0, 0, PlayOption.PLAY_ONCE_THEN_PAUSE);
                specializedStatistic = "Orbs transferred to other players: " + (cumulativeOrbsTransferred + droppingOrbs.size()) + "\n";
                break;
            case PUZZLE_CLEARED:
                visualFlourish = new Animation(AnimationName.CLEAR_SCREEN, 0, 0, PlayOption.PLAY_ONCE_THEN_PAUSE);
                specializedStatistic = "Orbs dropped: " + cumulativeOrbsDropped + "\n";
                break;
            case PUZZLE_FAILED:
                visualFlourish = new Animation(AnimationName.LOSE_SCREEN, 0, 0, PlayOption.PLAY_ONCE_THEN_PAUSE);
                specializedStatistic = "Orbs dropped: " + cumulativeOrbsDropped + "\n";
                break;
            default:
                System.err.println("Unrecognized VictoryType. setting default visual Flourish.");
                visualFlourish = new Animation(AnimationName.WIN_SCREEN,0,0, PlayOption.PLAY_ONCE_THEN_PAUSE);
                specializedStatistic = "\n";
        }

        // Add an appropriate Win/Lose/Cleared animation
        visualFlourish.relocate(PLAYPANEL_WIDTH_PER_PLAYER*players.size()/2, PLAYPANEL_HEIGHT/2-100);
        visualFlourishes.add(visualFlourish);

        // Display statistics for this PlayPanel
        statistics = new Text("total orbs fired: " + cumulativeShotsFired + "\n" +
                "Orbs burst: " + cumulativeOrbsBurst + "\n" +
                specializedStatistic +
                "Largest group explosion: " + largestGroupExplosion);
        VBox vBox = new VBox(); // For centering the text on the PlayPanel
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(3.0);
        statistics.setFont(new Font(32));
        statistics.setFill(Color.WHITE);
        statistics.setStroke(Color.BLACK);
        statistics.setStrokeWidth(1.0);
        statistics.setStyle("-fx-font-weight: bold");
        statistics.setTextAlignment(TextAlignment.CENTER);
        vBox.getChildren().addAll(statistics);
        vBox.prefWidthProperty().bind(widthProperty());
        getChildren().add(vBox);
        vBox.relocate(0,PLAYPANEL_HEIGHT/2 -50);
    }

    public enum VictoryType{PUZZLE_CLEARED, PUZZLE_FAILED, VS_WIN, VS_LOSE}


    // Utility function for debugging
    public void printOrbArray(){
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<arrayWidth; j++){
                if(orbArray.getData()[i][j]==null) System.out.print('X');
                if(orbArray.getData()[i][j]==NULL) System.out.print(' ');
                else System.out.print(orbArray.getData()[i][j].getOrbColor().ordinal());
            }
            System.out.print('\n');
        }
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
            int orbEnumBound = OrbColor.values().length;
            OrbColor[] orbImages = OrbColor.values();

            for(int i=0; i<rows; ++i){
                for(int j=0; j<arrayWidth; j++){
                    if(j%2==i%2){
                        int randomOrdinal = randomPuzzleGenerator.nextInt(orbEnumBound);
                        OrbColor orbImage = orbImages[randomOrdinal];
                        orbArray.setModify(i, j, new Orb(orbImage,i,j, Orb.OrbAnimationState.STATIC));
                    }
                    else orbArray.setModify(i, j, NULL);
                }
            }
            for(int i=rows; i<ARRAY_HEIGHT; i++){
                for(int j=0; j<arrayWidth; j++){
                    orbArray.setModify(i, j, NULL);
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
                    for (j=0; j<line.length() && j<arrayWidth; j++){
                        char orbSymbol = line.charAt(j);
                        OrbColor orbEnum = OrbColor.lookupOrbImageBySymbol(orbSymbol);
                        if(orbEnum==null) orbArray.setModify(i, j, NULL);
                        else orbArray.setModify(i, j, new Orb(orbEnum,i,j, Orb.OrbAnimationState.STATIC));
                    }
                    // if the input line was too short to fill the entire puzzle line, fill in the rest of the line with NULL orbs:
                    for(/*j is already set*/; j<arrayWidth; j++){
                        orbArray.setModify(i, j, NULL);
                    }
                }
                // fill the rest of the orb array with NULL orbs:
                for(/*i is already set*/; i<ARRAY_HEIGHT; i++){
                    for(j=0; j<arrayWidth; j++){
                        orbArray.setModify(i, j, NULL);
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
        int i = orbArray.getData().length-1;
        for(int j=0; j<orbArray.getData()[i].length; j++){
            if(orbArray.getData()[i][j]!=NULL){
                System.out.println("This team has lost");
                orbArray.getData()[i][j].setIJ(i+1,j);
                deathOrbs[j] = orbArray.getData()[i][j];
                orbArray.setModify(i, j, NULL);
            }
        }
        for(i=orbArray.getData().length-2; i>=0; i--){
            for(int j=0; j<orbArray.getData()[i].length; j++){
                if(orbArray.getData()[i][j]!=NULL) orbArray.getData()[i][j].setIJ(i+1, j);
                orbArray.setModify(i+1, j, orbArray.getData()[i][j]);
            }
        }

        // Move all transferring orbs down 1 index:
        for(Orb transferOrb : transferInOrbs){
            transferOrb.setIJ(transferOrb.getI()+1,transferOrb.getJ());
        }

        // Determine whether the new row has "odd" or "even" placement:
        i = 1;
        int newRowOffset = 0;
        for(int j=0; j<orbArray.getData()[i].length; j++){
            if(orbArray.getData()[i][j] != NULL){
                newRowOffset = 1-j%2;
                break;
            }
        }

        // finally, add the new row
        i = 0;
        int orbEnumBound = OrbColor.values().length;
        OrbColor[] orbImages = OrbColor.values();
        for(int j=0; j<orbArray.getData()[i].length; j++){
            if(j%2==newRowOffset){
                int randomOrdinal = randomPuzzleGenerator.nextInt(orbEnumBound);
                OrbColor orbImage = orbImages[randomOrdinal];
                orbArray.setModify(i, j, new Orb(orbImage,i,j, Orb.OrbAnimationState.STATIC));
            }
            else orbArray.setModify(i, j, NULL);
        }
    }

    public int getLowestOccupiedRow(Orb[][] orbArray, Orb[] deathOrbs) {
        for(Orb orb : deathOrbs){
            if (orb != NULL) return ARRAY_HEIGHT;
        }

        for (int i = ARRAY_HEIGHT-1; i>=0; i--) {
            Orb[] row = orbArray[i];
            for (int j=arrayWidth-1; j >= 0; j--) {
                if (row[j] != NULL) {
                    return i;
                }
            }
        }

        // otherwise, there are no orbs on the board, so return -1
        return -1;
    }

    // called 24 times per second to update all animations and Orb positions for the next animation frame.
    void tick(Set<SoundEffect> soundEffectsToPlay){
        // New Sets and lists that will be filled via side-effects:
        List<Orb> orbsToDrop = new LinkedList<>(); // Orbs that are no longer connected to the ceiling by the end of this frame
        List<Orb> orbsToTransfer = new LinkedList<>(); // Orbs to transfer to other PlayPanels
        List<Collision> collisions = new LinkedList<>(); // All collisions that happen during this frame
        Set<Orb> connectedOrbs = new HashSet<>(); // Orbs connected to the ceiling
        List<Orb> arrayOrbsToBurst = new LinkedList<>(); // Orbs in the array that will be burst this frame

        // Most of the computation work is done in here. All the lists are updated via side-effects:
        simulateOrbs(orbArray.getData(), burstingOrbs, shootingOrbs, droppingOrbs, deathOrbs, soundEffectsToPlay, orbsToDrop, orbsToTransfer, collisions, connectedOrbs, arrayOrbsToBurst, 1/(double) DATA_FRAME_RATE);

        // Advance the animation frame of the existing visual flourishes:
        List<Animation> flourishesToRemove = advanceVisualFlourishes(visualFlourishes);
        visualFlourishes.removeAll(flourishesToRemove);

        // If orbs were dropped or a sufficient number were burst, add visual flourishes for the orbs to be transferred:
        if(!orbsToDrop.isEmpty() || !orbsToTransfer.isEmpty()){
            soundEffectsToPlay.add(SoundEffect.DROP);
            for(Orb orb : orbsToDrop){
                visualFlourishes.add(new Animation(AnimationName.EXCLAMATION_MARK, orb.getXPos(), orb.getYPos(), PlayOption.PLAY_ONCE_THEN_VANISH));
            }
            for(Orb orb : orbsToTransfer){
                visualFlourishes.add(new Animation(AnimationName.EXCLAMATION_MARK, orb.getXPos(), orb.getYPos(), PlayOption.PLAY_ONCE_THEN_VANISH));
            }
        }

        // Advance the animation frame of existing bursting orbs:
        List<Orb> orbsToRemove = advanceBurstingOrbs(burstingOrbs);
        burstingOrbs.removeAll(orbsToRemove);

        // Advance the animation frame of the electrified orbs:
        advanceElectrifyingOrbs();

        // Advance the animation and audio of the thunder orbs:
        orbsToRemove = advanceThunderOrbs();
        thunderOrbs.removeAll(orbsToRemove);

        // Advance existing dropping orbs:
        List<Orb> orbsToThunder = advanceDroppingOrbs();
        droppingOrbs.removeAll(orbsToThunder);

        // If orbs dropped off the bottom of the PlayPanel, add them to the orbsToTransfer list AND the thunderOrbs list.
        if(!orbsToThunder.isEmpty()){
            for(Orb orb : orbsToThunder) orbsToTransfer.add(new Orb(orb)); // We must create a copy so that we can change the animationEnum without affecting the transferOutOrbs.
            setAddThunderOrbs(orbsToThunder);
        }

        // Add all of the transfer orbs to the transferOutOrbs list:
        if(!orbsToTransfer.isEmpty()) changeAddTransferOutOrbs(orbsToTransfer);

        // Advance the existing transfer-in Orbs, adding visual flourishes if they're done:
        List<Orb> transferOrbsToSnap = advanceTransferringOrbs();
        snapTransferOrbs(transferOrbsToSnap, orbArray.getData(), connectedOrbs, soundEffectsToPlay, visualFlourishes, transferInOrbs);

        // If there are no orbs connected to the ceiling, then this team has finished the puzzle. Move on to the next one or declare victory
        if(connectedOrbs.isEmpty()){
            shootingOrbs.clear();
            if(puzzleUrl.substring(0,6).equals("RANDOM")){ // this was a random puzzle. Declare victory
                teamState.changeTo(TeamState.VICTORIOUS);
            }
            else{ // This was a pre-built puzzle. Load the next one, if there is one.
                int currentIndex = Integer.parseInt(puzzleUrl.substring(puzzleUrl.length()-2,puzzleUrl.length()));
                puzzleUrl = String.format("%s%02d",puzzleUrl.substring(0,puzzleUrl.length()-2),currentIndex+1);
                if(!initializeOrbArray(puzzleUrl)){ // There was no next puzzle. Declare victory.
                    teamState.changeTo(TeamState.VICTORIOUS);
                }
                else{
                    for(int i=0; i<players.size(); i++){
                        Player player = players.get(i);
                        player.readAmmunitionOrbs(puzzleUrl, seed);
                        player.positionAmmunitionOrbs();
                    }
                }
            }
        }

        // reset shotsUntilNewRow
        if(shotsUntilNewRow<=0) shotsUntilNewRow = SHOTS_BETWEEN_DROPS*players.size() + shotsUntilNewRow;

        // If the player has fired a sufficient number of times, then add a new row of orbs:
        decrementShotsUntilNewRow(collisions.size());
        if(shotsUntilNewRow==1) soundEffectsToPlay.add(SoundEffect.ALMOST_NEW_ROW);
        else if(shotsUntilNewRow<=0){
            addNewRow();
            soundEffectsToPlay.add(SoundEffect.NEW_ROW);
        }

        // check to see whether this team has lost due to uncleared deathOrbs:
        if(!isDeathOrbsEmpty()){
            for(Player defeatedPlayer : players){
                defeatedPlayer.getPlayerStatus().changeTo(Player.PlayerStatus.DEFEATED);
            }
        }

        // update each player's animation state:
        int lowestRow = getLowestOccupiedRow(orbArray.getData(), deathOrbs);
        for(Player player : players){
            if (player instanceof BotPlayer) continue; // we've already ticked() the BotPlayers.
            player.tick(lowestRow);
        }

        removeStrayOrbs();
    }

    // removes orbs that have wandered off the edges of the canvas. This should only ever happen with dropping orbs, but
    // shooting orbs are also checked, just in case.
    private void removeStrayOrbs()
    {
        List<Orb> orbsToRemove = new LinkedList<>();
        // Although it should never happen, let's look for stray shooting orbs and remove them:
        for(Orb orb : shootingOrbs)
        {
            if(orb.getXPos()<-ORB_RADIUS
                    || orb.getXPos()>PLAYPANEL_WIDTH_PER_PLAYER*players.size()+2*ORB_RADIUS
                    || orb.getYPos()<-ORB_RADIUS
                    || orb.getYPos()>PLAYPANEL_HEIGHT)
                orbsToRemove.add(orb);
        }
        shootingOrbs.removeAll(orbsToRemove);
    }

    public List<Orb> advanceBurstingOrbs(List<Orb> burstingOrbs) {
        List<Orb> orbsToRemove = new LinkedList<>();
        for(Orb orb : burstingOrbs){
            if(orb.tick()){
                orbsToRemove.add(orb);
            }
        }
        return orbsToRemove;
    }

    private void advanceElectrifyingOrbs(){
        Orb orb;
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<arrayWidth; j++) {
                orb = orbArray.getData()[i][j];
                if(orb !=NULL){
                    switch(orb.getOrbAnimationState()){
                        case STATIC:
                            if(miscRandomGenerator.nextDouble()< ELECTRIFICATION_PROBABILITY){
                                orb.setOrbAnimationState(Orb.OrbAnimationState.ELECTRIFYING);
                            }
                            break;
                        case ELECTRIFYING:
                            orb.tick();
                            break;
                    }
                }
            }
        }
    }

    private List<Orb> advanceTransferringOrbs(){
        List<Orb> transferOrbsToSnap = new LinkedList<>();
        for(Orb orb : transferInOrbs){
            if (orb.tick()){
                transferOrbsToSnap.add(orb);
            }
        }
        return transferOrbsToSnap;
    }

    // Todo: there are several methods exactly like this one. Can it be reduced to one using generics? Actually, it might require casting outside this method call, so think about it carefully.
    private List<Animation> advanceVisualFlourishes(List<Animation> visualFlourishes){
        List<Animation> visualFlourishesToRemove = new LinkedList<>();
        for(Animation visualFlourish : visualFlourishes){
            if(visualFlourish.tick()) visualFlourishesToRemove.add(visualFlourish);
        }
        return visualFlourishesToRemove;
    }

    private List<Orb> advanceDroppingOrbs(){
        List<Orb> orbsToTransfer = new LinkedList<>();
        for(Orb orb : droppingOrbs){
            orb.setSpeed(orb.getSpeed() + GameScene.GRAVITY/ DATA_FRAME_RATE);
            orb.relocate(orb.getXPos(), orb.getYPos() + orb.getSpeed()/ DATA_FRAME_RATE);
            if(orb.getYPos() > PLAYPANEL_HEIGHT){
                orbsToTransfer.add(orb);
            }
        }
        return orbsToTransfer;
    }

    private List<Orb> advanceThunderOrbs(){
        List<Orb> orbsToRemove = new LinkedList<>();
        for(Orb orb : thunderOrbs){
            if(orb.tick()){
                orbsToRemove.add(orb);
            }
        }
        return orbsToRemove;
    }

    /* *********************************************** UTILITY *********************************************** */

    public void findPatternCompletions(List<Collision> collisions, Orb[][] orbArray, List<Orb> shootingOrbsToBurst, Set<SoundEffect> soundEffectsToPlay, List<Orb> orbsToTransfer, List<Orb> arrayOrbsToBurst){
        for(Collision collision : collisions){
            if(shootingOrbsToBurst.contains(collision.shooterOrb)) continue; // Only consider orbs that are not already in the bursting orbs list

            // find all connected orbs of the same color
            Set<Orb> connectedOrbs = new HashSet<>();
            cumulativeDepthFirstSearch(collision.shooterOrb, connectedOrbs, orbArray, FilterOption.SAME_COLOR);
            if(connectedOrbs.size() > 2) arrayOrbsToBurst.addAll(connectedOrbs);

            // If there are a sufficient number grouped together, then add a transfer out orb of the same color:
            int numTransferOrbs;
            if((numTransferOrbs = (connectedOrbs.size()-3)/2) > 0) {
                soundEffectsToPlay.add(SoundEffect.DROP);
                Iterator<Orb> orbIterator = connectedOrbs.iterator();
                for(int k=0; k<numTransferOrbs; k++){
                    Orb orbToTransfer = orbIterator.next();
                    orbsToTransfer.add(new Orb(orbToTransfer)); // add a copy of the orb, so we can change the animationEnum without messing up the original (which still needs to burst).
                }
            }
            // todo: move this code outside of this class.
            if(orbArray == orbArray && connectedOrbs.size()>largestGroupExplosion){
                largestGroupExplosion = connectedOrbs.size();
            }
        }
    }

    public void findConnectedOrbs(Orb[][] orbArray, Set<Orb> connectedOrbs){
        // find all orbs connected to the ceiling:
        for(int j=0; j<orbArray[0].length; j++){
            if(orbArray[0][j]==NULL) continue;
            cumulativeDepthFirstSearch(orbArray[0][j], connectedOrbs, orbArray, FilterOption.ALL);
        }
    }

    // Note to self: This still mostly works even if the orb is on the deathOrbs list. It will find all neightbors of
    // that deathOrb that are in the orbArray. It will NOT, however, find its neighbors that are also on the deathOrbs array.
    // The returned List does not contain the source object.
    public <E extends PointInt> List<E> getNeighbors(E source, E[][] array){
        int i = source.getI();
        int j = source.getJ();
        List<E> neighbors = new LinkedList<>();

        //test all possible neighbors for valid coordinates
        int[] iTests = {i-1, i-1, i, i, i+1, i+1};
        int[] jTests = {j-1, j+1, j-2, j+2, j-1, j+1};
        for(int k=0; k<iTests.length; k++){
            if(validArrayCoordinates(iTests[k], jTests[k], array) && array[iTests[k]][jTests[k]]!=NULL){
                neighbors.add(array[iTests[k]][jTests[k]]);
            }
        }
        return neighbors;
    }

    // Finds floating orbs and drops them. Also checks for victory conditions
    public void findFloatingOrbs(Set<Orb> connectedOrbs, Orb[][] orbArray, List<Orb> orbsToDrop){
        // any orbs in the array that are not among the connectedOrbs Set are floating.
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<orbArray[i].length; j++){
                Orb arrayOrb = orbArray[i][j];
                if(arrayOrb!=NULL && !connectedOrbs.contains(arrayOrb)){
                    orbsToDrop.add(arrayOrb);
                }
            }
        }
    }

    public void cumulativeDepthFirstSearch(Orb source, Set<Orb> matches, Orb[][] orbArray, FilterOption filter) {

        // A boolean orbArray that has the same size as the orbArray, to mark orbs as "examined"
        Boolean[][] examined = new Boolean[orbArray.length][orbArray[0].length];
        for(int i=0; i<orbArray.length; i++){
            for(int j=0; j<orbArray[i].length; j++){
                examined[i][j] = false;
            }
        }

        // A stack containing the "active" elements to be examined next
        Deque<Orb> active = new LinkedList<>();

        // Add the source Orb to the active list and mark it as "examined"
        active.push(source);
        if(validArrayCoordinates(source, examined)) examined[source.getI()][source.getJ()] = true; // deathOrbs have "invalid" coordinates, so we have to check whether the coordinates are valid.

        // Mark everything in the starting set as "examined"
        for(Orb orb : matches) examined[orb.getI()][orb.getJ()] = true;

        // Do a depth-first search
        while (!active.isEmpty()) {
            Orb activeOrb = active.pop();
            matches.add(activeOrb);
            List<Orb> neighbors = getNeighbors(activeOrb, orbArray); // recall: neighbors does not contain any death Orbs. Only orbArray Orbs.
            for (Orb neighbor : neighbors) {
                if (!examined[neighbor.getI()][neighbor.getJ()]) {
                    // apply the filter option
                    boolean passesFilter = false;
                    switch(filter){
                        case ALL:
                            passesFilter = true;
                            break;
                        case SAME_COLOR:
                            if(orbArray[neighbor.getI()][neighbor.getJ()].getOrbColor() == source.getOrbColor()) passesFilter = true;
                            break;
                    }
                    if(passesFilter){
                        active.add(neighbor);
                        examined[neighbor.getI()][neighbor.getJ()] = true;
                    }
                }
            }
        }
    }










    /* *********************************************** SIMULATION *********************************************** */


    public void simulateOrbs(Orb[][] orbArray, List<Orb> burstingOrbs, List<Orb> shootingOrbs, List<Orb> droppingOrbs, Orb[] deathOrbs, Set<SoundEffect> soundEffectsToPlay, List<Orb> orbsToDrop, List<Orb> orbsToTransfer, List<Collision> collisions, Set<Orb> connectedOrbs, List<Orb> arrayOrbsToBurst, double deltaTime){
        // Advance shooting orbs and detect collisions:
        advanceShootingOrbs(shootingOrbs, orbArray, deltaTime, soundEffectsToPlay, collisions); // Updates model

        // Snap any landed shooting orbs into place on the orbArray (or deathOrbs array):
        List<Orb> shootingOrbsToBurst = snapOrbs(collisions, orbArray, deathOrbs,soundEffectsToPlay);

        // Remove the collided shooting orbs from the shootingOrbs list
        for(Collision collision : collisions) shootingOrbs.remove(collision.shooterOrb);

        // Determine whether any of the snapped orbs cause any orbs to burst:
        findPatternCompletions(collisions, orbArray, shootingOrbsToBurst, soundEffectsToPlay, orbsToTransfer, arrayOrbsToBurst);

        // Burst new orbs:
        if(!shootingOrbsToBurst.isEmpty() || !arrayOrbsToBurst.isEmpty()){
            soundEffectsToPlay.add(SoundEffect.EXPLOSION);
            changeBurstShootingOrbs(shootingOrbsToBurst, shootingOrbs, burstingOrbs);
            changeBurstArrayOrbs(arrayOrbsToBurst,orbArray,deathOrbs,burstingOrbs);
        }

        // Find floating orbs and drop them:
        findConnectedOrbs(orbArray, connectedOrbs); // orbs that are connected to the ceiling.
        findFloatingOrbs(connectedOrbs, orbArray, orbsToDrop);
        changeDropArrayOrbs(orbsToDrop, droppingOrbs, orbArray);
    }

    // Initiated 24 times per second, and called recursively.
    // advances all shooting orbs, detecting collisions along the way and stopping orbs that collide with arrayOrbs or
    // with the ceiling.
    // A single recursive call to this function might only advance the shooting orbs a short distance, but when all the
    // recursive calls are over, all shooting orbs will have been advanced one full frame.
    // Returns a list of all orbs that will attempt to snap; some of them may end up bursting instead during the call to
    // snapOrbs if (and only if) s-s collisions are turned off.
    // Note: recall that the y-axis points downward and shootingOrb.getCannonAngle() returns a negative value.
    public void advanceShootingOrbs(List<Orb> shootingOrbs, Orb[][] orbArray, double timeRemainingInFrame, Set<SoundEffect> soundEffectsToPlay, List<Collision> collisions) {
        // Put all possible collisions in here. If a shooter orb's path this frame would put it on a collision course
        // with the ceiling, a wall, or an array orb, then that collision will be added to this list, even if there is
        // another orb in the way.
        List<Collision> possibleCollisionPoints = new LinkedList<>();

        for (Orb shootingOrb : shootingOrbs) {
            double speed = shootingOrb.getSpeed();
            double angle = shootingOrb.getAngle();
            double x0 = shootingOrb.getXPos(); // x-position of the shooting orb before it is advanced.
            double y0 = shootingOrb.getYPos(); // y-position of the shooting orb before it is advanced.
            double distanceToTravel = speed * timeRemainingInFrame;
            double x1P = distanceToTravel * Math.cos(angle); // Theoretical x-position of the shooting orb after it is advanced, relative to x0.
            double y1P = distanceToTravel * Math.sin(angle); // Theoretical y-position of the shooting orb after it is advanced, relative to y0
            double tanAngle = y1P/x1P;
            double onePlusTanAngleSquared = 1+Math.pow(tanAngle, 2.0); // cached for speed

            // Cycle through the entire Orb array and find all possible collision points along this shooting orb's path:
            boolean collisionsFoundOnRow = false;
            for (int i = ARRAY_HEIGHT-1; i >= 0; i--) {
                for (int j = 0; j < orbArray[i].length; j ++) {
                    if (orbArray[i][j] != NULL) {
                        double xAP = orbArray[i][j].getXPos() - x0;
                        double yAP = orbArray[i][j].getYPos() - y0;
                        double lhs = FOUR_R_SQUARED * onePlusTanAngleSquared;
                        double rhs = Math.pow(tanAngle * xAP - yAP, 2.0);
                        // Test whether collision is possible. If it is, then compute its 2 possible collision points.
                        if (lhs > rhs) {
                            // Compute the two possible intersection points, (xPP, yPP) and (xPN, yPN)
                            double xPP;
                            double yPP;
                            double xPN;
                            double yPN;

                            // if the Orb is traveling nearly vertically, use a special solution to prevent a near-zero denominator:
                            if(Math.abs(x1P) < 0.01) {
                                xPP = 0;
                                xPN = 0;
                                double sqrt4R2mxAP2 = Math.sqrt(FOUR_R_SQUARED - xAP * xAP);
                                yPP = yAP + sqrt4R2mxAP2;
                                yPN = yAP - sqrt4R2mxAP2;
                            }
                            else {
                                double numerator1 = xAP + tanAngle * yAP;
                                double numerator2 = Math.sqrt(lhs - rhs);
                                xPP = (numerator1 + numerator2) / onePlusTanAngleSquared;
                                yPP = xPP * tanAngle;
                                xPN = (numerator1 - numerator2) / onePlusTanAngleSquared;
                                yPN = xPN * tanAngle;
                            }

                            // Figure out which intersection point is closer, and only add the collision to the list of
                            // possible collisions if its time-to-collision is less than the time remaining in the frame.
                            double distanceToCollisionPSquared = xPP * xPP + yPP * yPP;
                            double distanceToCollisionNSquared = xPN * xPN + yPN * yPN;
                            if (distanceToCollisionPSquared < distanceToTravel * distanceToTravel
                                    && distanceToCollisionPSquared < distanceToCollisionNSquared) {
                                double timeToCollision = Math.sqrt(distanceToCollisionPSquared) / speed;
                                possibleCollisionPoints.add(new Collision(shootingOrb, orbArray[i][j], timeToCollision));
                                collisionsFoundOnRow = true;
                            }
                            else if (distanceToCollisionNSquared < distanceToTravel * distanceToTravel) {
                                double timeToCollision = Math.sqrt(distanceToCollisionNSquared) / speed;
                                possibleCollisionPoints.add(new Collision(shootingOrb, orbArray[i][j], timeToCollision));
                                collisionsFoundOnRow = true;
                            }
                        }
                    }
                    // note to self: don't break out of the inner loop if collisionFoundOnRow == true. There may be
                    // multiple Orbs in the shootingOrb's path on this row and it's not necessarily the case that the
                    // first one we find is the closest one.
                }
                if(collisionsFoundOnRow) break;
            }

            // Check for and add collisions with the wall:
            double xRightWallP = (PLAYPANEL_WIDTH_PER_PLAYER*players.size() + ORB_RADIUS) - ORB_RADIUS - x0;
            double xLeftWallP = ORB_RADIUS - x0;
            double yCeilingP = ORB_RADIUS - y0;
            if (x1P >= xRightWallP) {
                double timeToCollision = timeRemainingInFrame * xRightWallP / x1P;
                possibleCollisionPoints.add(new Collision(shootingOrb, Orb.WALL, timeToCollision));
            }
            if (x1P <= xLeftWallP) {
                double timeToCollision = timeRemainingInFrame * xLeftWallP / x1P;
                possibleCollisionPoints.add(new Collision(shootingOrb, Orb.WALL, timeToCollision));
            }

            // ToDo: check for and add collisions with other shooting orbs

            // Check for and add collisions with the ceiling:
            if(y1P<yCeilingP){
                double timeToCollision = timeRemainingInFrame * yCeilingP / y1P;
                possibleCollisionPoints.add(new Collision(shootingOrb, Orb.CEILING, timeToCollision));
            }
        }

        // Find the *soonest* collision among the list of possible collisions.
        Collision soonestCollision = null;
        double soonestCollisionTime = Long.MAX_VALUE;
        for (Collision collision : possibleCollisionPoints) {
            if (collision.timeToCollision <= soonestCollisionTime) {
                soonestCollisionTime = collision.timeToCollision;
                soonestCollision = collision;
            }
        }

        // Advance all shooting orbs to that point in time and deal with the collision.
        if (soonestCollision != null) {
            for (Orb shootingOrb : shootingOrbs) {
                double angle = shootingOrb.getAngle();
                double distanceToTravel = shootingOrb.getSpeed() * soonestCollisionTime;
                shootingOrb.relocate(shootingOrb.getXPos() + distanceToTravel * Math.cos(angle), shootingOrb.getYPos() + distanceToTravel * Math.sin(angle));
            }

            // If there was a collision with a wall, then just reflect the shooter orb's angle.
            if (soonestCollision.arrayOrb == Orb.WALL) {
                soundEffectsToPlay.add(SoundEffect.CHINK);
                Orb shooterOrb = soonestCollision.shooterOrb;
                shooterOrb.setAngle(PI - shooterOrb.getAngle());
            }

            // If the collision is between two shooter orbs, compute new angles and speeds. If the other shooting orb is
            // in the process of snapping, then burst this shooting orb.
            else if (shootingOrbs.contains(soonestCollision.arrayOrb)) {
                // Todo: do this.
            }

            // If the collision was with the ceiling, set that orb's speed to zero and add it to the collisions list.
            else if(soonestCollision.arrayOrb == Orb.CEILING){
                soonestCollision.shooterOrb.setSpeed(0.0);
                collisions.add(soonestCollision);
            }

            // If the collision is between a shooter orb and an array orb, set that orb's speed to zero and add it to
            // the collisions list.
            else {
                soonestCollision.shooterOrb.setSpeed(0.0);
                collisions.add(soonestCollision);
            }

            // Recursively call this function.
            advanceShootingOrbs(shootingOrbs,orbArray, timeRemainingInFrame - soonestCollisionTime, soundEffectsToPlay, collisions);
        }

        // If there are no more collisions, just advance all orbs to the end of the frame.
        else {
            for (Orb shootingOrb : shootingOrbs) {
                double angle = shootingOrb.getAngle();
                double distanceToTravel = shootingOrb.getSpeed() * timeRemainingInFrame;
                shootingOrb.relocate(shootingOrb.getXPos() + distanceToTravel * Math.cos(angle), shootingOrb.getYPos() + distanceToTravel * Math.sin(angle));
            }
        }
    }

    public List<Orb> snapOrbs(List<Collision> snaps, Orb[][] orbArray, Orb[] deathOrbs, Set<SoundEffect> soundEffectsToPlay){

        List<Orb> orbsToBurst = new LinkedList<>();

        for(Collision snap : snaps){
            int iSnap;
            int jSnap;

            // Compute snap coordinates for orbs that collided with the ceiling
            if(snap.arrayOrb== Orb.CEILING){
                int offset = 0;
                for(int j=0; j<orbArray[0].length; j++){
                    if(orbArray[0][j] != NULL){
                        offset = j%2;
                        break;
                    }
                }
                double xPos = snap.shooterOrb.getXPos();
                iSnap = 0;
                jSnap = 2*((int) Math.round((xPos - ORB_RADIUS)/(2*ORB_RADIUS))) + offset;
            }

            // Compute snap coordinates for orbs that collided with an array orb
            else{
                // Recompute the collision angle:
                double collisionAngleDegrees = Math.toDegrees(Math.atan2(snap.shooterOrb.getYPos()-snap.arrayOrb.getYPos(),
                        snap.shooterOrb.getXPos()-snap.arrayOrb.getXPos()));

                // set snap coordinates based on angle:
                if(collisionAngleDegrees<30 && collisionAngleDegrees>=-30){ // Collided with right side of array orb
                    iSnap = snap.arrayOrb.getI();
                    jSnap = snap.arrayOrb.getJ()+2;
                }
                else if(collisionAngleDegrees<90 && collisionAngleDegrees>=30){ // Collided with lower-right side of array orb
                    iSnap = snap.arrayOrb.getI()+1;
                    jSnap = snap.arrayOrb.getJ()+1;
                }
                else if(collisionAngleDegrees<150 && collisionAngleDegrees>=90){ // Collided with lower-left side of array orb
                    iSnap = snap.arrayOrb.getI()+1;
                    jSnap = snap.arrayOrb.getJ()-1;
                }
                else if(collisionAngleDegrees<-150 || collisionAngleDegrees>=150){ // Collided with left side of array orb
                    iSnap = snap.arrayOrb.getI();
                    jSnap = snap.arrayOrb.getJ()-2;
                }
                else if(collisionAngleDegrees<-90 && collisionAngleDegrees>=-150){ // Collided with upper-left side of array orb
                    iSnap = snap.arrayOrb.getI()-1;
                    jSnap = snap.arrayOrb.getJ()-1;
                }
                else { // Collided with upper-right side of the array orb
                    iSnap = snap.arrayOrb.getI()-1;
                    jSnap = snap.arrayOrb.getJ()+1;
                }
            }

            // If the i coordinate is below the bottom of the array, then put the orb on the deathOrbs list.
            if(validDeathOrbsCoordinates(iSnap, jSnap, deathOrbs)){
                // If s-s collisions are turned off, it is possible for two shooter orbs to try to snap to the same location
                // on the deathOrbs array. If that's the case, then burst the second orb that attempts to snap
                if(deathOrbs[jSnap] != NULL){
                    orbsToBurst.add(snap.shooterOrb);
                }
                else{
                    deathOrbs[jSnap] = snap.shooterOrb;
                    snap.shooterOrb.setIJ(iSnap, jSnap);
                    soundEffectsToPlay.add(SoundEffect.PLACEMENT);
                }
            }
            // If the snap coordinates are somehow off the edge of the array in a different fashion, then just burst
            // the orb. This should never happen, but... you never know.
            else if(!validArrayCoordinates(iSnap, jSnap, orbArray)){
                System.err.println("Invalid snap coordinates [" + iSnap + ", " + jSnap + "] detected. Bursting orb.");
                System.err.println("   shooter orb info: " + snap.shooterOrb.getOrbColor() + " " + snap.shooterOrb.getOrbAnimationState() + " x=" + snap.shooterOrb.getXPos() + " y=" + snap.shooterOrb.getYPos() + " speed=" + snap.shooterOrb.getSpeed());
                System.err.println("   array orb info: " + snap.arrayOrb.getOrbColor() + " " + snap.arrayOrb.getOrbAnimationState() + "i=" + snap.arrayOrb.getI() + " j=" + snap.arrayOrb.getJ() + " x=" + snap.arrayOrb.getXPos() + " y=" + snap.arrayOrb.getYPos());
                orbsToBurst.add(snap.shooterOrb);
            }
            // If s-s collisions are turned off, it is possible for two shooter orbs to try to snap to the same location.
            // If that's the case, then burst the second orb that attempts to snap.
            else if(orbArray[iSnap][jSnap] != NULL){
                orbsToBurst.add(snap.shooterOrb);
            }
            else{
                orbArray[iSnap][jSnap] = snap.shooterOrb;
                snap.shooterOrb.setIJ(iSnap, jSnap);
                soundEffectsToPlay.add(SoundEffect.PLACEMENT);
            }
        }
        return orbsToBurst;
    }






    /* *********************************************** CHANGERS *********************************************** */

    public void changeBurstShootingOrbs(List<Orb> newBurstingOrbs, List<Orb> shootingOrbs, List<Orb> burstingOrbs){
        shootingOrbs.removeAll(newBurstingOrbs);
        burstingOrbs.addAll(newBurstingOrbs);
        for(Orb orb : newBurstingOrbs){
            orb.setOrbAnimationState(Orb.OrbAnimationState.IMPLODING);
        }
    }

    public void changeBurstArrayOrbs(List<Orb> newBurstingOrbs, Orb[][] orbArray, Orb[] deathOrbs, List<Orb> burstingOrbs){
        for(Orb orb : newBurstingOrbs){
            if(validArrayCoordinates(orb, orbArray)){
                orbArray[orb.getI()][orb.getJ()] = NULL;
            }
            else{ // the Orb is on the deathOrbs array.
                deathOrbs[orb.getJ()] = NULL;
            }
            orb.setOrbAnimationState(Orb.OrbAnimationState.IMPLODING);
            burstingOrbs.add(orb);
        }
        // todo: move this code outside of the simulation.
        if(burstingOrbs == this.burstingOrbs){ // to prevent a simulated shot from setting these values.
            cumulativeOrbsBurst += newBurstingOrbs.size();
        }
    }
    public void changeDropArrayOrbs(List<Orb> newDroppingOrbs, List<Orb> droppingOrbs, Orb[][] orbArray){
        for(Orb orb : newDroppingOrbs){
            droppingOrbs.add(orb);
            orbArray[orb.getI()][orb.getJ()] = NULL;
        }
        // todo: move this code outside of the simulation.
        if(droppingOrbs == this.droppingOrbs){ // to prevent a simulated shot from setting these values.
            cumulativeOrbsDropped += newDroppingOrbs.size();
        }
    }
    public void changeAddDeathOrb(Orb orb, Orb[] deathOrbs){
        deathOrbs[orb.getJ()] = orb;
    }

    public void transferOrbs(List<Orb> transferOutOrbs, Set<Orb> transferInOrbs, Random randomTransferOrbGenerator, Orb[][] orbArray){
        // Make a deep copy of the orbs to be transferred. We can't place the same orb instance in 2 PlayPanels
        List<Orb> newTransferOrbs = deepCopyOrbList(transferOutOrbs);

        // The new transfer orbs need to be placed appropriately. Find open, connected spots:
        int offset = 0;
        for(int j=0; j<orbArray[0].length; j++){
            if(orbArray[0][j]!=NULL){
                offset = j%2;
                break;
            }
        }
        List<PointInt> openSpots = new LinkedList<>();
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=offset + i%2 - 2*offset*i%2; j<orbArray[0].length; j+=2){
                if(orbArray[i][j]==NULL && (!getNeighbors(new PointInt(i,j), orbArray).isEmpty() || i==0) && !isTransferInOrbOccupyingPosition(i,j,transferInOrbs)){
                    openSpots.add(new PointInt(i,j));
                }
            }
        }

        // Now pick one spot for each orb:
        //todo: if the orbArray is full, put transfer orbs in the deathOrbs list
        //todo: if there are otherwise not enough open, connected spots, then place transferOrbs in secondary and tertiary locations.
        List<Orb> addedTransferOrbs = new LinkedList<>();
        for(Orb orb : newTransferOrbs){
            if(openSpots.isEmpty()) break; //todo: temporary fix to avoid calling nextInt(0). In the future, place transferOrbs in secondary and tertiary locations.
            int index = randomTransferOrbGenerator.nextInt(openSpots.size());
            PointInt openSpot = openSpots.get(index);
            orb.setIJ(openSpot.getI(),openSpot.getJ());
            openSpots.remove(index);
            addedTransferOrbs.add(orb);
        }

        // now, finally add them to the appropriate Orb list:
        transferInOrbs.addAll(addedTransferOrbs);
    }

    public void snapTransferOrbs(List<Orb> transferOrbsToSnap, Orb[][] orbArray, Set<Orb> connectedOrbs, Set<SoundEffect> soundEffectsToPlay, List<Animation> visualFlourishes, Set<Orb> transferInOrbs){
        for(Orb orb : transferOrbsToSnap){
            // only those orbs that would be connected to the ceiling should materialize:
            List<Orb> neighbors = getNeighbors(orb, orbArray);
            if((orb.getI()==0 || !Collections.disjoint(neighbors,connectedOrbs)) && orbArray[orb.getI()][orb.getJ()] == NULL){
                orbArray[orb.getI()][orb.getJ()] = orb;
                soundEffectsToPlay.add(SoundEffect.MAGIC_TINKLE);
                visualFlourishes.add(new Animation(AnimationName.MAGIC_TELEPORTATION, orb.getXPos(), orb.getYPos(), PlayOption.PLAY_ONCE_THEN_VANISH));
            }
        }

        // remove the snapped transfer orbs from the inbound transfer orbs list
        transferInOrbs.removeAll(transferOrbsToSnap);
    }






    /* *********************************************** BOUNDS CHECKERS *********************************************** */

    public<E> boolean validArrayCoordinates(PointInt testPoint, E[][] array)
    {
        return validArrayCoordinates(testPoint.getI(), testPoint.getJ(), array);
    }

    /**
     * Checks whether the given indices are valid coordinates in the array. Works for any array of any type!
     * @param i The first index (i-coordinate or row coordinate)
     * @param j The second index (j-coordinate or column coordinate)
     * @param array Any 2-dimensional array.
     * @return true, if the coordinates are valid, false if they are outside the bounds of the array.
     */
    private <E> boolean validArrayCoordinates(int i, int j, E[][] array){
        return (i>=0 && i<array.length && j>=0 && j<array[i].length);
    }

    public boolean validDeathOrbsCoordinates(PointInt testPoint, Orb[] deathOrbsArray){
        return validDeathOrbsCoordinates(testPoint.getI(), testPoint.getJ(), deathOrbsArray);
    }

    // Overloaded method to avoid the creation of a temporary object.
    public boolean validDeathOrbsCoordinates(int iCoordinate, int jCoordinate, Orb[] deathOrbsArray){
        return (iCoordinate==ARRAY_HEIGHT && jCoordinate>=0 && jCoordinate<deathOrbsArray.length);
    }

    private boolean isTransferInOrbOccupyingPosition(int i, int j, Set<Orb> transferInOrbs){
        for(Orb orb : transferInOrbs){
            if(orb.getI()==i && orb.getJ()==j) return true;
        }
        return false;
    }



/* *********************************************** DEEP COPIERS *********************************************** */

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

    public Orb[] deepCopyOrbArray(Orb[] other){
        Orb[] copiedArray = new Orb[other.length];
        for(int j=0; j<other.length; j++){
            if(other[j].equals(NULL)) copiedArray[j] = NULL;
            else other[j] = new Orb(other[j]);
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

    public List<Animation> deepCopyVisualFlourishesList(List<Animation> other){
        List<Animation> copiedList = new LinkedList<>();
        for(Animation visualFlourish : other){
            copiedList.add(new Animation(visualFlourish));
        }
        return copiedList;
    }

    public Set<Orb> deepCopyOrbSet(Set<Orb> other){
        Set<Orb> copiedSet = new HashSet<>();
        for(Orb orb : other){
            copiedSet.add(new Orb(orb));
        }
        return copiedSet;
    }
}

