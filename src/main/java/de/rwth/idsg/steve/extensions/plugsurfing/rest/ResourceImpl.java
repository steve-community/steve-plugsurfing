package de.rwth.idsg.steve.extensions.plugsurfing.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.extensions.plugsurfing.PsApiException;
import de.rwth.idsg.steve.extensions.plugsurfing.PsApiJsonParser;
import de.rwth.idsg.steve.extensions.plugsurfing.PsApiOperation;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.ExternalChargePointSelect;
import de.rwth.idsg.steve.extensions.plugsurfing.model.ErrorResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.receive.request.SessionStart;
import de.rwth.idsg.steve.extensions.plugsurfing.model.receive.request.SessionStop;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.SessionStartResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.SessionStopResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.OcppExternalTagRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.SessionRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.StationRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.service.PlugSurfingOcpp12Mediator;
import de.rwth.idsg.steve.extensions.plugsurfing.service.PlugSurfingOcpp15Mediator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Slf4j
@RestController
public class ResourceImpl implements Resource {

    @Autowired private StationRepository stationRepository;
    @Autowired private PlugSurfingOcpp12Mediator ocpp12Mediator;
    @Autowired private PlugSurfingOcpp15Mediator ocpp15Mediator;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private OcppExternalTagRepository ocppExternalTagRepository;
    @Autowired private Validator validator;

    private static final Joiner JOINER = Joiner.on(", ");

    @Override
    @RequestMapping(
            value = "/ps-api",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.POST
    )
    public DeferredResult<?> dispatch(HttpServletRequest request) {
        try {
            String messageBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

            try (JsonParser parser = PsApiJsonParser.SINGLETON.getMapper().getFactory().createParser(messageBody)) {
                parser.nextToken();
                parser.nextToken();

                String operationName = parser.getCurrentName();
                PsApiOperation operation = PsApiOperation.fromValue(operationName);

                return dispatchInternal(operation, messageBody);
            }
        } catch (IOException e) {
            throw new PsApiException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private DeferredResult<?> dispatchInternal(PsApiOperation operation, String messageBody) throws IOException {
        switch (operation) {
            case SESSION_START:
                SessionStart start = PsApiJsonParser.SINGLETON.deserialize(messageBody, operation.getObjectClazz());
                return sessionStart(start);

            case SESSION_STOP:
                SessionStop stop = PsApiJsonParser.SINGLETON.deserialize(messageBody, operation.getObjectClazz());
                return sessionStop(stop);

            default:
                throw new PsApiException("Unknown operation", HttpStatus.BAD_REQUEST);
        }
    }

    private DeferredResult<ResponseEntity<SessionStartResponse>> sessionStart(SessionStart request) {

        validate(request);

        String rfid = request.getUser().getIdentifier();
        boolean hasOngoingSession = !sessionRepository.hasNoSession(rfid);

        if (hasOngoingSession) {
            throw new PsApiException("User already in session", HttpStatus.BAD_REQUEST);
        }

        int connectorPK = Integer.valueOf(request.getConnectorPrimaryKey());
        ExternalChargePointSelect selectInfo = getExternalChargePointSelect(connectorPK);

        final DeferredResult<ResponseEntity<SessionStartResponse>> response = new DeferredResult<>();
        switch (selectInfo.getVersion()) {
            case V_12:
                ocpp12Mediator.processStartTransaction(rfid, selectInfo, response);
                break;
            case V_15:
                ocpp15Mediator.processStartTransaction(rfid, selectInfo, response);
                break;
        }
        return response;
    }

    private DeferredResult<ResponseEntity<SessionStopResponse>> sessionStop(SessionStop request) {

        validate(request);

        String rfid = request.getUser().getIdentifier();
        boolean isNotExternal = !ocppExternalTagRepository.isExternal(rfid);

        if (isNotExternal) {
            throw new PsApiException("No such rfid", HttpStatus.UNAUTHORIZED);
        }

        int connectorPK = Integer.valueOf(request.getConnectorPrimaryKey());
        ExternalChargePointSelect selectInfo = getExternalChargePointSelect(connectorPK);

        int sessionId = Integer.valueOf(request.getSessionId());
        Optional<Integer> transactionPKRecord = sessionRepository.getTransactionPkFromSessionId(sessionId);

        if (!transactionPKRecord.isPresent()) {
            throw new PsApiException("No transaction for the given session", HttpStatus.BAD_REQUEST);
        }

        int transactionPk = transactionPKRecord.get();
        boolean isSameRfid = rfid.equals(sessionRepository.getOcppTagOfActiveTransaction(transactionPk));

        if (!isSameRfid) {
            throw new PsApiException("Wrong rfid", HttpStatus.UNAUTHORIZED);
        }

        //Check if the connectorPk from request and from the session are matching
        boolean isSameConnPk = sessionRepository.checkConnectorPk(connectorPK, sessionId);
        if (!isSameConnPk) {
            throw new PsApiException("Connector identifier doesn't match the given session", HttpStatus.UNAUTHORIZED);
        }

        final DeferredResult<ResponseEntity<SessionStopResponse>> response = new DeferredResult<>();
        switch (selectInfo.getVersion()) {
            case V_12:
                ocpp12Mediator.processStopTransaction(transactionPk, selectInfo, response);
                break;
            case V_15:
                ocpp15Mediator.processStopTransaction(transactionPk, selectInfo, response);
                break;
        }
        return response;
    }

    // -------------------------------------------------------------------------
    // Controller advice stuff
    // -------------------------------------------------------------------------

    @ExceptionHandler(PsApiException.class)
    public ResponseEntity<ErrorResponse> handlePsApiError(PsApiException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), e.getResponseStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleError(Exception e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private <T> void validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            List<String> errorMessages = new ArrayList<>(violations.size());
            for (ConstraintViolation<T> v : violations) {
                errorMessages.add(v.getMessage());
            }
            String errors = JOINER.join(errorMessages);
            String msg = "Validation failed: " + errors;
            throw new PsApiException(msg, HttpStatus.BAD_REQUEST);
        }
    }

    private ExternalChargePointSelect getExternalChargePointSelect(int connectorPK) {
        try {
            return stationRepository.getStationFromConnector(connectorPK);

        } catch (SteveException ex) {
            // EVSE Not Found
            throw new PsApiException("EVSE not found", HttpStatus.NOT_FOUND);

        } catch (RuntimeException ex) {
            // ChargeBox Information missing
            throw new PsApiException("ChargeBox information missing", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
