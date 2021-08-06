package science.atlarge.opencraft.opencraft.gateway.listener;

import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import science.atlarge.opencraft.opencraft.gateway.RemoteServer;

public class CommandClientListner extends SessionAdapter {
    private RemoteServer server;

    public CommandClientListner(RemoteServer server) {
        this.server = server;
    }

    @Override
    public void packetReceived(PacketReceivedEvent event) {
//        System.out.println(event.getPacket().getClass().getName());
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        System.out.println("commandClient,serverid=" + server.getId() + ",disconnect,cause=" + event.getCause() + ",reason=" + event.getReason());
    }
}
