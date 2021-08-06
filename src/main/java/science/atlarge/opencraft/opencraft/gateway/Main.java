package science.atlarge.opencraft.opencraft.gateway;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import science.atlarge.opencraft.opencraft.gateway.policy.Policy;
import science.atlarge.opencraft.opencraft.gateway.policy.SimplePolicy;


public class Main {
    private static final boolean VERIFY_USERS = false;
    private static String HOST;
    private static int PORT;
    private static String MOTD;
    private static Policy policy;
    public static String functionURL;
    private static final String GAME_VERSION = "1.12.2";
    public static ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, RemoteServer> servers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        parseConfig();

        Server server = new Server(HOST, PORT, MinecraftProtocol.class, new TcpSessionFactory());
        server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, VERIFY_USERS);
        server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, new ServerInfoBuilder() {
            @Override
            public ServerStatusInfo buildInfo(Session session) {
                return new ServerStatusInfo(new VersionInfo(GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION),
                        new PlayerInfo(1000, 0, new GameProfile[0]), new TextMessage(MOTD), null);
            }
        });

        server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 100);

        server.addListener(new ServerAdapter() {

            @Override
            public void serverClosed(ServerClosedEvent event) {
                System.out.println("Gateway closed.");
            }

            @Override
            public void sessionAdded(SessionAddedEvent event) {
                event.getSession().addListener(new SessionAdapter() {
                    @Override
                    public void packetReceived(PacketReceivedEvent event) {
                        policy.packetReceived(event);
                    }
                    @Override
                    public void packetSending(PacketSendingEvent event) {
                        policy.packetSending(event);
                    }
                });
            }

            @Override
            public void sessionRemoved(SessionRemovedEvent event) {
                policy.sessionRemoved(event);
            }
        });

        server.bind();
        System.out.println("Gateway listening on " + HOST + ":" + PORT);
    }

    private static void parseConfig() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("gateway-config.json")) {
            JSONObject obj = (JSONObject)  jsonParser.parse(reader);
            JSONObject gatewayJson = (JSONObject) obj.get("gateway");
            HOST = (String) gatewayJson.getOrDefault("ip", "0.0.0.0");
            PORT = ((Long) gatewayJson.getOrDefault("port", 25565L)).intValue();
            MOTD = (String) gatewayJson.getOrDefault("motd", "");
            functionURL = (String) gatewayJson.getOrDefault("functionEndpoint", "");

            JSONObject serversJson = (JSONObject) (obj).get("servers");
            Iterator srvs = serversJson.keySet().iterator();
            for(int i = 0; srvs.hasNext(); i++) {
                String srv = (String) srvs.next();
                JSONObject srvObj = (JSONObject) serversJson.get(srv);
                int port = ((Long) srvObj.get("port")).intValue();
                RemoteServer server = new RemoteServer(i, srv, port);
                // start a client for each server to send commands
//                connectCommandClient(server);
                servers.put(i, server);
            }

            for (Integer name: servers.keySet()) {
                String key = name.toString();
                String value = servers.get(name).toString();
                System.out.println(key + " " + value);
            }

            String POLICY = (String) gatewayJson.get("policy");
            switch(POLICY) {
                case "simple":
                    policy = new SimplePolicy();
                    ((SimplePolicy) policy).setSpawnX(((Long)gatewayJson.getOrDefault("spawnx",0L)).intValue());
                    ((SimplePolicy) policy).setSpawnZ(((Long)gatewayJson.getOrDefault("spawnz", 0L)).intValue());
                    System.out.println("Simple Policy, spawnx="+ ((SimplePolicy)policy).getSpawnX() + ",spawnz="+((SimplePolicy)policy).getSpawnZ());
                    break;
                default:
                    System.out.println("Invalid Policy: " + POLICY);
                    System.exit(-1);
            }
            System.out.println("HOST="+HOST+",PORT="+PORT+",POLICY="+POLICY);

        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void connectCommandClient(RemoteServer server) {
        if (!server.connect()) {
            System.out.println("Exit; Cannot connect to remote server " +server.getId() + "," + server.getHost() + ":" + server.getPort());
//            System.exit(-1);
        }
    }


}