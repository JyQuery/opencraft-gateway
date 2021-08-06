package science.atlarge.opencraft.opencraft.gateway.policy;

import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;

public interface Policy {
    void packetReceived(PacketReceivedEvent event);
    void packetSending(PacketSendingEvent event);
    void sessionRemoved(SessionRemovedEvent event);
}
