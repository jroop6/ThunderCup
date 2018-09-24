package Classes;

import Classes.Animation.*;
import Classes.Audio.SoundEffect;
import Classes.Images.StaticBgImages;
import Classes.NetworkCommunication.PlayPanelData;
import Classes.NetworkCommunication.PlayerData;
import Classes.PlayerTypes.BotPlayer;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.*;
import java.util.List;

import static Classes.GameScene.FRAME_RATE;
import static Classes.NetworkCommunication.PlayPanelData.ARRAY_HEIGHT;
import static Classes.NetworkCommunication.PlayPanelData.ARRAY_WIDTH_PER_CHARACTER;
import static Classes.NetworkCommunication.PlayPanelData.SHOTS_BETWEEN_DROPS;
import static Classes.OrbData.*;
import static java.lang.Math.PI;

/**
 * Think of this as the Controller part of a Model-View-Controller scheme. The Model part is the PlayPanelData and the View
 * part is the Canvas (and the Orb class in a sense, because orbs know how to draw themselves).
 */
public class PlayPanel extends Pane {

    PlayPanelData playPanelData;
    PlayPanelUtility playPanelUtility = new PlayPanelUtility();

    // Constants determining PlayPanel layout:
    // TODO: the Cannon data should probably be moved to the Cannon class or CannonImages enumeration
    public static final double PLAYPANEL_WIDTH_PER_PLAYER = 690;
    public static final double PLAYPANEL_HEIGHT = 1080;
    public static final double CANNON_X_POS = ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2.0; // The x-position of the cannon's axis of rotation in a 1-player playpanel.
    public static final double CANNON_Y_POS = 975; // The y-position of the cannon's axis of rotation in a 1-player playpanel.
    private static final double[] VIBRATION_FREQUENCIES = {15.2, 10.7, 5.3}; // cycles per second
    private static final double[] VIBRATION_MAGNITUDES = {2.5, 1.5, 1}; // How much the array orbs vibrate before they drop 1 level.

    // Misc constants:
    private static final double ELECTRIFCATION_PROBABILITY = .004;

    // Constants cached for speed:
    public static final double ROW_HEIGHT = Math.sqrt(Math.pow(2* ORB_RADIUS,2) - Math.pow(ORB_RADIUS,2)); // Vertical distance between Orb rows.

    private List<PlayerData> playerList = new LinkedList<>();
    private Rectangle liveBoundary;

    private StaticBgImages foregroundCloudsEnum;
    private StaticBgImages dropCloudEnum;
    private int numPlayers;
    private Canvas orbCanvas;
    private GraphicsContext orbDrawer;

    // For generating the puzzle and ammunition:
    private String puzzleUrl;
    private int seed;
    private Random randomTransferOrbGenerator;
    private Random miscRandomGenerator = new Random();


    // Audio
    MediaPlayer rumbleSoundEffect;

    PlayPanel(int team, List<PlayerData> players, LocationType locationType, int seed, String puzzleUrl){
        this.numPlayers = players.size();
        this.randomTransferOrbGenerator = new Random(seed);
        this.puzzleUrl = puzzleUrl;

        foregroundCloudsEnum = locationType.getForegroundCloudsEnum();
        dropCloudEnum = locationType.getDropCloudEnum();

        // The size of the PlayPanel is determined by the liveBoundary rectangle.
        liveBoundary = new Rectangle(PLAYPANEL_WIDTH_PER_PLAYER*numPlayers + ORB_RADIUS, PLAYPANEL_HEIGHT, Color.TRANSPARENT);
        getChildren().add(liveBoundary);

        // The Orbs are placed on a Canvas. Initialize the Canvas:
        orbCanvas = new Canvas();
        orbCanvas.setWidth(liveBoundary.getWidth());
        orbCanvas.setHeight(PLAYPANEL_HEIGHT);
        orbDrawer = orbCanvas.getGraphicsContext2D();
        getChildren().add(orbCanvas);

        // Add and initialize players to the PlayPanel:
        addPlayers(players);

        // Initialize PlayPanelData:
        playPanelData = new PlayPanelData(team, numPlayers, seed, puzzleUrl);
    }

    private void addPlayers(List<PlayerData> players){
        playerList.addAll(players);

        for(int i=0; i<numPlayers; ++i){
            PlayerData player = players.remove(0);

            // register the new player, add his/her cannon and character to the UI, and add other background image components:
            player.registerToPlayPanel(this);
            player.relocateCannon(CANNON_X_POS + PLAYPANEL_WIDTH_PER_PLAYER*i, CANNON_Y_POS);
            player.relocateCharacter(player.getCannonType().getCharacterX() + (PLAYPANEL_WIDTH_PER_PLAYER)*(i) + ORB_RADIUS,
                    player.getCannonType().getCharacterY());
            ImageView foregroundClouds = foregroundCloudsEnum.getImageView();
            foregroundClouds.relocate(PLAYPANEL_WIDTH_PER_PLAYER*i,PLAYPANEL_HEIGHT-foregroundCloudsEnum.getHeight());
            getChildren().add(foregroundClouds);

            // Scale the character and cannon:
            player.setScale(1.0);

            // load the player's ammunition:
            player.readAmmunitionOrbs(puzzleUrl, seed, i);

            // Inform the player where they are located in the playpanel and initialize the positions of the 1st two shooting orbs:
            player.initializePlayerPos(i);
            player.positionAmmunitionOrbs();
        }

    }

    // Called from GameScene only
    void updatePlayer(PlayerData playerData, boolean isHost){
        //ToDo: put players into a hashmap or put the playerData in a list or something, for easier lookup.
        //Todo: note: simply adding a getPlayerData() method to PlayerData won't work (the reference has been lost over transmission).
        PlayerData player = playerList.get(0);
        for(PlayerData tempPlayer: playerList){
            if(tempPlayer.getPlayerID() == playerData.getPlayerID()){
                player = tempPlayer;
                break;
            }
        }

        if(isHost){
            if(playerData.isFiring()) {
                playPanelData.changeAddShootingOrbs(playerData.getFiredOrbs()); //updates model
            }
            player.updateWithChangers(playerData, null); // Relevant changes to playerData include: cannonAngle, defeated status, whether he/she is firing his/her cannon, and changes in BubbleData
        }
        else{
            if(playerData.isFiring() && !(player.getPlayerType()== PlayerData.PlayerType.LOCAL)){
                System.out.println("CLIENT: Another player has fired. Adding their obs to the playpanel");
                playPanelData.setAddShootingOrbs(playerData.getFiredOrbs()); // updates model
            }
            player.updateWithSetters(playerData, player.getPlayerType());
        }
    }


    // called 24 times per second to update all animations and Orb positions for the next animation frame.
    void tick(Set<SoundEffect> soundEffectsToPlay){
        // Existing data that will be affected via side-effects:
        OrbData[][] orbArray = playPanelData.getOrbArray();
        List<OrbData> burstingOrbs = playPanelData.getBurstingOrbs();
        List<OrbData> shootingOrbs = playPanelData.getShootingOrbs();
        List<OrbData> droppingOrbs = playPanelData.getDroppingOrbs();
        OrbData[] deathOrbs = playPanelData.getDeathOrbs();
        List<AnimationData> visualFlourishes = playPanelData.getVisualFlourishes();
        Set<OrbData> transferInOrbs = playPanelData.getTransferInOrbs();

        // New Sets and lists that will be filled via side-effects:
        List<OrbData> orbsToDrop = new LinkedList<>(); // Orbs that are no longer connected to the ceiling by the end of this frame
        List<OrbData> orbsToTransfer = new LinkedList<>(); // Orbs to transfer to other PlayPanels
        List<Collision> collisions = new LinkedList<>(); // All collisions that happen during this frame
        Set<OrbData> connectedOrbs = new HashSet<>(); // Orbs connected to the ceiling
        List<OrbData> arrayOrbsToBurst = new LinkedList<>(); // Orbs in the array that will be burst this frame

        // Most of the computation work is done in here. All the lists are updated via side-effects:
        playPanelUtility.simulateOrbs(orbArray, burstingOrbs, shootingOrbs, droppingOrbs, deathOrbs, soundEffectsToPlay, orbsToDrop, orbsToTransfer, collisions, connectedOrbs, arrayOrbsToBurst, 1/(double) FRAME_RATE);

        // Advance the animation frame of the existing visual flourishes:
        List<AnimationData> flourishesToRemove = advanceVisualFlourishes(visualFlourishes);
        visualFlourishes.removeAll(flourishesToRemove);

        // If orbs were dropped or a sufficient number were burst, add visual flourishes for the orbs to be transferred:
        if(!orbsToDrop.isEmpty() || !orbsToTransfer.isEmpty()){
            soundEffectsToPlay.add(SoundEffect.DROP);
            for(OrbData orbData : orbsToDrop){
                visualFlourishes.add(new AnimationData(Animation.EXCLAMATION_MARK, orbData.getXPos(), orbData.getYPos(), PlayOption.PLAY_ONCE_THEN_VANISH));
            }
            for(OrbData orbData : orbsToTransfer){
                visualFlourishes.add(new AnimationData(Animation.EXCLAMATION_MARK, orbData.getXPos(), orbData.getYPos(), PlayOption.PLAY_ONCE_THEN_VANISH));
            }
        }

        // Advance the animation frame of existing bursting orbs:
        List<OrbData> orbsToRemove = advanceBurstingOrbs(burstingOrbs);
        burstingOrbs.removeAll(orbsToRemove);

        // Advance the animation frame of the electrified orbs:
        advanceElectrifyingOrbs();

        // Advance the animation and audio of the thunder orbs:
        orbsToRemove = advanceThunderOrbs();
        playPanelData.getThunderOrbs().removeAll(orbsToRemove);

        // Advance existing dropping orbs:
        List<OrbData> orbsToThunder = advanceDroppingOrbs();
        playPanelData.getDroppingOrbs().removeAll(orbsToThunder);

        // If orbs dropped off the bottom of the PlayPanel, add them to the orbsToTransfer list AND the thunderOrbs list.
        if(!orbsToThunder.isEmpty()){
            for(OrbData orbData : orbsToThunder) orbsToTransfer.add(new OrbData(orbData)); // We must create a copy so that we can change the animationEnum without affecting the transferOutOrbs.
            playPanelData.setAddThunderOrbs(orbsToThunder);
        }

        // Add all of the transfer orbs to the transferOutOrbs list:
        if(!orbsToTransfer.isEmpty()) playPanelData.changeAddTransferOutOrbs(orbsToTransfer);

        // Advance the existing transfer-in Orbs, adding visual flourishes if they're done:
        List<OrbData> transferOrbsToSnap = advanceTransferringOrbs();
        playPanelUtility.snapTransferOrbs(transferOrbsToSnap, orbArray, connectedOrbs, soundEffectsToPlay, visualFlourishes, transferInOrbs);

        // If there are no orbs connected to the ceiling, then this team has finished the puzzle. Move on to the next one or declare victory
        if(connectedOrbs.isEmpty()){
            playPanelData.getShootingOrbs().clear();
            if(puzzleUrl.substring(0,6).equals("RANDOM")){ // this was a random puzzle. Declare victory
                playPanelData.changeDeclareVictory();
            }
            else{ // This was a pre-built puzzle. Load the next one, if there is one.
                int currentIndex = Integer.parseInt(puzzleUrl.substring(puzzleUrl.length()-2,puzzleUrl.length()));
                puzzleUrl = String.format("%s%02d",puzzleUrl.substring(0,puzzleUrl.length()-2),currentIndex+1);
                if(!playPanelData.initializeOrbArray(puzzleUrl)){ // There was no next puzzle. Declare victory.
                    playPanelData.changeDeclareVictory();
                }
                else{
                    for(int i=0; i<playerList.size(); i++){
                        PlayerData player = playerList.get(i);
                        player.readAmmunitionOrbs(puzzleUrl,seed,i);
                        player.positionAmmunitionOrbs();
                    }
                }
            }
        }

        // reset shotsUntilNewRow
        if(playPanelData.getShotsUntilNewRow()<=0) playPanelData.setShotsUntilNewRow(SHOTS_BETWEEN_DROPS*numPlayers + playPanelData.getShotsUntilNewRow());

        // If the player has fired a sufficient number of times, then add a new row of orbs:
        playPanelData.decrementShotsUntilNewRow(collisions.size());
        if(playPanelData.getShotsUntilNewRow()==1) soundEffectsToPlay.add(SoundEffect.ALMOST_NEW_ROW);
        else if(playPanelData.getShotsUntilNewRow()<=0){
            playPanelData.addNewRow();
            soundEffectsToPlay.add(SoundEffect.NEW_ROW);
        }

        // check to see whether this team has lost due to uncleared deathOrbs:
        if(!getPlayPanelData().isDeathOrbsEmpty()){
            for(PlayerData defeatedPlayer : getPlayerList()){
                defeatedPlayer.changeResignPlayer();
            }
        }

        // update each player's animation state:
        int lowestRow = playPanelData.getLowestOccupiedRow(orbArray, deathOrbs);
        for(PlayerData player : playerList){
            if (player instanceof BotPlayer) continue; // we've already ticked() the BotPlayers.
            player.tick(lowestRow);
        }

        removeStrayOrbs();
    }

    // removes orbs that have wandered off the edges of the canvas. This should only ever happen with dropping orbs, but
    // shooting orbs are also checked, just in case.
    private void removeStrayOrbs()
    {
        List<OrbData> orbsToRemove = new LinkedList<>();
        // Although it should never happen, let's look for stray shooting orbs and remove them:
        for(OrbData orbData : playPanelData.getShootingOrbs())
        {
            if(orbData.getXPos()<-ORB_RADIUS
                    || orbData.getXPos()>PLAYPANEL_WIDTH_PER_PLAYER*numPlayers+2*ORB_RADIUS
                    || orbData.getYPos()<-ORB_RADIUS
                    || orbData.getYPos()>PLAYPANEL_HEIGHT)
                orbsToRemove.add(orbData);
        }
        playPanelData.getShootingOrbs().removeAll(orbsToRemove);
    }

    public List<OrbData> advanceBurstingOrbs(List<OrbData> burstingOrbs) {
        List<OrbData> orbsToRemove = new LinkedList<>();
        for(OrbData orbData : burstingOrbs){
            if(orbData.tick()){
                orbsToRemove.add(orbData);
            }
        }
        return orbsToRemove;
    }

    private void advanceElectrifyingOrbs(){
        OrbData[][] orbArray = playPanelData.getOrbArray();
        OrbData orbData;
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++) {
                orbData = orbArray[i][j];
                if(orbData !=NULL){
                    switch(orbData.getOrbAnimationState()){
                        case STATIC:
                            if(miscRandomGenerator.nextDouble()< ELECTRIFCATION_PROBABILITY){
                                orbData.setOrbAnimationState(OrbAnimationState.ELECTRIFYING);
                            }
                            break;
                        case ELECTRIFYING:
                            orbData.tick();
                            break;
                    }
                }
            }
        }
    }

    private List<OrbData> advanceTransferringOrbs(){
        Set<OrbData> transferInOrbs = playPanelData.getTransferInOrbs();
        List<OrbData> transferOrbsToSnap = new LinkedList<>();
        for(OrbData orbData : transferInOrbs){
            if (orbData.tick()){
                transferOrbsToSnap.add(orbData);
            }
        }
        return transferOrbsToSnap;
    }

    // Todo: there are several methods exactly like this one. Can it be reduced to one using generics? Actually, it might require casting outside this method call, so think about it carefully.
    private List<AnimationData> advanceVisualFlourishes(List<AnimationData> visualFlourishes){
        List<AnimationData> visualFlourishesToRemove = new LinkedList<>();
        for(AnimationData visualFlourish : visualFlourishes){
            if(visualFlourish.tick()) visualFlourishesToRemove.add(visualFlourish);
        }
        return visualFlourishesToRemove;
    }

    private List<OrbData> advanceDroppingOrbs(){
        List<OrbData> orbsToTransfer = new LinkedList<>();
        for(OrbData orbData : playPanelData.getDroppingOrbs()){
            orbData.setSpeed(orbData.getSpeed() + GameScene.GRAVITY/ FRAME_RATE);
            orbData.relocate(orbData.getXPos(), orbData.getYPos() + orbData.getSpeed()/ FRAME_RATE);
            if(orbData.getYPos() > PLAYPANEL_HEIGHT){
                orbsToTransfer.add(orbData);
            }
        }
        return orbsToTransfer;
    }

    private List<OrbData> advanceThunderOrbs(){
        List<OrbData> orbsToRemove = new LinkedList<>();
        for(OrbData orbData : playPanelData.getThunderOrbs()){
            if(orbData.tick()){
                orbsToRemove.add(orbData);
            }
        }
        return orbsToRemove;
    }

    // repaints all orbs and Character animations on the PlayPanel.
    public void repaint(PlayPanelData playPanelData, List<PlayerData> playerDataList){
        // Clear the canvas
        orbDrawer.clearRect(0,0,orbCanvas.getWidth(),orbCanvas.getHeight());
        double vibrationOffset = 0.0;

        // A simple line shows the limit of the orbArray. If any orb is placed below this line, the player loses.
        double deathLineY = ROW_HEIGHT*(ARRAY_HEIGHT-1)+2*ORB_RADIUS;
        orbDrawer.setStroke(Color.PINK);
        orbDrawer.setLineWidth(2.0);
        orbDrawer.strokeLine(0,deathLineY,liveBoundary.getWidth(),deathLineY);

        // Paint dropping Orbs:
        for(OrbData orbData : playPanelData.getDroppingOrbs()) orbData.drawSelf(orbDrawer, vibrationOffset);

        // If we are close to adding 1 more level to the orbArray, apply a vibration effect to the array orbs:
        if(playPanelData.getShotsUntilNewRow()<=VIBRATION_FREQUENCIES.length && playPanelData.getShotsUntilNewRow()>0){
            vibrationOffset = VIBRATION_MAGNITUDES[playPanelData.getShotsUntilNewRow()-1]*Math.sin(2*PI*System.nanoTime()*VIBRATION_FREQUENCIES[playPanelData.getShotsUntilNewRow()-1]/1000000000);
        }

        // paint Array orbs:
        OrbData[][] orbArray = playPanelData.getOrbArray();
        for(int i=0; i<ARRAY_HEIGHT; ++i){
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                orbArray[i][j].drawSelf(orbDrawer, vibrationOffset);
            }
        }

        // paint Death orbs:
        for(OrbData orbData : playPanelData.getDeathOrbs()) orbData.drawSelf(orbDrawer,vibrationOffset);

        // paint VisualFlourishes:
        for(AnimationData visualFlourish : playPanelData.getVisualFlourishes()) visualFlourish.drawSelf(orbDrawer);

        // paint transferring orbs:
        for(OrbData orbData : playPanelData.getTransferInOrbs()) orbData.drawSelf(orbDrawer, vibrationOffset);

        // remaining orbs should not vibrate:
        vibrationOffset = 0.0;

        // Paint bursting orbs:
        for(OrbData orbData : playPanelData.getBurstingOrbs()){
            orbData.drawSelf(orbDrawer, vibrationOffset);
        }

        // Paint each player and his/her ammunition orbs:
        for(PlayerData playerData : playerDataList){
            if(playerData.getTeam().getData() == playPanelData.getTeam()){
                playerData.drawSelf(orbDrawer);
                List<OrbData> ammunitionOrbs = playerData.getAmmunition();
                ammunitionOrbs.get(0).drawSelf(orbDrawer, vibrationOffset);
                ammunitionOrbs.get(1).drawSelf(orbDrawer, vibrationOffset);
            }
        }

        // Paint shooting orbs:
        for(OrbData orbData : playPanelData.getShootingOrbs()) orbData.drawSelf(orbDrawer, vibrationOffset);
    }

    public Random getRandomTransferOrbGenerator(){
        return randomTransferOrbGenerator;
    }

    public PlayPanelData getPlayPanelData(){
        return playPanelData;
    }

    public PlayPanelUtility getPlayPanelUtility(){
        return playPanelUtility;
    }

    // todo: this is horribly inefficient. Can I make it faster? It is somewhat amortized, though...
    public OrbColor getNextShooterOrbEnum(double randomNumber){
        // Count the number of each type of orb in the orbArray.
        LinkedHashMap<OrbColor,Double> counts = new LinkedHashMap<>();
        OrbData[][] orbArray = playPanelData.getOrbArray();
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                if(orbArray[i][j]==NULL) continue;
                else if(counts.containsKey(orbArray[i][j].getOrbColor())){
                    counts.replace(orbArray[i][j].getOrbColor(),counts.get(orbArray[i][j].getOrbColor())+1);
                }
                else{
                    counts.put(orbArray[i][j].getOrbColor(),1.0);
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

    public List<PlayerData> getPlayerList(){
        return playerList;
    }

    public void displayVictoryResults(VictoryType victoryType){
        Text statistics;
        AnimationData visualFlourish;
        String specializedStatistic;

        // Note: we must add getDroppingOrbs().size() to getCumulativeOrbsTransferred() because orbs that are dropping at the very end of the game haven't been added to the transferOutOrbs list, yet.
        switch(victoryType) {
            case VS_WIN:
                visualFlourish = new AnimationData(Animation.WIN_SCREEN, 0, 0, PlayOption.PLAY_ONCE_THEN_PAUSE);
                specializedStatistic = "Orbs transferred to other players: " + (playPanelData.getCumulativeOrbsTransferred() + playPanelData.getDroppingOrbs().size()) + "\n";
                break;
            case VS_LOSE:
                visualFlourish = new AnimationData(Animation.LOSE_SCREEN, 0, 0, PlayOption.PLAY_ONCE_THEN_PAUSE);
                specializedStatistic = "Orbs transferred to other players: " + (playPanelData.getCumulativeOrbsTransferred() + playPanelData.getDroppingOrbs().size()) + "\n";
                break;
            case PUZZLE_CLEARED:
                visualFlourish = new AnimationData(Animation.CLEAR_SCREEN, 0, 0, PlayOption.PLAY_ONCE_THEN_PAUSE);
                specializedStatistic = "Orbs dropped: " + playPanelData.getCumulativeOrbsDropped() + "\n";
                break;
            case PUZZLE_FAILED:
                visualFlourish = new AnimationData(Animation.LOSE_SCREEN, 0, 0, PlayOption.PLAY_ONCE_THEN_PAUSE);
                specializedStatistic = "Orbs dropped: " + playPanelData.getCumulativeOrbsDropped() + "\n";
                break;
            default:
                System.err.println("Unrecognized VictoryType. setting default visual Flourish.");
                visualFlourish = new AnimationData(Animation.WIN_SCREEN,0,0, PlayOption.PLAY_ONCE_THEN_PAUSE);
                specializedStatistic = "\n";
        }

        // Add an appropriate Win/Lose/Cleared animation
        visualFlourish.relocate(PLAYPANEL_WIDTH_PER_PLAYER*numPlayers/2, PLAYPANEL_HEIGHT/2-100);
        playPanelData.getVisualFlourishes().add(visualFlourish);

        // Display statistics for this PlayPanel
        statistics = new Text("total orbs fired: " + playPanelData.getCumulativeShotsFired() + "\n" +
                "Orbs burst: " + playPanelData.getCumulativeOrbsBurst() + "\n" +
                specializedStatistic +
                "Largest group explosion: " + playPanelData.getLargestGroupExplosion());
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

}

