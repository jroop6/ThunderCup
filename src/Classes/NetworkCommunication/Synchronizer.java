package Classes.NetworkCommunication;

import java.io.Serializable;
import java.util.*;

/**
 * A container for all the data that must be kept synchronized between host and client. All the data is held in a
 * 2-level HashMap structure. The first level organizes data by an ID value (such as playerID) and the second level
 * organizes the data by the String variable name (such as "username"). Hence, to retrieve the username of the player
 * whose ID is 1234, you could use synchronizedDataMap.get(1234).get("username"). For convenience, a special getter of
 * the form get(long id, String varName) is provided.
 *
 * Any data that is changed via methods like changeTo(), changeAdd(), changeModify(), etc. are added to a changedData
 * list. Immediately after the Synchronizer is sent across the network, the user should clear this list by calling
 * resetChangedData() so that the change is broadcast only once. Failing to do this won't cause any major problems, but
 * will result in the receiving end doing more work.
 */
public class Synchronizer implements Serializable {
    private HashMap<Long, HashMap<String, SynchronizedData>> synchronizedDataMap = new HashMap<>();
    private LinkedList<SynchronizedData> changedData = new LinkedList<>(); // todo: consider making this a Set instead of a list.

    private long id; // uniquely identifies this host or client among all the network nodes. Probably best to make id == playerID.

    // missedPacketsCount: The key is a Synchronizer ID. The value is the number of times we've sent a packet without
    // receiving any packet from that network node. This allows us to detect disconnected players.
    private HashMap<Long, Long> missedPacketsCount = new HashMap<>();

    public Synchronizer(long id){
        this.id = id;
    }

    // Returns a deep copy of this Synchronizer, minus any nonserializable data (such as lambda expressions).
    // todo: After the new synchronization system is complete, eliminate this method completely. We can simply wrap the prepareAndSendPacket method in synchronized(connectionManaer.getSynchronizer())
    // todo: also, remove copyForNetworking() from SynchronizedData and its derived classes.
    public Synchronizer copyForNetworking(){
        Synchronizer synchronizerCopy = new Synchronizer(id);

        synchronized (this){ // we don't want the other data to be modified while we're trying to copy it.
            // Copy the synchronizedDataMap
            for(Map.Entry<Long, HashMap<String, SynchronizedData>> entry : synchronizedDataMap.entrySet()){
                HashMap<String, SynchronizedData> mapCopy = new HashMap<>();
                synchronizerCopy.synchronizedDataMap.put(entry.getKey(), mapCopy);
                for(SynchronizedData synchronizedData : entry.getValue().values()){
                    SynchronizedData synchronizedDataCopy = synchronizedData.copyForNetworking(synchronizerCopy);
                    mapCopy.put(synchronizedDataCopy.getName(), synchronizedDataCopy);
                }
            }
            // Copy the changedData List
            for(SynchronizedData synchronizedData : changedData){
                SynchronizedData synchronizedDataCopy = synchronizerCopy.get(synchronizedData.getParentID(),synchronizedData.getName());
                // A quick sanity check to help detect problems in the future:
                if(synchronizedDataCopy==null){
                    System.err.println("Error in Synchronizer copy constructor! The data (" +
                            synchronizedData.getParentID() + ", " + synchronizedData.getName() + ") exists in the " +
                            "changedData list, but not in the synchronizedDataMap. This should not be possible.");
                }
                synchronizerCopy.changedData.add(synchronizedDataCopy);
            }
        }
        return synchronizerCopy;
    }

    public void resetChangedData(){
        changedData.clear();

        // increment each entry of missedPacketsCount
        for(Map.Entry<Long, Long> entry : missedPacketsCount.entrySet()){
            entry.setValue(entry.getValue()+1);
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

            // remove this id from missedPacketsCount:
            missedPacketsCount.remove(id);
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

            // set the missedPacketsCount to zero, or add this Synchronizer's id to the HashMap if it doesn't exist:
            missedPacketsCount.putIfAbsent(other.id, 0L);
            missedPacketsCount.replace(other.id, 0L);
        }
    }

    public void waitForReconnect(long id, long tolerance){
        missedPacketsCount.replace(id, -tolerance);
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

    public long getId(){
        return id;
    }

    // returns a list of all Synchronizer IDs that have been inactive for the specified number of frames.
    public List<Long> getDisconnectedIDs(long cutoff){
        List<Long> disconnectedIDs = new LinkedList<>();
        for(Map.Entry<Long, Long> entry : missedPacketsCount.entrySet()){
            if(entry.getValue()>cutoff) disconnectedIDs.add(entry.getKey());
        }
        return disconnectedIDs;
    }

    // todo: temporary.
    public long getDisconnectedTime(long playerID){
        return missedPacketsCount.get(playerID);
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
