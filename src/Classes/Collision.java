package Classes;

import Classes.OrbData;

public class Collision{
    public OrbData shooterOrb;
    public OrbData arrayOrb;
    public double timeToCollision;

    // Constructor
    public Collision(OrbData shooterOrb, OrbData arrayOrb, double timeToCollision) {
        this.shooterOrb = shooterOrb;
        this.arrayOrb = arrayOrb;
        this.timeToCollision = timeToCollision;
    }
}