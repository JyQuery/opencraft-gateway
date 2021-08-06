package science.atlarge.opencraft.opencraft.gateway;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import science.atlarge.opencraft.opencraft.gateway.listener.CommandClientListner;

/**
 * a simple class to store server info
 */
public class RemoteServer {
    private String host;
    private int port;
    private int id;
    private Client client;
    private Session session;

    public RemoteServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public RemoteServer(int id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public boolean connect() {
        client = new Client(getHost(), getPort(),
                new MinecraftProtocol("Gateway"), new TcpSessionFactory());
        CommandClientListner listener = new CommandClientListner(this);
        session = client.getSession();

        session.addListener(listener);
        session.connect();
        return session.isConnected();
    }

    public void sendMsg(String msg) {
        if (session == null)
            return;
        ClientChatPacket pkt = new ClientChatPacket(msg);
        session.send(pkt);
    }

    public void sendMsg(Player pi, ClientChatPacket pkt) {
        if (session == null)
            return;
        String msg = pkt.getMessage();
        String username = pi.getUsername();

        if (msg.charAt(0) != '/') {
            session.send(new ClientChatPacket(username + " says: " + msg));
        }
        else {
            session.send(pkt);
        }

        // TODO: verify op

    }

    @Override
    public String toString() {
        return "host = " + host + ",port = " + port;
    }


}

