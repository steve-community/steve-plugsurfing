package de.rwth.idsg.steve.extensions.plugsurfing.rest;

import de.rwth.idsg.steve.extensions.plugsurfing.model.SuccessResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.ConnectorPostStatus;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.RfidVerify;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.SessionPost;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.StationPost;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.ConnectorPostStatusResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.RfidVerifyResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.SessionPostResponse;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
public interface Client {
    ConnectorPostStatusResponse connectorPostStatus(ConnectorPostStatus request);
    RfidVerifyResponse rfidVerify(RfidVerify request);
    SessionPostResponse sessionPost(SessionPost request);
    SuccessResponse stationPost(StationPost request);
}
