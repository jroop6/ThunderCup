package Classes.NetworkCommunication;

import Classes.SceneManager;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Optional;


/**
 * Created by Jonathan Roop on 8/4/2017.
 */ // Listens for connection requests. Sets up 1 receiver/sender Worker pair per client connected.
public class HostConnectionManager extends ConnectionManager{

    ServerSocket serverSocket;
    int openSlots = 0; // A player can only connect if there is slot open for them.


    // The Constructor is a lot less complicated than it looks! It simply attempts to open a ServerSocket, and then
    // there's just a bunch of code for error handling and for helping with user troubleshooting.
    public HostConnectionManager(int port) {
        this.playerID = playerID;
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
        } catch (IOException e) {
            System.err.println("Failed to open ServerSocket for network communication.");
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(SceneManager.getPrimaryStage());
            alert.setTitle("Error!");
            alert.setHeaderText("Failed to open ServerSocket for network communication on port " + DEFAULT_PORT + ". \n" +
                    "This can happen if that port is being used by something else, or if there is another \n" +
                    "instance of the game already running. If you receive this error message after you've already \n" +
                    "successfully run a multiplayer game on this network, it is possible that your first game \n" +
                    "didn't shut down all the way; restarting your computer should fix this. As a quick remedy in \n" +
                    "the meantime, you can try using a different port.");
            ButtonType anotherPort = new ButtonType("Try opening socket on a different port");
            ButtonType returnBtn = new ButtonType("Return to Main Menu", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(anotherPort, returnBtn);
            alert.setGraphic(null);
            Optional<ButtonType> result = alert.showAndWait();
            if(result.isPresent()){
                if(result.get()==anotherPort){
                    try {
                        serverSocket = new ServerSocket(0);
                    } catch (IOException e2) {
                        System.err.println("Failed to open ServerSocket for network communication.");
                        Alert alert2 = new Alert(Alert.AlertType.CONFIRMATION);
                        alert2.initOwner(SceneManager.getPrimaryStage());
                        alert2.setTitle("Error!");
                        alert2.setHeaderText("Failed to open ServerSocket for network communication again. Perhaps \n" +
                                "the network firewall is prohibiting the game from communicating. Please enable \n" +
                                "the game to use port " + DEFAULT_PORT + " and try again.");
                        ButtonType returnBtn2 = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                        alert2.getButtonTypes().setAll(returnBtn2);
                        alert2.setGraphic(null);
                        alert2.showAndWait();
                    }
                    Alert alert3 = new Alert(Alert.AlertType.CONFIRMATION);
                    alert3.initOwner(SceneManager.getPrimaryStage());
                    alert3.setTitle("Success!");
                    alert3.setHeaderText("ServerSocket opened for the following port number. Please have your friends" +
                                    " connect to this port: \n\n" + serverSocket.getLocalPort());
                    ButtonType returnBtn2 = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                    alert3.getButtonTypes().setAll(returnBtn2);
                    alert3.setGraphic(null);
                    alert3.showAndWait();
                }
            }

        }

        // Set a timeout for the upcoming accept() method by calling setSoTimeout(). The timeout is useful for
        // periodically checking whether the connection has been shut down:
        if(serverSocket != null){
            try{
                serverSocket.setSoTimeout(1000);
            } catch(SocketException e){
                System.err.println("Socket exception while trying to set serverSocket Timeout");
                serverSocket = null;
            }
        }

        // success!
        if(serverSocket != null){
            isConnected = true;
            System.out.println("HostConnectionManager established. Listening for connections over port " + serverSocket.getLocalPort());
        }

    }

    @Override
    public void run(){
        System.out.println("Running HostConnectionManager");
        while(isConnected){
            try{
                Socket hostSideSocket = serverSocket.accept();
                if(!isConnected) break;
                if(openSlots == 0) rejectPlayer(hostSideSocket);
                else{
                    addPlayer(hostSideSocket);
                    System.out.println("New Player connection established");
                }
            }
            catch(SocketTimeoutException e){ // thrown periodically to allow for the thread to check for shutdown conditions.
                if(!isConnected) break;
            }
            catch(IOException e){
                System.err.println("IOException encountered while attempting to create host-side socket");
            }
        }
        System.out.println("HostConnectionManager shutting down");
        try{
            serverSocket.close();
        } catch (IOException e){
            System.err.println("Exception encountered while trying to close socket. The port associated with the " +
                    "socket might not be usable again until the socket is released.");
        }
    }

    private void rejectPlayer(Socket hostSideSocket){
        // Create a temporary objectOutputStream to send the client the rejection notice:
        ObjectOutputStream tempOutputStream;
        try{
            tempOutputStream = new ObjectOutputStream(hostSideSocket.getOutputStream());

            // Create said rejection notice:
            PlayerData tempPlayerData = new PlayerData("Host",0, synchronizer);
            Packet tempServerPacket = new Packet(tempPlayerData, new GameData(synchronizer), new Synchronizer());
            tempServerPacket.rejectConnection();

            // Send the rejection notice
            tempOutputStream.writeObject(tempServerPacket);
            tempOutputStream.flush();
            tempOutputStream.reset();
        } catch (IOException e){
            // If an IOException is encountered, then there's no way to inform the client that he/she has been rejected.
            // Oh well. They'll figure it out eventually...
            // ToDo: set up some sort of client-side connection timeout that displays an alert to inform him/her that the connection doesn't appear to have gone through.
        }
    }

    // Call this after a socket is opened
    public void addPlayer(Socket hostSideSocket){
        SenderWorker newSenderWorker = new SenderWorker(hostSideSocket);
        ReceiverWorker newReceiverWorker = new ReceiverWorker(this, hostSideSocket);
        senderWorkers.add(newSenderWorker);
        receiverWorkers.add(newReceiverWorker);
        newSenderWorker.start();
        newReceiverWorker.start();
        --openSlots;
        System.out.println("AddPlayer() called and openSlots decremented. openSlots = " + openSlots);
    }

    public void addOpenSlot(){
        ++openSlots;
        System.out.println("addOponSlot() called and openSlots incremented. openSlots = " + openSlots);
    }

    public void removeOpenSlot(){
        --openSlots;
        System.out.println("removeOponSlot() called and openSlots decremented. openSlots = " + openSlots);
    }

    public int getPort(){
        return serverSocket.getLocalPort();
    }
}
