package Classes.NetworkCommunication;

import java.io.Serializable;
import java.util.*;

public class SynchronizedList<T extends Comparable<T> & Serializable> extends SynchronizedData<LinkedList<T>> {
    public SynchronizedList(String name, LinkedList<T> data, Setable<LinkedList<T>> setInterface, Setable<LinkedList<T>> changeInterface, Precedence precedence, long parentID, Synchronizer synchronizer, int syncTolerance){
        super(name, parentID, synchronizer, precedence, syncTolerance);
        registerExternalSetters(setInterface, changeInterface);
        this.data = new LinkedList<>();
        setTo(data);
    }

    public SynchronizedList(String name, LinkedList<T> data, Precedence precedence, long parentID, Synchronizer synchronizer){
        super(name, parentID, synchronizer, precedence, 24);
        this.data = new LinkedList<>();
        setTo(data);
    }

    public SynchronizedList(String name, LinkedList<T> data, Precedence precedence, long parentID, Synchronizer synchronizer, int syncTolerance){
        super(name, parentID, synchronizer, precedence, syncTolerance);
        this.data = new LinkedList<>();
        setTo(data);
    }


    public SynchronizedList(SynchronizedList<T> other, Synchronizer synchronizer){
        super(other.getName(), other.getParentID(), synchronizer, other.getPrecedence(), other.getSynchTolerance());
        // Perform a deep copy of each element in the List of data:
        LinkedList<T> otherDataListCopy = new LinkedList<>();
        for(T element : other.getData()){
            T elementCopy = deepCopyDataElement(element);
            otherDataListCopy.add(elementCopy);
        }
        this.data = new LinkedList<>();
        setTo(otherDataListCopy);
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

    @Override
    public void changeTo(LinkedList<T> newList){
        synchronized (synchronizer){
            // call the ExternalChanger on each existing element as it is removed.
            changeClear();

            // replace data with the new list, calling the ExternalChanger on each element as it is added
            changeAddAll(newList);
        }
    }

    public static void main(String[] args){
        Synchronizer synchronizer = new Synchronizer();
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
