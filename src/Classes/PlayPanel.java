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
import static java.lang.Math.abs;

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
    public static final int SHOTS_BETWEEN_DROPS = 15*ARRAY_WIDTH_PER_CHARACTER; // After the player shoots this many times, a new row of orbs appears at the top.

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
    private SynchronizedArray<Orb> orbArray;
    private Orb deathOrbs[]; // orbs below the line of death. If these are not immediately cleared in 1 frame, then this team has lost.
    private List<Orb> shootingOrbs = new LinkedList<>();
    private List<Orb> burstingOrbs = new LinkedList<>();
    private List<Orb> droppingOrbs = new LinkedList<>();
    private List<Orb> transferOutOrbs = new LinkedList<>(); // orbs to be transferred to other players
    private SynchronizedList<Orb> transferInOrbs;
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

    // Options available for depth-first search
    public enum FilterOption {ALL, SAME_COLOR}

    // For generating the puzzle and ammunition, and determining where transfer orbs appear:
    private Random randomPuzzleGenerator;
    private Random randomTransferOrbGenerator;
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

        orbArray = new SynchronizedArray<>("orbArray",new Orb[ARRAY_HEIGHT][arrayWidth], SynchronizedData.Precedence.HOST, team, synchronizer);
        transferInOrbs = new SynchronizedList<Orb>("transferInOrbs", new LinkedList<>(), SynchronizedData.Precedence.HOST, team, synchronizer);
        initializeOrbArray(this.puzzleUrl);

        deathOrbs = new Orb[arrayWidth];
        Arrays.fill(deathOrbs,NULL);

        shotsUntilNewRow = SHOTS_BETWEEN_DROPS*players.size();
    }

    /* Specialized Changers */
    public void changeAddTransferOutOrbs(List<Orb> newTransferOrbs){
        for(Orb orb : newTransferOrbs){
            orb.setOrbAnimationState(Orb.OrbAnimationState.TRANSFERRING);
        }
        transferOutOrbs.addAll(newTransferOrbs);
        cumulativeOrbsTransferred += newTransferOrbs.size();
    }

    /* Setters: These are called by clients when they are updating their data according to data from the host*/
    public void setAddShootingOrb(Orb newShootingOrb){
        // To distinguish same-colored shooting Orbs from one another, attach a little extra int:
        int distinguishingInt = 0;
        for(Orb shootingOrb : shootingOrbs){
            if(shootingOrb.getOrbColor()==newShootingOrb.getOrbColor() && shootingOrb.getDistinguishingInt()>distinguishingInt){
                distinguishingInt = shootingOrb.getDistinguishingInt();
            }
        }
        distinguishingInt++;
        newShootingOrb.setDistinguishingInt(distinguishingInt);
        shootingOrbs.add(newShootingOrb);
        cumulativeShotsFired ++;
    }
    public void setAddThunderOrbs(List<Orb> newThunderOrbs){
        for(Orb orb : newThunderOrbs){
            orb.setOrbAnimationState(Orb.OrbAnimationState.THUNDERING);
        }
        thunderOrbs.addAll(newThunderOrbs);
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
    public SynchronizedList<Orb> getTransferInOrbs(){
        return transferInOrbs;
    }
    public Orb[] getDeathOrbs(){
        return deathOrbs;
    }
    public SynchronizedComparable<TeamState> getTeamState(){
        return teamState;
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
            for(Orb orb : transferInOrbs.getData()) orb.drawSelf(orbDrawer, vibrationOffset);

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
                    if(orbArray.getData()[i][j].equals(NULL)) continue;
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
                else if(orbArray.getData()[i][j].equals(NULL)) System.out.print(' ');
                else System.out.print(orbArray.getData()[i][j].getOrbColor().getSymbol());
            }
            System.out.print('\n');
        }
    }

    public boolean isDeathOrbsEmpty(){
        for (Orb orb : deathOrbs){
            if(!orb.equals(NULL)) return false;
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

            synchronized (synchronizer){ // The application thread might be in the middle of drawing the orb array when the next puzzle in a set is loaded.
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
                synchronized (synchronizer){ // The application thread might be in the middle of drawing the orb array when the next puzzle in a set is loaded.
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

        // Determine whether the new row will have "odd" or "even" placement:
        int i = 1;
        int newRowOffset = 0;
        for(int j=0; j<orbArray.getData()[i].length; j++){
            if(!orbArray.getData()[i][j].equals(NULL)){
                newRowOffset = 1-j%2;
                break;
            }
        }

        // todo: use System.arrayCopy
        // Move the existing array down 1 index:
        synchronized (synchronizer){ // The application thread might be in the middle of drawing the orbArray and transfer Orbs.

            i = orbArray.getData().length-1;
            for(int j=0; j<orbArray.getData()[i].length; j++){
                if(!orbArray.getData()[i][j].equals(NULL)){
                    orbArray.getData()[i][j].setIJ(i+1,j);
                    deathOrbs[j] = orbArray.getData()[i][j];
                    orbArray.setModify(i, j, NULL);
                }
            }
            for(i=orbArray.getData().length-2; i>=0; i--){
                for(int j=0; j<orbArray.getData()[i].length; j++){
                    if(!orbArray.getData()[i][j].equals(NULL)) orbArray.getData()[i][j].setIJ(i+1, j);
                    orbArray.setModify(i+1, j, orbArray.getData()[i][j]);
                }
            }

            // Move all transferring orbs down 1 index:
            for(Orb transferOrb : transferInOrbs.getData()){
                transferOrb.setIJ(transferOrb.getI()+1,transferOrb.getJ());
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
    }

    public int getLowestOccupiedRow(Orb[][] orbArray, Orb[] deathOrbs) {
        for(Orb orb : deathOrbs){
            if (!orb.equals(NULL)) return ARRAY_HEIGHT;
        }

        for (int i = ARRAY_HEIGHT-1; i>=0; i--) {
            Orb[] row = orbArray[i];
            for (int j=arrayWidth-1; j >= 0; j--) {
                if (!row[j].equals(NULL)) {
                    return i;
                }
            }
        }

        // otherwise, there are no orbs on the board, so return -1
        return -1;
    }

    // called 24 times per second to update all animations and Orb positions for the next animation frame.
    public Outcome tick(boolean isHost){
        // Most of the computation work is done in here:
        Outcome outcome = simulateOrbs(orbArray.getData(), deathOrbs, shootingOrbs, 1/(double) DATA_FRAME_RATE);

        /* Apply the outcome of simulateOrbs: */
        // Advance shooting Orbs:
        int index = 0;
        for(Orb shootingOrb : shootingOrbs){
            shootingOrb.setAngle(outcome.newShootingOrbAngles.get(index));
            shootingOrb.setSpeed(outcome.newShootingOrbSpeeds.get(index));
            index++;
        }
        synchronized (synchronizer){ // The application thread might be in the middle of drawing the shooting Orbs, and uses the orb's (x,y) position. Fields that would affect the display of the victory screen are also being updated in this code block.
            index=0;
            for(Orb shootingOrb : shootingOrbs) {
                shootingOrb.relocate(outcome.newShootingOrbPositions.get(index).getX(), outcome.newShootingOrbPositions.get(index).getY());
                index++;
            }

            // Snap shooting Orbs that have collided (but NOT the ones that will also burst or drop!!!):
            for(Map.Entry<Orb, PointInt> entry : outcome.shootingOrbsToSnap.entrySet()){
                Orb orb = entry.getKey();
                if(outcome.shootingOrbsToBurst.contains(orb) || outcome.shootingOrbsToDrop.contains(orb)) continue; // we don't want to add the Orb to the array if it will also be added to the burstingOrbs or droppingOrbs list.
                int i = entry.getValue().i;
                int j = entry.getValue().j;
                orb.setIJ(i, j);
                if(validArrayCoordinates(i, j, orbArray.getData())) orbArray.setModify(i,j,orb);
                else if(validDeathOrbsCoordinates(i, j, deathOrbs)) deathOrbs[j] = orb;
                    // If the snap coordinates are somehow off the edge of the array, then just burst the orb. This should
                    // never happen, but... you never know.
                else{
                    System.err.println("Invalid snap coordinates [" + i + ", " + j + "] detected. Bursting orb.");
                    System.err.println("   shooter orb info: " + orb.getOrbColor() + " " + orb.getOrbAnimationState() + " x=" + orb.getXPos() + " y=" + orb.getYPos() + " speed=" + orb.getSpeed());
                    System.err.println("   array orb info: " + orb.getOrbColor() + " " + orb.getOrbAnimationState() + "i=" + orb.getI() + " j=" + orb.getJ() + " x=" + orb.getXPos() + " y=" + orb.getYPos());
                    outcome.shootingOrbsToBurst.add(orb);
                }
                shootingOrbs.remove(orb);
            }

            // Misc:
            if (outcome.burstOrbsToTransfer.size() > largestGroupExplosion) {
                largestGroupExplosion = outcome.burstOrbsToTransfer.size();
            }
            cumulativeOrbsDropped += (outcome.arrayOrbsToDrop.size() + outcome.shootingOrbsToDrop.size());
            cumulativeOrbsBurst += outcome.arrayOrbsToBurst.size();
        }

        // Burst shooting Orbs and array Orbs:
        if(!outcome.shootingOrbsToBurst.isEmpty() || !outcome.arrayOrbsToBurst.isEmpty()){
            outcome.soundEffectsToPlay.add(SoundEffect.EXPLOSION);
            synchronized (synchronizer) { // The application thread might be in the middle of drawing the shooting Orbs, bursting Orbs, arrayOrbs, deathOrbs, or dropping Orbs.
                shootingOrbs.removeAll(outcome.shootingOrbsToBurst);
                burstingOrbs.addAll(outcome.shootingOrbsToBurst);
                for(Orb orb : outcome.shootingOrbsToBurst) orb.setOrbAnimationState(Orb.OrbAnimationState.IMPLODING);
                for(Orb orb : outcome.arrayOrbsToBurst){
                    if(validArrayCoordinates(orb, orbArray.getData())) orbArray.setModify(orb.i,orb.j,NULL);
                    else deathOrbs[orb.getJ()] = NULL;
                    orb.setOrbAnimationState(Orb.OrbAnimationState.IMPLODING);
                    burstingOrbs.add(orb);
                }
            }
        }

        // drop floating orbs:
        synchronized(synchronizer){
            shootingOrbs.removeAll(outcome.shootingOrbsToDrop);
            droppingOrbs.addAll(outcome.shootingOrbsToDrop);
            for (Orb orb : outcome.arrayOrbsToDrop) {
                droppingOrbs.add(orb);
                orbArray.setModify(orb.i, orb.j, NULL);
            }
        }

        synchronized(synchronizer){ // The application thread might be in the middle of drawing visual flourishes, bursting Orbs, array Orbs, thunder Orbs, dropping Orbs, transferring Orbs, shootingOrbs, or ammunition Orbs.
            // Advance the animation frame of the existing visual flourishes:
            visualFlourishes.removeIf(visualFlourish -> visualFlourish.tick());

            // If orbs were dropped or a sufficient number were burst, add visual flourishes for the orbs to be transferred:
            if(!outcome.arrayOrbsToDrop.isEmpty() || !outcome.burstOrbsToTransfer.isEmpty()){
                outcome.soundEffectsToPlay.add(SoundEffect.DROP);
                for(Orb orb : outcome.arrayOrbsToDrop){
                    visualFlourishes.add(new Animation(AnimationName.EXCLAMATION_MARK, orb.getXPos(), orb.getYPos(), PlayOption.PLAY_ONCE_THEN_VANISH));
                }
                for(Orb orb : outcome.deathOrbsToDrop){
                    visualFlourishes.add(new Animation(AnimationName.EXCLAMATION_MARK, orb.getXPos(), orb.getYPos(), PlayOption.PLAY_ONCE_THEN_VANISH));
                }
                for(Orb orb : outcome.burstOrbsToTransfer){
                    visualFlourishes.add(new Animation(AnimationName.EXCLAMATION_MARK, orb.getXPos(), orb.getYPos(), PlayOption.PLAY_ONCE_THEN_VANISH));
                }
                changeAddTransferOutOrbs(outcome.burstOrbsToTransfer);
            }

            // Advance the animation frame of existing bursting orbs:
            List<Orb> orbsToRemove = advanceBurstingOrbs(burstingOrbs);
            burstingOrbs.removeAll(orbsToRemove);

            // Advance the animation frame of the electrified orbs:
            advanceArrayOrbs();

            // Advance the animation and audio of the thunder orbs:
            orbsToRemove = advanceThunderOrbs();
            thunderOrbs.removeAll(orbsToRemove);

            // Advance existing dropping orbs:
            List<Orb> orbsToThunder = advanceDroppingOrbs();
            droppingOrbs.removeAll(orbsToThunder);

            // If orbs dropped off the bottom of the PlayPanel, add them to the orbsToTransfer list AND the thunderOrbs list.
            if(!orbsToThunder.isEmpty()){
                for(Orb orb : orbsToThunder) changeAddTransferOutOrbs(Collections.singletonList(new Orb(orb))); // We must create a copy so that we can change the animationEnum without affecting the transferOutOrbs.
                setAddThunderOrbs(orbsToThunder);
            }

            // Advance the existing transfer-in Orbs, adding visual flourishes if they're done:
            List<Orb> transferOrbsToSnap = advanceTransferringOrbs();
            snapTransferOrbs(outcome, transferOrbsToSnap, orbArray, deathOrbs, outcome.soundEffectsToPlay, visualFlourishes, transferInOrbs);

            // If there are no orbs connected to the ceiling, then this team has finished the puzzle. Move on to the next one or declare victory
            if(isPuzzleCleared(orbArray.getData())){
                shootingOrbs.clear();
                if(puzzleUrl.substring(0,6).equals("RANDOM")){ // this was a random puzzle. Declare victory
                    if(isHost) teamState.changeTo(TeamState.VICTORIOUS);
                    else teamState.setTo(TeamState.VICTORIOUS);
                }
                else{ // This was a pre-built puzzle. Load the next one, if there is one.
                    int currentIndex = Integer.parseInt(puzzleUrl.substring(puzzleUrl.length()-2));
                    puzzleUrl = String.format("%s%02d",puzzleUrl.substring(0,puzzleUrl.length()-2),currentIndex+1);
                    if(!initializeOrbArray(puzzleUrl)){ // There was no next puzzle. Declare victory.
                        if(isHost) teamState.changeTo(TeamState.VICTORIOUS);
                        else teamState.setTo(TeamState.VICTORIOUS);
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
        }


        // reset shotsUntilNewRow
        if(shotsUntilNewRow<=0) shotsUntilNewRow = SHOTS_BETWEEN_DROPS*players.size() + shotsUntilNewRow;

        // If the player has fired a sufficient number of times, then add a new row of orbs:
        shotsUntilNewRow-=outcome.shootingOrbsToSnap.size();
        if(shotsUntilNewRow==1) outcome.soundEffectsToPlay.add(SoundEffect.ALMOST_NEW_ROW);
        else if(shotsUntilNewRow<=0){
            addNewRow();
            outcome.soundEffectsToPlay.add(SoundEffect.NEW_ROW);
        }

        // check to see whether this team has lost due to uncleared deathOrbs:
        if(!isDeathOrbsEmpty()){
            synchronized (synchronizer){ // The application might be in the middle of drawing the characters.
                if(isHost) teamState.changeTo(TeamState.DEFEATED);
                else teamState.setTo(TeamState.DEFEATED);
            }
        }

        // update each player's animation state:
        int lowestRow = getLowestOccupiedRow(orbArray.getData(), deathOrbs);
        synchronized (synchronizer){ // The application might be in the middle of drawing the characters, and tick() can change characterAnimation.
            for(Player player : players) {
                if (player instanceof BotPlayer) continue; // we've already ticked() the BotPlayers.
                player.tick(lowestRow);
            }
        }

        removeStrayOrbs();
        return outcome;
    }

    private boolean isPuzzleCleared(Orb[][] orbArray){
        Orb[] firstRow = orbArray[0];
        for(int j=0; j<arrayWidth; j++) {
            if(!firstRow[j].equals(NULL)) return false;
        }
        return true;
    }

    // removes orbs that have wandered off the edges of the canvas. This should only ever happen with dropping orbs, but
    // shooting orbs are also checked, just in case.
    private void removeStrayOrbs(){
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
        synchronized (synchronizer){ // The application thread might be in the middle of drawing the shooting Orbs.
            shootingOrbs.removeAll(orbsToRemove);
        }
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

    private void advanceArrayOrbs(){
        Orb[][] cachedOrbArray = orbArray.getData();
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<arrayWidth; j++) {
                Orb orb = cachedOrbArray[i][j];
                if(!orb.equals(NULL)) orb.tick();
            }
        }
    }

    private List<Orb> advanceTransferringOrbs(){
        List<Orb> transferOrbsToSnap = new LinkedList<>();
        for(Orb orb : transferInOrbs.getData()){
            if (orb.tick()){
                transferOrbsToSnap.add(orb);
            }
        }
        return transferOrbsToSnap;
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

    // note to self: watch out for snapping and bursting shootingOrbs, as well as arrayOrbsToBurst and deathOrbsToBurst that were added in an earlier loop iteration.
    public void findPatternCompletions(Outcome outcome, Orb[][] orbArray, Orb[] deathOrbs){
        for(Map.Entry<Orb, PointInt> entry : outcome.shootingOrbsToSnap.entrySet()){
            Orb orb = entry.getKey();

            // find all connected orbs of the same color
            DFSresult connectedOrbs = cumulativeDepthFirstSearch(outcome, Collections.singletonList(orb), orbArray, deathOrbs, FilterOption.SAME_COLOR);

            // determine whether there are enough connected Orbs to burst them:
            if(connectedOrbs.size() >= 3){
                // add the orbs to the appropriate OrbsToBurst Set:
                outcome.arrayOrbsToBurst.addAll(connectedOrbs.connectedArrayOrbs);
                outcome.deathOrbsToBurst.addAll(connectedOrbs.connectedDeathOrbs);
                outcome.shootingOrbsToBurst.addAll(connectedOrbs.connectedShootingOrbs);

                // If there are a sufficient number grouped together, then add a transfer-out Orb of the same color:
                int numTransferOrbs;
                if((numTransferOrbs = (connectedOrbs.size()-3)/2) > 0) {
                    outcome.soundEffectsToPlay.add(SoundEffect.DROP);
                    Iterator<Orb> orbIterator = connectedOrbs.iterator();
                    for(int k=0; k<numTransferOrbs; k++){
                        Orb orbToTransfer = orbIterator.next();
                        outcome.burstOrbsToTransfer.add(new Orb(orbToTransfer)); // add a copy of the orb, so we can change the animationEnum without messing up the original (which still needs to burst).
                    }
                }
            }
        }
    }

    // find all orbs connected to the ceiling:
    public void findConnectedOrbs(Outcome outcome, Orb[][] orbArray, Orb[] deathOrbs){
        List<Orb> topRow = new LinkedList<>();
        for(int j=0; j<arrayWidth; j++){
            if(!orbArray[0][j].equals(NULL)){
                if (!outcome.arrayOrbsToBurst.contains(orbArray[0][j])) topRow.add(orbArray[0][j]);
            }
            else{
                Orb shootingOrb = findShootingOrbToSnap(outcome,0,j);
                if(shootingOrb!=null && !outcome.shootingOrbsToBurst.contains(shootingOrb)) topRow.add(shootingOrb);
            }
        }
        outcome.connectedSets = cumulativeDepthFirstSearch(outcome, topRow, orbArray, deathOrbs, FilterOption.ALL);
    }

    // note to self: watch out for snapping, bursting, AND dropping orbs.
    public NeighborSets getNeighbors(Outcome outcome, Orb source, Orb[][] array, Orb[] deathOrbs){
        NeighborSets neighborSets = new NeighborSets();
        int i;
        int j;
        if(source.i==-1){ // source Orb is a shootingOrb.
            PointInt pos = outcome.shootingOrbsToSnap.get(source);
            i = pos.i;
            j = pos.j;
        }
        else if(source.i<ARRAY_HEIGHT){ // source Orb is an arrayOrb
            i = source.i;
            j = source.j;
        }
        else{ // source Orb is a deathOrb
            i = source.i;
            j = source.j;
        }

        //test all possible neighbors for valid coordinates
        int[] iTests = {i-1, i-1, i, i, i+1, i+1};
        int[] jTests = {j-1, j+1, j-2, j+2, j-1, j+1};
        for(int k=0; k<iTests.length; k++){
            if (outcome.shootingOrbsToSnap.containsValue(new PointInt(iTests[k], jTests[k]))){
                Orb shootingOrb = findShootingOrbToSnap(outcome, iTests[k],jTests[k]);
                if(!outcome.shootingOrbsToBurst.contains(shootingOrb) && !outcome.shootingOrbsToDrop.contains(shootingOrb)){
                    neighborSets.neighborShootingOrbs.add(shootingOrb);
                }
            }
            else if(validArrayCoordinates(iTests[k],jTests[k],array) && (!array[iTests[k]][jTests[k]].equals(NULL) && !isArrayOrbBursting(outcome, iTests[k],jTests[k]))) {
                neighborSets.neighborArrayOrbs.add(array[iTests[k]][jTests[k]]);
            }
            else if(validDeathOrbsCoordinates(iTests[k],jTests[k],deathOrbs) && (!deathOrbs[jTests[k]].equals(NULL) && !isDeathOrbsBursting(outcome, iTests[k],jTests[k]))) {
                neighborSets.neighborDeathOrbs.add(deathOrbs[jTests[k]]);
            }
        }
        return neighborSets;
    }

    public Orb findShootingOrbToSnap(Outcome outcome, int i, int j){
        PointInt pointOfInterest = new PointInt(i,j);
        for(Map.Entry<Orb, PointInt> entry : outcome.shootingOrbsToSnap.entrySet()){
            if(entry.getValue().equals(pointOfInterest)){
                return entry.getKey();
            }
        }
        return null;
    }

    // todo: This is inefficient, considering it is called 6 times in getNeighbors. Consider using an array instead, for quick lookup with (i,j) coordinates.
    public boolean isArrayOrbBursting(Outcome outcome, int i, int j){
        for(Orb orb : outcome.arrayOrbsToBurst){
            if(orb.i==i && orb.j==j){
                return true;
            }
        }
        return false;
    }

    // todo: This is inefficient, considering it is called 6 times in getNeighbors. Consider using an array instead, for quick lookup with (i,j) coordinates.
    public boolean isDeathOrbsBursting(Outcome outcome, int i, int j){
        for(Orb orb : outcome.deathOrbsToBurst){
            if(orb.i==i && orb.j==j){
                return true;
            }
        }
        return false;
    }

    // Finds floating orbs and drops them.
    public void findFloatingOrbs(Outcome outcome, Orb[][] orbArray, Orb[] deathOrbs){
        // any orbs in the array that are not in connectedSets are floating.
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<arrayWidth; j++){
                Orb arrayOrb = orbArray[i][j];
                if(!arrayOrb.equals(NULL) && !outcome.arrayOrbsToBurst.contains(arrayOrb) && !outcome.connectedSets.connectedArrayOrbs.contains(arrayOrb)){
                    outcome.arrayOrbsToDrop.add(arrayOrb);
                }
            }
        }

        // any orbs in the deathOrbs array that are not in connectedSets are floating
        for(int j=0; j<arrayWidth; j++){
            Orb deathOrb = deathOrbs[j];
            if(!deathOrb.equals(NULL) && !outcome.deathOrbsToBurst.contains(deathOrb) && !outcome.connectedSets.connectedDeathOrbs.contains(deathOrb)){
                outcome.arrayOrbsToDrop.add(deathOrb);
            }
        }

        // any snapped shootingOrbs that are not in connectedSets are floating
        for(Map.Entry<Orb,PointInt> entry : outcome.shootingOrbsToSnap.entrySet()){
            Orb shootingOrb = entry.getKey();
            if(!outcome.shootingOrbsToBurst.contains(shootingOrb) && !outcome.connectedSets.connectedShootingOrbs.contains(shootingOrb)){
                outcome.shootingOrbsToDrop.add(shootingOrb);
            }
        }
    }

    private class DFSresult{
        Set<Orb> connectedArrayOrbs = new HashSet<>();
        Set<Orb> connectedDeathOrbs = new HashSet<>();
        Set<Orb> connectedShootingOrbs = new HashSet<>();

        public int size(){
            return connectedArrayOrbs.size() + connectedDeathOrbs.size() + connectedShootingOrbs.size();
        }

        public Iterator<Orb> iterator(){
            return getAll().iterator();
        }

        public Set<Orb> getAll(){
            Set<Orb> combinedSet = new HashSet<>();
            combinedSet.addAll(connectedArrayOrbs);
            combinedSet.addAll(connectedDeathOrbs);
            combinedSet.addAll(connectedShootingOrbs);
            return combinedSet;
        }
    }

    // todo: consider combining DFSresult and NeighborSets into one class.
    public class NeighborSets{
        Set<Orb> neighborArrayOrbs = new HashSet<>();
        Set<Orb> neighborDeathOrbs = new HashSet<>();
        Set<Orb> neighborShootingOrbs = new HashSet<>();

        public Set<Orb> getAll(){
            Set<Orb> combinedSet = new HashSet<>();
            combinedSet.addAll(neighborArrayOrbs);
            combinedSet.addAll(neighborDeathOrbs);
            combinedSet.addAll(neighborShootingOrbs);
            return combinedSet;
        }

        public boolean isEmpty(){
            return neighborArrayOrbs.isEmpty() && neighborDeathOrbs.isEmpty() && neighborShootingOrbs.isEmpty();
        }
    }

    private DFSresult cumulativeDepthFirstSearch(Outcome outcome, Collection<Orb> sources, Orb[][] orbArray, Orb[] deathOrbs, FilterOption filter) {
        DFSresult result = new DFSresult();

        // A boolean orbArray that has the same size as the orbArray + deathOrbs, to mark orbs as "examined"
        Boolean[][] examined = new Boolean[ARRAY_HEIGHT+1][arrayWidth];
        for(int i=0; i<ARRAY_HEIGHT+1; i++){
            for(int j=0; j<arrayWidth; j++){
                examined[i][j] = false;
            }
        }

        // A stack containing the "active" elements to be examined next
        Deque<Orb> active = new LinkedList<>();

        // Add the sources to the active list and mark them as "examined"
        for(Orb source : sources){
            active.push(source);
            if(source.i==-1){ // the source is a shooting orb
                PointInt pos = outcome.shootingOrbsToSnap.get(source);
                examined[pos.i][pos.j] = true;
                result.connectedShootingOrbs.add(source);
            }
            else if(source.i<ARRAY_HEIGHT){ // the source is an arrayOrb
                examined[source.i][source.j] = true;
                result.connectedArrayOrbs.add(source);
            }
            else{ // the source is a deathOrb
                examined[source.i][source.j] = true;
                result.connectedDeathOrbs.add(source);
            }
        }

        // Do a depth-first search
        while (!active.isEmpty()) {
            Orb activeOrb = active.pop();
            OrbColor sourceColor = activeOrb.getOrbColor();
            NeighborSets neighborSets = getNeighbors(outcome, activeOrb, orbArray, deathOrbs);

            // todo: yuck! Duplicate code. See if there's an efficient way to fix this. The problem is that we want to call add() on a different set in each case, deep within the for-if-if construct.
            for (Orb arrayNeighbor : neighborSets.neighborArrayOrbs){
                PointInt pos = arrayNeighbor;
                if (!examined[pos.i][pos.j]) {
                    if(passesFilter(filter, arrayNeighbor, sourceColor)){
                        active.push(arrayNeighbor);
                        result.connectedArrayOrbs.add(arrayNeighbor);
                    }
                    examined[pos.i][pos.j] = true;
                }
            }

            for (Orb deathNeighbor : neighborSets.neighborDeathOrbs){
                PointInt pos = deathNeighbor;
                if (!examined[pos.i][pos.j]) {
                    if(passesFilter(filter, deathNeighbor, sourceColor)){
                        active.push(deathNeighbor);
                        result.connectedDeathOrbs.add(deathNeighbor);
                    }
                    examined[pos.i][pos.j] = true;
                }
            }

            for (Orb shootingNeighbor : neighborSets.neighborShootingOrbs){
                PointInt pos = outcome.shootingOrbsToSnap.get(shootingNeighbor);
                if (!examined[pos.i][pos.j]) {
                    if(passesFilter(filter, shootingNeighbor, sourceColor)){
                        active.push(shootingNeighbor);
                        result.connectedShootingOrbs.add(shootingNeighbor);
                    }
                    examined[pos.i][pos.j] = true;
                }
            }
        }
        return result;
    }

    private boolean passesFilter(FilterOption filter, Orb neighbor, OrbColor sourceColor){
        boolean passesFilter = false;
        switch(filter){
            case ALL:
                passesFilter = true;
                break;
            case SAME_COLOR:
                if(neighbor.getOrbColor() == sourceColor) passesFilter = true;
                break;
        }
        return passesFilter;
    }

    private void addNodeToResult(PointInt node, DFSresult result, Outcome outcome, Orb[][] orbArray){
        if(validArrayCoordinates(node, orbArray) && !orbArray[node.i][node.j].equals(NULL)) result.connectedArrayOrbs.add(orbArray[node.i][node.j]);
        else if(validDeathOrbsCoordinates(node, deathOrbs) && !deathOrbs[node.j].equals(NULL)) result.connectedDeathOrbs.add(deathOrbs[node.j]);
        else{
            for(Map.Entry<Orb, PointInt> entry2 : outcome.shootingOrbsToSnap.entrySet()){
                if(entry2.getValue().i==node.i && entry2.getValue().j==node.j){
                    result.connectedShootingOrbs.add(entry2.getKey());
                    break;
                }
            }
        }
    }

    /*private void cumulativeDepthFirstSearch(Outcome outcome, PointInt source, OrbColor sourceColor, Set<PointInt> matchesSoFar, Orb[][] orbArray, FilterOption filter) {
        // A boolean orbArray that has the same size as the orbArray, to mark orbs as "examined"
        Boolean[][] examined = new Boolean[orbArray.length][orbArray[0].length];
        for(int i=0; i<orbArray.length; i++){
            for(int j=0; j<orbArray[i].length; j++){
                examined[i][j] = false;
            }
        }

        // A stack containing the "active" elements to be examined next
        Deque<PointInt> active = new LinkedList<>();

        // Add the source Orb to the active list and mark it as "examined"
        active.push(source);
        if(validArrayCoordinates(source, examined)) examined[source.i][source.j] = true; // deathOrbs have "invalid" coordinates, so we have to check whether the coordinates are valid.

        // Mark everything in the starting set as "examined"
        for(PointInt match : matchesSoFar) examined[match.i][match.j] = true;

        // Do a depth-first search
        while (!active.isEmpty()) {
            PointInt activeNode = active.pop();
            matchesSoFar.add(activeNode);
            List<PointInt> neighbors = getNeighbors(outcome.shootingOrbsToSnap, outcome, activeNode, orbArray); // recall: neighbors does not contain any death Orbs. Only orbArray Orbs.
            for (PointInt neighbor : neighbors) {
                if (!examined[neighbor.i][neighbor.j]) {
                    // apply the filter option
                    boolean passesFilter = false;
                    switch(filter){
                        case ALL:
                            passesFilter = true;
                            break;
                        case SAME_COLOR:
                            if(orbArray[neighbor.i][neighbor.j].getOrbColor() == sourceColor) passesFilter = true;
                            break;
                    }
                    if(passesFilter){
                        active.add(neighbor);
                    }
                    examined[neighbor.i][neighbor.j] = true;
                }
            }
        }
    }*/










    /* *********************************************** SIMULATION *********************************************** */

    public class Outcome{
        public List<Point2D> newShootingOrbPositions = new LinkedList<>(); // AFTER snapping.
        public List<Double> newShootingOrbAngles = new LinkedList<>(); // AFTER all collisions.
        public List<Double> newShootingOrbSpeeds = new LinkedList<>(); // AFTER all collisions.
        public List<Collision> collisions = new LinkedList<>(); // All collisions occurring this frame go here. Includes collisions with walls and with other shooting orbs (if S-S collisions are turned on)
        public HashMap<Orb,PointInt> shootingOrbsToSnap = new HashMap<>(); // The PointInt contains the array coordinates the Orb will snap to.
        public List<Orb> shootingOrbsToBurst = new LinkedList<>();
        public List<Orb> arrayOrbsToBurst = new LinkedList<>();
        public List<Orb> deathOrbsToBurst = new LinkedList<>();
        public List<Orb> arrayOrbsToDrop = new LinkedList<>();
        public List<Orb> deathOrbsToDrop = new LinkedList<>();
        public List<Orb> shootingOrbsToDrop = new LinkedList<>();
        public List<Orb> burstOrbsToTransfer = new LinkedList<>();
        public DFSresult connectedSets; // All Orbs that are connected to the ceiling at the end of the frame will be put in here. Used for finding floating orbs.
        public Set<SoundEffect> soundEffectsToPlay = new HashSet<>();

        public Outcome(List<Orb> shootingOrbs){
            for(Orb orb : shootingOrbs){
                newShootingOrbPositions.add(new Point2D(orb.getXPos(), orb.getYPos()));
                newShootingOrbAngles.add(orb.getAngle());
                newShootingOrbSpeeds.add(orb.getSpeed());
            }

        }
    }

    public Outcome simulateOrbs(Orb[][] orbArray, Orb[] deathOrbs, List<Orb> shootingOrbs, double deltaTime){
        Outcome outcome = new Outcome(shootingOrbs);

        // Advance shooting orbs and detect collisions:
        advanceShootingOrbs(outcome, orbArray, shootingOrbs, deltaTime); // Updates model

        // Snap any landed shooting orbs into place on the orbArray (or deathOrbs array):
        snapOrbs(outcome, orbArray, shootingOrbs);

        // Determine whether any of the snapped orbs cause any orbs to burst:
        findPatternCompletions(outcome, orbArray, deathOrbs);

        // Drop floating orbs
        findConnectedOrbs(outcome, orbArray, deathOrbs);
        if(!outcome.arrayOrbsToBurst.isEmpty()){ // floating orbs are possible only if array orbs have burst.
            findFloatingOrbs(outcome, orbArray, deathOrbs);
        }

        return outcome;
    }

    // Initiated 24 times per second, and called recursively.
    // advances all shooting orbs, detecting collisions along the way and stopping orbs that collide with arrayOrbs or
    // with the ceiling.
    // A single recursive call to this function might only advance the shooting orbs a short distance, but when all the
    // recursive calls are over, all shooting orbs will have been advanced one full frame.
    // Returns a list of all orbs that will attempt to snap; some of them may end up bursting instead during the call to
    // snapOrbs if (and only if) s-s collisions are turned off.
    // Note: recall that the y-axis points downward and shootingOrb.getCannonAngle() returns a negative value.
    public void advanceShootingOrbs(Outcome outcome, Orb[][] orbArray, List<Orb> shootingOrbs, double timeRemainingInFrame) {
        // Put all possible collisions in here. If a shooter orb's path this frame would put it on a collision course
        // with the ceiling, a wall, or an array orb, then that collision will be added to this list, even if there is
        // another orb in the way.
        List<Collision> possibleCollisionPoints = new LinkedList<>();

        for(int index=0; index<shootingOrbs.size(); index++){
            Orb shootingOrb = shootingOrbs.get(index);
            double speed = outcome.newShootingOrbSpeeds.get(index);
            if(abs(speed)<0.001) continue; // Skip ahead if it appears that this orb is stationary.
            double angle = outcome.newShootingOrbAngles.get(index);
            double x0 = outcome.newShootingOrbPositions.get(index).getX();
            double y0 = outcome.newShootingOrbPositions.get(index).getY();
            double distanceToTravel = speed * timeRemainingInFrame;
            double x1P = distanceToTravel * Math.cos(angle); // Theoretical x-position of the shooting orb after it is advanced, relative to x0.
            double y1P = distanceToTravel * Math.sin(angle); // Theoretical y-position of the shooting orb after it is advanced, relative to y0
            double tanAngle = y1P/x1P;
            double onePlusTanAngleSquared = 1+Math.pow(tanAngle, 2.0); // cached for speed

            // Cycle through the Orb array from bottom to top until we find possible collision points on some row:
            boolean collisionsFoundOnRow = false;
            for (int i = ARRAY_HEIGHT; i >= 0; i--) {
                for (int j=0; j<arrayWidth; j ++) {
                    Orb arrayOrDeathOrb;
                    if(i==ARRAY_HEIGHT) arrayOrDeathOrb = deathOrbs[j];
                    else arrayOrDeathOrb = orbArray[i][j];
                    if (!arrayOrDeathOrb.equals(NULL)) { // note to self: we don't need to check for snapping/dropping shooting Orbs or bursting/dropping shooting/array/death Orbs yet.
                        double xAP = arrayOrDeathOrb.getXPos() - x0;
                        double yAP = arrayOrDeathOrb.getYPos() - y0;
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
                                possibleCollisionPoints.add(new Collision(shootingOrb, arrayOrDeathOrb, timeToCollision));
                                collisionsFoundOnRow = true;
                            }
                            else if (distanceToCollisionNSquared < distanceToTravel * distanceToTravel) {
                                double timeToCollision = Math.sqrt(distanceToCollisionNSquared) / speed;
                                possibleCollisionPoints.add(new Collision(shootingOrb, arrayOrDeathOrb, timeToCollision));
                                collisionsFoundOnRow = true;
                            }
                        }
                    }
                    // note to self: don't break out of the inner loop if collisionFoundOnRow == true. There may be
                    // multiple Orbs in the shootingOrb's path on this row and it's not necessarily the case that the
                    // first one we find is the closest one.

                    // another note to self: If collisions between shooting orbs are turned on, then also consider the
                    // possibility that an Orb may be traveling downwards. For such orbs, we want to traverse the rows
                    // in the other order: from 0 to ARRAY_HEIGHT-1.
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
            for(int i=0; i<shootingOrbs.size(); i++){
                double angle = outcome.newShootingOrbAngles.get(i);
                double distanceToTravel = outcome.newShootingOrbSpeeds.get(i) * soonestCollisionTime;
                outcome.newShootingOrbPositions.set(i,outcome.newShootingOrbPositions.get(i).add(distanceToTravel * Math.cos(angle), distanceToTravel * Math.sin(angle)));
            }

            // If there was a collision with a wall, then just reflect the shooter orb's angle.
            if (soonestCollision.arrayOrb == Orb.WALL) {
                outcome.soundEffectsToPlay.add(SoundEffect.CHINK);
                Orb shooterOrb = soonestCollision.shooterOrb;
                int i = shootingOrbs.indexOf(shooterOrb);
                outcome.newShootingOrbAngles.set(i, PI - outcome.newShootingOrbAngles.get(i));
            }

            // If the collision is between two shooter orbs, compute new angles and speeds. If the other shooting orb is
            // in the process of snapping, then burst this shooting orb.
            else if (shootingOrbs.contains(soonestCollision.arrayOrb)) {
                // Todo: do this.
            }

            // If the collision was with the ceiling, set that orb's speed to zero and add it to the collisions list.
            else if(soonestCollision.arrayOrb == Orb.CEILING){
                int i = shootingOrbs.indexOf(soonestCollision.shooterOrb);
                outcome.newShootingOrbSpeeds.set(i, 0.0);
                outcome.collisions.add(soonestCollision);
            }

            // If the collision is between a shooter orb and an array orb, set that orb's speed to zero and add it to
            // the collisions list.
            else {
                int i = shootingOrbs.indexOf(soonestCollision.shooterOrb);
                outcome.newShootingOrbSpeeds.set(i, 0.0);
                outcome.collisions.add(soonestCollision);
            }

            // Recursively call this function.
            advanceShootingOrbs(outcome, orbArray, shootingOrbs, timeRemainingInFrame - soonestCollisionTime);
        }

        // If there are no more collisions, just advance all orbs to the end of the frame.
        else {
            for (int i=0; i<shootingOrbs.size(); i++){
                double angle = outcome.newShootingOrbAngles.get(i);
                double distanceToTravel = outcome.newShootingOrbSpeeds.get(i) * timeRemainingInFrame;
                //System.out.println("before advancing to end of frame: " + outcome.newShootingOrbPositions.get(i).getX() + ", " + outcome.newShootingOrbPositions.get(i).getY());
                outcome.newShootingOrbPositions.set(i,outcome.newShootingOrbPositions.get(i).add(distanceToTravel * Math.cos(angle),distanceToTravel * Math.sin(angle)));
                //System.out.println("after advancing to end of frame: " + outcome.newShootingOrbPositions.get(i).getX() + ", " + outcome.newShootingOrbPositions.get(i).getY());
            }
        }
    }

    // note to self: watch out for shootingOrbsToSnap and shootingOrbsToBurst that were added in a previous loop iteration.
    public void snapOrbs(Outcome outcome, Orb[][] orbArray, List<Orb> shootingOrbs){
        for(Collision snap : outcome.collisions){
            int iSnap;
            int jSnap;

            // Compute snap coordinates for orbs that collided with the ceiling
            if(snap.arrayOrb == Orb.CEILING){
                int offset = 0;
                for(int j=0; j<orbArray[0].length; j++){
                    if(!orbArray[0][j].equals(NULL)){
                        offset = j%2;
                        break;
                    }
                }
                int index = shootingOrbs.indexOf(snap.shooterOrb);
                double xPos = outcome.newShootingOrbPositions.get(index).getX();
                iSnap = 0;
                jSnap = 2*((int) Math.round((xPos - ORB_RADIUS)/(2*ORB_RADIUS))) + offset;
            }

            // Compute snap coordinates for orbs that collided with an array orb
            else{
                // Recompute the collision angle:
                int index = shootingOrbs.indexOf(snap.shooterOrb);
                double shooterX = outcome.newShootingOrbPositions.get(index).getX();
                double shooterY = outcome.newShootingOrbPositions.get(index).getY();
                double arrayX = snap.arrayOrb.getXPos();
                double arrayY = snap.arrayOrb.getYPos();
                double collisionAngleDegrees = Math.toDegrees(Math.atan2(shooterY-arrayY, shooterX-arrayX));

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

            // If s-s collisions are turned off, it is possible for two shooter orbs to try to snap to the same
            // location. If that's the case, then burst the second orb that attempts to snap there.
            boolean clear = true;
            for(PointInt snappedOrbPos : outcome.shootingOrbsToSnap.values()){
                if(snappedOrbPos.i==iSnap && snappedOrbPos.j==jSnap){
                    clear = false;
                    break;
                }
            }
            if(!clear){
                outcome.shootingOrbsToBurst.add(snap.shooterOrb);
            }
            else{
                outcome.shootingOrbsToSnap.put(snap.shooterOrb, new PointInt(iSnap, jSnap));
                outcome.soundEffectsToPlay.add(SoundEffect.PLACEMENT);
            }
        }
    }






    /* *********************************************** CHANGERS *********************************************** */

    public void burstArrayOrbs(List<Orb> newBurstingOrbs, SynchronizedArray<Orb> orbArray, Orb[] deathOrbs, List<Orb> burstingOrbs){

    }

    public void transferOrbs(List<Orb> transferOutOrbs, Collection<Orb> transferInOrbs, Random randomTransferOrbGenerator, Orb[][] orbArray, Orb[] deathOrbs){
        // Make a deep copy of the orbs to be transferred. We can't place the same orb instance in 2 PlayPanels
        List<Orb> newTransferOrbs = deepCopyOrbList(transferOutOrbs);

        // The new transfer orbs need to be placed appropriately. Find open, connected spots:
        int offset = 0;
        for(int j=0; j<arrayWidth; j++){
            if(!orbArray[0][j].equals(NULL)){
                offset = j%2;
                break;
            }
        }

        List<PointInt> openSpots = new LinkedList<>();
        Orb tempOrb = new Orb(OrbColor.BLUE,0,0,OrbAnimationState.STATIC);
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=offset + i%2 - 2*offset*i%2; j<orbArray[0].length; j+=2){
                if(orbArray[i][j].equals(NULL)){
                    tempOrb.setIJ(i,j);
                    if ((!getNeighbors(new Outcome(new LinkedList<>()), tempOrb, orbArray, deathOrbs).isEmpty() || i==0) && !isTransferInOrbOccupyingPosition(i,j,transferInOrbs)) {
                        openSpots.add(new PointInt(i, j));
                    }
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

    public void snapTransferOrbs(Outcome outcome, List<Orb> transferOrbsToSnap, SynchronizedArray<Orb> orbArray, Orb[] deathOrbs, Set<SoundEffect> soundEffectsToPlay, List<Animation> visualFlourishes, SynchronizedList<Orb> transferInOrbs){
        Set<Orb> allConnectedSets = outcome.connectedSets.getAll();
        for(Orb orb : transferOrbsToSnap){
            // only those orbs that would be connected to the ceiling should materialize:
            NeighborSets neighborSets = getNeighbors(outcome, orb, orbArray.getData(), deathOrbs);
            if((orb.i==0 || !Collections.disjoint(neighborSets.getAll(), allConnectedSets)) && orbArray.getData()[orb.i][orb.j].equals(NULL)){
                orbArray.setModify(orb.i,orb.j,orb);
                soundEffectsToPlay.add(SoundEffect.MAGIC_TINKLE);
                visualFlourishes.add(new Animation(AnimationName.MAGIC_TELEPORTATION, orb.getXPos(), orb.getYPos(), PlayOption.PLAY_ONCE_THEN_VANISH));
            }
        }

        // remove the snapped transfer orbs from the inbound transfer orbs list
        transferInOrbs.setRemoveAll(transferOrbsToSnap);
    }






    /* *********************************************** BOUNDS CHECKERS *********************************************** */

    public static <E> boolean validArrayCoordinates(PointInt testPoint, E[][] array)
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
    public static <E> boolean validArrayCoordinates(int i, int j, E[][] array){
        return (i>=0 && i<array.length && j>=0 && j<array[i].length);
    }

    public boolean validDeathOrbsCoordinates(PointInt testPoint, Orb[] deathOrbsArray){
        return validDeathOrbsCoordinates(testPoint.getI(), testPoint.getJ(), deathOrbsArray);
    }

    // Overloaded method to avoid the creation of a temporary object.
    public static boolean validDeathOrbsCoordinates(int iCoordinate, int jCoordinate, Orb[] deathOrbsArray){
        return (iCoordinate==ARRAY_HEIGHT && jCoordinate>=0 && jCoordinate<deathOrbsArray.length);
    }

    private boolean isTransferInOrbOccupyingPosition(int i, int j, Collection<Orb> transferInOrbs){
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

