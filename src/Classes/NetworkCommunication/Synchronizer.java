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
        for(SynchronizedData synchronizedData : other.synchronizedDataMap.values()){
            if(synchronizedData instanceof SynchronizedComparable){
                SynchronizedComparable synchronizedComparableCopy = new SynchronizedComparable((SynchronizedComparable)synchronizedData, this);
                synchronizedDataMap.put(synchronizedComparableCopy.getKey(), synchronizedComparableCopy);
            }
            if(synchronizedData instanceof SynchronizedList){
                SynchronizedList synchronizedListCopy = new SynchronizedList((SynchronizedList)synchronizedData, this);
                synchronizedDataMap.put(synchronizedListCopy.getKey(), synchronizedListCopy);
            }
        }
        for(SynchronizedData synchronizedData : other.changedData){
            SynchronizedData officialSynchronizedData = synchronizedDataMap.get(synchronizedData.getKey());
            if(officialSynchronizedData==null){
                System.err.println("Error in Synchronizer copy constructor! The data " + synchronizedData.getKey() +
                        " exists in the changedData list, but not in the synchronizedDataMap. This should not be " +
                        "possible.");
            }
            changedData.add(officialSynchronizedData);
        }
    }

    public void register(SynchronizedData synchronizedData){
        synchronizedDataMap.put(synchronizedData.getKey(), synchronizedData);
    }

    // todo: Consider grouping data by id somehow instead of putting everything into a single hashmap, to make this operation simpler (as well as the processPacketsAsHost and processPacketsAsClient operations).
    public void deRegisterAllWithID(long id){
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

    public void addToChangedData(SynchronizedData synchronizedData){
        SynchronizedData officialSynchronizedData = synchronizedDataMap.get(synchronizedData.getKey());
        if(officialSynchronizedData==null){
            System.err.println("Error! Attempted to change a data value that was somehow not registered to the " +
                    "synchronizer! This issue needs to be debugged. Perhaps you have multiple Synchronizers running " +
                    "around and you're calling addToChangedData() directly instead of using handle()? Registering " +
                    "the data to this Synchronizer now...");
            register(synchronizedData);
            officialSynchronizedData = synchronizedData;
        }
        if(!changedData.contains(officialSynchronizedData)) changedData.add(officialSynchronizedData);
    }

    public List<SynchronizedData> getChangedData(){
        return changedData;
    }
    public void resetChangedData(){
        changedData.clear();
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
    private boolean sanitizeHostData(SynchronizedData hostData, SynchronizedData clientData, boolean checkingChangedData){
        if(clientData == null){
            System.err.println("Error! The host sent us unrecognized data: " + hostData.getKey());
            return false;
        }
        if(checkingChangedData){
            if(clientData.getPrecedence() == SynchronizedData.Precedence.CLIENT){
                System.err.println("Warning! The host attempted to set data without permission! " + clientData.getKey());
                return false;
            }
        }
        return true;
    }

    public void synchronizeWith(Synchronizer other, boolean isHost){
        if(isHost){
            for(SynchronizedData clientData : other.changedData){
                SynchronizedData hostData = synchronizedDataMap.get(clientData.getKey());
                if(!sanitizeClientData(hostData, clientData)) continue;
                switch(hostData.getPrecedence()){
                    case HOST:
                        hostData.changeTo(clientData.data);
                        break;
                    case CLIENT:
                        hostData.changeTo(clientData.data);
                        break;
                    case INFORMATIONAL:
                        break;
                }
            }
        }
        else{
            // The client looks at the host's changedData first, and immediately syncs with anything in there.
            for(SynchronizedData hostData : other.changedData){
                SynchronizedData clientData = synchronizedDataMap.get(hostData.getKey());
                if(!sanitizeHostData(hostData,clientData, false)) continue;
                if(clientData.compareTo(hostData)!=0){
                    switch(clientData.getPrecedence()){
                        case HOST:
                            // The host has precedence, so we must accept whatever the host says.
                            clientData.setTo(hostData.data);
                            break;
                        case CLIENT:
                            //todo: in this case, should we just ignore what the host says?
                            clientData.setTo(hostData.data);
                            break;
                        case INFORMATIONAL:
                            break;
                    }
                }
            }
            // Now the client checks the consistency of the rest of its data with the host.
            for(SynchronizedData hostData : other.synchronizedDataMap.values()){
                SynchronizedData clientData = synchronizedDataMap.get(hostData.getKey());
                if(!sanitizeHostData(hostData, clientData, false)) continue;
                if(clientData.compareTo(hostData)!=0){
                    clientData.incrementFramesOutOfSync();
                    if(clientData.isOutOfSync()){
                        switch(clientData.getPrecedence()){
                            case HOST:
                                // The host has precedence and we've been out of sync for too long, so override the locally-held data with what the host says.
                                clientData.setTo(hostData.data);
                                break;
                            case CLIENT:
                                // todo: should we re-send the command to the host? This makes sense for messages, but not for firing orbs. For now, I'll just synchronize on the host.
                                clientData.setTo(hostData.data);
                                break;
                            case INFORMATIONAL:
                                break;
                        }
                        clientData.resetFramesOutOfSync();
                    }
                }
                else clientData.resetFramesOutOfSync();
            }
        }
    }

    public SynchronizedData get(long parentID, String key){
        return synchronizedDataMap.get(parentID + key);
    }

    public HashMap<String, SynchronizedData> getAll(){
        return synchronizedDataMap;
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
