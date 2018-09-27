package Classes.NetworkCommunication;

import java.io.Serializable;
import java.util.*;

public class SynchronizedList<T extends Comparable<T> & Serializable> extends SynchronizedData<LinkedList<T>> {
    public SynchronizedList(String name, LinkedList<T> data, Precedence precedence, long parentID, Synchronizer synchronizer){
        super(name, parentID, synchronizer, precedence, 24);
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
        data.add(newItem);
        int index = data.indexOf(newItem);
        LinkedList<T> newItemInList = new LinkedList<>(Collections.singleton(newItem));
        if(getExternalSetter()!=null) getExternalSetter().handle(newItemInList, Mode.ADD, index, index);
    }

    public void setAddAll(Collection<T> newItems){
        for(T t : newItems){
            setAdd(t);
        }
    }

    public void setRemove(T itemToRemove){
        int index = data.indexOf(itemToRemove);
        LinkedList<T> removedItemInList = new LinkedList<>(Collections.singleton(itemToRemove));
        data.remove(itemToRemove);
        if(getExternalSetter()!=null) getExternalSetter().handle(removedItemInList, Mode.REMOVE, index, index);
    }

    public void setRemoveAll(Collection<T> itemsToRemove){
        for(T t : itemsToRemove){
            setRemove(t);
        }
    }

    public void changeAdd(T newItem){
        data.add(newItem);
        int index = data.indexOf(newItem);
        LinkedList<T> newItemInList = new LinkedList<>(Collections.singletonList(newItem));
        if(getExternalChanger()!=null) getExternalChanger().handle(newItemInList, Mode.ADD, index, index);
        getSynchronizer().addToChangedData(this);
    }

    public void changeAddAll(Collection<T> newItems){
        for(T t : newItems){
            changeAdd(t);
        }
    }

    public void changeRemove(T itemToRemove){
        int index = data.indexOf(itemToRemove);
        LinkedList<T> removedItemInList = new LinkedList<>(Collections.singleton(itemToRemove));
        data.remove(itemToRemove);
        if(getExternalChanger()!=null) getExternalChanger().handle(removedItemInList, Mode.REMOVE, index, index);
        getSynchronizer().addToChangedData(this);
    }

    public void changeRemoveAll(Collection<T> itemsToRemove){
        for(T t : itemsToRemove){
            changeRemove(t);
        }
    }

    @Override
    public LinkedList<T> getData(){
        return data;
    }

    @Override
    public void setTo(LinkedList<T> newList){
        // call the ExternalSetter on each existing element as it is removed.
        if(data == null) data = new LinkedList<>(); // to avoid a nullPointerException
        else setRemoveAll(new LinkedList<>(data)); // a new LinkedList is used to avoid a ConcurrentModificationException.

        // create a temporary copy of the new list.
        LinkedList<T> newListCopy = new LinkedList<>(newList);

        // replace data with the new list, then call the ExternalSetter on each new element as it is re-added to the
        // list. This is done to ensure that the index passed to the ExternalSetter is correct.
        data = newList;
        try{
            data.clear();
        } catch(UnsupportedOperationException e){
            System.err.println("Warning! A List passed to the setTo() method in SynchronizedList appears to be " +
                    "fixed-size (perhaps you used Arrays.asList()?). SynchronizedList requires a List that supports " +
                    "add, remove, and clear. Defaulting to a LinkedList.");
            data = new LinkedList<>();
        }
        setAddAll(newListCopy);
    }

    @Override
    public void changeTo(LinkedList<T> newList){
        if(data == null) data = new LinkedList<>(); // to avoid a nullPointerException
        changeRemoveAll(new LinkedList<>(data)); // a new LinkedList is used to avoid a ConcurrentModificationException.

        // create a temporary copy of the new list.
        LinkedList<T> newListCopy = new LinkedList<>(newList);

        // replace data with the new list, then call the ExternalSetter on each new element as it is re-added to the
        // list. This is done to ensure that the index passed to the ExternalSetter is correct.
        data = newList;
        data.clear();
        changeAddAll(newListCopy);
    }

    public static void main(String[] args){
        Synchronizer synchronizer = new Synchronizer();
        SynchronizedList<String> myList = new SynchronizedList<String>("test", new LinkedList<String>(), Precedence.CLIENT, 21, synchronizer);
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
        myList.setRemoveAll(Arrays.asList("hello","hi","howdy"));
        myList.setAddAll(myList2.data);
        System.out.println("comparing again: " + myList.compareTo(myList2));
    }
}
