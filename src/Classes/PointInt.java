package Classes;

import java.io.Serializable;

// My own implementation of a point with integer coordinates. I'm implementing it myself here instead of using
// java.awt.Point in order to avoid mixing the awt and JavaFX libraries.
public class PointInt implements Serializable {
    protected int i;
    protected int j;

    public PointInt(int i, int j){
        this.i = i;
        this.j = j;
    }

    public int getI(){
        return i;
    }
    public int getJ(){
        return j;
    }

    protected void setIJ(int i, int j){
        this.i = i;
        this.j = j;
    }

    @Override
    public boolean equals(Object other){
        if(other instanceof PointInt){
            return ((PointInt)other).i==this.i && ((PointInt)other).j==this.j;
        }
        else return false;
    }

    @Override
    public int hashCode(){
        // The Cantor pairing function is used for the hash, which is one-to-one, onto, and returns small outputs for
        // small inputs:
        return ((i+j)*(i+j+1))/2 + j;
    }
}
