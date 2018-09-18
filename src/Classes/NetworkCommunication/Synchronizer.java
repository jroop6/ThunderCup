package Classes.NetworkCommunication;


import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/** A container for all the data that must be kept synchronized between host and client */
public class Synchronizer implements Serializable {
    private HashMap<String, SynchronizedData> synchronizedDataMap = new HashMap<>();
    private List<SynchronizedData> changedData = new LinkedList<>();

    public Synchronizer(){
    }

    // copy constructor
    //todo: I think I need to have copy constructors for all SynchronizedData classes... yuck.
    public Synchronizer(Synchronizer other){
        for(SynchronizedData synchronizedData : other.synchronizedDataMap.values()){
            if(synchronizedData instanceof SynchronizedComparable){
                SynchronizedComparable synchronizedComparableCopy = new SynchronizedComparable((SynchronizedComparable)synchronizedData, this);
                synchronizedDataMap.put(synchronizedComparableCopy.getKey(), synchronizedComparableCopy);
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
        synchronizedDataMap.put(synchronizedData.getKey(),synchronizedData);
    }

    public void addToChangedData(SynchronizedData synchronizedData){
        SynchronizedData officialSynchronizedData = synchronizedDataMap.get(synchronizedData.getKey());
        if(officialSynchronizedData==null){
            System.err.println("Error! Attempted to change a data value that was somehow not registered to the " +
                    "synchronizer! This issue needs to be debugged. Perhaps you have multiple Synchronizers running " +
                    "around and you're calling addToChangedData directly instead of using changeTo? Registering the " +
                    "data to this Synchronizer now...");
            register(synchronizedData);
            officialSynchronizedData = synchronizedData;
        }
        changedData.add(officialSynchronizedData);
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
        // todo: oops; this one doesn't actually turn out to be the case. I need to rethink the security of this game.
        /*if(hostData.getPrecedence()== SynchronizedData.Precedence.HOST){
            System.err.println("Warning! A client attempted to set data without permission! " + clientData.getKey());
            return false;
        }*/
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
        //System.out.println("hey! we called synchronizeWith! How much data do we have in store? us: " + synchronizedDataMap.values().size() + " other: " + other.synchronizedDataMap.values().size());
        if(isHost){
            // if we're the host, we don't need to call compareTo(), because we're only checking the data in the client's changedData list.
            if(other.changedData.size()>0) System.out.println("changed data detected!");
            for(SynchronizedData clientData : other.changedData){
                SynchronizedData hostData = synchronizedDataMap.get(clientData.getKey());
                if(!sanitizeClientData(hostData, clientData)) continue;
                System.out.println("new username is " + clientData.getData());
                hostData.changeTo(clientData.getData());
            }
        }
        else{
            // The client looks at the host's changedData first, and immediately syncs with anything in there.
            for(SynchronizedData hostData : other.changedData){
                SynchronizedData clientData = synchronizedDataMap.get(hostData.getKey());
                if(!sanitizeHostData(hostData,clientData)) continue;
                if(clientData.compareTo(hostData)!=0){
                    clientData.setTo(hostData.getData());
                }
            }
            // Now the client checks the consistency of the rest of its data with the host.
            for(SynchronizedData hostData : other.synchronizedDataMap.values()){
                SynchronizedData clientData = synchronizedDataMap.get(hostData.getKey());
                if(!sanitizeHostData(hostData,clientData)) continue;

                if(clientData.compareTo(hostData)!=0){
                    clientData.incrementFramesOutOfSync();
                    if(clientData.isOutOfSync()){
                        switch(clientData.getPrecedence()){
                            case HOST:
                                clientData.setTo(hostData.getData());
                                break;
                            case CLIENT:
                                // todo: should we re-send the command to the host? This makes sense for messages, but not for firing orbs. For now, I'll just synchronize on the host.
                                clientData.setTo(hostData.getData());
                                break;
                        }
                        clientData.resetFramesOutOfSync();
                    }
                }
            }
        }
    }

    public SynchronizedData get(long parentID, String key){
        return synchronizedDataMap.get(parentID + key);
    }

    public HashMap<String, SynchronizedData> getAll(){
        return synchronizedDataMap;
    }
}
