package Classes.NetworkCommunication;

public class SynchronizedComparable<T extends Comparable<T>> extends SynchronizedData<T>{
    public SynchronizedComparable(String name, T data, Setable<T> setInterface, Changeable<T> changeInterface, Precedence precedence, long parentID, Synchronizer synchronizer, int syncTolerance){
        super(name, parentID, synchronizer, precedence, syncTolerance);
        registerExternalSetters(setInterface,changeInterface);
        setTo(data);
    }

    public SynchronizedComparable(String name, T data, Precedence precedence, long parentID, Synchronizer synchronizer){
        super(name, parentID, synchronizer, precedence, 24);
        setTo(data);
    }

    public SynchronizedComparable(SynchronizedComparable other, Synchronizer synchronizer){
        this(other.getName(), (T)other.getData(), other.getPrecedence(), other.getParentID(), synchronizer);
    }

    public int compareTo(SynchronizedData other){
        return getData().compareTo((T)other.getData());
    }
}
