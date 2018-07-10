package Classes.NetworkCommunication;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by HydrusBeta on 8/4/2017.
 */
public class Packet implements Serializable{
    private Queue<PlayerData> playerDataList = new LinkedList<>();
    private GameData gameData;
    private Queue<PlayPanelData> playPanelDataList = new LinkedList<>();
    private boolean connectionRejected = false;

    // Constructor used in MultiplayerSelectionScene
    public Packet(PlayerData playerData, GameData gameData){
        this.gameData = gameData;
        playerDataList.add(playerData);
    }

    // Constructor used in GameScene, where playPanelData is also relevant.
    public Packet(PlayerData playerData, GameData gameData, PlayPanelData playPanelData){
        this.gameData = gameData;
        playerDataList.add(playerData);
        playPanelDataList.add(playPanelData);
    }

    public void addPlayerData(PlayerData playerData){
        playerDataList.add(playerData);
    }

    public void addOrbData(PlayPanelData playPanelData){
        playPanelDataList.add(playPanelData);
    }

    public void rejectConnection(){
        connectionRejected = true;
    }

    public GameData getGameData(){
        return gameData;
    }

    public Queue<PlayPanelData> getPlayPanelDataList(){
        return playPanelDataList;
    }

    public PlayerData popPlayerData(){
        return playerDataList.poll();
    }

    public PlayPanelData popPlayPanelData(){
        return playPanelDataList.poll();
    }

    public boolean isConnectionRejected(){
        return connectionRejected;
    }
}
