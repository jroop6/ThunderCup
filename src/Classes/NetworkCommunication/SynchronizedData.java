package Classes.NetworkCommunication;

import java.io.Serializable;


public abstract class SynchronizedData<T> implements Comparable<SynchronizedData>, Serializable {
    // The actual data:
    private T data;

    // Keys for uniquely identifying this data in a HashMap:
    private long parentID;
    private String name;

    // these functional interfaces provide additional code that is executed whenever this data is set or changed.
    private Setable<T> externalSetter;
    private Changeable<T> externalChanger;

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

    public void registerExternalSetters(Setable<T> externalSetter, Changeable<T> externalChanger){
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

    public T getData(){
        return data;
    }
    // changeTo is called by whichever machine holds precedence (host or client):
    public void changeTo(T newValue){
        data = newValue;
        if(externalChanger!=null) externalChanger.changeTo(data);
        synchronizer.addToChangedData(this);
    }
    // setTo is called by the machine that does NOT have precedence:
    public void setTo(T newValue){
        data = newValue;
        if(externalSetter!=null) externalSetter.setTo(data);
    }

    public Precedence getPrecedence(){
        return precedence;
    }
    public void setPrecedence(Precedence newPrecedence){
	    precedence = newPrecedence;
    }

}
