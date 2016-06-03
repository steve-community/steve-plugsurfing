package de.rwth.idsg.steve.extensions.plugsurfing.service;

import de.rwth.idsg.steve.extensions.plugsurfing.Constants;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.ExternalChargePointSelect;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.SessionStartResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.SessionStopResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.OcppExternalTagRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.SessionRepository;
import de.rwth.idsg.steve.handler.OcppCallback;
import de.rwth.idsg.steve.handler.ocpp12.RemoteStartTransactionResponseHandler;
import de.rwth.idsg.steve.handler.ocpp12.RemoteStopTransactionResponseHandler;
import de.rwth.idsg.steve.ocpp.OcppVersion;
import de.rwth.idsg.steve.repository.RequestTaskStore;
import de.rwth.idsg.steve.service.ChargePointService12_Dispatcher;
import de.rwth.idsg.steve.web.dto.task.ExternalRequestTask;
import lombok.extern.slf4j.Slf4j;
import ocpp.cp._2010._08.RemoteStartStopStatus;
import ocpp.cp._2010._08.RemoteStartTransactionRequest;
import ocpp.cp._2010._08.RemoteStartTransactionResponse;
import ocpp.cp._2010._08.RemoteStopTransactionRequest;
import ocpp.cp._2010._08.RemoteStopTransactionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 22.01.2016
 */
@Slf4j
@Service
public class PlugSurfingOcpp12Mediator {
    private static final OcppVersion VERSION = OcppVersion.V_12;

    @Autowired private RequestTaskStore requestTaskStore;
    @Autowired private ChargePointService12_Dispatcher dispatcher12;
    @Autowired private OcppExternalTagRepository ocppRepository;
    @Autowired private SessionRepository sessionRepository;

    public void processStartTransaction(String rfid,
                                        ExternalChargePointSelect selectInfo,
                                        DeferredResult<ResponseEntity<SessionStartResponse>> response) {

        OcppCallback<RemoteStartTransactionResponse> callback = new OcppCallback<RemoteStartTransactionResponse>() {

            @Override
            public void success(RemoteStartTransactionResponse response) {
                RemoteStartStopStatus status = response.getStatus();

                SessionStartResponse ack = new SessionStartResponse();

                //Respond with Http PlugSurfing Codes
                switch (status) {
                    case ACCEPTED:
                        //mark as external in DB
                        int ocppTagPk = ocppRepository.addOrIgnoreIfPresent(rfid);
                        ocppRepository.setInSessionTrue(rfid);
                        String sessionId = sessionRepository.addSessionWithoutTransactionPK(selectInfo.getConnectorPk(), ocppTagPk);
                        // Fill the values
                        ack.setIsStoppable(true);
                        ack.setSuccess(true);
                        ack.setSessionId(sessionId);
                        break;

                    case REJECTED:
                    default:
                        // Fill the values
                        ack.setSuccess(false);
                        ack.setIsStoppable(false);
                        break;
                }
                finish(ack);
            }

            @Override
            public void failed(String errorMessage) {
                SessionStartResponse ack = new SessionStartResponse();
                ack.setSuccess(false);
                finish(ack);
            }

            private void finish(SessionStartResponse ack) {
                ResponseEntity<SessionStartResponse> responseEntity =
                        new ResponseEntity<>(ack, HttpStatus.OK);
                response.setResult(responseEntity);
            }
        };

        RemoteStartTransactionRequest req = new RemoteStartTransactionRequest()
                .withIdTag(rfid)
                .withConnectorId(selectInfo.getConnectorId());

        ExternalRequestTask<RemoteStartTransactionRequest> task = ExternalRequestTask.builder(req)
                .ocppVersion(VERSION)
                .partnerName(Constants.CONFIG.getVendorName())
                .chargePoint(selectInfo.getSelect())
                .build();

        String cbId = selectInfo.getSelect().getChargeBoxId();
        RemoteStartTransactionResponseHandler handler = new RemoteStartTransactionResponseHandler(task, cbId);
        handler.addCallback(callback);

        requestTaskStore.add(task);
        dispatcher12.remoteStartTransaction(selectInfo.getSelect(), handler);

    }


    public void processStopTransaction(int transactionPk,
                                       ExternalChargePointSelect selectInfo,
                                       DeferredResult<ResponseEntity<SessionStopResponse>> response) {

        OcppCallback<RemoteStopTransactionResponse> callback = new OcppCallback<RemoteStopTransactionResponse>() {

            @Override
            public void success(RemoteStopTransactionResponse response) {
                RemoteStartStopStatus status = response.getStatus();
                SessionStopResponse ack = new SessionStopResponse();
                switch (status) {
                    case ACCEPTED:
                        ack.setSuccess(true);
                        break;
                    case REJECTED:
                    default:
                        ack.setSuccess(false);
                        break;
                }
                finish(ack);
            }

            @Override
            public void failed(String errorMessage) {
                SessionStopResponse ack = new SessionStopResponse();
                ack.setSuccess(false);
                finish(ack);
            }

            private void finish(SessionStopResponse ack) {
                ResponseEntity<SessionStopResponse> responseEntity =
                        new ResponseEntity<>(ack, HttpStatus.OK);
                response.setResult(responseEntity);
            }
        };

        RemoteStopTransactionRequest req = new RemoteStopTransactionRequest()
                .withTransactionId(transactionPk);

        ExternalRequestTask<RemoteStopTransactionRequest> task = ExternalRequestTask.builder(req)
                .ocppVersion(VERSION)
                .partnerName(Constants.CONFIG.getVendorName())
                .chargePoint(selectInfo.getSelect())
                .build();

        String cbId = selectInfo.getSelect().getChargeBoxId();
        RemoteStopTransactionResponseHandler handler = new RemoteStopTransactionResponseHandler(task, cbId);
        handler.addCallback(callback);

        requestTaskStore.add(task);
        dispatcher12.remoteStopTransaction(selectInfo.getSelect(), handler);

    }
}
