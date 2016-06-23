package de.rwth.idsg.steve.extensions.plugsurfing.repository;

import com.google.common.base.Optional;
import jooq.steve.db.tables.records.TransactionRecord;
import org.joda.time.DateTime;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 15.01.2016
 */
public interface SessionRepository {
    String addSessionWithoutTransactionPK(int connectorPK, int ocppTagPK);

    String addSessionWithoutTransactionPK(int connectorPK, int ocppTagPK, DateTime eventTimestamp);

    void updateSession(int connectorPK, int ocppTagPk, int transactionPk);

    boolean expireSession(int sessionId);

    Optional<Integer> getTransactionPkFromSessionId(int sessionId);

    String getOcppTagOfActiveTransaction(int transactionPK);

    Optional<Integer> getSessionPkFromTransactionPk(int transactionPK);

    TransactionRecord getTransaction(int transactionPK);

    boolean hasNoSession(String rfid);

    boolean checkConnectorPk(int connectorPK, int sessionId);
}
