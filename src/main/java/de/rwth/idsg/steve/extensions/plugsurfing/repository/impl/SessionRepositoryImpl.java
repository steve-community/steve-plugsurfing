package de.rwth.idsg.steve.extensions.plugsurfing.repository.impl;

import com.google.common.base.Optional;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.SessionRepository;
import jooq.steve.db.tables.records.TransactionRecord;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static jooq.steve.db.tables.OcppTag.OCPP_TAG;
import static jooq.steve.db.tables.PsSession.PS_SESSION;
import static jooq.steve.db.tables.Transaction.TRANSACTION;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 15.01.2016
 */
@Slf4j
@Repository
public class SessionRepositoryImpl implements SessionRepository {

    @Autowired private DSLContext ctx;

    private enum SessionStatus {
        // when session was started and used. default starting point.
        ACTIVE,
        // when session was started but not used (cable not plugged in i.e. no StartTransaction arrived).
        // status is changed from active to expired after a waiting period.
        // see de.rwth.idsg.steve.extensions.plugsurfing.service.SessionExpireService
        EXPIRED
    }

    @Override
    public String addSessionWithoutTransactionPK(int connectorPK, int ocppTagPK) {
        return addSessionWithoutTransactionPK(connectorPK, ocppTagPK, DateTime.now());
    }

    @Override
    public String addSessionWithoutTransactionPK(int connectorPK, int ocppTagPK, DateTime eventTimestamp) {
        int pk = ctx.insertInto(PS_SESSION)
                    .set(PS_SESSION.CONNECTOR_PK, connectorPK)
                    .set(PS_SESSION.OCPP_TAG_PK, ocppTagPK)
                    .set(PS_SESSION.EVENT_TIMESTAMP, eventTimestamp)
                    .set(PS_SESSION.EVENT_STATUS, SessionStatus.ACTIVE.name())
                    .returning(PS_SESSION.PS_SESSION_PK)
                    .fetchOne()
                    .getPsSessionPk();

        // Business logic: PS implementation needs this value as string
        return String.valueOf(pk);
    }

    @Override
    public void updateSession(int connectorPK, int ocppTagPk, int transactionPk) {
        ctx.update(PS_SESSION)
           .set(PS_SESSION.TRANSACTION_PK, transactionPk)
           .where(PS_SESSION.OCPP_TAG_PK.eq(ocppTagPk))
           .and(PS_SESSION.CONNECTOR_PK.eq(connectorPK))
           .and(PS_SESSION.TRANSACTION_PK.isNull())
           .execute();
    }

    @Override
    public boolean expireSession(int sessionId) {
        int count = ctx.update(PS_SESSION)
                       .set(PS_SESSION.EVENT_STATUS, SessionStatus.EXPIRED.name())
                       .where(PS_SESSION.PS_SESSION_PK.eq(sessionId))
                       .and(PS_SESSION.TRANSACTION_PK.isNull())
                       .execute();

        if (count == 0) {
            return false;

        } else if (count == 1) {
            return true;

        } else {
            log.warn(
                    "Multiple session records were found and have been expired for session id '{}'! This is not good.",
                    sessionId);
            return true;
        }
    }

    @Override
    public Optional<Integer> getTransactionPkFromSessionId(int sessionId) {
        Record1<Integer> record = ctx.select(PS_SESSION.TRANSACTION_PK)
                                     .from(PS_SESSION)
                                     .where(PS_SESSION.PS_SESSION_PK.eq(sessionId))
                                     .fetchOne();
        if (record == null) {
            return Optional.absent();
        }
        return Optional.of(record.value1());
    }

    @Override
    public String getOcppTagOfActiveTransaction(int transactionPK) {
        Record1<String> record = ctx.select(TRANSACTION.ID_TAG)
                                    .from(TRANSACTION)
                                    .where(TRANSACTION.TRANSACTION_PK.eq(transactionPK))
                                    .and(TRANSACTION.STOP_TIMESTAMP.isNull())
                                    .and(TRANSACTION.STOP_VALUE.isNull())
                                    .fetchOne();
        if (record == null) {
            return null;
        }
        return record.value1();
    }

    @Override
    public Optional<Integer> getSessionPkFromTransactionPk(int transactionPK) {
        Record1<Integer> record = ctx.select(PS_SESSION.PS_SESSION_PK)
                                     .from(PS_SESSION)
                                     .where(PS_SESSION.TRANSACTION_PK.eq(transactionPK))
                                     .fetchOne();
        if (record == null) {
            return Optional.absent();
        }
        return Optional.of(record.value1());
    }

    @Override
    public TransactionRecord getTransaction(int transactionPK) {
        return ctx.selectFrom(TRANSACTION)
                  .where(TRANSACTION.TRANSACTION_PK.eq(transactionPK))
                  .fetchOne();
    }

    @Override
    public boolean hasNoSession(String rfid) {
        Record1<Integer> record = ctx.selectOne()
                                     .from(PS_SESSION)
                                     .where(PS_SESSION.OCPP_TAG_PK.eq(ctx.select(OCPP_TAG.OCPP_TAG_PK)
                                                                         .from(OCPP_TAG)
                                                                         .where(OCPP_TAG.ID_TAG.eq(rfid))))
                                     .and(PS_SESSION.TRANSACTION_PK.isNull())
                                     .and(PS_SESSION.EVENT_STATUS.notEqual(SessionStatus.EXPIRED.name()))
                                     .fetchOne();

        return record == null;
    }

    @Override
    public boolean checkConnectorPk(int connectorPK, int sessionId) {
        Record1<Integer> record = ctx.select(PS_SESSION.CONNECTOR_PK)
                                     .from(PS_SESSION)
                                     .where(PS_SESSION.PS_SESSION_PK.eq(sessionId))
                                     .fetchOne();
        if (record == null) {
            return false;
        }
        return connectorPK == record.value1();
    }
}
