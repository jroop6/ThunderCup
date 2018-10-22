package Classes;

public class Collision{
    public Orb shooterOrb; // todo: consider replacing this with the *index* of the shooter Orb in the shootingOrbs list.
    public Orb arrayOrb;
    public double timeToCollision;

    // Constructor
    public Collision(Orb shooterOrb, Orb arrayOrb, double timeToCollision) {
        this.shooterOrb = shooterOrb;
        this.arrayOrb = arrayOrb;
        this.timeToCollision = timeToCollision;
    }
}