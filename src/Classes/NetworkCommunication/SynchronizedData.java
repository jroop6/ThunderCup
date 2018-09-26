package Classes.NetworkCommunication;

import java.io.Serializable;


public abstract class SynchronizedData<T> implements Comparable<SynchronizedData<T>>, Serializable {
    // The actual data:
    protected T data;

    // Keys for uniquely identifying this data in a HashMap:
    private long parentID;
    private String name;

    // these functional interfaces provide additional code that is executed whenever this data is set or changed.
    private Setable<T> externalSetter;
    private Setable<T> externalChanger;

    // for managing synchronization between host and client:
    public enum Precedence {HOST, CLIENT}
    private Synchronizer synchronizer;
    private Precedence precedence;
    private int syncTolerance;
    private int framesOutOfSync = 0;

	public SynchronizedData(String name, long parentID, Synchronizer synchronizer, Precedence precedence, int syncTolerance){
        this.name = name;
        this.parentID = parentID;
        this.externalSetter = null;
        this.externalChanger = null;
        this.precedence = precedence;
        this.syncTolerance = syncTolerance;
        synchronizer.register(this);
        this.synchronizer = synchronizer;
    }

    public void registerExternalSetters(Setable<T> externalSetter, Setable<T> externalChanger){
	    this.externalSetter = externalSetter;
	    this.externalChanger = externalChanger;
    }

    public void incrementFramesOutOfSync(){
	    framesOutOfSync++;
    }

    public boolean isOutOfSync(){
	    return framesOutOfSync > syncTolerance;
    }

    public void resetFramesOutOfSync(){
	    framesOutOfSync = 0;
    }

    public long getParentID(){
	    return parentID;
    }
    public String getName(){
	    return name;
    }
    public String getKey(){
        return parentID + name;
    }

    // This should try to return a read-only view of the data:
    abstract public T getData();
    // handle is called by whichever machine holds precedence (host or client):
    abstract public void changeTo(T newValue);
    // handle is called by the machine that does NOT have precedence:
    abstract public void setTo(T newValue);

    public Precedence getPrecedence(){
        return precedence;
    }
    public void setPrecedence(Precedence newPrecedence){
	    precedence = newPrecedence;
    }
    public Setable<T> getExternalSetter(){
	    return externalSetter;
    }
    public Setable<T> getExternalChanger(){
	    return externalChanger;
    }
    public Synchronizer getSynchronizer(){
	    return synchronizer;
    }

}
