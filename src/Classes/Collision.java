package Classes;

public class Collision{
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