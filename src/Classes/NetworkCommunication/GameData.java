package Classes.NetworkCommunication;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static Classes.NetworkCommunication.PlayerData.GAME_ID;

public class GameData implements Serializable{
    private SynchronizedComparable<Boolean> gameCanceled;
    private SynchronizedComparable<Boolean> pause;
    private SynchronizedComparable<Boolean> gameStarted;

    private Map<Long,Integer> missedPacketsCount = new ConcurrentHashMap<>(); // maps playerIDs to the number of misssed packets for that player.

    // Variables related to displaying victory/defeat graphics:
    private boolean victoryPauseStarted = false;
    private boolean victoryDisplayStarted = false;
    private long victoryTime = 0;
    private int victoriousTeam;

    public GameData(Synchronizer synchronizer){
        synchronized (synchronizer){
            gameCanceled = new SynchronizedComparable<>("cancelGame", false, SynchronizedData.Precedence.HOST, GAME_ID, synchronizer);
            pause = new SynchronizedComparable<>("pause", false, SynchronizedData.Precedence.CLIENT, GAME_ID, synchronizer);
            gameStarted = new SynchronizedComparable<>("gameStarted", false, SynchronizedData.Precedence.HOST, GAME_ID, synchronizer);
        }
    }

    // Copy constructor:
    public GameData(GameData other){
        Synchronizer synchronizer = new Synchronizer();
        pause = new SynchronizedComparable<>("pause", other.pause.getData(), other.pause.getPrecedence(), GAME_ID, synchronizer);
        gameCanceled = new SynchronizedComparable<>("cancelGame", other.gameCanceled.getData(), other.gameCanceled.getPrecedence(), GAME_ID, synchronizer);
        gameStarted = new SynchronizedComparable<>("gameStarted", other.gameStarted.getData(), other.gameStarted.getPrecedence(), GAME_ID, synchronizer);
        victoryPauseStarted = other.victoryPauseStarted;
        victoryDisplayStarted = other.victoryDisplayStarted;
        victoryTime = other.victoryTime;
        victoriousTeam = other.victoriousTeam;
        missedPacketsCount = new ConcurrentHashMap<>(other.getMissedPacketsCount());
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
    public SynchronizedComparable<Boolean> getPause(){
        return pause;
    }
    public SynchronizedComparable<Boolean> getGameCanceled(){
        return gameCanceled;
    }
    public SynchronizedComparable<Boolean> getGameStarted(){
        return gameStarted;
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
