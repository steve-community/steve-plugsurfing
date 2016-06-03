package de.rwth.idsg.steve.extensions.plugsurfing.rest;

import de.rwth.idsg.steve.extensions.plugsurfing.Constants;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.AbstractRequest;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.ConnectorPostStatus;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.RfidVerify;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.SessionPost;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.StationPost;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.ConnectorPostStatusResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.RfidVerifyResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.SessionPostResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.StationPostResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Slf4j
@Component
public class ClientImpl implements Client {

    @Autowired private RestTemplate restTemplate;

    public ConnectorPostStatusResponse connectorPostStatus(ConnectorPostStatus request) {
        setPartnerId(request);

        try {
            return restTemplate.postForObject(getPath(), request, ConnectorPostStatusResponse.class);
        } catch (Exception e) {
            log.error("Error occurred", e);
            throw e;
        }
    }

    public RfidVerifyResponse rfidVerify(RfidVerify request) {
        try {
            return restTemplate.postForObject(getPath(), request, RfidVerifyResponse.class);
        } catch (Exception e) {
            log.error("Error occurred", e);
            throw e;
        }
    }

    public SessionPostResponse sessionPost(SessionPost request) {
        setPartnerId(request);

        try {
            return restTemplate.postForObject(getPath(), request, SessionPostResponse.class);
        } catch (Exception e) {
            log.error("Error occurred", e);
            throw e;
        }
    }

    public StationPostResponse stationPost(StationPost request) {
        setPartnerId(request);

        try {
            return restTemplate.postForObject(getPath(), request, StationPostResponse.class);
        } catch (Exception e) {
            log.error("Error occurred", e);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void setPartnerId(AbstractRequest request) {
        request.setPartnerIdentifier(Constants.CONFIG.getPartnerIdentifier());
    }

    private static String getPath() {
        return Constants.CONFIG.getPath();
    }
}
