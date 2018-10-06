package Classes.NetworkCommunication;

import Classes.PlayPanel;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Jonathan Roop on 8/4/2017.
 */
public class Packet implements Serializable{
    private Queue<PlayerData> playerDataList = new LinkedList<>();
    private GameData gameData;
    private Queue<PlayPanel> playPanelList = new LinkedList<>();
    private Synchronizer synchronizer;
    private boolean connectionRejected = false;

    // Constructor used in MultiplayerSelectionScene
    public Packet(Synchronizer synchronizer){
        this.synchronizer = new Synchronizer(synchronizer);
    }

    // Constructor used in GameScene, where playPanelData is also relevant.
    public Packet(PlayerData playerData, GameData gameData, PlayPanel playPanel){
        this.gameData = gameData;
        playerDataList.add(playerData);
        playPanelList.add(playPanel);
    }

    public void addPlayerData(PlayerData playerData){
        playerDataList.add(playerData);
    }

    public void addPlayPanel(PlayPanel playPanel){
        playPanelList.add(playPanel);
    }

    public void rejectConnection(){
        connectionRejected = true;
    }

    public GameData getGameData(){
        return gameData;
    }

    public Queue<PlayPanel> getPlayPanelList(){
        return playPanelList;
    }

    public Synchronizer getSynchronizer(){
        return synchronizer;
    }

    public PlayerData popPlayer(){
        return playerDataList.poll();
    }

    public PlayPanel popPlayPanel(){
        return playPanelList.poll();
    }

    public boolean isConnectionRejected(){
        return connectionRejected;
    }

    public void print(){
        System.out.println("************************");
        System.out.println("* There are " + playerDataList.size() + " items in playerDataList");
        System.out.println("* GameData is " + gameData);
        System.out.println("* There are " + playPanelList.size() + " items in playPanelDataList");
        System.out.println("* Synchronizer is " + synchronizer);
        System.out.println("************************");
    }
}
