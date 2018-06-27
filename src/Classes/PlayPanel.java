package Classes;

import Classes.Animation.MiscAnimations;
import Classes.Audio.Music;
import Classes.Audio.SoundEffect;
import Classes.Audio.SoundManager;
import Classes.Animation.OrbImages;
import Classes.Images.StaticBgImages;
import Classes.NetworkCommunication.PlayPanelData;
import Classes.NetworkCommunication.PlayerData;
import Classes.PlayerTypes.LocalPlayer;
import Classes.PlayerTypes.Player;
import javafx.geometry.Point2D;
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

import static Classes.GameScene.ANIMATION_FRAME_RATE;
import static Classes.NetworkCommunication.PlayPanelData.ARRAY_HEIGHT;
import static Classes.NetworkCommunication.PlayPanelData.ARRAY_WIDTH_PER_CHARACTER;
import static Classes.NetworkCommunication.PlayPanelData.SHOTS_BETWEEN_DROPS;
import static Classes.Orb.NULL;
import static Classes.Orb.ORB_RADIUS;

/**
 * Think of this as the Controller part of a Model-View-Controller scheme. The Model part is the PlayPanelData and the View
 * part is the Canvas (and the Orb class in a sense, because orbs know how to draw themselves).
 */
public class PlayPanel extends Pane {

    PlayPanelData playPanelData;

    // Constants determining PlayPanel layout:
    // TODO: the Cannon data should probably be moved to the Cannon class or CannonImages enumeration
    public static final double PLAYPANEL_WIDTH_PER_PLAYER = 690;
    public static final double PLAYPANEL_HEIGHT = 1080;
    public static final double CANNON_X_POS = ORB_RADIUS + PLAYPANEL_WIDTH_PER_PLAYER/2.0; // The x-position of the cannon's axis of rotation in a 1-player playpanel.
    public static final double CANNON_Y_POS = 975; // The y-position of the cannon's axis of rotation in a 1-player playpanel.
    public static final double ROW_HEIGHT = Math.sqrt(Math.pow(2* ORB_RADIUS,2) - Math.pow(ORB_RADIUS,2)); // Vertical distance between Orb rows.
    private static final double[] VIBRATION_OFFSETS = {2.5, 1.5, 1}; // How much the array orbs vibrate before they drop 1 level.
    private static final double[] VIBRATION_FREQUENCIES = {15.2, 10.7, 5.3}; // cycles per second

    // Misc constants:
    private static final double ELECTRIFCATION_PROBABILITY = .004;

    private List<Player> playerList = new LinkedList<>();
    private Rectangle liveBoundary;

    private StaticBgImages foregroundCloudsEnum;
    private StaticBgImages dropCloudEnum;
    private int numPlayers;
    private Canvas orbCanvas;
    private GraphicsContext orbDrawer;
    private List<VisualFlourish> visualFlourishes = new LinkedList<>();

    // For generating the puzzle and ammunition:
    private String puzzleUrl;
    private int seed;
    private Random randomTransferOrbGenerator;
    private Random miscRandomGenerator = new Random();

    // Audio
    MediaPlayer rumbleSoundEffect;

    PlayPanel(int team, List<Player> players, LocationType locationType, int seed, String puzzleUrl){
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

    private void addPlayers(List<Player> players){
        playerList.addAll(players);

        for(int i=0; i<numPlayers; ++i){
            Player player = players.remove(0);

            // register the new player, add his/her cannon and character to the UI, and add other background image components:
            player.registerToPlayPanel(this);
            getChildren().add(player.getCannonStaticBackground());
            getChildren().add(player.getCannonMovingPart());
            getChildren().add(player.getCannonStaticForeground());
            player.relocateCannon(CANNON_X_POS + PLAYPANEL_WIDTH_PER_PLAYER*i, CANNON_Y_POS);
            player.relocateCharacter(player.getPlayerData().getCannonEnum().getCharacterX() + (PLAYPANEL_WIDTH_PER_PLAYER)*(i) + ORB_RADIUS,
                    player.getPlayerData().getCannonEnum().getCharacterY());
            getChildren().add(player.getCharacterSprite());
            ImageView foregroundClouds = foregroundCloudsEnum.getImageView();
            foregroundClouds.relocate(PLAYPANEL_WIDTH_PER_PLAYER*i,PLAYPANEL_HEIGHT-foregroundCloudsEnum.getHeight());
            getChildren().add(foregroundClouds);

            // Scale the character and cannon:
            player.setScale(1.0);

            // load the player's ammunition:
            player.readAmmunitionOrbs(puzzleUrl, seed, i);

            // Inform the player where they are located in the playpanel and initialize the positions of the 1st two shooting orbs:
            player.getPlayerData().initializePlayerPos(i);
            player.getPlayerData().positionAmmunitionOrbs();
        }

        // todo: temporary. Delete this eventually.
        // Add a keylistener for testing transferOrbs.
        setOnKeyPressed(event -> {
            switch(event.getCode()){
                case T:
                    System.out.println("Testing Transfer Orb...");

                    break;
            }
        });
    }



    // Called from GameScene only
    void updatePlayer(PlayerData playerData, boolean isHost){
        //ToDo: put players into a hashmap or put the playerData in a list or something, for easier lookup.
        //Todo: note: simply adding a getPlayer() method to PlayerData won't work (the reference has been lost over transmission).
        Player player = playerList.get(0);
        for(Player tempPlayer: playerList){
            if(tempPlayer.getPlayerData().getPlayerID() == playerData.getPlayerID()){
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
            if(playerData.isFiring() && !(player instanceof LocalPlayer)){
                System.out.println("CLIENT: Another player has fired. Adding their obs to the playpanel");
                playPanelData.setAddShootingOrbs(playerData.getFiredOrbs()); // updates model
            }
            player.updateWithSetters(playerData, player instanceof LocalPlayer);
        }
    }


    // called 24 times per second to update all animations and Orb positions for the next animation frame.
    void tick(){
        // reset shotsUntilNewRow
        if(playPanelData.getShotsUntilNewRow()<=0) playPanelData.setShotsUntilNewRow(SHOTS_BETWEEN_DROPS*numPlayers + playPanelData.getShotsUntilNewRow());

        // Advance visual flourishes
        List<VisualFlourish> flourishesToRemove = advanceVisualFlourishes();
        visualFlourishes.removeAll(flourishesToRemove);

        // Advance shooting orbs and deal with all their collisions:
        List<Collision> orbsToSnap = advanceShootingOrbs(1/(double)ANIMATION_FRAME_RATE); // Updates model
        List<Orb> shootingOrbsToBurst = snapOrbs(orbsToSnap);
        boolean[] playDropSound = new boolean[1];
        playDropSound[0] = false; // findPatternCompletions will make this value true if appropriate
        List<PointInt> arrayOrbsToBurst = findPatternCompletions(orbsToSnap, shootingOrbsToBurst, playDropSound);

        // Advance the animation frame of existing bursting orbs, then burst new orbs:
        advanceBurstingOrbs();
        if(!shootingOrbsToBurst.isEmpty() || !arrayOrbsToBurst.isEmpty()){
            SoundManager.playSoundEffect(SoundEffect.EXPLOSION);
            playPanelData.changeBurstShootingOrbs(shootingOrbsToBurst);
            playPanelData.changeBurstArrayOrbs(arrayOrbsToBurst);
        }

        // Advance the animation frame of the electrified orbs:
        advanceElectrifyingOrbs();

        // Advance the animation and audio of the thunder orbs:
        List<Orb> orbsToRemove = advanceThunderOrbs();
        playPanelData.getThunderOrbs().removeAll(orbsToRemove);

        // Advance existing dropping orbs:
        List<Orb> orbsToTransfer = advanceDroppingOrbs();
        playPanelData.getDroppingOrbs().removeAll(orbsToTransfer);
        if(!orbsToTransfer.isEmpty()){
            playPanelData.changeAddTransferOutOrbs(orbsToTransfer); // Note: the transferOutOrbs list will get cleared before the end of this frame, in GameScene.tick().
            List<Orb> orbsToTransferCopy = new LinkedList<>();
            for(Orb orb : orbsToTransfer){ // We must create a copy so that we can change the animationEnum without affecting the transferOutOrbs.
                Orb orbCopy = new Orb(orb.getOrbEnum(), 0,0, Orb.BubbleAnimationType.THUNDERING);
                orbsToTransferCopy.add(orbCopy);
            }
            playPanelData.setAddThunderOrbs(orbsToTransferCopy);
        }

        // Find floating orbs and drop them, adding visual flourishes.
        Set<PointInt> connectedOrbs = playPanelData.findConnectedOrbs(); // orbs that are connected to the ceiling.
        List<PointInt> orbsToDrop = playPanelData.findFloatingOrbs(connectedOrbs);
        if(!orbsToDrop.isEmpty()){
            playDropSound[0] = true;
            for(PointInt orbToDrop : orbsToDrop){
                Orb orb = playPanelData.getOrbArray()[orbToDrop.i][orbToDrop.j];
                visualFlourishes.add(new VisualFlourish(MiscAnimations.EXCLAMATION_MARK, orb.getXPos(), orb.getYPos(),false));
            }
            playPanelData.changeDropArrayOrbs(orbsToDrop);
        }

        // Advance the transfer orbs, adding visual flourishes if they're done:
        List<Orb> transferOrbsToSnap = advanceTransferringOrbs();
        snapTransferOrbs(transferOrbsToSnap);

        // If the player dropped any orbs or burst a sufficient number of orbs, play the Drop sound effect:
        if(playDropSound[0]){
            SoundManager.playSoundEffect(SoundEffect.DROP);
        }

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
                        Player player = playerList.get(i);
                        player.readAmmunitionOrbs(puzzleUrl,seed,i);
                        player.getPlayerData().positionAmmunitionOrbs();
                    }
                }
            }
        }

        // If the player has fired a sufficient number of times, then add a new row of orbs:
        playPanelData.decrementShotsUntilNewRow(orbsToSnap.size());
        if(playPanelData.getShotsUntilNewRow()<=0){
            playPanelData.addNewRow();
            SoundManager.playSoundEffect(SoundEffect.NEW_ROW);
        }

        // check to see whether this team has lost due to uncleared deathOrbs:
        if(!getPlayPanelData().isDeathOrbsEmpty()){
            for(Player defeatedPlayer : getPlayerList()){
                defeatedPlayer.changeResignPlayer();
            }
        }

        removeStrayOrbs();
        repaint(); // Updates view
    }

    // Initiated 24 times per second, and called recursively.
    // advances all shooting orbs, detecting collisions along the way and stopping orbs that collide with arrayOrbs or
    // with the ceiling.
    // A single recursive call to this function might only advance the shooting orbs a short distance, but when all the
    // recursive calls are over, all shooting orbs will have been advanced one full frame.
    // Returns a list of all orbs that will attempt to snap; some of them may end up bursting instead during the call to
    // snapOrbs if (and only if) s-s collisions are turned off.
    private List<Collision> advanceShootingOrbs(double timeRemainingInFrame) {

        // Put all possible collisions in here. If a shooter orb's path this frame would put it on a collision course
        // with the ceiling, a wall, or an array orb, then that collision will be added to this list, even if there is
        // another orb in the way.
        List<Collision> possibleCollisionPoints = new LinkedList<>();

        // Container for orbs to snap.
        List<Collision> orbsToSnap = new LinkedList<>();

        for (Orb shootingOrb : playPanelData.getShootingOrbs()) {
            double speed = shootingOrb.getSpeed();
            double angle = shootingOrb.getAngle();
            double x0 = shootingOrb.getXPos(); // x-position of the shooting orb before it is advanced.
            double y0 = shootingOrb.getYPos(); // y-position of the shooting orb before it is advanced.
            double distanceToTravel = speed * timeRemainingInFrame;
            double x1 = x0 + distanceToTravel * Math.cos(angle); // Theoretical x-position of the shooting orb after it is advanced.
            double y1 = y0 + distanceToTravel * Math.sin(angle); // Theoretical y-position of the shooting orb after it is advanced.
            double x1P = x1 - x0;
            double y1P = y1 - y0;
            Orb orbArray[][] = getPlayPanelData().getOrbArray();

            // Cycle through the entire Orb array and find all possible collision points along this shooting orb's path:
            for (int i = 0; i < PlayPanelData.ARRAY_HEIGHT; i++) {
                for (int j = 0; j < ARRAY_WIDTH_PER_CHARACTER * numPlayers; j ++) {
                    if (orbArray[i][j] != NULL) {
                        double xAP = orbArray[i][j].getXPos() - x0;
                        double yAP = orbArray[i][j].getYPos() - y0;
                        double lhs = 4 * ORB_RADIUS * ORB_RADIUS * (1 + Math.pow(y1P / x1P, 2.0));
                        double rhs = Math.pow(y1P * xAP / x1P - yAP, 2.0);
                        // Test whether collision is possible. If it is, then compute its 2 possible collision points.
                        if (lhs > rhs) {
                            // Compute the two possible intersection points, (xPP, yPP) and (xPN, yPN)
                            double xPP;
                            double yPP;
                            double xPN;
                            double yPN;

                            // if the Orb is traveling nearly vertically, use a special solution to prevent a near-zero denominator:
                            if (-0.01 < x1P && x1P < 0.01) {
                                xPP = 0;
                                xPN = 0;
                                yPP = yAP + Math.sqrt(4 * ORB_RADIUS * ORB_RADIUS - xAP * xAP);
                                yPN = yAP - Math.sqrt(4 * ORB_RADIUS * ORB_RADIUS - xAP * xAP);
                            }
                            else {
                                double numerator1 = xAP + y1P * yAP / x1P;
                                double numerator2 = Math.sqrt(lhs - rhs);
                                double divisor = 1 + Math.pow(y1P / x1P, 2.0);
                                xPP = (numerator1 + numerator2) / divisor;
                                yPP = xPP * y1P / x1P;
                                xPN = (numerator1 - numerator2) / divisor;
                                yPN = xPN * y1P / x1P;
                            }

                            // Figure out which intersection point is closer, and only add the collision to the list of
                            // possible collisions if its time-to-collision is less than the time remaining in the frame.
                            double distanceToCollisionPSquared = xPP * xPP + yPP * yPP;
                            double distanceToCollisionNSquared = xPN * xPN + yPN * yPN;
                            if (distanceToCollisionPSquared < distanceToTravel * distanceToTravel
                                    && distanceToCollisionPSquared < distanceToCollisionNSquared) {
                                double timeToCollision = Math.sqrt(distanceToCollisionPSquared) / speed;
                                possibleCollisionPoints.add(new Collision(shootingOrb, orbArray[i][j], timeToCollision));
                            }
                            if (distanceToCollisionNSquared < distanceToTravel * distanceToTravel
                                    && distanceToCollisionNSquared < distanceToCollisionPSquared) {
                                double timeToCollision = Math.sqrt(distanceToCollisionNSquared) / speed;
                                possibleCollisionPoints.add(new Collision(shootingOrb, orbArray[i][j], timeToCollision));
                            }
                        }
                    }
                }
            }

            // Check for and add collisions with the wall:
            double xRightWallP = (PLAYPANEL_WIDTH_PER_PLAYER * numPlayers + ORB_RADIUS) - ORB_RADIUS - x0;
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
            if(y1<ORB_RADIUS){
                double timeToCollision = timeRemainingInFrame * yCeilingP / y1P;
                possibleCollisionPoints.add(new Collision(shootingOrb, Orb.CEILING, timeToCollision));
            }
        }

        // Find the *soonest* collision among the list of possible collisions.
        Collision soonestCollision = null;
        long soonestCollisionTime = Long.MAX_VALUE;
        for (Collision collision : possibleCollisionPoints) {
            if (collision.timeToCollision <= soonestCollisionTime) {
                soonestCollisionTime = (long) collision.timeToCollision;
                soonestCollision = collision;
            }
        }

        // Advance all shooting orbs to that point in time and deal with the collision.
        if (soonestCollision != null) {
            for (Orb shootingOrb : playPanelData.getShootingOrbs()) {
                double speed = shootingOrb.getSpeed();
                double angle = shootingOrb.getAngle();
                double x0 = shootingOrb.getXPos(); // x-position of the shooting orb before it is advanced.
                double y0 = shootingOrb.getYPos(); // y-position of the shooting orb before it is advanced.
                double distanceToTravel = speed * (soonestCollisionTime / 1000000000.0);
                double x1 = x0 + distanceToTravel * Math.cos(angle);
                double y1 = y0 + distanceToTravel * Math.sin(angle);
                shootingOrb.setXPos(x1);
                shootingOrb.setYPos(y1);
            }

            // If there was a collision with a wall, then just reflect the shooter orb's angle. Recursively call this function.
            if (soonestCollision.arrayOrb == Orb.WALL) {
                SoundManager.playSoundEffect(SoundEffect.CHINK);
                Orb shooterOrb = soonestCollision.shooterOrb;
                shooterOrb.setAngle(Math.PI - shooterOrb.getAngle());
                List<Collision> moreOrbsToSnap = advanceShootingOrbs(timeRemainingInFrame - soonestCollisionTime);
                orbsToSnap.addAll(moreOrbsToSnap);
                return orbsToSnap;
            }

            // If the collision is between two shooter orbs, compute new angles and speeds. If the other shooting orb is
            // in the process of snapping, then burst this shooting orb. Recursively call this function.
            else if (playPanelData.getShootingOrbs().contains(soonestCollision.arrayOrb)) {
                // Todo: do this.
                List<Collision> moreOrbsToSnap = advanceShootingOrbs(timeRemainingInFrame - soonestCollisionTime);
                orbsToSnap.addAll(moreOrbsToSnap);
                return orbsToSnap;
            }

            // If the collision was with the ceiling, set that orb's speed to zero and add it to the orbsToSnap list.
            // Recursively call this function.
            else if(soonestCollision.arrayOrb == Orb.CEILING){
                soonestCollision.shooterOrb.setSpeed(0.0);
                orbsToSnap.add(soonestCollision);
                List<Collision> moreOrbsToSnap = advanceShootingOrbs(timeRemainingInFrame - soonestCollisionTime);
                orbsToSnap.addAll(moreOrbsToSnap);
                return orbsToSnap;
            }

            // If the collision is between a shooter orb and an array orb, set that orb's speed to zero and add it to
            // the orbsToSnap list. Recursively call this function.
            else {
                soonestCollision.shooterOrb.setSpeed(0.0);
                orbsToSnap.add(soonestCollision);
                List<Collision> moreOrbsToSnap = advanceShootingOrbs(timeRemainingInFrame - soonestCollisionTime);
                orbsToSnap.addAll(moreOrbsToSnap);
                return orbsToSnap;
            }
        }

        // If there are no more collisions, just advance all orbs to the end of the frame.
        else {
            for (Orb shootingOrb : playPanelData.getShootingOrbs()) {
                double speed = shootingOrb.getSpeed();
                double angle = shootingOrb.getAngle();
                double x0 = shootingOrb.getXPos(); // x-position of the shooting orb before it is advanced.
                double y0 = shootingOrb.getYPos(); // y-position of the shooting orb before it is advanced.
                double distanceToTravel = speed * timeRemainingInFrame;
                double x1 = x0 + distanceToTravel * Math.cos(angle);
                double y1 = y0 + distanceToTravel * Math.sin(angle);
                shootingOrb.setXPos(x1);
                shootingOrb.setYPos(y1);
            }
            return orbsToSnap;
        }

    }

    private List<Orb> snapOrbs(List<Collision> snaps){

        List<Orb> orbsToBurst = new LinkedList<>();

        for(Collision snap : snaps){
            int iSnap;
            int jSnap;

            // Compute snap coordinates for orbs that collided with the ceiling
            if(snap.arrayOrb==Orb.CEILING){
                int offset = 0;
                Orb[][] orbArray = playPanelData.getOrbArray();
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
                Point2D arrayOrbLoc = snap.arrayOrb.xyToIj();
                int iArrayOrb = (int)Math.round(arrayOrbLoc.getX());
                int jArrayOrb = (int)Math.round(arrayOrbLoc.getY());

                // Recompute the collision angle:
                double collisionAngleDegrees = Math.atan2(snap.shooterOrb.getYPos()-snap.arrayOrb.getYPos(),
                        snap.shooterOrb.getXPos()-snap.arrayOrb.getXPos())*180.0/Math.PI;

                // set snap coordinates based on angle:
                if(collisionAngleDegrees<30 && collisionAngleDegrees>=-30){ // Collided with right side of array orb
                    iSnap = iArrayOrb;
                    jSnap = jArrayOrb+2;
                }
                else if(collisionAngleDegrees<90 && collisionAngleDegrees>=30){ // Collided with lower-right side of array orb
                    iSnap = iArrayOrb+1;
                    jSnap = jArrayOrb+1;
                }
                else if(collisionAngleDegrees<150 && collisionAngleDegrees>=90){ // Collided with lower-left side of array orb
                    iSnap = iArrayOrb+1;
                    jSnap = jArrayOrb-1;
                }
                else if(collisionAngleDegrees<-150 || collisionAngleDegrees>=150){ // Collided with left side of array orb
                    iSnap = iArrayOrb;
                    jSnap = jArrayOrb-2;
                }
                else if(collisionAngleDegrees<-90 && collisionAngleDegrees>=-150){ // Collided with upper-left side of array orb
                    iSnap = iArrayOrb-1;
                    jSnap = jArrayOrb-1;
                }
                else { // Collided with upper-right side of the array orb
                    iSnap = iArrayOrb-1;
                    jSnap = jArrayOrb+1;
                }
            }

            // If the i coordinate is below the bottom of the array, then put the orb on the deathOrbs list.
            if(playPanelData.validDeathOrbsCoordinates(new PointInt(iSnap, jSnap))){
                // If s-s collisions are turned off, it is possible for two shooter orbs to try to snap to the same location
                // on the deathOrbs array. If that's the case, then burst the second orb that attempts to snap
                if(playPanelData.getDeathOrbs()[jSnap] != NULL){
                    orbsToBurst.add(snap.shooterOrb);
                    System.out.println("Two orbs attempted to snap to the same deathOrbs index. Bursting second orb");
                }
                else{
                    playPanelData.getDeathOrbs()[jSnap] = snap.shooterOrb;
                    snap.shooterOrb.setIJ(iSnap, jSnap);
                    SoundManager.playSoundEffect(SoundEffect.PLACEMENT);
                }
            }
            // If the snap coordinates are somehow off the edge of the array in a different fashion, then just burst
            // the orb. This should never happen, but... you never know.
            else if(!playPanelData.validOrbArrayCoordinates(new PointInt(iSnap, jSnap))){
                System.err.println("Invalid snap coordinates detected. Bursting orb.");
                orbsToBurst.add(snap.shooterOrb);
            }
            // If s-s collisions are turned off, it is possible for two shooter orbs to try to snap to the same location.
            // If that's the case, then burst the second orb that attempts to snap.
            else if(playPanelData.getOrbArray()[iSnap][jSnap] != NULL){
                orbsToBurst.add(snap.shooterOrb);
                System.out.println("Two orbs attempted to snap to the same orbArray coordinate. Bursting second orb");
            }
            else{
                playPanelData.getOrbArray()[iSnap][jSnap] = snap.shooterOrb;
                snap.shooterOrb.setYPos(ORB_RADIUS + iSnap*ROW_HEIGHT);
                snap.shooterOrb.setXPos(ORB_RADIUS + jSnap*ORB_RADIUS);
                SoundManager.playSoundEffect(SoundEffect.PLACEMENT);
            }

            // Remove the shooting orb from the shootingOrbs list
            playPanelData.getShootingOrbs().remove(snap.shooterOrb);
        }

        return orbsToBurst;
    }

    private List<PointInt> findPatternCompletions(List<Collision> orbsToSnap, List<Orb> shootingOrbsToBurst, boolean[] playDropSound){

        List<PointInt> arrayOrbsToBurst = new LinkedList<>();
        List<Orb> transferOrbList = new LinkedList<>();

        for(Collision collision : orbsToSnap){
            Orb sourceOrb = collision.shooterOrb;
            if(shootingOrbsToBurst.contains(sourceOrb)) continue; // Only consider orbs that are not already in the bursting orbs list

            // Get the source orb's (i, j) coordinates. Only consider orbs that have valid coordinates
            Point2D arrayOrbLoc = sourceOrb.xyToIj();
            int i = (int)Math.round(arrayOrbLoc.getX());
            int j = (int)Math.round(arrayOrbLoc.getY());
            //if(!playPanelData.validOrbArrayCoordinates(new PointInt(i,j))) continue;

            // find all connected orbs of the same color
            List<PointInt> connectedOrbs = playPanelData.depthFirstSearch(new PointInt(i,j), PlayPanelData.FilterOption.SAME_COLOR);
            if(connectedOrbs.size() > 2) arrayOrbsToBurst.addAll(connectedOrbs);

            // If there are more than 3 grouped together, then add a transfer out orb of the same color, as well as a visual flourish:
            if(connectedOrbs.size() > 3) {
                playDropSound[0] = true;
                int numTransferOrbs = (connectedOrbs.size()-3)/2;
                OrbImages orbEnum = sourceOrb.getOrbEnum();
                for(int k=0; k<numTransferOrbs; k++){
                    //transferOutOrbs.add(new Orb(orbEnum,miscRandomGenerator.nextInt(),miscRandomGenerator.nextInt(),Orb.BubbleAnimationType.TRANSFERRING));
                    transferOrbList.add(new Orb(orbEnum,0,0,Orb.BubbleAnimationType.STATIC));
                }
                Orb[][] orbArray = playPanelData.getOrbArray();
                for(int k=0; k<numTransferOrbs; k++){
                    PointInt point = connectedOrbs.get(k);
                    Orb orb = orbArray[point.i][point.j];
                    visualFlourishes.add(new VisualFlourish(MiscAnimations.EXCLAMATION_MARK, orb.getXPos(), orb.getYPos(), false));
                }
            }
            if(connectedOrbs.size()>playPanelData.getLargestGroupExplosion()){
                playPanelData.setLargestGroupExplosion(connectedOrbs.size());
            }
        }

        //if(!transferOrbList.isEmpty()) playPanelData.changeAddTransferOutOrbs(transferOrbList);
        return arrayOrbsToBurst;
    }

    // removes orbs that have wandered off the edges of the canvas. This should only ever happen with dropping orbs, but
    // shooting orbs are also checked, just in case.
    private void removeStrayOrbs()
    {
        List<Orb> orbsToRemove = new LinkedList<>();
        // Although it should never happen, let's look for stray shooting orbs and remove them:
        for(Orb orb : playPanelData.getShootingOrbs())
        {
            if(orb.getXPos()<-ORB_RADIUS
                    || orb.getXPos()>PLAYPANEL_WIDTH_PER_PLAYER*numPlayers+2*ORB_RADIUS
                    || orb.getYPos()<-ORB_RADIUS
                    || orb.getYPos()>PLAYPANEL_HEIGHT)
                orbsToRemove.add(orb);
        }
        playPanelData.getShootingOrbs().removeAll(orbsToRemove);
    }

    private void advanceBurstingOrbs() {
        List<Orb> orbsToRemove = new LinkedList<>();
        for(Orb orb : playPanelData.getBurstingOrbs()){
            if(orb.animationTick()){
                orbsToRemove.add(orb);
            }
        }
        playPanelData.getBurstingOrbs().removeAll(orbsToRemove);
    }

    private void advanceElectrifyingOrbs(){
        Orb[][] orbArray = playPanelData.getOrbArray();
        Orb orb;
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++) {
                orb = orbArray[i][j];
                if(orb!=NULL){
                    switch(orb.getAnimationEnum()){
                        case STATIC:
                            if(miscRandomGenerator.nextDouble()< ELECTRIFCATION_PROBABILITY){
                                orb.setAnimationEnum(Orb.BubbleAnimationType.ELECTRIFYING);
                            }
                            break;
                        case ELECTRIFYING:
                            orb.animationTick();
                            break;
                    }
                }
            }
        }
    }

    private List<Orb> advanceTransferringOrbs(){
        List<Orb> transferInOrbs = playPanelData.getTransferInOrbs();
        List<Orb> transferOrbsToSnap = new LinkedList<>();
        for(Orb orb : transferInOrbs){
            if(playPanelData.getTeam()==2) System.out.println("ticking a transfer orb with animationEnum " + orb.getAnimationEnum());
            if (orb.animationTick()){
                if(playPanelData.getTeam()==2) System.out.println("tick returned true. animationEnum is now " + orb.getAnimationEnum());
                transferOrbsToSnap.add(orb);
            }
            else {
                if(playPanelData.getTeam()==2) System.out.println("tick returned false. animationEnum is still " + orb.getAnimationEnum());
            }
        }
        return transferOrbsToSnap;
    }

    // Todo: there are several methods exactly like this one. Can it be reduced to one using generics? Actually, it might require casting outside this method call, so think about it carefully.
    /*private List<T> advanceAnimationController<T>(List<T>){
    }*/
    private List<VisualFlourish> advanceVisualFlourishes(){
        List<VisualFlourish> visualFlourishesToRemove = new LinkedList<>();
        for(VisualFlourish visualFlourish : visualFlourishes){
            if(visualFlourish.animationTick()) visualFlourishesToRemove.add(visualFlourish);
        }
        return visualFlourishesToRemove;
    }



    private void snapTransferOrbs(List<Orb> transferOrbsToSnap){
        // Find all array points that are connected to the ceiling
        boolean playSoundEffect = false;
        Orb[][] orbArray = playPanelData.getOrbArray();
        List<PointInt> connectedOrbs = new LinkedList<>();
        for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
            PointInt sourcePoint = new PointInt(0, j);
            if(orbArray[0][j]==Orb.NULL) continue;
            connectedOrbs.addAll(playPanelData.depthFirstSearch(sourcePoint, PlayPanelData.FilterOption.ALL));
        }

        // snap the transferringOrbs to the array
        for(Orb orb : transferOrbsToSnap){
            // only those orbs that would be connected to the ceiling should materialize:
            List<PointInt> neighbors = playPanelData.getNeighbors(new PointInt(orb.getI(),orb.getJ()));
            if((!Collections.disjoint(neighbors,connectedOrbs) || orb.getI()==0) && orbArray[orb.getI()][orb.getJ()] == NULL){
                orbArray[orb.getI()][orb.getJ()] = orb;
                playSoundEffect = true;
                visualFlourishes.add(new VisualFlourish(MiscAnimations.MAGIC_TELEPORTATION,orb.getXPos(),orb.getYPos(), false));
            }
        }

        // remove the snapped transfer orbs from the inbound transfer orbs list
        playPanelData.getTransferInOrbs().removeAll(transferOrbsToSnap);

        // play a sound effect, if appropriate:
        if(playSoundEffect) SoundManager.playSoundEffect(SoundEffect.MAGIC_TINKLE);
    }

    private List<Orb> advanceDroppingOrbs(){
        List<Orb> orbsToTransfer = new LinkedList<>();
        for(Orb orb : playPanelData.getDroppingOrbs()){
            orb.setSpeed(orb.getSpeed() + GameScene.GRAVITY/ANIMATION_FRAME_RATE);
            orb.setYPos(orb.getYPos() + orb.getSpeed()/ANIMATION_FRAME_RATE);
            if(orb.getYPos() > PLAYPANEL_HEIGHT){
                orbsToTransfer.add(orb);
            }
        }
        return orbsToTransfer;
    }

    private List<Orb> advanceThunderOrbs(){
        List<Orb> orbsToRemove = new LinkedList<>();
        for(Orb orb : playPanelData.getThunderOrbs()){
            if(orb.animationTick()){
                orbsToRemove.add(orb);
            }
        }
        return orbsToRemove;
    }

    // repaints all orbs and Character animations on the PlayPanel.
    private void repaint(){
        // Clear the canvas
        orbDrawer.clearRect(0,0,orbCanvas.getWidth(),orbCanvas.getHeight());
        double vibrationOffset = 0.0;

        // A simple line shows the limit of the orbArray. If any orb is placed below this line, the player loses.
        double deathLineY = ROW_HEIGHT*(ARRAY_HEIGHT-1)+2*ORB_RADIUS;
        orbDrawer.setStroke(Color.PINK);
        orbDrawer.setLineWidth(2.0);
        orbDrawer.strokeLine(0,deathLineY,liveBoundary.getWidth(),deathLineY);

        // Paint dropping Orbs:
        for(Orb orb: playPanelData.getDroppingOrbs()) orb.drawSelf(orbDrawer, vibrationOffset);

        // If we are close to adding 1 more level to the orbArray, apply a vibration effect to the array orbs:
        if(playPanelData.getShotsUntilNewRow()<=VIBRATION_FREQUENCIES.length && playPanelData.getShotsUntilNewRow()>0){
            vibrationOffset = Math.sin(2*Math.PI*System.nanoTime()*VIBRATION_FREQUENCIES[playPanelData.getShotsUntilNewRow()-1]/1000000000)*VIBRATION_OFFSETS[playPanelData.getShotsUntilNewRow()-1];
        }

        // paint Array orbs:
        Orb[][] orbArray = playPanelData.getOrbArray();
        for(int i=0; i<ARRAY_HEIGHT; ++i){
            for(int j=0; j<ARRAY_WIDTH_PER_CHARACTER*numPlayers; j++){
                orbArray[i][j].drawSelf(orbDrawer, vibrationOffset);
            }
        }

        // paint Death orbs:
        for(Orb orb: playPanelData.getDeathOrbs()) orb.drawSelf(orbDrawer,vibrationOffset);

        // paint VisualFlourishes:
        for(VisualFlourish visualFlourish : visualFlourishes) visualFlourish.drawSelf(orbDrawer);

        // paint transferring orbs:
        for(Orb orb: playPanelData.getTransferInOrbs()) orb.drawSelf(orbDrawer, vibrationOffset);

        // remaining orbs should not vibrate:
        vibrationOffset = 0.0;

        // Paint bursting orbs:
        for(Orb orb: playPanelData.getBurstingOrbs()) orb.drawSelf(orbDrawer, vibrationOffset);

        // Paint ammunition orbs:
        for(Player player : playerList){
            List<Orb> ammunitionOrbs = player.getPlayerData().getAmmunition();
            ammunitionOrbs.get(0).drawSelf(orbDrawer, vibrationOffset);
            ammunitionOrbs.get(1).drawSelf(orbDrawer, vibrationOffset);
        }

        // Paint shooting orbs:
        for(Orb orb : playPanelData.getShootingOrbs()) orb.drawSelf(orbDrawer, vibrationOffset);
    }

    public PlayPanelData getPlayPanelData(){
        return playPanelData;
    }

    public void changeAddTransferInOrbs(List<Orb> transferOutOrbs){
        playPanelData.changeAddTransferInOrbs(transferOutOrbs,randomTransferOrbGenerator);
    }

    private class Collision{
        public Orb shooterOrb;
        public Orb arrayOrb;
        public double timeToCollision;

        // Constructor
        public Collision(Orb shooterOrb, Orb arrayOrb, double timeToCollision) {
            this.shooterOrb = shooterOrb;
            this.arrayOrb = arrayOrb;
            this.timeToCollision = timeToCollision;
        }
    }

    public List<Player> getPlayerList(){
        return playerList;
    }

    public void displayVictoryResults(VictoryType victoryType){
        Text statistics;
        VisualFlourish visualFlourish;
        String specializedStatistic;

        // Note: we must add getDroppingOrbs().size() to getCumulativeOrbsTransferred() because orbs that are dropping at the very end of the game haven't been added to the transferOutOrbs list, yet.
        switch(victoryType) {
            case VS_WIN:
                visualFlourish = new VisualFlourish(MiscAnimations.WIN_SCREEN, 0, 0, true);
                specializedStatistic = "Orbs transferred to other players: " + (playPanelData.getCumulativeOrbsTransferred() + playPanelData.getDroppingOrbs().size()) + "\n";
                break;
            case VS_LOSE:
                visualFlourish = new VisualFlourish(MiscAnimations.LOSE_SCREEN, 0, 0, true);
                specializedStatistic = "Orbs transferred to other players: " + (playPanelData.getCumulativeOrbsTransferred() + playPanelData.getDroppingOrbs().size()) + "\n";
                break;
            case PUZZLE_CLEARED:
                visualFlourish = new VisualFlourish(MiscAnimations.WIN_SCREEN, 0, 0, true);
                specializedStatistic = "Orbs dropped: " + playPanelData.getCumulativeOrbsDropped() + "\n";
                break;
            case PUZZLE_FAILED:
                visualFlourish = new VisualFlourish(MiscAnimations.LOSE_SCREEN, 0, 0, true);
                specializedStatistic = "Orbs dropped: " + playPanelData.getCumulativeOrbsDropped() + "\n";
                break;
            default:
                System.err.println("Unrecognized VictoryType. setting default visual Flourish.");
                visualFlourish = new VisualFlourish(MiscAnimations.WIN_SCREEN,0,0,true);
                specializedStatistic = "\n";
        }

        // Add an appropriate Win/Lose/Cleared animation
        visualFlourish.relocate(PLAYPANEL_WIDTH_PER_PLAYER*numPlayers/2, PLAYPANEL_HEIGHT/2-100);
        visualFlourishes.add(visualFlourish);

        // Display statistics for this playpanel
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

