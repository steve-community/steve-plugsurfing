package de.rwth.idsg.steve.extensions.plugsurfing.service;

import de.rwth.idsg.steve.extensions.plugsurfing.repository.OcppExternalTagRepository;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * We should identify PS sessions, that have been started but not used (cable not plugged in i.e. no StartTransaction
 * arrived) and expire them and send an empty SessionPost to PS.
 *
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 23.06.2016
 */
@Slf4j
@Service
public class SessionExpireService {

    @Autowired private ScheduledExecutorService executorService;
    @Autowired private PlugSurfingService plugSurfingService;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private OcppExternalTagRepository ocppRepository;

    // Key : session id
    private final ConcurrentHashMap<Integer, ScheduledFuture> lookupTable = new ConcurrentHashMap<>();

    // When to check after a session-start?
    private static final int BUFFER_IN_SECONDS = 30;

    /**
     * We have to cancel scheduled jobs, otherwise JVM does not shut down!
     */
    @PreDestroy
    public void destroy() {
        for (ScheduledFuture sf : lookupTable.values()) {
            sf.cancel(true);
        }
    }

    public void registerCheck(String rfid, String sessionId, int connectorPk, DateTime start) {
        registerCheckInternal(rfid, Integer.valueOf(sessionId), connectorPk, start);
    }

    private void registerCheckInternal(String rfid, int sessionId, int connectorPk, DateTime start) {
        Runnable r = () -> {
            lookupTable.remove(sessionId);
            boolean success = sessionRepository.expireSession(sessionId);
            if (success) {
                ocppRepository.setInSessionFalse(rfid);
                plugSurfingService.postExpiredSession(sessionId, connectorPk, rfid, start);
            }
        };

        ScheduledFuture sf = executorService.schedule(r, BUFFER_IN_SECONDS, TimeUnit.SECONDS);
        lookupTable.put(sessionId, sf);
    }

}
