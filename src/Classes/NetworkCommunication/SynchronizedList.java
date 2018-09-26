package Classes.NetworkCommunication;

import java.util.*;

public class SynchronizedList<T> extends SynchronizedData<List<T>> {
    public SynchronizedList(String name, List<T> data, Precedence precedence, long parentID, Synchronizer synchronizer){
        super(name, parentID, synchronizer, precedence, 24);
        setTo(data);
    }

    public SynchronizedList(SynchronizedList<T> other, Synchronizer synchronizer){
        this(other.getName(), other.data, other.getPrecedence(), other.getParentID(), synchronizer);
    }

    // this kinda sorta returns the "edit distance" between the two lists.
    public int compareTo(SynchronizedData<List<T>> other){
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
        List<T> newItemInList = data.subList(index,index+1);
        if(getExternalSetter()!=null) getExternalSetter().handle(newItemInList, Mode.ADD, index, index);
    }

    public void setAddAll(Collection<T> newItems){
        for(T t : newItems){
            setAdd(t);
        }
    }

    public void setRemove(T itemToRemove){
        int index = data.indexOf(itemToRemove);
        List<T> removedItemInList = new LinkedList<>(Collections.singleton(itemToRemove));
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
        List<T> newItemInList = data.subList(index,index+1);
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
        List<T> removedItemInList = new LinkedList<>(Collections.singleton(itemToRemove));
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
    public List<T> getData(){
        return Collections.unmodifiableList(data);
    }

    @Override
    public void setTo(List<T> newList){
        // call the ExternalSetter on each existing element as it is removed.
        if(data == null) data = new LinkedList<>(); // to avoid a nullPointerException
        else setRemoveAll(new LinkedList<>(data)); // a new LinkedList is used to avoid a ConcurrentModificationException.

        // create a temporary copy of the new list.
        LinkedList<T> newListCopy = new LinkedList<>(newList);

        // replace data with the new list, then call the ExternalSetter on each new element as it is re-added to the
        // list. This is done to ensure that the index passed to the ExternalSetter is correct. Also, the type of list
        // might change (i.e. LinkedList becomes ArrayList).
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
    public void changeTo(List<T> newList){
        if(data == null) data = new LinkedList<>(); // to avoid a nullPointerException
        changeRemoveAll(new LinkedList<>(data)); // a new LinkedList is used to avoid a ConcurrentModificationException.

        // create a temporary copy of the new list.
        LinkedList<T> newListCopy = new LinkedList<>(newList);

        // replace data with the new list, then call the ExternalSetter on each new element as it is re-added to the
        // list. This is done to ensure that the index passed to the ExternalSetter is correct. Also, the type of list
        // might change (i.e. LinkedList becomes ArrayList).
        data = newList;
        try{
            data.clear();
        } catch(UnsupportedOperationException e){
            System.err.println("Warning! A List passed to the setTo() method in SynchronizedList appears to be " +
                    "fixed-size (perhaps you used Arrays.asList()?). SynchronizedList requires a List that supports " +
                    "add, remove, and clear. Defaulting to a LinkedList.");
            data = new LinkedList<>();
        }
        changeAddAll(newListCopy);
    }

    public static void main(String[] args){
        Synchronizer synchronizer = new Synchronizer();
        SynchronizedList<String> myList = new SynchronizedList<>("test", new LinkedList<>(), Precedence.CLIENT, 21, synchronizer);
        myList.setAdd("hello");
        myList.setAdd("yellow");
        myList.setAdd("hi");
        synchronizer.printData();
        System.out.println("number of changed datas: " + synchronizer.getChangedData().size());

        SynchronizedList<String> myList2 = new SynchronizedList<>("test2", Arrays.asList("blah", "blue", "blot"), Precedence.CLIENT, 21, synchronizer);
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
