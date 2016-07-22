package de.rwth.idsg.steve.extensions.plugsurfing.service.extend;

import de.rwth.idsg.steve.extensions.plugsurfing.model.data.ConnectorStatus;
import de.rwth.idsg.steve.extensions.plugsurfing.service.PlugSurfingService;
import de.rwth.idsg.steve.extensions.plugsurfing.utils.ConnectorStatusConverter;
import de.rwth.idsg.steve.handler.OcppCallback;
import de.rwth.idsg.steve.handler.OcppResponseHandler;
import de.rwth.idsg.steve.repository.dto.ChargePointSelect;
import de.rwth.idsg.steve.service.ChargePointService15_Dispatcher;
import lombok.extern.slf4j.Slf4j;
import ocpp.cp._2012._06.AvailabilityStatus;
import ocpp.cp._2012._06.CancelReservationRequest;
import ocpp.cp._2012._06.CancelReservationResponse;
import ocpp.cp._2012._06.CancelReservationStatus;
import ocpp.cp._2012._06.ChangeAvailabilityRequest;
import ocpp.cp._2012._06.ChangeAvailabilityResponse;
import ocpp.cp._2012._06.ReservationStatus;
import ocpp.cp._2012._06.ReserveNowRequest;
import ocpp.cp._2012._06.ReserveNowResponse;
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
public class PlugSurfingOcpp15Dispatcher extends ChargePointService15_Dispatcher {

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
                            cp.getChargeBoxId());
                } else {
                    plugSurfingService.postConnectorStatus(
                            ConnectorStatusConverter.getConnectorStatus(request.getType()),
                            cp.getChargeBoxId(),
                            request.getConnectorId());
                }
            }

            @Override
            public void failed(String errorMessage) {
                //Do nothing
            }
        };

        handler.addCallback(callback);
        super.changeAvailability(cp, handler);
    }

    @Override
    public void reserveNow(ChargePointSelect cp,
                           OcppResponseHandler<ReserveNowRequest, ReserveNowResponse> handler) {

        OcppCallback<ReserveNowResponse> callback = new OcppCallback<ReserveNowResponse>() {
            @Override
            public void success(ReserveNowResponse response) {
                if (response.getStatus() != ReservationStatus.ACCEPTED) {
                    return;
                }

                plugSurfingService.postConnectorStatus(
                        ConnectorStatus.Reserved,
                        cp.getChargeBoxId(),
                        handler.getRequest().getConnectorId()
                );
            }

            @Override
            public void failed(String errorMessage) {
                //Do nothing
            }
        };

        handler.addCallback(callback);
        super.reserveNow(cp, handler);
    }

    @Override
    public void cancelReservation(ChargePointSelect cp,
                                  OcppResponseHandler<CancelReservationRequest, CancelReservationResponse> handler) {

        OcppCallback<CancelReservationResponse> callback = new OcppCallback<CancelReservationResponse>() {
            @Override
            public void success(CancelReservationResponse response) {
                if (response.getStatus() != CancelReservationStatus.ACCEPTED) {
                    return;
                }

                //This has additional BL, that is why through PlugSurfingService
                plugSurfingService.postCancelReservation(
                        handler.getRequest().getReservationId(),
                        cp.getChargeBoxId());
            }

            @Override
            public void failed(String errorMessage) {
                //Do nothing
            }
        };

        handler.addCallback(callback);
        super.cancelReservation(cp, handler);
    }
}
