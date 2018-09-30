package Classes.NetworkCommunication;

import Classes.SceneManager;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Initiates 1 connection with the host computer
 */
public class ClientConnectionManager extends ConnectionManager{

    public ClientConnectionManager(String host, int port){
        try{
            Socket clientSideSocket = new Socket(host, port);
            SenderWorker newSenderWorker = new SenderWorker(clientSideSocket);
            ReceiverWorker newReceiverWorker = new ReceiverWorker(this, clientSideSocket);
            senderWorkers.add(newSenderWorker);
            receiverWorkers.add(newReceiverWorker);
            newSenderWorker.start();
            newReceiverWorker.start();
            isConnected = getConfirmation(host, port);
            if(!isConnected){
                cleanUp();
            }
        } catch(ConnectException e){
            displayTimeoutNotice();
            System.err.println("ConnectException encountered while attempting to create client-side socket");
        } catch(UnknownHostException e){
            displayUnknownHostNotice();
            System.err.println("Unknown Host exception encountered while attempting to create client-side socket");
        } catch(IOException e){
            System.err.println("IOException encountered while attempting to create client-side socket");
            e.printStackTrace();
        }
    }

    // Unlike the HostConnectionManager, the ClientConnectionManager does not need to listen for incomming connection
    // requests. Its associated SenderWorker and ReceiverWorker perform all necessary communications. Therefore, the
    // run() method here does nothing.
    /*@Override
    public void run(){
    }*/

    // Waits for and examines the first packet received from the Host. If it contains a rejection notice, then inform
    // the client that they were not able to join the game.
    public boolean getConfirmation(String host, int port){
        System.out.println("checking confirmation");
        Alert connectingDialog = createConnectingDialog(host, port);
        connectingDialog.initOwner(SceneManager.getPrimaryStage());
        connectingDialog.show();
        int timeoutAttempts = 40;
        while(timeoutAttempts>0){
            try{
                sleep(125);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
            Packet packet = retrievePacket();
            if(packet!=null){
                if(packet.isConnectionRejected()){
                    connectingDialog.setResult(ButtonType.CLOSE);
                    displayRejectionNotice();
                    return false; // Host rejected the player from the game!
                }
                else{
                    connectingDialog.setResult(ButtonType.CLOSE);
                    return true; // Host has accepted the client.
                }
            }
            --timeoutAttempts;
        }
        connectingDialog.setResult(ButtonType.CLOSE);
        displayTimeoutNotice();
        return false; // Connection to Host has timed out!
    }

    private Alert createConnectingDialog(String host, int port){
        Alert connectingDialog = new Alert(Alert.AlertType.NONE);
        connectingDialog.initOwner(SceneManager.getPrimaryStage());
        connectingDialog.initStyle(StageStyle.UNDECORATED);
        connectingDialog.setHeaderText("Connecting");
        connectingDialog.setContentText("Connecting to " + host + " on port " + port + "...");
        connectingDialog.setGraphic(null);
        return connectingDialog;
    }

    private void displayUnknownHostNotice(){
        Alert unknownHostNotice = new Alert(Alert.AlertType.CONFIRMATION);
        unknownHostNotice.initOwner(SceneManager.getPrimaryStage());
        unknownHostNotice.setTitle("Host not found.");
        unknownHostNotice.setHeaderText(
                "The host name you entered could not be recognized on the network. Please ask the host of the game \n" +
                "to tell you his/her computer name. If you are sure you've entered the name correctly and this \n" +
                "error message persists, then it is possible that the host's computer is blocking inbound \n" +
                "connections. Make sure that the host's firewall is allowing Java connections through and that \n" +
                "Network Discovery is turned on for that machine.");
        ButtonType returnBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        unknownHostNotice.getButtonTypes().setAll(returnBtn);
        unknownHostNotice.setGraphic(null);
        unknownHostNotice.showAndWait();
    }

    private void displayRejectionNotice(){
        Alert rejectionNotice = new Alert(Alert.AlertType.CONFIRMATION);
        rejectionNotice.initOwner(SceneManager.getPrimaryStage());
        rejectionNotice.setTitle("Join request declined.");
        rejectionNotice.setHeaderText("Unable to join the game; no open player slots are available.");
        ButtonType returnBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        rejectionNotice.getButtonTypes().setAll(returnBtn);
        rejectionNotice.setGraphic(null);
        rejectionNotice.showAndWait();
    }

    private void displayTimeoutNotice(){
        Alert timeoutNotice = new Alert(Alert.AlertType.CONFIRMATION);
        timeoutNotice.initOwner(SceneManager.getPrimaryStage());
        timeoutNotice.setTitle("Connection Timed Out");
        timeoutNotice.setHeaderText("Unable to communicate with the host. Perhaps there is a problem with your network\n " +
                "connection or the host's network connection? Also, make sure you are entering the\n correct hostName and " +
                "port number and that the host is running the same version of the game that you are.");
        ButtonType returnBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        timeoutNotice.getButtonTypes().setAll(returnBtn);
        timeoutNotice.setGraphic(null);
        timeoutNotice.showAndWait();
    }


}
