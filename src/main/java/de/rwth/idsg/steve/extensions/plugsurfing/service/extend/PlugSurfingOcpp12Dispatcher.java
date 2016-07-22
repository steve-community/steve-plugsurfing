package de.rwth.idsg.steve.extensions.plugsurfing.service.extend;

import de.rwth.idsg.steve.extensions.plugsurfing.service.PlugSurfingService;
import de.rwth.idsg.steve.extensions.plugsurfing.utils.ConnectorStatusConverter;
import de.rwth.idsg.steve.handler.OcppCallback;
import de.rwth.idsg.steve.handler.OcppResponseHandler;
import de.rwth.idsg.steve.repository.dto.ChargePointSelect;
import de.rwth.idsg.steve.service.ChargePointService12_Dispatcher;
import lombok.extern.slf4j.Slf4j;
import ocpp.cp._2010._08.AvailabilityStatus;
import ocpp.cp._2010._08.ChangeAvailabilityRequest;
import ocpp.cp._2010._08.ChangeAvailabilityResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 22.03.2016
 */
@Slf4j
@Service
@Primary
public class PlugSurfingOcpp12Dispatcher extends ChargePointService12_Dispatcher {
    @Autowired private PlugSurfingService plugSurfingService;

    @Override
    public void changeAvailability(ChargePointSelect cp,
                                   OcppResponseHandler<ChangeAvailabilityRequest, ChangeAvailabilityResponse> handler) {

        OcppCallback<ChangeAvailabilityResponse> callback = new OcppCallback<ChangeAvailabilityResponse>() {
            @Override
            public void success(ChangeAvailabilityResponse response) {
                if (response.getStatus() != AvailabilityStatus.ACCEPTED) {
                    return;
                }

                ChangeAvailabilityRequest request = handler.getRequest();

                // Edge case of connId == 0, which is the whole charging station
                if (request.getConnectorId() == 0) {
                    plugSurfingService.postChangeAvailability(
                            ConnectorStatusConverter.getConnectorStatus(request.getType()),
                            cp.getChargeBoxId()
                    );
                } else {
                    plugSurfingService.postConnectorStatus(
                            ConnectorStatusConverter.getConnectorStatus(request.getType()),
                            cp.getChargeBoxId(),
                            request.getConnectorId());
                }
            }

            @Override
            public void failed(String errorMessage) {
                // Do nothing
            }
        };

        handler.addCallback(callback);
        super.changeAvailability(cp, handler);
    }
}
