package Classes.NetworkCommunication;

import java.io.Serializable;
import java.util.*;

import static Classes.Player.HOST_ID;

public class SynchronizedList<T extends Comparable<T> & Serializable> extends SynchronizedData<LinkedList<T>> {

    // SEND_ONCE: Use this option for data you only want to send once over the network. Local data is cleared after the
    // packet is sent. Example: chat messages.
    // KEEP_SYNCHRONIZED: Use this option for data you want to keep sending back and forth between host and client to
    // ensure consistency. Example: AmmunitionOrbs list.
    public enum SynchronizationType {SEND_ONCE, KEEP_SYNCHRONIZED}

    SynchronizationType synchronizationType = SynchronizationType.KEEP_SYNCHRONIZED;

    public SynchronizedList(String name, LinkedList<T> data, Setable<LinkedList<T>> setInterface, Setable<LinkedList<T>> changeInterface, Precedence precedence, long parentID, Synchronizer synchronizer, SynchronizationType synchronizationType, int syncTolerance){
        super(name, parentID, synchronizer, precedence, syncTolerance);
        registerExternalSetters(setInterface, changeInterface);
        this.synchronizationType = synchronizationType;
        this.data = new LinkedList<>();
        setTo(data);
    }

    public SynchronizedList(String name, LinkedList<T> data, Precedence precedence, long parentID, Synchronizer synchronizer){
        super(name, parentID, synchronizer, precedence, 24);
        this.data = new LinkedList<>();
        setTo(data);
    }

    public SynchronizedList(String name, LinkedList<T> data, Precedence precedence, long parentID, Synchronizer synchronizer, SynchronizationType synchronizationType){
        super(name, parentID, synchronizer, precedence, 24);
        this.synchronizationType = synchronizationType;
        this.data = new LinkedList<>();
        setTo(data);
    }

    // this kinda sorta returns the "edit distance" between the two lists.
    public int compareTo(SynchronizedData<LinkedList<T>> other){
        if(data.containsAll(other.data) || other.data.containsAll(data)){
            return data.size()-other.data.size();
        }
        else{
            int diff = 0;
            for(T t : data) if(!other.data.contains(t)) diff++;
            for(T t : other.data) if(!data.contains(t)) diff++;
            return diff;
        }
    }

    public void setAdd(T newItem){
        synchronized (synchronizer){
            data.add(newItem);
            int index = data.indexOf(newItem);
            LinkedList<T> newItemInList = new LinkedList<>(Collections.singleton(newItem));
            if(getExternalSetter()!=null) getExternalSetter().handle(newItemInList, Mode.ADD, index, index);
        }
    }

    public void setAddAll(Collection<T> newItems){
        synchronized (synchronizer){
            for(T t : newItems){
                setAdd(t);
            }
        }
    }

    public void setRemove(T itemToRemove){
        synchronized (synchronizer){
            int index = data.indexOf(itemToRemove);
            LinkedList<T> removedItemInList = new LinkedList<>(Collections.singleton(itemToRemove));
            data.remove(itemToRemove);
            if(getExternalSetter()!=null) getExternalSetter().handle(removedItemInList, Mode.REMOVE, index, index);
        }
    }

    public T setRemove(int index){
        synchronized (synchronizer){
            T removedData = data.remove(index);
            LinkedList<T> removedItemInList = new LinkedList<>(Collections.singleton(removedData));
            if(getExternalSetter()!=null) getExternalSetter().handle(removedItemInList, Mode.REMOVE, index, index);
            return removedData;
        }
    }

    public void setRemoveAll(Collection<T> itemsToRemove){
        synchronized (synchronizer){
            for(T t : itemsToRemove){
                setRemove(t);
            }
        }
    }

    public void setClear(){
        synchronized (synchronizer){
            setRemoveAll(new LinkedList<>(data)); // a new LinkedList is used to avoid a ConcurrentModificationException.
        }
    }

    public void changeAdd(T newItem){
        synchronized (synchronizer){
            data.add(newItem);
            int index = data.indexOf(newItem);
            LinkedList<T> newItemInList = new LinkedList<>(Collections.singletonList(newItem));
            if(getExternalChanger()!=null) getExternalChanger().handle(newItemInList, Mode.ADD, index, index);
            getSynchronizer().addToChangedData(this);
        }
    }

    public void changeAddAll(Collection<T> newItems){
        synchronized (synchronizer){
            for(T t : newItems){
                changeAdd(t);
            }
        }
    }

    public void changeRemove(T itemToRemove){
        synchronized (synchronizer){
            int index = data.indexOf(itemToRemove);
            LinkedList<T> removedItemInList = new LinkedList<>(Collections.singleton(itemToRemove));
            data.remove(itemToRemove);
            if(getExternalChanger()!=null) getExternalChanger().handle(removedItemInList, Mode.REMOVE, index, index);
            getSynchronizer().addToChangedData(this);
        }
    }

    public T changeRemove(int index){
        synchronized (synchronizer){
            T removedItem = data.get(index);
            LinkedList<T> removedItemInList = new LinkedList<>(Collections.singleton(removedItem));
            data.remove(index);
            if(getExternalChanger()!=null) getExternalChanger().handle(removedItemInList, Mode.REMOVE, index, index);
            getSynchronizer().addToChangedData(this);
            return removedItem;
        }
    }

    public void changeRemoveAll(Collection<T> itemsToRemove){
        synchronized (synchronizer){
            for(T t : itemsToRemove){
                changeRemove(t);
            }
        }
    }

    public void changeClear(){
        synchronized (synchronizer){
            changeRemoveAll(new LinkedList<>(data)); // a new LinkedList is used to avoid a ConcurrentModificationException.
        }
    }

    @Override
    public void setTo(LinkedList<T> newList){
        synchronized (synchronizer){
            // call the ExternalSetter on each existing element as it is removed.
            setClear();

            // replace data with the new list, calling the ExternalSetter on each element as it is added
            setAddAll(newList);
        }
    }

    // Note: changeTo() is called by synchronizeWith() in the Synchronizer class. If the SynchronizationType is
    // SEND_ONCE, we don't want to clear the data until we've called copyForNetworking().
    @Override
    public void changeTo(LinkedList<T> newList){
        synchronized (synchronizer){
            switch(synchronizationType){
                case KEEP_SYNCHRONIZED:
                    // call the ExternalChanger on each existing element as it is removed.
                    changeClear();
                case SEND_ONCE:
                    // add new data to the list, calling the ExternalChanger on each element as it is added
                    changeAddAll(newList);
                    break;
            }
        }
    }

    public static void main(String[] args){
        Synchronizer synchronizer = new Synchronizer(HOST_ID);
        SynchronizedList<String> myList = new SynchronizedList<String>("test", new LinkedList<>(), Precedence.CLIENT, 21, synchronizer);
        myList.setAdd("hello");
        myList.setAdd("yellow");
        myList.setAdd("hi");
        synchronizer.printData();
        System.out.println("number of changed datas: " + synchronizer.getChangedData().size());

        SynchronizedList<String> myList2 = new SynchronizedList<>("test2", new LinkedList<>(Arrays.asList("blah", "blue", "blot")), Precedence.CLIENT, 21, synchronizer);
        myList2.changeAddAll(Arrays.asList("blick","block","bluke"));

        myList.setRemove("yellow");
        myList.changeAdd("howdy");
        synchronizer.printData();
        System.out.println("number of changed datas: " + synchronizer.getChangedData().size());

        myList2.setRemoveAll(Arrays.asList("blick","block","bluke"));
        synchronizer.printData();
        System.out.println("number of changed datas: " + synchronizer.getChangedData().size());

        System.out.println("comparing the two lists: " + myList.compareTo(myList2));
        myList2.setTo(myList.getData());
        System.out.println("comparing again: " + myList.compareTo(myList2));

        SynchronizedList<String> myList3 = new SynchronizedList<>("test",new LinkedList<>(Arrays.asList("test1", "test2", "test3")), Precedence.CLIENT, 21, synchronizer);

    }
}
