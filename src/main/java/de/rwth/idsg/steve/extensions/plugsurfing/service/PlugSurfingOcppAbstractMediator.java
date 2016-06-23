package de.rwth.idsg.steve.extensions.plugsurfing.service;

import de.rwth.idsg.steve.extensions.plugsurfing.dto.ExternalChargePointSelect;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.OcppExternalTagRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.SessionRepository;
import de.rwth.idsg.steve.repository.RequestTaskStore;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 23.06.2016
 */
public abstract class PlugSurfingOcppAbstractMediator {

    @Autowired protected RequestTaskStore requestTaskStore;
    @Autowired protected ScheduledExecutorService executorService;

    @Autowired private OcppExternalTagRepository ocppRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private SessionExpireService sessionExpireService;

    String handleAcceptedStartTransaction(String rfid, ExternalChargePointSelect selectInfo) {
        // mark as external in DB
        int ocppTagPk = ocppRepository.addOrIgnoreIfPresent(rfid);
        
        ocppRepository.setInSessionTrue(rfid);
        DateTime start = DateTime.now();
        String sessionId = sessionRepository.addSessionWithoutTransactionPK(selectInfo.getConnectorPk(), ocppTagPk, start);
        sessionExpireService.registerCheck(rfid, sessionId, selectInfo.getConnectorPk(), start);
        return sessionId;
    }

}
