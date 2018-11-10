package Classes.NetworkCommunication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by Jonathan Roop on 8/4/2017.
 * Dedicated thread for receiving packets
 */
class ReceiverWorker extends Thread{
    private final ConnectionManager master;
    private final Socket socket;
    private final boolean isHost;
    private ObjectInputStream objectInputStream;
    private boolean shuttingDown = false;

    ReceiverWorker(ConnectionManager master, Socket socket){
        this.master = master;
        this.socket = socket;
        this.isHost = (master instanceof HostConnectionManager);

        // Create something the worker can read from:
        try{
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e){
            System.err.println("Error while trying to create Object output stream.");
            e.printStackTrace();
            //ToDo: refuse this client connection.
        }

    }

    // The incoming packet may be one of two types of objects: a Synchronizer or a LatencyPacket. LatencyPackets are
    // very simple and are used to probe the latency between the host and clients (unsurprisingly).
    @Override
    public void run(){
        while(!shuttingDown){
            try{
                byte[] byteArrayIn = (byte[]) objectInputStream.readObject();
                Object objectIn = new ObjectInputStream(new ByteArrayInputStream(byteArrayIn)).readObject();
                if(objectIn instanceof Synchronizer) master.addPacket((Synchronizer) objectIn);
                else if (objectIn instanceof LatencyPacket){
                    if(isHost) master.updateLatencies((LatencyPacket) objectIn); // update the host's latency data.
                    else{
                        ((LatencyPacket) objectIn).setPlayerID(master.getPlayerID());
                        master.send(objectIn); // clients immediately return the packet.
                    }
                }
                else{
                    System.err.println("The receiver worker has received an unrecognizable Object. Perhaps a peer is running a different version of the game? Ignoring that packet...");
                }
            } catch(SocketException e){
                //ToDo: I *think* this specific exception is only thrown if kill() is called. But it might be called if, say a network cable gets unplugged. In the latter case, we should maybe pause the game until the player's connection is reaffirmed (so ToDo under IOException)
                System.err.println("socket closed. shutting down ReceiverWorker");
            } catch (IOException e){
                // If an IOException occurred, assume this means that the player disconnected. For now, just shut down this ReceiverWorker
                //ToDo: Instead of just shutting down the ReceiverWorker, try pausing the game for a bit first to see whether the player reconnects?
                System.err.println("Player disconnect detected. Shutting down ReceiverWorker...");
                e.printStackTrace();
                shuttingDown = true;
            } catch (ClassNotFoundException e){
                //ToDo: display a non-modal informational dialog to the player about this.
                System.err.println("item received through network is not recognizable. Perhaps the player is running a different version of the game?");
            }
        }
        System.out.println("the number of reciever workers before removal is " + master.receiverWorkers.size());
        master.removeReceiverWorker(this);
        System.out.println("the number of reciever workers left is " + master.receiverWorkers.size());
    }

    // This shuts down socket and should cause a SocketException to be thrown in this class's run() method, thereby
    // stopping its thread. In the offhand chance that this class's thread is in the middle of adding a packet when
    // kill() is called, kill() also sets shuttingDown to true as another way of getting the thread to break out of its
    // loop.
    void kill(){
        shuttingDown = true;
        try{
            socket.close();
        } catch (IOException e){
            System.err.println("Problem encountered while killing ReceiverWorker. Thread might not terminate.");
            e.printStackTrace();
        }
    }
}
