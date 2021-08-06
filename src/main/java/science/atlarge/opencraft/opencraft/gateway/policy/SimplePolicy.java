package science.atlarge.opencraft.opencraft.gateway.policy;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.packet.handshake.client.HandshakePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.packet.Packet;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import science.atlarge.opencraft.opencraft.gateway.Main;
import science.atlarge.opencraft.opencraft.gateway.Player;
import science.atlarge.opencraft.opencraft.gateway.serverless.HttpCall;

import java.util.Random;
import java.util.UUID;

import static science.atlarge.opencraft.opencraft.gateway.policy.ConnectRemote.connectRemote;
import static science.atlarge.opencraft.opencraft.gateway.policy.ConnectRemote.transitRemote;

public class SimplePolicy implements Policy {
    private final int serverCount;
    private int spawnX;
    private int spawnZ;

    public SimplePolicy () {
        serverCount = Main.servers.size();
    }

    @Override
    public void packetReceived(PacketReceivedEvent event) {
        Packet pkt = event.getPacket();
        Session localSession = event.getSession();
        MinecraftProtocol mp = (MinecraftProtocol) localSession.getPacketProtocol();
        boolean pktForwarded = false;

//                            System.out.println("pkt type = " + pkt.getClass().getName());

        if (pkt instanceof HandshakePacket || pkt instanceof ClientKeepAlivePacket)
            return;

        // Login Start is the first packet of login
        // log user and start a client to remote server
        if (pkt instanceof LoginStartPacket) {
            String username = ((LoginStartPacket) pkt).getUsername();
            Player pi = new Player(username, localSession);

            new Thread(() -> connectRemote(pi, getInitServer(pi))).start();
            return;
        }

        GameProfile profile = event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);
        String username = profile.getName();
        UUID uuid = profile.getId();
        Player pi = Main.players.get(username);

        // check client position and perform policy
        if (pkt instanceof ClientPlayerPositionPacket) {
            ClientPlayerPositionPacket lastPos = (ClientPlayerPositionPacket) pkt;

//            System.out.println("username=" + username + ",uuid=" + uuid
//                    + ",pos=" + ((ClientPlayerPositionPacket) pkt).getX() + ","
//                    + ((ClientPlayerPositionPacket) pkt).getY() + ","
//                    + ((ClientPlayerPositionPacket) pkt).getZ());

            int currServerId = pi.getServerId();
            double x = ((ClientPlayerPositionPacket) pkt).getX();
            double z = ((ClientPlayerPositionPacket) pkt).getZ();
            int offsetX = 0;
            int offsetZ = 0;
            int offsetDis = 2;
            int newServerId = getServerByXZ(x,z,1);

//            switch (serverCount) {
//
//                case 2: case 3:
//                    // x>0, serverid = 0
//                    // x<0, serverid = 1
//                    if (x < -1 + spawnx)
//                        offsetX = -offsetDis;
//
//                    if (x >= 1 + spawnx)
//                        offsetX = offsetDis;
//
//                    break;
//                case 4: default:
//                    // x>0, z>0, serverid = 0
//                    // x>0, z<0, serverid = 1
//                    // x<0, z>0, serverid = 2
//                    // x<0, z<0, serverid = 3
//                    if (x>=1 + spawnx & z>=1 + spawnz) {
//                        offsetX = offsetDis;
//                        offsetZ = offsetDis;
//                    }
//
//                    if (x>=1 + spawnx && z<-1 + spawnz) {
//                        offsetX = offsetDis;
//                        offsetZ = -offsetDis;
//                    }
//
//                    if (x<-1 + spawnx && z>=1 + spawnz) {
//                        offsetX = -offsetDis;
//                        offsetZ = offsetDis;
//                    }
//
//                    if (x<-1 + spawnx && z<-1 + spawnz) {
//                        offsetX = -offsetDis;
//                        offsetZ = -offsetDis;
//                    }
//                    break;
//            }

            // transit if the current location belongs to a different server
            if (currServerId != newServerId && newServerId != -1) {
//                pi.setOffsetPos(new ClientPlayerPositionPacket(lastPos.isOnGround(),
//                        lastPos.getX() + offsetX, lastPos.getY(), lastPos.getZ() + offsetZ));
                new Thread(() -> transitRemote(pi, newServerId)).start();
            }

        }

//        if (pi.isTransitting()) {
//            // all offset packets are sent
//            if (pi.isServerOffset() && pi.isClientOffset()) {
//                pi.setServerOffset(false);
//                pi.setClientOffset(false);
//                pi.setTransitting(false);
//            }
//        }

        // forward these to all servers
//        if (pkt instanceof ClientChatPacket) {
//            for (int id : Main.servers.keySet()) {
//                if (id != pi.getServerId()) {
//                    Main.servers.get(id).sendMsg(pi, (ClientChatPacket)pkt);
//                }
//            }
//        }

        // forward all other packets to remote server
        // when remote session is connected
        if (pi.getRemoteSession().isConnected()) {
            // forward GAME packets only when player is in Game
            if (mp.getSubProtocol() == SubProtocol.GAME) {
                if (((MinecraftProtocol)pi.getRemoteSession().getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) {
                    pi.getRemoteSession().send(pkt);
                    pktForwarded = true;

                    if (pi.isTransitting())
                        pi.setTransitting(false);
                }
            }
            else {
                pi.getRemoteSession().send(pkt);
                pktForwarded = true;
            }
        }
        // cache packet if not forwarded
        if (!pktForwarded)
            pi.getCachePacket().put(++pi.pktCounter, pkt);
    }

    /**
     * Serverless: Check if player has played before. If yes, get its last location
     */
    private int getInitServer(Player pi) {
        double x = 0, y = 0, z = 0;
        boolean hasPlayed = false;
        JSONObject jObj = new JSONObject();
        jObj.put("command", "getLocation");
        jObj.put("username", pi.getUsername());

        HttpCall httpCall = new HttpCall(Main.functionURL);
        String res = httpCall.requestWithPayload(jObj.toString());
        try {
            JSONObject resObj = (JSONObject) new JSONParser().parse(res);
            x = (double) resObj.get("x");
            y = (double) resObj.get("y");
            z = (double) resObj.get("z");
            hasPlayed = true;
        } catch (Exception e) {
            hasPlayed = false;
        }

        int initServer;
        if (hasPlayed)
            initServer = getServerByXZ(x, z, 0);
        else {
            // assign a random server for new player
            Random random = new Random(System.nanoTime());
            initServer = random.nextInt(Main.servers.size());
        }
        pi.print("x="+x+",y="+y+",z="+z+",initServer="+initServer);
        return initServer;
    }

    public int getServerByXZ(double x, double z, int offset) {
        int serverId = -1;
        switch(serverCount) {
            case 1:
                serverId = 0;
                break;
            case 2: case 3:
                if (x >= offset + spawnX)
                    serverId = 0;
                if (x < -offset + spawnX)
                    serverId = 1;
                break;
            case 4: default:
                if (x>=offset + spawnX & z>=offset + spawnZ)
                    serverId = 0;

                if (x>=offset + spawnX && z<-offset + spawnZ)
                    serverId = 1;

                if (x<-offset + spawnX && z>=offset + spawnZ)
                    serverId = 2;

                if (x<-offset + spawnX && z<-offset + spawnZ)
                    serverId = 3;
                break;
        }
        return serverId;
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        //                        System.out.println("sent,pkt=" + event.getPacket().getClass().getName());
        Packet pkt = event.getPacket();

//        if (pkt instanceof ServerPlayerPositionRotationPacket) {
//            GameProfile profile = event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);
//            String username = profile.getName();
//            UUID uuid = profile.getId();
//            Player pi = Main.players.get(username);

//            if (pi.isTransitting() && !pi.isClientOffset()) {
//                pi.setClientOffset(true);
//                pi.setOp(null);
//                // cache for the first time
//                if (pi.getOp() == null)
//                    pi.setOp((ServerPlayerPositionRotationPacket) pkt);
//                else {
////                    ServerPlayerPositionRotationPacket op = pi.getOp();
//                    ServerPlayerPositionRotationPacket op = (ServerPlayerPositionRotationPacket) pkt;
//                    ServerPlayerPositionRotationPacket np = new ServerPlayerPositionRotationPacket(
//                            pi.getOffsetPos().getX(), op.getY(), op.getZ(), op.getYaw(), op.getPitch(), op.getTeleportId());
//                    System.out.println(pi.getUsername() + ",send offset position to client, ServerPlayerPositionRotationPacket=" + np.getX() + "," + np.getZ());
//                    pi.setClientOffset(true);
//                    pi.setOp(null);
//                    event.setPacket(np);
//                    // send twice
//                    pi.getLocalSession().send(np);
//                }
//        }

    }


    @Override
    public void sessionRemoved(SessionRemovedEvent event) {
        MinecraftProtocol protocol = (MinecraftProtocol) event.getSession().getPacketProtocol();

        GameProfile profile = event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);
        String username = profile.getName();
//                System.out.println(username + ",sessionRemoved()");

        if (Main.players.containsKey(username)) {
            Player player = Main.players.get(username);
            player.getRemoteSession().disconnect("local disconnect");
            Main.players.remove(username);
        }
    }

    public int getSpawnX() {
        return spawnX;
    }

    public void setSpawnX(int spawnX) {
        this.spawnX = spawnX;
    }

    public int getSpawnZ() {
        return spawnZ;
    }

    public void setSpawnZ(int spawnZ) {
        this.spawnZ = spawnZ;
    }
}