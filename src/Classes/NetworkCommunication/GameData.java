package Classes.NetworkCommunication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * There are 3 collections of messages - messages here in GameData, and newMessagesIn and newMessagesOut in the ChatBox
 * class. When a player types a message into the TextBox and hits ENTER, the message is added to newMessagesOut by the
 * JavaFX Application Thread. Later, in the updateGameData method of GameScene, a worker thread moves this message into
 * the messages list so that it can be broadcast to other players in a packet. The receiving player's worker thread
 * copies this message over to newMessagesIn. Finally, the JavaFX thread reads newMessagesIn and displays the messages
 * in the ChatBox. Separating messages and messagesOut prevents duplicate messages from appearing in the ChatBox. Otherwise,
 * a client wouldn't be able to distinguish messages that were just typed by the user from messages that were echoed
 * back by the host computer. The synchronized newMessagesIn queue in ChatBox is necessary to prevent a race condition.
 * If we were to put newly-typed messages directly into messages, then it would be possible for the following sequence
 * of events to happen in this order:
 *    In prepareAndSendXXXXPacket(), the worker thread creates a copy of gameData and puts it into a Packet.
 *    the JavaFX application thread adds a new message to gameData (bad timing!)
 *    the worker thread sends the Packet off and calls resetMessages(), destroying the message that was just added.
 */
public class GameData implements Serializable, SynchronizedParent{
    private boolean pause;
    private boolean cancelGame;
    private List<Message> messages = new ArrayList<>(); // new messages to send to other players
    private String ammunitionUrl = "";
    private Map<Long,Integer> missedPacketsCount = new ConcurrentHashMap<>(); // maps playerIDs to the number of misssed packets for that player.
    private boolean gameStarted;

    // Flags indicating changes to GameData:
    private boolean messagesChanged = false;
    private boolean pauseRequested = false;
    private boolean cancelGameRequested = false;
    private boolean gameStartedRequested = false;
    private boolean ammunitionUrlRequested = false;

    // Variables related to displaying victory/defeat graphics:
    private boolean victoryPauseStarted = false;
    private boolean victoryDisplayStarted = false;
    private long victoryTime = 0;
    private int victoriousTeam;

    // TEST
    SynchronizedComparable<Integer> myTestInteger;

    public GameData(Synchronizer synchronizer){
        myTestInteger = new SynchronizedComparable<>("myTestInteger", 42, SynchronizedData.Precedence.HOST, this, synchronizer);
    }

    // Copy constructor:
    public GameData(GameData other){
        messages = new ArrayList<>(other.messages);
        pause = other.getPause();
        cancelGame = other.getCancelGame();
        gameStarted = other.getGameStarted();
        ammunitionUrl = other.getAmmunitionUrl();
        victoryPauseStarted = other.getVictoryPauseStarted();
        victoryDisplayStarted = other.getVictoryDisplayStarted();
        victoryTime = other.getVictoryTime();
        victoriousTeam = other.getVictoriousTeam();
        missedPacketsCount = new ConcurrentHashMap<>(other.getMissedPacketsCount());

        messagesChanged = other.isMessagesChanged();
        pauseRequested = other.isPauseRequested();
        cancelGameRequested = other.isCancelGameRequested();
        gameStartedRequested = other.isGameStartedRequested();
        ammunitionUrlRequested = other.isAmmunitionUrlRequested();
    }

    /* Changers: These are called when a client wants to notify the host that he/she is actively changing something
     * (e.g. sending a message, pausing the game, etc). The host will then notify all clients of the change. These
     * are also called when the host wants to notify clients that the game is starting or is cancelled.*/
    public void changeAddMessages(List<Message> messages){
        this.messages.addAll(messages);
        messagesChanged = true;
    }
    public void changeAddMessage(Message message){
        this.messages.add(message);
        messagesChanged = true;
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
    public void setVictoryPauseStarted(boolean newVal){
        victoryPauseStarted = newVal;
    }
    public void setVictoryDisplayStarted(boolean newVal){
        victoryDisplayStarted = newVal;
    }
    public void setVictoryTime(long newVal){
        victoryTime = newVal;
    }
    public void setVictoriousTeam(int team){
        victoriousTeam = team;
    }

    public void resetFlags(){
        messagesChanged = false;
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
    public boolean isMessagesChanged(){
        return messagesChanged;
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
    public boolean getVictoryPauseStarted(){
        return victoryPauseStarted;
    }
    public boolean getVictoryDisplayStarted(){
        return victoryDisplayStarted;
    }
    public long getVictoryTime(){
        return victoryTime;
    }
    public int getVictoriousTeam(){
        return victoriousTeam;
    }
    public Map<Long,Integer> getMissedPacketsCount(){
        return missedPacketsCount;
    }


    /* MissedPacketsCount manipulators */
    public int getMissedPacketsCount(long playerID){
        return missedPacketsCount.get(playerID);
    }
    public void incrementMissedPacketsCount(long playerID){
        missedPacketsCount.replace(playerID,missedPacketsCount.get(playerID)+1);
    }
    public void setMissedPacketsCount(long playerID, int newVal){
        missedPacketsCount.replace(playerID, newVal);
    }

    // SynchronizedParent interface
    public String getID(){
        return "GAME";
    }


}
