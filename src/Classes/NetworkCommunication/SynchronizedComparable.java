package Classes.NetworkCommunication;

public class SynchronizedComparable<T extends Comparable<T>> extends SynchronizedData<T>{
    public SynchronizedComparable(String name, T data, Setable<T> setInterface, Setable<T> changeInterface, Precedence precedence, long parentID, Synchronizer synchronizer, int syncTolerance){
        super(name, parentID, synchronizer, precedence, syncTolerance);
        registerExternalSetters(setInterface,changeInterface);
        setTo(data);
    }

    public SynchronizedComparable(String name, T data, Precedence precedence, long parentID, Synchronizer synchronizer){
        super(name, parentID, synchronizer, precedence, 24);
        setTo(data);
    }

    public SynchronizedComparable(SynchronizedComparable<T> other, Synchronizer synchronizer){
        this(other.getName(), other.data, other.getPrecedence(), other.getParentID(), synchronizer);
    }

    @Override
    public T getData(){
        return data;
    }

    @Override
    public int compareTo(SynchronizedData<T> other){
        return data.compareTo(other.data);
    }

    @Override
    public void changeTo(T newValue){
        if(getExternalChanger()!=null) getExternalChanger().handle(newValue, Mode.SET, 0, 0);
        data = newValue;
        getSynchronizer().addToChangedData(this);
    }

    @Override
    public void setTo(T newValue){
        if(getExternalSetter()!=null) getExternalSetter().handle(newValue, Mode.SET, 0, 0);
        data = newValue;
    }
}
