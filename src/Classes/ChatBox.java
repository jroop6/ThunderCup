package Classes;

import Classes.Images.StaticBgImages;
import Classes.NetworkCommunication.*;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static javafx.scene.layout.HBox.setHgrow;
import static javafx.scene.layout.VBox.setVgrow;


/**
 * How to Network Chat Messages:
 * what the host does:
 * 	every frame:
 * 	 gather everyone's messages by calling hostSynchronizer.synchronizingWith(clientSynchronizer) for each clientSynchronizer
 * 	 put all of these messages into the host's messagesOut (located in PlayerData) using changeAdd()
 *
 * 	24 times per second:
 * 	 display all messages in messagesOut to the chat
 * 	 send a packet to all the other players
 * 	 call changeClear() on our own messagesOut
 *
 * 	variable timing:
 * 	 hitting enter in the chat should call changeAdd(newMessage) on messagesOut
 *
 *
 * what the client does:
 * 	every frame:
 * 	 retrieve the host's messages by calling synchronizeWith()
 * 	 display the host's new messages
 *
 * 	24 times per second:
 * 	 send a packet to the host
 * 	 call changeClear() on our own messagesOut
 *
 * 	variable timing:
 * 	 hitting enter in the chat should call changeAdd(newMessage) on messagesOut
 */
public class ChatBox extends StackPane {
    private final Font font = new Font(14.0);
    private VBox messageContainer;
    private ScrollPane messageScrollPane;
    private boolean showBackground;
    private boolean textFieldShowing = true;
    private TextField textField;
    private HBox messageEntryContainer;

    // dataChanger is the threadPool that should handle changes to the localPlayerData's SynchronizedDatas. If it is
    // null, then the change will occur on the JavaFX Application Thread.
    public ChatBox(PlayerData localPlayerData, double minHeight, boolean showBackground){
        this.showBackground = showBackground;
        setMinHeight(minHeight);
        setMaxHeight(minHeight);
        if(!showBackground) setMouseTransparent(true);

        // The message displayer (ScrollPane) and message input box (TextField) are oriented vertically to each other, so put everything in a VBox.
        VBox verticalOrienter = new VBox();
        getChildren().add(verticalOrienter);

        // Messages are displayed in a ScrollPane:
        messageScrollPane = new ScrollPane();
        messageContainer = new VBox();
        if(showBackground){
            messageContainer.setBackground(new Background(new BackgroundImage(StaticBgImages.CHATBOX_SCROLLPANE_BACKGROUND.getImageView().getImage(),null,null,null,null)));
            messageScrollPane.setFitToWidth(true);  // This ensures that the background spans the entire width of the ScrollPane
            messageScrollPane.setFitToHeight(true); // This ensures that the background spans the entire height of the ScrollPane
        }
        else{
            // Unfortunately, the background of a ScrollPane cannot easily be made transparent without CSS. We must traverse its list of child Nodes within a ChangeListener and make each one transparent:
            messageScrollPane.skinProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue != null && newValue.getNode() instanceof Region){
                    Region r = (Region) newValue.getNode();
                    r.setBackground(Background.EMPTY);
                    r.getChildrenUnmodifiable().stream().filter(n -> n instanceof Region).map(n-> (Region) n).forEach(n -> n.setBackground(Background.EMPTY));
                }
            });
            displayMessage(new Message("Press Enter to open Chat interface",localPlayerData.getPlayerID()));
        }
        messageScrollPane.setContent(messageContainer);
        verticalOrienter.getChildren().add(messageScrollPane);
        setVgrow(messageScrollPane, Priority.ALWAYS);
        //if(!showBackground) messageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Player enters messages in an editable TextField:
        messageEntryContainer = new HBox();
        textField = new TextField();
        textField.setPromptText("Enter chat messages here");
        textField.setStyle("-fx-prompt-text-fill: rgba(0,0,0,0.75);");
        if(showBackground) textField.setBackground(new Background(new BackgroundImage(StaticBgImages.CHATBOX_TEXTFIELD_BACKGROUND.getImageView().getImage(),null,null,null,null)));
        else{
            textField.setBackground(new Background(new BackgroundFill(new Color(0,0,0,0.5), CornerRadii.EMPTY,Insets.EMPTY)));
            textField.setStyle("-fx-text-fill: white;");
            // Initially make the TextField invisible:
            messageEntryContainer.setOpacity(0.0);
            textFieldShowing = false;
        }
        textField.setFont(font);
        messageEntryContainer.getChildren().add(textField);

        // Messages are sent by clicking on a send button (or by hitting ENTER):
        Button sendButton = new Button("Send");
        if(showBackground){
            sendButton.setFont(font);
            messageEntryContainer.getChildren().add(sendButton);
        }
        setHgrow(textField,Priority.ALWAYS);
        messageEntryContainer.setBorder(new Border(new BorderStroke(Color.BLACK,BorderStrokeStyle.SOLID,new CornerRadii(5.0),new BorderWidths(2.0))));
        verticalOrienter.getChildren().add(messageEntryContainer);

        // Make the scrollpane automatically scroll to the bottom of the messages when a new message is added:
        messageContainer.boundsInLocalProperty().addListener((ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue)-> {
            // If the user has manually moved the scroll away from the bottom, he/she is probably trying to read
            // an old message, so only set the vValue if its scroll pane is already near the bottom:
            if(messageScrollPane.getVvalue()>0.85 || messageContainer.getChildren().size()<15){
                messageScrollPane.setVvalue(messageScrollPane.getVmax());
            }
        });

        // Set the actionListener for the send button:
        sendButton.setOnAction((event)->{
            if(textField.getText().isEmpty()){
                if(showBackground) return;
                else{
                    hideTextField();
                    return;
                }
            }
            Message newMessage = new Message ("<" + localPlayerData.getUsername().getData() + "> " + textField.getText(), localPlayerData.getPlayerID());
            localPlayerData.getMessagesOut().changeAdd(newMessage);
            textField.setPromptText("");
            textField.clear();
            if(!showBackground) hideTextField();
        });

        // Set the actionListener for the pressing the ENTER key:
        textField.setOnAction((event)->{
            sendButton.fire();
        });
    }

    public boolean isTextFieldShowing(){
        return textFieldShowing;
    }

    public void hideTextField(){
        System.out.println("User hit ENTER or SEND and chat box was showing. Turning it off...");
        messageEntryContainer.setOpacity(0.0);
        textFieldShowing = false;
        getParent().requestFocus();
    }

    public void showTextField(){
        System.out.println("User hit ENTER or SEND and chat box was not showing. Turning it on...");
        messageEntryContainer.setOpacity(1.0);
        textFieldShowing = true;
        textField.requestFocus();
    }

    public synchronized void displayMessage(Message message){
        Text newText = new Text(message.getString());
        newText.setFont(font);
        if(!showBackground){
            newText.setFill(Color.WHITE);
            newText.setStroke(Color.BLACK);
            newText.setStrokeWidth(1.0);
            newText.setStyle("-fx-font-weight: bold");
        }
        Platform.runLater(()->messageContainer.getChildren().add(newText));
    }

    public synchronized void displayMessages(List<Message> messages){
        for(Message message : messages){
            displayMessage(message);
        }
    }

    // For manually scrolling 1 tick, either up or down.
    // sign is +1 for scrolling down, -1 for scrolling up.
    public void scroll(int direction){
        int numMessages = messageContainer.getChildren().size();
        double vMax = messageScrollPane.getVmax();
        double vMin = messageScrollPane.getVmin();
        double incrementAmount = (vMax-vMin)/numMessages;
        double newVvalue = messageScrollPane.getVvalue() + direction*incrementAmount;

        // Set the Vvalue, respecting the vMax and vMin limits.
        if(newVvalue>vMax) messageScrollPane.setVvalue(vMax);
        else if(newVvalue<vMin) messageScrollPane.setVvalue(vMin);
        else messageScrollPane.setVvalue(newVvalue);
    }

}


