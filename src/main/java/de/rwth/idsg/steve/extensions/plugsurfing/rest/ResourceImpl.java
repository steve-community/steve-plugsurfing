package de.rwth.idsg.steve.extensions.plugsurfing.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.extensions.plugsurfing.AsyncHttpWrapper;
import de.rwth.idsg.steve.extensions.plugsurfing.PsApiException;
import de.rwth.idsg.steve.extensions.plugsurfing.PsApiJsonParser;
import de.rwth.idsg.steve.extensions.plugsurfing.PsApiOperation;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.ExternalChargePointSelect;
import de.rwth.idsg.steve.extensions.plugsurfing.model.ErrorResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.User;
import de.rwth.idsg.steve.extensions.plugsurfing.model.receive.request.SessionStart;
import de.rwth.idsg.steve.extensions.plugsurfing.model.receive.request.SessionStop;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.OcppExternalTagRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.SessionRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.StationRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.impl.SessionRepositoryImpl;
import de.rwth.idsg.steve.extensions.plugsurfing.service.EvcoIdService;
import de.rwth.idsg.steve.extensions.plugsurfing.service.PlugSurfingOcpp12Mediator;
import de.rwth.idsg.steve.extensions.plugsurfing.service.PlugSurfingOcpp15Mediator;
import jooq.steve.db.tables.records.PsSessionRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

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
    @Autowired private EvcoIdService evcoIdService;
    @Autowired private Validator validator;

    private static final Joiner JOINER = Joiner.on(", ");
    private final AtomicLong counter = new AtomicLong(0);

    @Override
    @RequestMapping(
            value = "/ps-api",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.POST
    )
    public void dispatch(HttpServletRequest request, HttpServletResponse response) {
        AsyncHttpWrapper wrapper = new AsyncHttpWrapper(request, response, counter.incrementAndGet());

        try {
            String messageBody = wrapper.parseRequestBody();

            try (JsonParser parser = PsApiJsonParser.SINGLETON.getMapper().getFactory().createParser(messageBody)) {
                parser.nextToken();
                parser.nextToken();

                String operationName = parser.getCurrentName();
                PsApiOperation operation = PsApiOperation.fromValue(operationName);

                dispatchInternal(wrapper, operation, messageBody);
            }
        } catch (Throwable e) {
            log.error("Error occurred", e);
            try {
                wrapper.finishExceptionally(e);
            } catch (Throwable t) {
                log.error("Error occurred", t);
            }
        }
    }

    private void dispatchInternal(AsyncHttpWrapper wrapper, PsApiOperation operation, String messageBody)
            throws IOException {
        switch (operation) {
            case SESSION_START:
                SessionStart start = PsApiJsonParser.SINGLETON.deserialize(messageBody, operation.getObjectClazz());
                sessionStart(wrapper, start);
                break;

            case SESSION_STOP:
                SessionStop stop = PsApiJsonParser.SINGLETON.deserialize(messageBody, operation.getObjectClazz());
                sessionStop(wrapper, stop);
                break;

            default:
                throw new PsApiException("Unknown operation", HttpStatus.BAD_REQUEST);
        }
    }

    private void sessionStart(AsyncHttpWrapper wrapper, SessionStart request) {

        validate(request);

        String rfid = getRfid(request.getUser());
        boolean hasOngoingSession = !sessionRepository.hasNoSession(rfid);

        if (hasOngoingSession) {
            throw new PsApiException("User already in session", HttpStatus.BAD_REQUEST);
        }

        int connectorPK = Integer.valueOf(request.getConnectorPrimaryKey());
        ExternalChargePointSelect selectInfo = getExternalChargePointSelect(connectorPK);

        switch (selectInfo.getVersion()) {
            case V_12:
                ocpp12Mediator.processStartTransaction(rfid, selectInfo, wrapper);
                break;
            case V_15:
                ocpp15Mediator.processStartTransaction(rfid, selectInfo, wrapper);
                break;
        }
    }

    private void sessionStop(AsyncHttpWrapper wrapper, SessionStop request) {

        validate(request);

        int sessionId = Integer.valueOf(request.getSessionId());
        Optional<PsSessionRecord> optionalRecord = sessionRepository.getSessionRecord(sessionId);
        if (!optionalRecord.isPresent()) {
            throw new PsApiException("No such session", HttpStatus.BAD_REQUEST);
        }

        PsSessionRecord sessionRecord = optionalRecord.get();
        if (SessionRepositoryImpl.SessionStatus.EXPIRED.name().equals(sessionRecord.getEventStatus())) {
            throw new PsApiException("Session already expired/closed", HttpStatus.BAD_REQUEST);
        }

        Integer transactionPk = sessionRecord.getTransactionPk();
        if (transactionPk == null) {
            throw new PsApiException("No transaction for the given session", HttpStatus.BAD_REQUEST);
        }

        // Check if the connectorPk from request and from the session are matching
        int connectorPkFromRequest = Integer.valueOf(request.getConnectorPrimaryKey());
        int connectorPkFromDatabase = sessionRecord.getConnectorPk();
        boolean isSameConnPk = connectorPkFromRequest == connectorPkFromDatabase;
        if (!isSameConnPk) {
            throw new PsApiException("Connector identifier doesn't match the given session", HttpStatus.UNAUTHORIZED);
        }

        String rfid = getRfid(request.getUser());
        boolean isNotExternal = !ocppExternalTagRepository.isExternal(rfid);
        if (isNotExternal) {
            throw new PsApiException("No such rfid", HttpStatus.UNAUTHORIZED);
        }

        boolean isSameRfid = rfid.equals(sessionRepository.getOcppTagOfActiveTransaction(transactionPk));
        if (!isSameRfid) {
            throw new PsApiException("No active transactions found for given rfid", HttpStatus.UNAUTHORIZED);
        }

        ExternalChargePointSelect selectInfo = getExternalChargePointSelect(connectorPkFromRequest);
        switch (selectInfo.getVersion()) {
            case V_12:
                ocpp12Mediator.processStopTransaction(transactionPk, selectInfo, wrapper);
                break;
            case V_15:
                ocpp15Mediator.processStopTransaction(transactionPk, selectInfo, wrapper);
                break;
        }
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

    private String getRfid(User user) {
        switch (user.getIdentifierType()) {
            case EVCO_ID:
                return evcoIdService.getOcppIdTag(user.getIdentifier());

            case RFID:
                return user.getIdentifier();

            default:
                // Cannot happen
                throw new PsApiException("Unknown identifier-type", HttpStatus.BAD_REQUEST);
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
