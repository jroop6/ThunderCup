package Classes;

import Classes.Animation.Animation;
import Classes.Animation.AnimationData;
import Classes.Animation.PlayOption;
import Classes.Audio.SoundEffect;
import Classes.NetworkCommunication.PlayPanelData;

import java.util.*;

import static Classes.NetworkCommunication.PlayPanelData.ARRAY_HEIGHT;
import static Classes.NetworkCommunication.PlayPanelData.ARRAY_WIDTH_PER_CHARACTER;
import static Classes.OrbData.NULL;
import static Classes.OrbData.ORB_RADIUS;
import static Classes.PlayPanel.PLAYPANEL_WIDTH_PER_PLAYER;
import static java.lang.Math.PI;

public class PlayPanelUtility {

    public static final double FOUR_R_SQUARED = 4 * ORB_RADIUS * ORB_RADIUS;


    /* *********************************************** UTILITY *********************************************** */

    public void findPatternCompletions(List<Collision> collisions, OrbData[][] orbArray, List<OrbData> shootingOrbsToBurst, Set<SoundEffect> soundEffectsToPlay, List<OrbData> orbsToTransfer, List<OrbData> arrayOrbsToBurst){
        for(Collision collision : collisions){
            if(shootingOrbsToBurst.contains(collision.shooterOrb)) continue; // Only consider orbs that are not already in the bursting orbs list

            // find all connected orbs of the same color
            Set<OrbData> connectedOrbs = new HashSet<>();
            cumulativeDepthFirstSearch(collision.shooterOrb, connectedOrbs, orbArray, PlayPanelData.FilterOption.SAME_COLOR);
            if(connectedOrbs.size() > 2) arrayOrbsToBurst.addAll(connectedOrbs);

            // If there are a sufficient number grouped together, then add a transfer out orb of the same color:
            int numTransferOrbs;
            if((numTransferOrbs = (connectedOrbs.size()-3)/2) > 0) {
                soundEffectsToPlay.add(SoundEffect.DROP);
                Iterator<OrbData> orbIterator = connectedOrbs.iterator();
                for(int k=0; k<numTransferOrbs; k++){
                    OrbData orbToTransfer = orbIterator.next();
                    orbsToTransfer.add(new OrbData(orbToTransfer)); // add a copy of the orb, so we can change the animationEnum without messing up the original (which still needs to burst).
                }
            }
            // todo: move this code outside of this class.
            /*if(orbArray == playPanelData.getOrbArray() && connectedOrbs.size()>playPanelData.getLargestGroupExplosion()){
                playPanelData.setLargestGroupExplosion(connectedOrbs.size());
            }*/
        }
    }

    public void findConnectedOrbs(OrbData[][] orbArray, Set<OrbData> connectedOrbs){
        // find all orbs connected to the ceiling:
        for(int j=0; j<orbArray[0].length; j++){
            if(orbArray[0][j]==NULL) continue;
            cumulativeDepthFirstSearch(orbArray[0][j], connectedOrbs, orbArray, PlayPanelData.FilterOption.ALL);
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
    public void findFloatingOrbs(Set<OrbData> connectedOrbs, OrbData[][] orbArray, List<OrbData> orbsToDrop){
        // any orbs in the array that are not among the connectedOrbs Set are floating.
        for(int i=0; i<ARRAY_HEIGHT; i++){
            for(int j=0; j<orbArray[i].length; j++){
                OrbData arrayOrb = orbArray[i][j];
                if(arrayOrb!=NULL && !connectedOrbs.contains(arrayOrb)){
                    orbsToDrop.add(arrayOrb);
                }
            }
        }
    }

    public void cumulativeDepthFirstSearch(OrbData source, Set<OrbData> matches, OrbData[][] orbArray, PlayPanelData.FilterOption filter) {

        // A boolean orbArray that has the same size as the orbArray, to mark orbs as "examined"
        Boolean[][] examined = new Boolean[orbArray.length][orbArray[0].length];
        for(int i=0; i<orbArray.length; i++){
            for(int j=0; j<orbArray[i].length; j++){
                examined[i][j] = false;
            }
        }

        // A stack containing the "active" elements to be examined next
        Deque<OrbData> active = new LinkedList<>();

        // Add the source Orb to the active list and mark it as "examined"
        active.push(source);
        if(validArrayCoordinates(source, examined)) examined[source.getI()][source.getJ()] = true; // deathOrbs have "invalid" coordinates, so we have to check whether the coordinates are valid.

        // Mark everything in the starting set as "examined"
        for(OrbData orbData : matches) examined[orbData.getI()][orbData.getJ()] = true;

        // Do a depth-first search
        while (!active.isEmpty()) {
            OrbData activeOrb = active.pop();
            matches.add(activeOrb);
            List<OrbData> neighbors = getNeighbors(activeOrb, orbArray); // recall: neighbors does not contain any death Orbs. Only orbArray Orbs.
            for (OrbData neighbor : neighbors) {
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


    public void simulateOrbs(OrbData[][] orbArray, List<OrbData> burstingOrbs, List<OrbData> shootingOrbs, List<OrbData> droppingOrbs, OrbData[] deathOrbs, Set<SoundEffect> soundEffectsToPlay, List<OrbData> orbsToDrop, List<OrbData> orbsToTransfer, List<Collision> collisions, Set<OrbData> connectedOrbs, List<OrbData> arrayOrbsToBurst, double deltaTime){
        // Advance shooting orbs and detect collisions:
        advanceShootingOrbs(shootingOrbs, orbArray, deltaTime, soundEffectsToPlay, collisions); // Updates model

        // Snap any landed shooting orbs into place on the orbArray (or deathOrbs array):
        List<OrbData> shootingOrbsToBurst = snapOrbs(collisions, orbArray, deathOrbs,soundEffectsToPlay);

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
    // Note: recall that the y-axis points downward and shootingOrb.getAngle() returns a negative value.
    public void advanceShootingOrbs(List<OrbData> shootingOrbs, OrbData[][] orbArray, double timeRemainingInFrame, Set<SoundEffect> soundEffectsToPlay, List<Collision> collisions) {
        // Put all possible collisions in here. If a shooter orb's path this frame would put it on a collision course
        // with the ceiling, a wall, or an array orb, then that collision will be added to this list, even if there is
        // another orb in the way.
        List<Collision> possibleCollisionPoints = new LinkedList<>();

        for (OrbData shootingOrb : shootingOrbs) {
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
            int numPlayers = orbArray[0].length/ARRAY_WIDTH_PER_CHARACTER;
            double xRightWallP = (PLAYPANEL_WIDTH_PER_PLAYER * numPlayers + ORB_RADIUS) - ORB_RADIUS - x0;
            double xLeftWallP = ORB_RADIUS - x0;
            double yCeilingP = ORB_RADIUS - y0;
            if (x1P >= xRightWallP) {
                double timeToCollision = timeRemainingInFrame * xRightWallP / x1P;
                possibleCollisionPoints.add(new Collision(shootingOrb, OrbData.WALL, timeToCollision));
            }
            if (x1P <= xLeftWallP) {
                double timeToCollision = timeRemainingInFrame * xLeftWallP / x1P;
                possibleCollisionPoints.add(new Collision(shootingOrb, OrbData.WALL, timeToCollision));
            }

            // ToDo: check for and add collisions with other shooting orbs

            // Check for and add collisions with the ceiling:
            if(y1P<yCeilingP){
                double timeToCollision = timeRemainingInFrame * yCeilingP / y1P;
                possibleCollisionPoints.add(new Collision(shootingOrb, OrbData.CEILING, timeToCollision));
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
            for (OrbData shootingOrb : shootingOrbs) {
                double angle = shootingOrb.getAngle();
                double distanceToTravel = shootingOrb.getSpeed() * soonestCollisionTime;
                shootingOrb.relocate(shootingOrb.getXPos() + distanceToTravel * Math.cos(angle), shootingOrb.getYPos() + distanceToTravel * Math.sin(angle));
            }

            // If there was a collision with a wall, then just reflect the shooter orb's angle.
            if (soonestCollision.arrayOrb == OrbData.WALL) {
                soundEffectsToPlay.add(SoundEffect.CHINK);
                OrbData shooterOrb = soonestCollision.shooterOrb;
                shooterOrb.setAngle(PI - shooterOrb.getAngle());
            }

            // If the collision is between two shooter orbs, compute new angles and speeds. If the other shooting orb is
            // in the process of snapping, then burst this shooting orb.
            else if (shootingOrbs.contains(soonestCollision.arrayOrb)) {
                // Todo: do this.
            }

            // If the collision was with the ceiling, set that orb's speed to zero and add it to the collisions list.
            else if(soonestCollision.arrayOrb == OrbData.CEILING){
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
            for (OrbData shootingOrb : shootingOrbs) {
                double angle = shootingOrb.getAngle();
                double distanceToTravel = shootingOrb.getSpeed() * timeRemainingInFrame;
                shootingOrb.relocate(shootingOrb.getXPos() + distanceToTravel * Math.cos(angle), shootingOrb.getYPos() + distanceToTravel * Math.sin(angle));
            }
        }
    }

    public List<OrbData> snapOrbs(List<Collision> snaps, OrbData[][] orbArray, OrbData[] deathOrbs, Set<SoundEffect> soundEffectsToPlay){

        List<OrbData> orbsToBurst = new LinkedList<>();

        for(Collision snap : snaps){
            int iSnap;
            int jSnap;

            // Compute snap coordinates for orbs that collided with the ceiling
            if(snap.arrayOrb== OrbData.CEILING){
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

    public void changeBurstShootingOrbs(List<OrbData> newBurstingOrbs, List<OrbData> shootingOrbs, List<OrbData> burstingOrbs){
        shootingOrbs.removeAll(newBurstingOrbs);
        burstingOrbs.addAll(newBurstingOrbs);
        for(OrbData orbData : newBurstingOrbs){
            orbData.setOrbAnimationState(OrbData.OrbAnimationState.IMPLODING);
        }
    }

    public void changeBurstArrayOrbs(List<OrbData> newBurstingOrbs, OrbData[][] orbArray, OrbData[] deathOrbs, List<OrbData> burstingOrbs){
        for(OrbData orbData : newBurstingOrbs){
            if(validArrayCoordinates(orbData, orbArray)){
                orbArray[orbData.getI()][orbData.getJ()] = NULL;
            }
            else{ // the Orb is on the deathOrbs array.
                deathOrbs[orbData.getJ()] = NULL;
            }
            orbData.setOrbAnimationState(OrbData.OrbAnimationState.IMPLODING);
            burstingOrbs.add(orbData);
        }
        // todo: move this code outside of the simulation.
        /*if(burstingOrbs == this.burstingOrbs){ // to prevent a simulated shot from setting these values.
            cumulativeOrbsBurst += newBurstingOrbs.size();
        }*/
    }
    public void changeDropArrayOrbs(List<OrbData> newDroppingOrbs, List<OrbData> droppingOrbs, OrbData[][] orbArray){
        for(OrbData orbData : newDroppingOrbs){
            droppingOrbs.add(orbData);
            orbArray[orbData.getI()][orbData.getJ()] = NULL;
        }
        // todo: move this code outside of the simulation.
        /*if(droppingOrbs == this.droppingOrbs){ // to prevent a simulated shot from setting these values.
            cumulativeOrbsDropped += newDroppingOrbs.size();
        }*/
    }
    public void changeAddDeathOrb(OrbData orbData, OrbData[] deathOrbs){
        deathOrbs[orbData.getJ()] = orbData;
    }

    public void transferOrbs(List<OrbData> transferOutOrbs, Set<OrbData> transferInOrbs, Random randomTransferOrbGenerator, OrbData[][] orbArray){
        // Make a deep copy of the orbs to be transferred. We can't place the same orb instance in 2 PlayPanels
        List<OrbData> newTransferOrbs = deepCopyOrbList(transferOutOrbs);

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
        List<OrbData> addedTransferOrbs = new LinkedList<>();
        for(OrbData orbData : newTransferOrbs){
            if(openSpots.isEmpty()) break; //todo: temporary fix to avoid calling nextInt(0). In the future, place transferOrbs in secondary and tertiary locations.
            int index = randomTransferOrbGenerator.nextInt(openSpots.size());
            PointInt openSpot = openSpots.get(index);
            orbData.setIJ(openSpot.getI(),openSpot.getJ());
            openSpots.remove(index);
            addedTransferOrbs.add(orbData);
        }

        // now, finally add them to the appropriate Orb list:
        transferInOrbs.addAll(addedTransferOrbs);
    }

    public void snapTransferOrbs(List<OrbData> transferOrbsToSnap, OrbData[][] orbArray, Set<OrbData> connectedOrbs, Set<SoundEffect> soundEffectsToPlay, List<AnimationData> visualFlourishes, Set<OrbData> transferInOrbs){
        for(OrbData orbData : transferOrbsToSnap){
            // only those orbs that would be connected to the ceiling should materialize:
            List<OrbData> neighbors = getNeighbors(orbData, orbArray);
            if((orbData.getI()==0 || !Collections.disjoint(neighbors,connectedOrbs)) && orbArray[orbData.getI()][orbData.getJ()] == NULL){
                orbArray[orbData.getI()][orbData.getJ()] = orbData;
                soundEffectsToPlay.add(SoundEffect.MAGIC_TINKLE);
                visualFlourishes.add(new AnimationData(Animation.MAGIC_TELEPORTATION, orbData.getXPos(), orbData.getYPos(), PlayOption.PLAY_ONCE_THEN_VANISH));
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

    public boolean validDeathOrbsCoordinates(PointInt testPoint, OrbData[] deathOrbsArray){
        return validDeathOrbsCoordinates(testPoint.getI(), testPoint.getJ(), deathOrbsArray);
    }

    // Overloaded method to avoid the creation of a temporary object.
    public boolean validDeathOrbsCoordinates(int iCoordinate, int jCoordinate, OrbData[] deathOrbsArray){
        return (iCoordinate==ARRAY_HEIGHT && jCoordinate>=0 && jCoordinate<deathOrbsArray.length);
    }

    private boolean isTransferInOrbOccupyingPosition(int i, int j, Set<OrbData> transferInOrbs){
        for(OrbData orbData : transferInOrbs){
            if(orbData.getI()==i && orbData.getJ()==j) return true;
        }
        return false;
    }








    /* *********************************************** DEEP COPIERS *********************************************** */

    // todo: This doesn't work and I can't figure out why. An alternative implementation that does work is given below.
    /*public Orb[][] deepCopyOrbArray(Orb[][] other){
        Orb[][] copiedArray = new Orb[other.length][];
        // deep copy one row at a time:
        for(int i=0; i<other.length; i++){
            copiedArray[i] = deepCopyOrbArray(other[i]);
        }
        return copiedArray;
    }*/

    public OrbData[][] deepCopyOrbArray(OrbData[][] other){
        OrbData[][] copiedArray = new OrbData[other.length][other[0].length];
        // deep copy one row at a time:
        for(int i=0; i<other.length; i++){
            for(int j=0; j<other[0].length; j++){
                if(other[i][j].equals(NULL)) copiedArray[i][j] = NULL;
                else copiedArray[i][j] = new OrbData(other[i][j]);
            }
        }
        return copiedArray;
    }

    // Failed attempt to be clever
    /*public <E extends Collection<Orb>> E deepCopyOrbCollection(E other){
        E copiedCollection = Collections.checkedCollection(other, Orb.class); // Does not work, unfortunately...
        copiedCollection.clear();
        for(Orb orb : other){
            copiedCollection.add(new Orb(orb));
        }
        return copiedCollection;
    }*/

    public OrbData[] deepCopyOrbArray(OrbData[] other){
        OrbData[] copiedArray = new OrbData[other.length];
        for(int j=0; j<other.length; j++){
            if(other[j].equals(NULL)) copiedArray[j] = NULL;
            else other[j] = new OrbData(other[j]);
        }
        return copiedArray;
    }

    public List<OrbData> deepCopyOrbList(List<OrbData> other){
        List<OrbData> copiedList = new LinkedList<>();
        for(OrbData orbData : other){
            copiedList.add(new OrbData(orbData));
        }
        return copiedList;
    }

    public List<AnimationData> deepCopyVisualFlourishesList(List<AnimationData> other){
        List<AnimationData> copiedList = new LinkedList<>();
        for(AnimationData visualFlourish : other){
            copiedList.add(new AnimationData(visualFlourish));
        }
        return copiedList;
    }

    public Set<OrbData> deepCopyOrbSet(Set<OrbData> other){
        Set<OrbData> copiedSet = new HashSet<>();
        for(OrbData orbData : other){
            copiedSet.add(new OrbData(orbData));
        }
        return copiedSet;
    }
}
