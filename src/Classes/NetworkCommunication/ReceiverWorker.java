package Classes.NetworkCommunication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by Jonathan Roop on 8/4/2017.
 */ // Dedicated thread that receives packets
class ReceiverWorker extends Thread{
    ConnectionManager master;
    Socket socket;
    ObjectInputStream objectInputStream;
    boolean isHost;
    boolean shuttingDown = false;

    public ReceiverWorker(ConnectionManager master, Socket socket){
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

    // The incoming packet may be one of two types of objects: a Packet or a LatencyPacket. Plain old Packets contain
    // GameData and PlayerData while LatencyPackets are very simple and are used to probe the latency between the host
    // and clients (unsurprisingly).
    @Override
    public void run(){
        while(!shuttingDown){
            try{
                Object objectIn = objectInputStream.readObject();
                if(objectIn instanceof Packet) master.addPacket((Packet) objectIn);
                else if (objectIn instanceof LatencyPacket){
                    if(isHost) master.updateLatencies((LatencyPacket)objectIn); // update the host's latency data.
                    else{
                        ((LatencyPacket) objectIn).setPlayerID(master.getPlayerID());
                        master.send(objectIn); // clients immediately return the packet.
                    }
                }
            } catch(SocketException e){
                //ToDo: I *think* this specific exception is only thrown if kill() is called. But it might be called if, say a network cable gets unplugged. In the latter case, we should maybe pause the game until the player's connection is reaffirmed (so ToDo under IOException)
                System.err.println("socket closed. shutting down ReceiverWorker");
            } catch (IOException e){
                // If an IOException occurred, assume this means that the player disconnected. For now, just shut down this ReceiverWorker
                //ToDo: Instead of just shutting down the ReceiverWorker, try pausing the game for a bit first to see whether the player reconnects?
                System.err.println("Player disconnect detected. Shutting down ReceiverWorker...");
                shuttingDown = true;
            } catch (ClassNotFoundException e){
                //ToDo: display a non-modal informational dialog to the player about this.
                System.err.println("item received through network is not recognizable. Perhaps the players is running a different version of the game?");
            }
        }
    }

    // This shuts down socket and should cause a SocketException to be thrown in this class's run() method, thereby
    // stopping its thread. In the offhand chance that this class's thread is in the middle of adding a packet when
    // kill() is called, kill() also sets shuttingDown to true as another way of getting the thread to break out of its
    // loop.
    public void kill(){
        shuttingDown = true;
        try{
            socket.close();
        } catch (IOException e){
            System.err.println("Problem encountered while killing ReceiverWorker. Thread might not terminate.");
            e.printStackTrace();
        }
    }
}
