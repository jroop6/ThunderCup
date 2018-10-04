package Classes.NetworkCommunication;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static Classes.NetworkCommunication.PlayerData.GAME_ID;

public class GameData implements Serializable{
    private SynchronizedComparable<Boolean> gameCanceled;
    private SynchronizedComparable<Boolean> pause;
    private SynchronizedComparable<Boolean> gameStarted;
    private String ammunitionUrl = "";
    private Map<Long,Integer> missedPacketsCount = new ConcurrentHashMap<>(); // maps playerIDs to the number of misssed packets for that player.

    // Flags indicating changes to GameData:
    private boolean pauseRequested = false;
    private boolean gameStartedRequested = false;
    private boolean ammunitionUrlRequested = false;

    // Variables related to displaying victory/defeat graphics:
    private boolean victoryPauseStarted = false;
    private boolean victoryDisplayStarted = false;
    private long victoryTime = 0;
    private int victoriousTeam;

    public GameData(Synchronizer synchronizer){
        synchronized (synchronizer){
            gameCanceled = new SynchronizedComparable<>("cancelGame", new Boolean(false), SynchronizedData.Precedence.HOST, GAME_ID, synchronizer);
            pause = new SynchronizedComparable<>("pause", new Boolean(false), SynchronizedData.Precedence.CLIENT, GAME_ID, synchronizer);
            gameStarted = new SynchronizedComparable<>("gameStarted", new Boolean(false), SynchronizedData.Precedence.HOST, GAME_ID, synchronizer);
        }
    }

    // Copy constructor:
    public GameData(GameData other){
        Synchronizer synchronizer = new Synchronizer();
        pause = new SynchronizedComparable<>("pause", other.getPause().getData(), other.getPause().getPrecedence(), GAME_ID, synchronizer);
        gameCanceled = new SynchronizedComparable<>("cancelGame", other.getGameCanceled().getData(), other.getGameCanceled().getPrecedence(), GAME_ID, synchronizer);
        gameStarted = new SynchronizedComparable<>("gameStarted", other.getGameStarted().getData(), other.getGameStarted().getPrecedence(), GAME_ID, synchronizer);
        ammunitionUrl = other.getAmmunitionUrl();
        victoryPauseStarted = other.getVictoryPauseStarted();
        victoryDisplayStarted = other.getVictoryDisplayStarted();
        victoryTime = other.getVictoryTime();
        victoriousTeam = other.getVictoriousTeam();
        missedPacketsCount = new ConcurrentHashMap<>(other.getMissedPacketsCount());

        pauseRequested = other.isPauseRequested();
        gameStartedRequested = other.isGameStartedRequested();
        ammunitionUrlRequested = other.isAmmunitionUrlRequested();
    }

    public void changeAmmunitionUrl(String ammunitionUrl){
        this.ammunitionUrl = ammunitionUrl;
        ammunitionUrlRequested = true;
    }

    /* Setters: These are called when a client simply wants to update locally-stored game information without
     * notifying the host. */
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
        pauseRequested = false;
        gameStartedRequested = false;
        ammunitionUrlRequested = false;
    }

    /* Change Getters: These are called to see whether the sending party has changed the data. They are always
     * called before retrieving the actual game data. */
    public boolean isPauseRequested(){
        return pauseRequested;
    }
    public boolean isGameStartedRequested(){
        return gameStartedRequested;
    }
    public boolean isAmmunitionUrlRequested(){
        return ammunitionUrlRequested;
    }

    /* Direct Getters: These are called to get the actual game data*/
    public SynchronizedComparable<Boolean> getPause(){
        return pause;
    }
    public SynchronizedComparable<Boolean> getGameStarted(){
        return gameStarted;
    }
    public SynchronizedComparable<Boolean> getGameCanceled(){
        return gameCanceled;
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

}
