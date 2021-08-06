package science.atlarge.opencraft.opencraft.gateway.listener;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.packet.handshake.client.HandshakePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSetCompressionPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import science.atlarge.opencraft.opencraft.gateway.Player;
import science.atlarge.opencraft.opencraft.gateway.policy.ConnectRemote;

public class PlayerRemoteListener extends SessionAdapter {
    private Player pi;
    private boolean localDisconnect;

    public PlayerRemoteListener(Player info) {
        this.pi = info;
    }

    @Override
    public void packetReceived(PacketReceivedEvent event) {
        Packet pkt = event.getPacket();
        MinecraftProtocol mp = (MinecraftProtocol) event.getSession().getPacketProtocol();

        // set in game status upon receiving first ingame packet
        if (!pi.isInGame() && mp.getSubProtocol() == SubProtocol.GAME)
            pi.setInGame(true);

//        System.out.println("remote pkt type=" + pkt.getClass().getName());
//        try {Thread.sleep(500);} catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        // do not process these packets
        if (pkt instanceof HandshakePacket
                || pkt instanceof ServerKeepAlivePacket
                || pkt instanceof ClientKeepAlivePacket
                || pkt instanceof LoginSuccessPacket
                || pkt instanceof LoginSetCompressionPacket
            ) {
            return;
        }


        // Only send ServerJoinGame once per local session; prevent Minecraft client to pop up loading screen.
        if (pkt instanceof ServerJoinGamePacket) {
            if (!pi.isServerJoinGame()) {
                pi.getLocalSession().send(pkt);
                pi.setServerJoinGame(true);
            }
            return;
        }

        // forward in game packet only
        if (mp.getSubProtocol() == SubProtocol.GAME) {
            pi.getLocalSession().send(pkt);
        }

        // TODO: check if kicked by server...
        if (pkt instanceof ServerDisconnectPacket)
            localDisconnect = false;

        // TODO: broadcast server chat to all clients
        if (pkt instanceof ServerChatPacket) {
//            System.out.println(((ServerChatPacket)pkt).getMessage());
//            System.out.println(((ServerChatPacket)pkt).getType());

        }
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        String username = pi.getUsername();

        localDisconnect = event.getReason().equals("local disconnect");

        Throwable cause = event.getCause();
        System.out.println(pi.getUsername() + ",serverid=" + pi.getServerId() + ",disconnect,cause=" + cause + ",reason=" + event.getReason());

        if (cause != null) {
            String causeClass = event.getCause().getClass().getName();
            switch (causeClass) {
                // send packet when remote server is not ready
                case "io.netty.channel.AbstractChannel$AnnotatedConnectException":
                // send packet when remote server hasn't finalized login; can retry
                case "io.netty.handler.codec.EncoderException":
                // remote server or gateway overloaded
                // Broken pipe
                // Connection reset by peer
                case "java.io.IOException": case "java.lang.NullPointerException":
                case "io.netty.handler.timeout.ReadTimeoutException":
                    try {
                        // retry most 8 times
                        if (pi.retryCounter ++ < 8) {
                            pi.setInGame(false);
                            Thread.sleep(1500);
                            pi.print("retry count=" + pi.retryCounter + ",remote server=" + pi.getServerId());
                            pi.getCachePacket().clear();
                            pi.setTransitting(false);
                            ConnectRemote.connectRemote(pi, pi.getServerId());
                        }
                    } catch (InterruptedException e) {
                        // ignored
                    }
                    break;
                default:
                    System.out.println("Unhandled Remote Disconnect Event: " + causeClass);
                    cause.printStackTrace();
                    break;
            }
            // disconnect; cannot disconnect - yardstick will quit
//            System.out.println(username +",Disconnecing local");
//            info.getLocalSession().disconnect("Failed to Connect to Remote Server");
//            info.remove();
        }

    }

}
