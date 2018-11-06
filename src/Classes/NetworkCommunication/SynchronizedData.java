package Classes.NetworkCommunication;

import java.io.*;

/**
 * Data is uniquely identified by 2 fields: parentID (a long) and name (a String). For example, the parentID might be a
 * player ID that uniquely identifies a player and the name could be something like "cannonAngle." Hence, we can have
 * many SynchronizedDatas with the same name, yet we are still able to distinguish which data belongs to which player.
 * For data belonging to a team and not just a particular player, you can use a team ID for parentID. For global game
 * data (e.g. pause), you can use a constant field like GAME_ID=0.
 */
public abstract class SynchronizedData<T extends Serializable> implements Comparable<SynchronizedData<T>>, Serializable {
    // The actual data:
    protected T data;

    // Keys for uniquely identifying this data:
    private final long parentID;
    private final String name;

    // these functional interfaces provide additional code that is executed whenever this data is set or changed.
    private transient Setable<T> externalSetter;

    // for managing synchronization between host and client:
    protected final Synchronizer synchronizer;
    private Precedence precedence;
    private int syncTolerance;
    private int framesOutOfSync = 0;

    // HOST = The host controls the value of the data. Client data *must* eventually agree with the host. Example: The final, official outcome of shooting a orb.
    // CLIENT = The client controls the value of the data. The host should accept new values from the client. Example: The team a player chooses to be on.
    // INFORMATIONAL = The host and client do not need to agree with each other. They still transmit the data over the network to inform each other of their own particular view. Example: playerType (Remote_HOSTVIEW vs BOT vs REMOTE_CLIENTVIEW, etc)
    public enum Precedence {HOST, CLIENT, INFORMATIONAL}

	public SynchronizedData(String name, long parentID, Synchronizer synchronizer, Precedence precedence, int syncTolerance){
        this.name = name;
        this.parentID = parentID;
        this.externalSetter = null;
        this.precedence = precedence;
        this.syncTolerance = syncTolerance;
        synchronizer.register(this);
        this.synchronizer = synchronizer;
    }

    void registerExternalSetter(Setable<T> externalSetter){
	    this.externalSetter = externalSetter;
    }

    void incrementFramesOutOfSync(){
	    framesOutOfSync++;
    }

    boolean isOutOfSync(){
	    return framesOutOfSync > syncTolerance;
    }

    void resetFramesOutOfSync(){
	    framesOutOfSync = 0;
    }

    long getParentID(){
	    return parentID;
    }
    public String getName(){
	    return name;
    }

    public T getData(){
        return data;
    }

    // changeTo is called by whichever machine holds precedence (host or client):
    abstract public void changeTo(T newValue);
    // setTo is called by the machine that does NOT have precedence:
    abstract public void setTo(T newValue);

    public Precedence getPrecedence(){
        return precedence;
    }
    public void setPrecedence(Precedence newPrecedence){
	    precedence = newPrecedence;
    }
    Setable<T> getExternalSetter(){
	    return externalSetter;
    }
    public Synchronizer getSynchronizer(){
	    return synchronizer;
    }
}
