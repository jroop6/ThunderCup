package Classes.NetworkCommunication;


import java.io.Serializable;
import java.util.*;

/** A container for all the data that must be kept synchronized between host and client */
public class Synchronizer implements Serializable {
    private HashMap<String, SynchronizedData> synchronizedDataMap = new HashMap<>();
    private LinkedList<SynchronizedData> changedData = new LinkedList<>(); // todo: consider making this a Set instead of a list.

    public Synchronizer(){
    }

    // copy constructor
    public Synchronizer(Synchronizer other){
        synchronized (other){ // we don't want the other data to be modified while we're trying to copy it.
            // Copy the synchronizedDataMap
            for(SynchronizedData synchronizedData : other.synchronizedDataMap.values()){
                SynchronizedData synchronizedDataCopy = synchronizedData.copyForNetworking(this);
                synchronizedDataMap.put(synchronizedDataCopy.getKey(), synchronizedDataCopy);
            }
            // Copy the changedData List
            for(SynchronizedData synchronizedData : other.changedData){
                // A quick sanity check to help detect problems in the future:
                SynchronizedData officialSynchronizedData = synchronizedDataMap.get(synchronizedData.getKey());
                if(officialSynchronizedData==null){
                    System.err.println("Error in Synchronizer copy constructor! The data " + synchronizedData.getKey() +
                            " exists in the changedData list, but not in the synchronizedDataMap. This should not be " +
                            "possible.");
                }
                changedData.add(officialSynchronizedData);
            }
        }
    }

    public void register(SynchronizedData synchronizedData){
        synchronized (this){ // we don't want to add data while someone else is trying to access the hashmap (put() is not thread-safe).
            synchronizedDataMap.put(synchronizedData.getKey(), synchronizedData);
        }
    }

    // todo: Group data by id somehow instead of putting everything into a single hashmap, to make this operation simpler (as well as the processPacketsAsHost and processPacketsAsClient operations).
    public void deRegisterAllWithID(long id){
        synchronized (this){ // we don't want to delete data while someone else is trying to access it.
            System.out.println("de-registering player with id " + id);
            Iterator<SynchronizedData> it = changedData.iterator();

            // remove the data from the changedData list:
            while(it.hasNext()){
                SynchronizedData data = it.next();
                if(data.getParentID()==id){
                    it.remove();
                }
            }

            // remove the data from the synchronizedDataMap:
            it = synchronizedDataMap.values().iterator();
            while(it.hasNext()){
                SynchronizedData data = it.next();
                if(data.getParentID()==id){
                    it.remove();
                }
            }
        }
    }

    public void addToChangedData(SynchronizedData synchronizedData){
        synchronized (this){ // we don't want to add data while someone else is trying to access the LinkedList (add() is not thread-safe), nor do we want someone else to modify the HashMap while we're trying to access it.
            SynchronizedData officialSynchronizedData = synchronizedDataMap.get(synchronizedData.getKey());
            if(officialSynchronizedData==null){
                System.err.println("Error! Attempted to change a data value that was somehow not registered to the " +
                        "synchronizer! This issue needs to be debugged. Perhaps you have multiple Synchronizers running " +
                        "around and you're calling addToChangedData() directly instead of using changeTo()? Registering " +
                        "the data to this Synchronizer now...");
                register(synchronizedData);
                officialSynchronizedData = synchronizedData;
            }
            if(!changedData.contains(officialSynchronizedData)) changedData.add(officialSynchronizedData);
        }
    }

    public void resetChangedData(){
        synchronized (this){
            changedData.clear();
        }
    }

    // todo: check the type of SynchronizedData (is it a SynchronizedComparable? SynchronizedList?)
    // todo: check the generic type (is it a SynchronizedData<Integer>? SynchronizedData<Boolean>?)

    private boolean sanitizeClientData(SynchronizedData hostData, SynchronizedData clientData){
        if(hostData == null){
            System.err.println("Error! A client sent us unrecognized data: " + clientData.getKey());
            return false;
        }
        if(hostData.getPrecedence() == SynchronizedData.Precedence.HOST){
            System.err.println("Warning! A client attempted to set data without permission! " + clientData.getKey());
            return false;
        }
        return true;
    }
    private boolean sanitizeHostData(SynchronizedData hostData, SynchronizedData clientData){
        if(clientData == null){
            System.err.println("Error! The host sent us unrecognized data: " + hostData.getKey());
            return false;
        }
        return true;
    }

    public void synchronizeWith(Synchronizer other, boolean isHost){
        synchronized (this){
            if(isHost){
                for(SynchronizedData clientData : other.changedData){
                    SynchronizedData hostData = synchronizedDataMap.get(clientData.getKey());
                    if(!sanitizeClientData(hostData, clientData)) continue;
                    switch(hostData.getPrecedence()){
                        case HOST:
                            // This is an error case. sanitizeClientData() should have already printed a message.
                            break;
                        case CLIENT:
                            // Accept data changes that this client has authority over:
                            hostData.changeTo(clientData.data);
                            break;
                        case INFORMATIONAL:
                            // The data is for informational purposes only, and doesn't need to be kept in sync.
                            break;
                    }
                }
            }
            else{
                // The client looks at the host's changedData first, and immediately syncs with anything in there.
                for(SynchronizedData hostData : other.changedData){
                    SynchronizedData clientData = synchronizedDataMap.get(hostData.getKey());
                    if(!sanitizeHostData(hostData, clientData)) continue;
                    if(clientData.compareTo(hostData)!=0){
                        switch(clientData.getPrecedence()){
                            case HOST:
                                // The host has precedence, so we must accept whatever the host says.
                                clientData.setTo(hostData.data);
                                break;
                            case CLIENT:
                                // The host is just echoing back a change that we made earlier. Since we've already made
                                // the change ourselves, we can just ignore this data.
                                break;
                            case INFORMATIONAL:
                                // The data is for informational purposes only, and doesn't need to be kept in sync.
                                break;
                        }
                    }
                }
                // Now the client checks the consistency of the rest of its data with the host.
                for(SynchronizedData hostData : other.synchronizedDataMap.values()){
                    SynchronizedData clientData = synchronizedDataMap.get(hostData.getKey());
                    if(!sanitizeHostData(hostData, clientData)) continue;
                    if(clientData.compareTo(hostData)!=0){
                        clientData.incrementFramesOutOfSync();
                        if(clientData.isOutOfSync()){
                            switch(clientData.getPrecedence()){
                                case HOST:
                                    // The host has precedence and we've been out of sync for too long, so override the locally-held data with what the host says.
                                    clientData.setTo(hostData.data);
                                    break;
                                case CLIENT:
                                    // The host must have never received a command we sent. It's probably too late to re-send the command automatically, so just accept the host data:
                                    System.err.println("It appears that the host failed to receive a command we sent.");
                                    clientData.setTo(hostData.data);
                                    break;
                                case INFORMATIONAL:
                                    // The data is for informational purposes only, and doesn't need to be kept in sync.
                                    break;
                            }
                            clientData.resetFramesOutOfSync();
                        }
                    }
                    else clientData.resetFramesOutOfSync();
                }
            }
        }
    }


    // Note: It's best to synchronize this class's getters externally. Simply synchronizing get(), getAll(), and
    // getChangedData() does not sufficiently protect read operations. Consider the following scenario:
    // 1. thread1 calls list=get(someID, someString).getData() to retrieve a LinkedList.
    // 2. thread1 even queries list.size() to make sure an element exists at index=1.
    // 3. thread2 removes an item from the same list, causing its size to become <= 1.
    // 4. thread1 calls list.get(1) <-- Boom!
    // Notice that thread1 didn't even try to change the data; just looking at it is dangerous.

    public SynchronizedData get(long parentID, String key){
        return synchronizedDataMap.get(parentID + key);
    }

    public HashMap<String, SynchronizedData> getAll(){
        return synchronizedDataMap;
    }

    public List<SynchronizedData> getChangedData(){
        return changedData;
    }

    // For debugging
    public void printData(){
        for(SynchronizedData synchronizedData : synchronizedDataMap.values()){
            if(synchronizedData instanceof SynchronizedArray){
                System.out.print("SynchronizedArray " + synchronizedData.getKey() + ": [");
                for(int i=0; i<((Object[][])synchronizedData.data).length; i++){
                    Object[] row = ((Object[][])synchronizedData.data)[i];
                    for(int j=0; j<row.length-1; j++){
                        System.out.print(row[j] + ", ");
                    }
                    System.out.print(row[row.length-1] + "; ");
                }
                System.out.print("]");
                System.out.println();
            }
            else System.out.println(synchronizedData.getKey() + ": " + synchronizedData.data);
        }
    }
}
