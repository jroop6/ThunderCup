package Classes;

import Classes.Images.StaticBgImages;
import Classes.NetworkCommunication.GameData;
import Classes.NetworkCommunication.Message;
import Classes.NetworkCommunication.PlayerData;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.LinkedList;
import java.util.List;

import static javafx.scene.layout.HBox.setHgrow;
import static javafx.scene.layout.VBox.setVgrow;


/**
 * The chatbox does not immediately display messages that are typed. Instead, It updates the gameData to send a message
 * to the host. When the AnimationTimer for the associated Scene examines the GameData, it should call addMessage() to
 * finally display the message.
 */
public class ChatBox extends StackPane {
    private GameData gameData;
    private PlayerData localPlayerData;
    private final Font font = new Font(14.0);
    private VBox messageContainer;
    private ScrollPane messageScrollPane;
    private boolean showBackground;
    private boolean textFieldShowing = true;
    private TextField textField;
    private HBox messageEntryContainer;

    public ChatBox(GameData gameData, PlayerData localPlayerData, double minHeight, boolean showBackground){
        this.localPlayerData = localPlayerData;
        this.gameData = gameData;
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
            addMessage("Press Enter to open Chat interface");
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
        messageContainer.boundsInLocalProperty().addListener(new ChangeListener<Bounds>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
                // If the user has manually moved the scroll away from the bottom, he/she is probably trying to read
                // and old message, so only set the vValue if its scroll pane is already near the bottom:
                if(messageScrollPane.getVvalue()>0.85 || messageContainer.getChildren().size()<15){
                    messageScrollPane.setVvalue(messageScrollPane.getVmax());
                }
            }
        });

        // Set the actionListener for the send button:
        sendButton.setOnAction((event)->{
            if(textField.getText().isEmpty()){
                if(showBackground) return;
                else{
                    toggleTextFieldVisibility();
                    return;
                }
            }
            List<Message> newMessage = new LinkedList<>();
            newMessage.add(new Message ("<" + localPlayerData.getUsername() + "> " + textField.getText(), localPlayerData.getPlayerID()));
            gameData.changeAddMessages(newMessage);
            addMessage(newMessage.get(0).getString());
            textField.setPromptText("");
            textField.clear();
            if(!showBackground) toggleTextFieldVisibility();
        });

        // Set the actionListener for the pressing the ENTER key:
        textField.setOnAction((event)->{
            sendButton.fire();
        });
    }

    public void toggleTextFieldVisibility(){
        if(textFieldShowing){
            System.out.println("User hit ENTER or SEND and chat box was showing. Turning it off...");
            messageEntryContainer.setOpacity(0.0);
            textFieldShowing = false;
            getParent().requestFocus();
        }
        else{
            System.out.println("User hit ENTER or SEND and chat box was not showing. Turning it on...");
            messageEntryContainer.setOpacity(1.0);
            textFieldShowing = true;
            textField.requestFocus();
        }
    }

    public void addMessage(String newMessage){
        System.out.println("addmessage called");
        Text newText = new Text(newMessage);
        newText.setFont(font);
        if(!showBackground){
            newText.setFill(Color.WHITE);
            newText.setStroke(Color.BLACK);
            newText.setStrokeWidth(1.0);
            newText.setStyle("-fx-font-weight: bold");
        }
        messageContainer.getChildren().add(newText);
    }

    // ignores the localplayer's own messages that have already been printed and have been echoed back by the host.
    public void addMessages(List<Message> newMessages){
        for (Message newMessage: newMessages) {
            if(newMessage.getPlayerID() != localPlayerData.getPlayerID()) addMessage(newMessage.getString());
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


