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
                synchronizedDataMap.put(synchronizedComparableCopy.getName(), synchronizedComparableCopy);
            }
        }
        for(SynchronizedData synchronizedData : other.changedData){
            SynchronizedData officialSynchronizedData = synchronizedDataMap.get(synchronizedData.getName());
            if(officialSynchronizedData==null){
                System.err.println("Error in Synchronizer copy constructor! The data " + synchronizedData.getName() +
                        " exists in the changedData list, but not in the synchronizedDataMap. This should not be " +
                        "possible.");
            }
            changedData.add(officialSynchronizedData);
        }
    }

    public void register(SynchronizedData synchronizedData){
        synchronizedDataMap.put(synchronizedData.getName(),synchronizedData);
    }

    public void addToChangedData(SynchronizedData synchronizedData){
        SynchronizedData officialSynchronizedData = synchronizedDataMap.get(synchronizedData.getName());
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

    public void synchronizeWith(Synchronizer other, boolean isHost){
        System.out.println("hey! we called synchronizeWith! How much data do we have in store? us: " + synchronizedDataMap.values().size() + " other: " + other.synchronizedDataMap.values().size());
        if(isHost){
            // if we're the host, we don't need to call compareTo(), because we're only checking the data in the client's changedData list.
            for(SynchronizedData clientData : other.changedData){
                SynchronizedData hostData = synchronizedDataMap.get(clientData.getName());
                if(!sanitizeClientData(hostData, clientData)) continue;
                hostData.changeTo(clientData.getData());
            }
        }
        else{
            for(SynchronizedData hostData : other.synchronizedDataMap.values()){
                SynchronizedData clientData = synchronizedDataMap.get(hostData.getName());
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

    // todo: check the type of SynchronizedData (is it a SynchronizedComparable? SynchronizedList?)
    // todo: check the generic type (is it a SynchronizedData<Integer>? SynchronizedData<Boolean>?)
    private boolean sanitizeClientData(SynchronizedData hostData, SynchronizedData clientData){
        if(hostData == null){
            System.err.println("Error! A client sent us unrecognized data: " + clientData.getName());
            return false;
        }
        if(hostData.getPrecedence()== SynchronizedData.Precedence.HOST){
            System.err.println("Warning! A client attempted to set data without permission! " + clientData.getName());
            return false;
        }
        return true;
    }

    private boolean sanitizeHostData(SynchronizedData hostData, SynchronizedData clientData){
        if(clientData == null){
            System.err.println("Error! The host sent us unrecognized data: " + hostData.getName());
            return false;
        }
        return true;
    }
}
