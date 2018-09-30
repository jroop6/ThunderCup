package Classes.NetworkCommunication;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by HydrusBeta on 8/4/2017.
 */ // Dedicated thread that sends packets
public class SenderWorker extends Thread{

    Socket socket;
    ObjectOutputStream outputStream;
    boolean shuttingDown = false;
    Object outPacket;

    public SenderWorker(Socket socket){
        this.socket = socket;

        // Create something the worker can write to:
        try{
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e){
            System.err.println("Error while trying to create Object output stream.");
            e.printStackTrace();
            //ToDo: refuse this client connection.
        }
    }

    @Override
    public void run(){
        while(!shuttingDown){
            waitingBlock(); // Thread blocks here until either kill() or send() are called;
            sendingBlock(); // Thread send data
        }

        // If the thread has gone outside the loop, that means that the shuttingDown flag has been set. Close the socket.
        try{
            socket.close();
        } catch (IOException e){
            // Note: If the socket fails to close, this means that the player will no longer be able to use the default port for future games this session.
            System.out.println("shutting down SenderWorker socket");
            e.printStackTrace();
        }
    }

    public synchronized void waitingBlock() {
        try {
            wait();
        } catch (InterruptedException e) {
            System.err.println("SenderWorker was interrupted during wait.");
        }
    }

    private synchronized void sendingBlock(){
        if(shuttingDown){
            System.out.println("Shutdown signal received. Shutting down SenderWorker.");
            return;
        }
        try{
            if(outPacket != null) outputStream.writeObject(outPacket);
            outputStream.flush();
            outputStream.reset();
        } catch (IOException e){
            // If an IOException is encountered, that player has probably disconnected. For now, just let the player's connection time out.
            // ToDo: Since the player's connection is known to be severed now, consider booting the player at once rather than waiting for the natural timeout.
            System.err.println("Player disconnect detected. Shutting down SenderWorker");
            // e.printStackTrace();
            shuttingDown = true;
        }
    }

    public synchronized void kill(){
        shuttingDown = true;
        this.notify();
    }

    public synchronized void send(Object object){
        this.outPacket = object;
        this.notify();
    }
}
