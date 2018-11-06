package Classes.NetworkCommunication;

import java.io.*;

public class SynchronizedComparable<T extends Comparable<T> & Serializable> extends SynchronizedData<T>{
    public SynchronizedComparable(String name, T data, Setable<T> setInterface, Precedence precedence, long parentID, Synchronizer synchronizer, int syncTolerance){
        super(name, parentID, synchronizer, precedence, syncTolerance);
        registerExternalSetter(setInterface);
        setTo(data);
    }

    public SynchronizedComparable(String name, T data, Precedence precedence, long parentID, Synchronizer synchronizer){
        super(name, parentID, synchronizer, precedence, 24);
        setTo(data);
    }

    @Override
    public int compareTo(SynchronizedData<T> other){
        return data.compareTo(other.data);
    }

    @Override
    public void changeTo(T newValue){
        synchronized(synchronizer){
            if(getExternalSetter()!=null) getExternalSetter().handle(newValue, Mode.SET, 0, 0);
            data = newValue;
            getSynchronizer().addToChangedData(this);
        }
    }

    @Override
    public void setTo(T newValue){
        synchronized(synchronizer){
            if(getExternalSetter()!=null) getExternalSetter().handle(newValue, Mode.SET, 0, 0);
            data = newValue;
        }
    }
}
