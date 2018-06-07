package Classes.NetworkCommunication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by HydrusBeta on 8/6/2017.
 */
public class GameData implements Serializable{
    private int frameRate;
    private boolean pause;
    private boolean cancelGame;
    private List<Message> messages = new ArrayList<>();
    private String ammunitionUrl = "";

    // Special Data that is set only by the host:
    private boolean gameStarted;

    // Flags indicating changes to GameData:
    private boolean frameRateRequested = false;
    private boolean messageRequested = false;
    private boolean pauseRequested = false;
    private boolean cancelGameRequested = false;
    private boolean gameStartedRequested = false;
    private boolean ammunitionUrlRequested = false;

    // Default constructor:
    public GameData(){
        super();
    }

    // Copy constructor:
    public GameData(GameData other){
        frameRate = other.getFrameRate();
        messages.addAll(other.messages);
        pause = other.getPause();
        cancelGame = other.getCancelGame();
        gameStarted = other.getGameStarted();
        ammunitionUrl = other.getAmmunitionUrl();

        frameRateRequested = other.isFrameRateRequested();
        messageRequested = other.isMessageRequested();
        pauseRequested = other.isPauseRequested();
        cancelGameRequested = other.isCancelGameRequested();
        gameStartedRequested = other.isGameStartedRequested();
        ammunitionUrlRequested = other.isAmmunitionUrlRequested();
    }

    /* Changers: These are called when a client wants to notify the host that he/she is actively changing something
     * (e.g. sending a message, pausing the game, etc). The host will then notify all clients of the change. These
     * are also called when the host wants to notify clients that the game is starting or is cancelled.*/
    public void changeFrameRate(int frameRate){
        this.frameRate = frameRate;
        frameRateRequested = true;
    }
    public void changeAddMessages(List<Message> messages){
        this.messages.addAll(messages);
        messageRequested = true;
    }
    public void changePause(boolean pause){
        this.pause = pause;
        pauseRequested = true;
    }
    public void changeCancelGame(boolean cancelGame){
        this.cancelGame = cancelGame;
        cancelGameRequested = true;
    }
    public void changeGameStarted(boolean gameStarted){
        this.gameStarted = gameStarted;
        gameStartedRequested = true;
    }
    public void changeAmmunitionUrl(String ammunitionUrl){
        this.ammunitionUrl = ammunitionUrl;
        ammunitionUrlRequested = true;
    }

    /* Setters: These are called when a client simply wants to update locally-stored game information without
     * notifying the host. */
    public void setFrameRate(int frameRate){
        this.frameRate = frameRate;
    }
    public void setAddMessages(List<Message> messages){
        this.messages.addAll(messages);
    }
    public void setPause(boolean pause){
        this.pause = pause;
    }
    public void setGameStarted(boolean gameStarted){
        this.gameStarted = gameStarted;
    }
    public void setCancelGame(boolean cancelGame){
        this.cancelGame = cancelGame;
    }
    public void setAmmunitionUrl(String ammunitionUrl){
        this.ammunitionUrl = ammunitionUrl;
    }

    public void resetFlags(){
        frameRateRequested = false;
        messageRequested = false;
        pauseRequested = false;
        cancelGameRequested = false;
        gameStartedRequested = false;
        ammunitionUrlRequested = false;
    }
    public void resetMessages(){
        messages.clear();
    }

    /* Change Getters: These are called to see whether the sending party has changed the data. They are always
     * called before retrieving the actual game data. */
    public boolean isFrameRateRequested(){
        return frameRateRequested;
    }
    public boolean isMessageRequested(){
        return messageRequested;
    }
    public boolean isPauseRequested(){
        return pauseRequested;
    }
    public boolean isCancelGameRequested(){
        return cancelGameRequested;
    }
    public boolean isGameStartedRequested(){
        return gameStartedRequested;
    }
    public boolean isAmmunitionUrlRequested(){
        return ammunitionUrlRequested;
    }

    /* Direct Getters: These are called to get the actual game data*/
    public int getFrameRate(){
        return frameRate;
    }
    public List<Message> getMessages(){
        return messages;
    }
    public boolean getPause(){
        return pause;
    }
    public boolean getCancelGame(){
        return cancelGame;
    }
    public boolean getGameStarted(){
        return gameStarted;
    }
    public String getAmmunitionUrl(){
        return ammunitionUrl;
    }

}
