package Classes.NetworkCommunication;

import java.util.*;

/**
 * Created by Jonathan Roop on 7/28/2017.
 */
public abstract class ConnectionManager extends Thread {
    protected boolean isConnected = false;
    Queue<Packet> inPackets = new LinkedList<>();
    List<SenderWorker> senderWorkers = new LinkedList<>();
    List<ReceiverWorker> receiverWorkers = new LinkedList<>();
    protected static final int DEFAULT_PORT = 5000;
    private long packetsSentPerSecond = 10;  // The game will process incoming packets as fast as possible, but only export this many packets per second, reducing the possibility of overloading the AnimationTimer with too many inPackets from other computer(s).
    public long latencyTestsPerSecond = 3; // How frequently the host probes the latency of its connected clients.
    Map<Long,Long> latencies = new HashMap<>(); // A continuously-updated record of the latencies between the server and various players.
    protected long playerID; // the ID of the LocalPlayer.
    protected Synchronizer synchronizer = new Synchronizer(); // A container for all data to be synchronized between client and host.

    public long getLatencyTestsPerSecond() {
        return latencyTestsPerSecond;
    }
    public long getPlayerID(){
        return playerID;
    }

    public Map<Long,Long> getLatencies(){
        return latencies;
    }

    public long getPacketsSentPerSecond(){
        return packetsSentPerSecond;
    }

    public Synchronizer getSynchronizer(){
        return synchronizer;
    }

    public void setPlayerID(long playerID){
        this.playerID = playerID;
    }

    public synchronized Packet retrievePacket(){
        return inPackets.poll();
    }

    // Note: A Client will only have 1 senderWorker.
    // Before sending, outPacket is to be built in either the GameScene or MultiplayerSelectionScene.
    public void send(Object object){
        for (SenderWorker senderWorker : senderWorkers) {
            senderWorker.send(object);
        }
    }

    public void updateLatencies(LatencyPacket latencyPacket){
        latencies.put(latencyPacket.getPlayerID(),latencyPacket.getLatency());
    }

    // ReceiverWorkers can add packets to the inPackets List using the following method:
    public synchronized void addPacket(Packet inPacket){
        inPackets.add(inPacket);
    }

    // used in graceful shutdown
    public void cleanUp(){
        System.out.println("shutting down connection manager...");
        for (SenderWorker senderWorker: senderWorkers) {
            senderWorker.kill();
        }
        for (ReceiverWorker receiverWorker: receiverWorkers) {
            receiverWorker.kill();
        }
        isConnected = false;
    }

    public boolean isConnected(){
        return isConnected;
    }

    public synchronized void removeReceiverWorker(ReceiverWorker rw){
        receiverWorkers.remove(rw);
    }
}

