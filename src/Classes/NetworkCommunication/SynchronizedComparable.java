package Classes.NetworkCommunication;

public class SynchronizedComparable<T extends Comparable<T>> extends SynchronizedData<T>{
    public SynchronizedComparable(String name, T data, Setable<T> setInterface, Changeable<T> changeInterface, Precedence precedence, SynchronizedParent synchronizedParent, Synchronizer synchronizer, int syncTolerance){
        super(name, synchronizedParent, synchronizer, precedence, syncTolerance);
        registerExternalSetters(setInterface,changeInterface);
        setTo(data);
    }

    public SynchronizedComparable(String name, T data, Precedence precedence, SynchronizedParent synchronizedParent, Synchronizer synchronizer){
        super(name, synchronizedParent, synchronizer, precedence, 0);
        setTo(data);
    }

    public SynchronizedComparable(SynchronizedComparable other, Synchronizer synchronizer){
        this(other.getName(), (T)other.getData(), other.getPrecedence(), null, synchronizer);
    }

    public int compareTo(SynchronizedData other){
        return data.compareTo((T)other.getData());
    }
}
