package de.rwth.idsg.steve.extensions.plugsurfing.service.extend;

import de.rwth.idsg.steve.extensions.plugsurfing.model.data.ConnectorStatus;
import de.rwth.idsg.steve.extensions.plugsurfing.service.PlugSurfingService;
import de.rwth.idsg.steve.ocpp.ws.ocpp15.Ocpp15WebSocketEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 22.07.2016
 */
@Component
@Primary
public class PlugSurfingOcpp15WebSocketEndpoint extends Ocpp15WebSocketEndpoint {

    @Autowired private PlugSurfingService plugSurfingService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        // Probably, charging station will send connector status info after connecting (which will override this
        // status send with more precise info). But in case it does not, we need to invalidate its "Offline"
        // status we sent after connection close.
        plugSurfingService.asyncPostStationStatus(getChargeBoxId(session), ConnectorStatus.Available);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        super.afterConnectionClosed(session, closeStatus);
        plugSurfingService.asyncPostStationStatus(getChargeBoxId(session), ConnectorStatus.Offline);
    }
}
