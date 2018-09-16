package Classes.NetworkCommunication;

import java.io.Serializable;

public abstract class SynchronizedData<T> implements Comparable<SynchronizedData>, Serializable {
    // The actual data:
    protected T data;

    // A key for identifying this data in a HashMap:
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


	public SynchronizedData(String name, SynchronizedParent synchronizedParent, Synchronizer synchronizer, Precedence precedence, int syncTolerance){
	    if(synchronizedParent!=null) this.name = synchronizedParent.getID() + name;
	    else this.name = name;
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

    public String getName(){
        return name;
    }
    public Precedence getPrecedence(){
	    return precedence;
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

    public T getData(){
	    return data;
    }

    public static void main(String[] args){
        System.out.println("hello!");
        Synchronizer synchronizer = new Synchronizer();
        SynchronizedParent exampleParent = () -> "123456";
        Setable<Integer> exampleSettable = (newValue -> System.out.println("setting to " + newValue));
        Changeable<Integer> exampleChangeable = (newValue -> System.out.println("changing to " + newValue));
        SynchronizedComparable<Integer> testInt = new SynchronizedComparable<>("testInt", 0, Precedence.HOST, exampleParent, synchronizer);

        System.out.println("value before set is " + testInt.getData());
        testInt.setTo(21);
        System.out.println("value after set is " + testInt.getData());

        System.out.println("size of changed data is " + synchronizer.getChangedData().size());
        testInt.changeTo(13);
        System.out.println("value after change is " + testInt.getData());
        System.out.println("size of changed data is " + synchronizer.getChangedData().size());

    }
}
