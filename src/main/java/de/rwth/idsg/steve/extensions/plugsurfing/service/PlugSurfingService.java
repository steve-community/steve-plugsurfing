package de.rwth.idsg.steve.extensions.plugsurfing.service;

import com.google.common.base.Optional;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.CompleteStationInfo;
import de.rwth.idsg.steve.extensions.plugsurfing.model.SuccessResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.ConnectorStatus;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.IdentifierType;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.Station;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.TimePeriod;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.User;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.ConnectorPostStatus;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.RfidVerify;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.SessionPost;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.StationPost;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.response.RfidVerifyResponse;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.ConnectorRepositroy;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.OcppExternalTagRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.SessionRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.StationRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.rest.Client;
import de.rwth.idsg.steve.extensions.plugsurfing.utils.StationUtils;
import jooq.steve.db.tables.records.TransactionRecord;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 19.02.2016
 */
@Service
public class PlugSurfingService {

    @Autowired private SessionRepository sessionRepository;
    @Autowired private ConnectorRepositroy connectorRepositroy;
    @Autowired private OcppExternalTagRepository ocppExternalTagRepository;
    @Autowired private StationRepository stationRepository;
    @Autowired private Client restClient;
    @Autowired private ScheduledExecutorService executorService;
    @Autowired private EvcoIdService evcoIdService;

    public void asyncHandleStatusNotification(String chargeBoxId, int connectorId, ConnectorStatus cs) {
        executorService.execute(() -> handleStatusNotification(chargeBoxId, connectorId, cs));
    }

    public void asyncUpdateSession(int transactionPk, String chargeBoxId, int connectorId, String rfid) {
        executorService.execute(() -> updateSession(transactionPk, chargeBoxId, connectorId, rfid));
    }

    public void asyncPostSession(int transactionPK, String rfid) {
        executorService.execute(() -> postSessionStop(transactionPK, rfid));
    }

    public void asyncPostStationStatus(String chargeboxId, ConnectorStatus status) {
        executorService.execute(() -> postStationStatus(chargeboxId, status));
    }

    public void postCompleteStationDataWithStatus(String chargeBoxId) {
        ShouldPostDTO dto = getShouldPost(chargeBoxId);

        if (dto.shouldPost()) {
            postStationInternal(chargeBoxId, dto.getDiscoveredConns());
            postConnectorStatusForAll(chargeBoxId);
        }
    }

    public void postStationStatus(String chargeboxId, ConnectorStatus status) {
        List<Integer> connPkList = connectorRepositroy.getDiscoveredConnPks(chargeboxId);
        for(Integer connPk : connPkList) {
            ConnectorPostStatus cps = StationUtils.buildConnectorPostStatus(connPk, status);
            restClient.connectorPostStatus(cps);
        }
    }

    /**
     * PS API does not provide actually deleting charging stations. For that reason, we must set
     * the connectors of a chargeBox to OFFLINE, when the user deletes a chargeBox.
     */
    public void postConnectorStatusOffline(int chargeBoxPk) {
        List<Integer> discoveredConns = connectorRepositroy.getDiscoveredConnPks(chargeBoxPk);
        for (Integer conn : discoveredConns) {
            ConnectorPostStatus cps = StationUtils.buildConnectorPostStatus(conn, ConnectorStatus.Offline);
            restClient.connectorPostStatus(cps);
        }
    }

    public void verifyRfid(String rfid) {
        boolean shouldAskPs = ocppExternalTagRepository.isExternalOrUnknown(rfid);

        if (shouldAskPs) {
            boolean verified = askPs(rfid);
            if (verified) {
                ocppExternalTagRepository.addOrIgnoreIfPresent(rfid);
                ocppExternalTagRepository.unblock(rfid);
            } else {
                ocppExternalTagRepository.block(rfid);
            }
        }
    }

    /**
     * Posting the availability of the Charge Point OCPP12, since it is always possible to be
     * triggered manually, by the user. In that case, we need to make sure that the information
     * is sent to PlugSurfing
     */
    public void postChangeAvailability(ConnectorStatus connectorStatus, String chargeBoxId) {
        List<ConnectorPostStatus> actualStatuses = connectorRepositroy.getChargePointConnectorStatus(chargeBoxId);
        if (actualStatuses == null || actualStatuses.isEmpty()) {
            //Not a PS Station, exit
            return;
        }
        //Change the status for each connector
        for (ConnectorPostStatus s : actualStatuses) {
            s.setStatus(connectorStatus);
            restClient.connectorPostStatus(s);
        }
    }

    /**
     * Post the connector status as Available to PlugSurfing
     */
    public void postCancelReservation(int reservationId, String chargeBoxId) {
        int connectorId = connectorRepositroy.getConnectorIdFromReservation(reservationId);
        postConnectorStatus(
                ConnectorStatus.Available,
                chargeBoxId,
                connectorId
        );
    }

    public void postConnectorStatus(ConnectorStatus status, String chargeBoxId, int connectorId) {
        Optional<Integer> integerOptional = connectorRepositroy.getConnectorPk(chargeBoxId, connectorId);
        if (!integerOptional.isPresent()) {
            // Not a PS station, no further processing.
            return;
        }

        ConnectorPostStatus cps = StationUtils.buildConnectorPostStatus(integerOptional.get(), status);
        restClient.connectorPostStatus(cps);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean askPs(String rfid) {
        try {
            RfidVerifyResponse responsePS = restClient.rfidVerify(getRfidVerifyRequest(rfid));
            return responsePS.getVerified();
        } catch (Exception e) {
            return false;
        }
    }

    private RfidVerify getRfidVerifyRequest(String rfid) {
        RfidVerify r = new RfidVerify();
        r.setRfid(rfid);
        return r;
    }

    private void handleStatusNotification(String chargeBoxId, int connectorId, ConnectorStatus cs) {
        if (stationRepository.isPosted(chargeBoxId)) {
            postConnectorStatus(cs, chargeBoxId, connectorId);
        } else {
            postCompleteStationDataWithStatus(chargeBoxId);
        }
    }

    private ShouldPostDTO getShouldPost(String chargeBoxId) {
        Optional<Integer> optionalConnNumber = stationRepository.getConnectorsNumber(chargeBoxId);

        // If not present, this is not a PS chargebox. Early exit.
        if (!optionalConnNumber.isPresent()) {
            return new ShouldPostDTO();
        }

        List<Integer> discoveredConns = connectorRepositroy.getDiscoveredConnPks(chargeBoxId);

        return new ShouldPostDTO(discoveredConns, optionalConnNumber);
    }

    /**
     * Initial post connector status after a station post
     */
    private void postConnectorStatusForAll(String chargeBoxId) {
        List<ConnectorPostStatus> statusList = connectorRepositroy.getChargePointConnectorStatus(chargeBoxId);

        for (ConnectorPostStatus s : statusList) {
            restClient.connectorPostStatus(s);
        }
    }

    private void updateSession(int transactionPk, String chargeBoxId, int connectorId, String rfid) {
        if (ocppExternalTagRepository.isLocal(rfid)) {
            return;
        }

        Optional<Integer> integerOptional = connectorRepositroy.getConnectorPk(chargeBoxId, connectorId);
        if (!integerOptional.isPresent()) {
            // Not a PS station, no further processing.
            return;
        }

        int connectorPk = integerOptional.get();
        int ocppTagPK = ocppExternalTagRepository.getOcppTagPkForRfid(rfid).get();

        // Normally, the session is started from PS by sending us a session-start.
        // But a roaming user can also directly go to the charging station and want to use it.
        // In this case, we do not have a session yet. So additional measures need to be taken.
        if (sessionRepository.hasNoSession(rfid)) {
            sessionRepository.addSessionWithoutTransactionPK(connectorPk, ocppTagPK);
            ocppExternalTagRepository.setInSessionTrue(rfid);
        }

        //Obtain the sessionId, so we could register the transactionPk in the external table for session
        //Since we don't have any info about the transactions in the session table, obtain it from connectorPk+ocppTagPk
        sessionRepository.updateSession(connectorPk, ocppTagPK, transactionPk);

        //Post to PlugSurfing the information about the session
        postSessionStart(transactionPk, rfid);
    }

    private void postSessionStart(int transactionPK, String rfid) {
        Optional<Integer> sessionIdResponse = sessionRepository.getSessionPkFromTransactionPk(transactionPK);

        // if this is not present, it's not related to roaming / plugsurfing
        if (sessionIdResponse.isPresent()) {
            callClient(sessionIdResponse.get(), transactionPK, rfid, false);
        }
    }

    private void postSessionStop(int transactionPK, String rfid) {
        Optional<Integer> sessionIdResponse = sessionRepository.getSessionPkFromTransactionPk(transactionPK);

        // if this is not present, it's not related to roaming / plugsurfing
        if (sessionIdResponse.isPresent()) {
            callClient(sessionIdResponse.get(), transactionPK, rfid, true);
            ocppExternalTagRepository.setInSessionFalse(rfid);
        }
    }

    private void callClient(int sessionId, int transactionPK, String rfid, boolean isStop) {
        int connectorPK = connectorRepositroy.getConnectorPkFromTransactionPk(transactionPK);

        User user = buildUserObject(rfid);

        // Set Session interval
        TransactionRecord transaction = sessionRepository.getTransaction(transactionPK);

        TimePeriod sessionTimePeriod = new TimePeriod();
        sessionTimePeriod.setStart(transaction.getStartTimestamp());

        SessionPost sessionRequest = new SessionPost();
        sessionRequest.setSessionId(String.valueOf(sessionId));

        if (isStop) {
            sessionRequest.setEnergyConsumed(StationUtils.calculateEnergy(transaction));
            sessionTimePeriod.setStop(transaction.getStopTimestamp());
        }

        // make sure: connector pk
        sessionRequest.setConnectorPrimaryKey(String.valueOf(connectorPK));

        sessionRequest.setUser(user);
        sessionRequest.setSessionInterval(sessionTimePeriod);

        restClient.sessionPost(sessionRequest);
    }

    public void postExpiredSession(int sessionId, int connectorPK, String rfid, DateTime start) {

        User user = buildUserObject(rfid);

        TimePeriod sessionTimePeriod = new TimePeriod();
        sessionTimePeriod.setStart(start);
        sessionTimePeriod.setStop(DateTime.now());

        SessionPost sessionRequest = new SessionPost();
        sessionRequest.setSessionId(String.valueOf(sessionId));
        sessionRequest.setEnergyConsumed(0d);
        sessionRequest.setUser(user);
        sessionRequest.setSessionInterval(sessionTimePeriod);

        // make sure: connector pk
        sessionRequest.setConnectorPrimaryKey(String.valueOf(connectorPK));

        restClient.sessionPost(sessionRequest);
    }

    private User buildUserObject(String rfid) {
        User user = new User();

        Optional<String> optionalEvcoId = evcoIdService.getEvcoId(rfid);
        if (optionalEvcoId.isPresent()) {
            user.setIdentifierType(IdentifierType.EVCO_ID);
            user.setIdentifier(optionalEvcoId.get());
        } else {
            user.setIdentifierType(IdentifierType.RFID);
            user.setIdentifier(rfid);
        }

        return user;
    }

    private void postStationInternal(String chargeBoxIdentity, List<Integer> discoveredConns) {
        CompleteStationInfo info = stationRepository.getForStationPost(chargeBoxIdentity);

        Station station = StationUtils.buildStation(info, discoveredConns);
        StationPost post = StationUtils.buildStationPost(station);

        SuccessResponse response = restClient.stationPost(post);
        if (response.getSuccess()) {
            stationRepository.setPosted(info.getChargeBox().getChargeBoxPk());
        }
    }

    /**
     * Simple wrapper to transfer more than one element between methods
     * (i.e. a method needs to return more than one element)
     */
    private static class ShouldPostDTO {
        private final List<Integer> discoveredConns;
        private final boolean shouldPost;

        private ShouldPostDTO(List<Integer> discoveredConns, Optional<Integer> optionalConnNumber) {
            this.discoveredConns = discoveredConns;
            this.shouldPost = optionalConnNumber.get() == discoveredConns.size();
        }

        private ShouldPostDTO() {
            shouldPost = false;
            discoveredConns = Collections.emptyList();
        }

        private boolean shouldPost() {
            return shouldPost;
        }

        private List<Integer> getDiscoveredConns() {
            return discoveredConns;
        }
    }

}
