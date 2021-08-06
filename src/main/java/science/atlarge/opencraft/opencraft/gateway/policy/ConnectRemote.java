package science.atlarge.opencraft.opencraft.gateway.policy;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerTitlePacket;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import science.atlarge.opencraft.opencraft.gateway.Main;
import science.atlarge.opencraft.opencraft.gateway.Player;
import science.atlarge.opencraft.opencraft.gateway.listener.PlayerRemoteListener;

public class ConnectRemote {

    /**
     * The gateway connects to remote server on behalf of client
     */
    public static void connectRemote(Player pi, int serverId) {
        Client client = new Client(Main.servers.get(serverId).getHost(), Main.servers.get(serverId).getPort(),
                new MinecraftProtocol(pi.getUsername()), new TcpSessionFactory());
        pi.setInGame(false);
        pi.setRemoteServer(client);
        pi.setServerId(serverId);

        Session remoteSession = client.getSession();
        pi.setRemoteSession(remoteSession);

        PlayerRemoteListener listener = new PlayerRemoteListener(pi);
        remoteSession.addListener(listener);
        remoteSession.connect();

        if (remoteSession.isConnected())
            pi.print("ConnectRemote,serverid="+serverId);
    }

    /**
     * The gateway switches server for client
     */
    public static void transitRemote(Player pi, int targetServer) {
        if (!pi.isTransitting()) {
            pi.setTransitting(true);
            pi.setInGame(false);
            pi.setServerId(targetServer);
            System.out.println(pi.getUsername() + ",TransitRemote,currServer=" + pi.getServerId());
            ServerTitlePacket tp = new ServerTitlePacket("Transitting", false);
            pi.getLocalSession().send(tp);

            // logout from current server
            pi.getRemoteSession().disconnect("local disconnect");

            // add a time gap for previous server to save player data
            // TODO: change it to confirm whether the player data is successfully saved.
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // login to another
            connectRemote(pi, targetServer);
        }
    }
}
