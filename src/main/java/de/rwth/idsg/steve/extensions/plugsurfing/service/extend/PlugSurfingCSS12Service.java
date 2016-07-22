package de.rwth.idsg.steve.extensions.plugsurfing.service.extend;

import de.rwth.idsg.steve.extensions.plugsurfing.service.PlugSurfingService;
import de.rwth.idsg.steve.extensions.plugsurfing.utils.ConnectorStatusConverter;
import de.rwth.idsg.steve.service.CentralSystemService12_Service;
import ocpp.cs._2010._08.AuthorizeRequest;
import ocpp.cs._2010._08.AuthorizeResponse;
import ocpp.cs._2010._08.StartTransactionRequest;
import ocpp.cs._2010._08.StartTransactionResponse;
import ocpp.cs._2010._08.StatusNotificationRequest;
import ocpp.cs._2010._08.StatusNotificationResponse;
import ocpp.cs._2010._08.StopTransactionRequest;
import ocpp.cs._2010._08.StopTransactionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 29.01.2016
 */
@Service
@Primary
public class PlugSurfingCSS12Service extends CentralSystemService12_Service {

    @Autowired private PlugSurfingService plugSurfingService;

    @Override
    public StartTransactionResponse startTransaction(StartTransactionRequest parameters, String chargeBoxIdentity) {
        StartTransactionResponse startTransactionRequest = super.startTransaction(parameters, chargeBoxIdentity);

        plugSurfingService.asyncUpdateSession(
                startTransactionRequest.getTransactionId(),
                chargeBoxIdentity,
                parameters.getConnectorId(),
                parameters.getIdTag()
        );

        return startTransactionRequest;
    }

    @Override
    public StopTransactionResponse stopTransaction(StopTransactionRequest parameters, String chargeBoxIdentity) {
        StopTransactionResponse response = super.stopTransaction(parameters, chargeBoxIdentity);

        plugSurfingService.asyncPostSession(
                parameters.getTransactionId(),
                parameters.getIdTag()
        );

        return response;
    }

    @Override
    public AuthorizeResponse authorize(AuthorizeRequest parameters, String chargeBoxIdentity) {
        plugSurfingService.verifyRfid(parameters.getIdTag());
        return super.authorize(parameters, chargeBoxIdentity);
    }

    @Override
    public StatusNotificationResponse statusNotification(StatusNotificationRequest parameters,
                                                         String chargeBoxIdentity) {
        StatusNotificationResponse response = super.statusNotification(parameters, chargeBoxIdentity);

        // Connector Id 0 means the whole Charging Station
        if (parameters.getConnectorId() == 0) {
            plugSurfingService.asyncPostStationStatus(
                    chargeBoxIdentity,
                    ConnectorStatusConverter.getConnectorStatus(parameters.getStatus())
            );
        } else {
            plugSurfingService.asyncHandleStatusNotification(
                    chargeBoxIdentity,
                    parameters.getConnectorId(),
                    ConnectorStatusConverter.getConnectorStatus(parameters.getStatus())
            );
        }

        return response;
    }

}
