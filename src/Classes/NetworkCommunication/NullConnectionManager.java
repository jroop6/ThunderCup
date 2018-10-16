package Classes.NetworkCommunication;

import static Classes.NetworkCommunication.Player.HOST_ID;

/**
 * The GameScene needs a ConnectionManager to operate. For single-player games, the NullConnectionManager keeps the
 * GameScene happy without actually establishing any network connections.
 */
public class NullConnectionManager extends ConnectionManager{
    public NullConnectionManager(){
        super(HOST_ID);
        isConnected = true;
    }
}
