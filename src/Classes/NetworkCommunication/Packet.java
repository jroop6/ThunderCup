package Classes.NetworkCommunication;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Jonathan Roop on 8/4/2017.
 */
public class Packet implements Serializable{
    private Queue<PlayerData> playerDataList = new LinkedList<>();
    private GameData gameData;
    private Queue<PlayPanelData> playPanelDataList = new LinkedList<>();
    private Synchronizer synchronizer;
    private boolean connectionRejected = false;

    // Constructor used in MultiplayerSelectionScene
    public Packet(Synchronizer synchronizer){
        this.synchronizer = new Synchronizer(synchronizer);
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

    public Synchronizer getSynchronizer(){
        return synchronizer;
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

    public void print(){
        System.out.println("************************");
        System.out.println("* There are " + playerDataList.size() + " items in playerDataList");
        System.out.println("* GameData is " + gameData);
        System.out.println("* There are " + playPanelDataList.size() + " items in playPanelDataList");
        System.out.println("* Synchronizer is " + synchronizer);
        System.out.println("************************");
    }
}
