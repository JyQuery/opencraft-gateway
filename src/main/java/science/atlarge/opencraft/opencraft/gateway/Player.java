package science.atlarge.opencraft.opencraft.gateway;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Player {
    private String username;
    private Session localSession;
    private Session remoteSession;
    private Client remoteServer;
    private boolean Transitting;
    private boolean inGame;
    private int serverId;
    private boolean serverJoinGame = false;

    public int retryCounter = 0;
    public int pktCounter = 0;
    private ConcurrentHashMap<Integer, Packet> cachePacket = new ConcurrentHashMap<>();

    /**
     * A new player connects to the gateway
     */
    public Player(String username, Session localSession) {
        this.username = username;
        this.localSession = localSession;
        Main.players.put(username, this);
        print("Login");
    }

    /**
     * Remove the player from gateway
     */
    public void remove() {
        Main.players.remove(this.username);

        // if sessions are still connected
        try {
            if (getRemoteSession().isConnected())
                getRemoteSession().disconnect("final clean up");
            if (getLocalSession().isConnected())
                getLocalSession().disconnect("final clean up");
        } catch (Exception e) {
            // ignored any exception here
        }
    }

    /**
     * Player Remote session is in game, replay packets
     * @param inGame
     */
    public void setInGame(boolean inGame) {
        this.inGame = inGame;

        if (inGame) {
            print("Replay packet count=" + cachePacket.size());
            Iterator<Map.Entry<Integer, Packet>> iter = cachePacket.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Integer, Packet> entry = iter.next();
                getRemoteSession().send(entry.getValue());
                iter.remove();
            }
        }
    }

    /**
     * Print player debug information
     */
    public void print(String msg) {
        System.out.println(username + "," + msg);
    }

    public Client getRemoteServer() {
        return remoteServer;
    }

    public void setRemoteServer(Client remoteServer) {
        this.remoteServer = remoteServer;
    }

    public Session getLocalSession() {
        return localSession;
    }

    public void setLocalSession(Session localSession) {
        this.localSession = localSession;
    }

    public Session getRemoteSession() {
        return remoteSession;
    }

    public void setRemoteSession(Session remoteSession) {
        this.remoteSession = remoteSession;
    }

    public String getUsername() {
        return username;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public boolean isTransitting() {
        return Transitting;
    }

    public void setTransitting(boolean transitting) {
        Transitting = transitting;
    }

    public boolean isInGame() {
        return inGame;
    }


    public boolean isServerJoinGame() {
        return serverJoinGame;
    }

    public void setServerJoinGame(boolean serverJoinGame) {
        this.serverJoinGame = serverJoinGame;
    }

    public ConcurrentHashMap<Integer, Packet> getCachePacket() {
        return cachePacket;
    }
}
