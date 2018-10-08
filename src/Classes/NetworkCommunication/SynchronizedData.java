package Classes.NetworkCommunication;

import java.io.*;


public abstract class SynchronizedData<T extends Serializable> implements Comparable<SynchronizedData<T>>, Serializable {
    // The actual data:
    protected T data;

    // Keys for uniquely identifying this data in a HashMap:
    private final long parentID;
    private final String name;

    // these functional interfaces provide additional code that is executed whenever this data is set or changed.
    private Setable<T> externalSetter;
    private Setable<T> externalChanger;

    // for managing synchronization between host and client:
    protected Synchronizer synchronizer;
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

    public T getData(){
        return data;
    }

    // changeTo is called by whichever machine holds precedence (host or client):
    abstract public void changeTo(T newValue);
    // setTo is called by the machine that does NOT have precedence:
    abstract public void setTo(T newValue);

    // returns a deep copy of this SynchronizedData, minus any non-serializable data such as lambda expressions.
    abstract public SynchronizedData<T> copyForNetworking(Synchronizer synchronizer);

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
    public int getSynchTolerance(){
        return syncTolerance;
    }

    public <E> E deepCopyDataElement(E dataToCopy){
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(dataToCopy);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            try{
                return (E)ois.readObject();
            } catch (ClassNotFoundException e){
                e.printStackTrace();
            }
        } catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }
}
