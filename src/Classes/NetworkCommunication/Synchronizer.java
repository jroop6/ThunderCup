package Classes.NetworkCommunication;


import java.io.Serializable;
import java.util.*;

/** A container for all the data that must be kept synchronized between host and client. All the data is held in a
 * 2-level HashMap structure. The first level organizes data by an ID value (such as playerID) and the second level
 * organizes the data by the String variable name (such as "username"). Hence, to retrieve the username of the player
 * whose ID is 1234, you could use synchronizedDataMap.get(1234).get("username"). For convenience, a special getter of
 * the form get(long id, String varName) is provided.*/
public class Synchronizer implements Serializable {
    private HashMap<Long, HashMap<String, SynchronizedData>> synchronizedDataMap = new HashMap<>();
    private LinkedList<SynchronizedData> changedData = new LinkedList<>(); // todo: consider making this a Set instead of a list.

    public Synchronizer(){
    }

    // copy constructor
    public Synchronizer(Synchronizer other){
        synchronized (other){ // we don't want the other data to be modified while we're trying to copy it.
            // Copy the synchronizedDataMap
            for(Map.Entry<Long, HashMap<String, SynchronizedData>> entry : other.synchronizedDataMap.entrySet()){
                HashMap<String, SynchronizedData> mapCopy = new HashMap<>();
                synchronizedDataMap.put(entry.getKey(), mapCopy);
                for(SynchronizedData synchronizedData : entry.getValue().values()){
                    SynchronizedData synchronizedDataCopy = synchronizedData.copyForNetworking(this);
                    mapCopy.put(synchronizedDataCopy.getName(), synchronizedDataCopy);
                }
            }
            // Copy the changedData List
            for(SynchronizedData synchronizedData : other.changedData){
                SynchronizedData officialSynchronizedData = get(synchronizedData.getParentID(),synchronizedData.getName());
                // A quick sanity check to help detect problems in the future:
                if(officialSynchronizedData==null){
                    System.err.println("Error in Synchronizer copy constructor! The data (" +
                            synchronizedData.getParentID() + ", " + synchronizedData.getName() + ") exists in the " +
                            "changedData list, but not in the synchronizedDataMap. This should not be possible.");
                }
                changedData.add(officialSynchronizedData);
            }
        }
    }

    public void register(SynchronizedData synchronizedData){
        synchronized (this){ // we don't want to add data while someone else is trying to access the hashmap (put() is not thread-safe).
            //synchronizedDataMap.put(synchronizedData.getKey(), synchronizedData);
            // First check to see whether we already have data associated with this ID. Create a group if one doesn't exist:
            long id = synchronizedData.getParentID();
            HashMap<String, SynchronizedData> entry = synchronizedDataMap.get(id);
            if(entry==null){
                entry = new HashMap<>();
                synchronizedDataMap.put(id, entry);
            }

            // Now add the data:
            entry.put(synchronizedData.getName(), synchronizedData);
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
            if(synchronizedDataMap.remove(id)==null){
                // To help with future debugging:
                System.err.println("Warning! While de-registering id " + id + ", no data data was actually found for " +
                        "that ID. Did you call deRegisterAllWithID(id) twice? Or perhaps the data never existed?");
            }
        }
    }

    public void addToChangedData(SynchronizedData synchronizedData){
        synchronized (this){ // we don't want to add data while someone else is trying to access the LinkedList (add() is not thread-safe), nor do we want someone else to modify the HashMap while we're trying to access it.
            SynchronizedData officialSynchronizedData = get(synchronizedData.getParentID(),synchronizedData.getName());
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
            System.err.println("Error! A client sent us unrecognized data: (" + clientData.getParentID() + ", "
                    + clientData.getName() + ")");
            return false;
        }
        if(hostData.getPrecedence() == SynchronizedData.Precedence.HOST){
            System.err.println("Warning! A client attempted to set data without permission! (" +
                    clientData.getParentID() + ", " + clientData.getName() + ")");
            return false;
        }
        return true;
    }
    private boolean sanitizeHostData(SynchronizedData hostData, SynchronizedData clientData){
        if(clientData == null){
            System.err.println("Error! The host sent us unrecognized data: (" + hostData.getParentID() + ", "
                    + hostData.getName() + ")");
            return false;
        }
        return true;
    }

    public void synchronizeWith(Synchronizer other, boolean isHost){
        synchronized (this){
            if(isHost){
                for(SynchronizedData clientData : other.changedData){
                    SynchronizedData hostData = get(clientData.getParentID(), clientData.getName());
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
                    SynchronizedData clientData = get(hostData.getParentID(), hostData.getName());
                    if(!sanitizeHostData(hostData, clientData)) continue;
                    //if(clientData.compareTo(hostData)!=0){
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
                    //}
                }
                // Now the client checks the consistency of the rest of its data with the host.
                for(Map.Entry<Long, HashMap<String, SynchronizedData>> entry : other.synchronizedDataMap.entrySet()){
                    for(SynchronizedData hostData : entry.getValue().values()){
                        SynchronizedData clientData = get(hostData.getParentID(), hostData.getName());
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
    }


    // Note: It's best to synchronize this class's getters externally. Simply synchronizing get(), getAll(), and
    // getChangedData() does not sufficiently protect read operations. Consider the following scenario:
    // 1. thread1 calls list=get(someID, someString).getData() to retrieve a LinkedList.
    // 2. thread1 even queries list.size() to make sure an element exists at index=1.
    // 3. thread2 removes an item from the same list, causing its size to become <= 1.
    // 4. thread1 calls list.get(1) <-- Boom!
    // Notice that thread1 didn't even try to change the data; just looking at it is dangerous.

    public SynchronizedData get(long parentID, String varName){
        HashMap<String, SynchronizedData> entry = synchronizedDataMap.get(parentID);
        if(entry==null) return null;
        return entry.get(varName);
    }

    public HashMap<Long, HashMap<String, SynchronizedData>> getAll(){
        return synchronizedDataMap;
    }

    public List<SynchronizedData> getChangedData(){
        return changedData;
    }

    // For debugging
    public void printData(){
        for(Map.Entry<Long, HashMap<String, SynchronizedData>> entry : synchronizedDataMap.entrySet()){
            System.out.println("Data for ID " + entry.getKey() + ":");
            for(SynchronizedData synchronizedData : entry.getValue().values()){
                if(synchronizedData instanceof SynchronizedArray){
                    System.out.print("   SynchronizedArray " + synchronizedData.getName() + ": [");
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
                else System.out.println("   " + synchronizedData.getName() + ": " + synchronizedData.data);
            }
        }
    }
}
