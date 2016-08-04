package de.rwth.idsg.steve.extensions.plugsurfing.repository.impl;

import com.google.common.base.Optional;
import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.extensions.plugsurfing.Constants;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.OcppExternalTagRepository;
import de.rwth.idsg.steve.repository.OcppTagRepository;
import jooq.steve.db.tables.records.OcppTagRecord;
import jooq.steve.db.tables.records.PsOcppTagRecord;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.exception.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static jooq.steve.db.tables.OcppTag.OCPP_TAG;
import static jooq.steve.db.tables.PsOcppTag.PS_OCPP_TAG;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 22.01.2016
 */
@Slf4j
@Repository
public class OcppExternalTagRepositoryImpl implements OcppExternalTagRepository {

    @Autowired private DSLContext ctx;
    @Autowired private OcppTagRepository normalRepository;

    @Override
    public Optional<Integer> getOcppTagPkForRfid(String rfid) {
        OcppTagRecord record = normalRepository.getRecord(rfid);

        if (record == null) {
            return Optional.absent();
        } else {
            return Optional.of(record.getOcppTagPk());
        }
    }

    @Override
    public boolean isLocal(String rfid) {
        Record1<Integer> onlyLocal = ctx.selectOne()
                                        .from(OCPP_TAG)
                                        .leftOuterJoin(PS_OCPP_TAG)
                                        .on(OCPP_TAG.OCPP_TAG_PK.eq(PS_OCPP_TAG.OCPP_TAG_PK))
                                        .where(PS_OCPP_TAG.OCPP_TAG_PK.isNull())
                                        .and(OCPP_TAG.ID_TAG.eq(rfid))
                                        .fetchOne();

        return onlyLocal != null;
    }

    @Override
    public boolean isExternal(String rfid) {
        Record1<Integer> externalAndActive =
                ctx.select(OCPP_TAG.OCPP_TAG_PK)
                   .from(OCPP_TAG)
                   .join(PS_OCPP_TAG).on(OCPP_TAG.OCPP_TAG_PK.eq(PS_OCPP_TAG.OCPP_TAG_PK))
                   .where(OCPP_TAG.ID_TAG.eq(rfid))
                   // since we mark the user as blocked/unblocked in the ocpp tag table based
                   // on the response from PS (see PlugSurfingAuthenticator.verifyRfid())
                   // we need to check this constraint !
                   .and(OCPP_TAG.BLOCKED.isFalse())
                   .fetchOne();

        return externalAndActive != null;
    }

    /**
     * local    external    askPs?
     * 0        0           yes
     * 0        1           (cannot happen)
     * 1        0           no
     * 1        1           yes
     *
     * So, we can actually find out whether the rfid is only local, and negate the result
     * => Either present in both local and roaming tables, or missing from both.
     */
    @Override
    public boolean isExternalOrUnknown(String rfid) {
        return !isLocal(rfid);
    }

    @Override
    public int addOcppTag(String rfid) {
        return ctx.transactionResult(configuration -> {
            try {
                int ocppTagPk = insertLocal(rfid);
                insertExternal(ocppTagPk);
                return ocppTagPk;
            } catch (DataAccessException e) {
                throw new SteveException("Failed to insert the OCPP tag", e);
            }
        });
    }

    /**
     * Only use for session-start. Returns the primary key!
     */
    @Override
    public int addOrIgnoreIfPresent(String rfid) {

        PsOcppTagRecord external = getExternalRecord(rfid);

        // two options:
        // 1) rfid missing in OCPP_TAG and PS_OCPP_TAG => unknown user
        // 2) rfid present in OCPP_TAG, but missing in PS_OCPP_TAG => local user
        if (external == null) {
            return processUnknownOrLocal(rfid);

        // rfid present in OCPP_TAG and PS_OCPP_TAG
        } else {
            return external.getOcppTagPk();
        }
    }

    @Override
    public void block(String rfid) {
        String note = "Blocked because PlugSurfing could not verify. Timestamp: " + DateTime.now();
        blockCallInternal(rfid, true, note);
    }

    @Override
    public void unblock(String rfid) {
        String note = "Verified by PlugSurfing. Timestamp: " + DateTime.now();
        blockCallInternal(rfid, false, note);
    }

    @Override
    public void setInSessionTrue(String rfid) {
        markUserInSession(rfid, true);
    }

    @Override
    public void setInSessionFalse(String rfid) {
        markUserInSession(rfid, false);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void markUserInSession(String rfid, boolean inSession) {
        ctx.update(PS_OCPP_TAG)
           .set(PS_OCPP_TAG.IN_SESSION, inSession)
           .where(PS_OCPP_TAG.OCPP_TAG_PK.eq(ctx.select(OCPP_TAG.OCPP_TAG_PK)
                                                .from(OCPP_TAG)
                                                .where(OCPP_TAG.ID_TAG.eq(rfid))))
           .execute();
    }

    private int processUnknownOrLocal(String rfid) {
        OcppTagRecord local = normalRepository.getRecord(rfid);

        // unknown user => insert in both tables
        if (local == null) {
            return addOcppTag(rfid);

        // local user => insert only in remote table
        } else {
            int ocppTagPk = local.getOcppTagPk();
            insertExternal(ocppTagPk);
            return local.getOcppTagPk();
        }
    }

    private void insertExternal(int ocppTagPk) {
        ctx.insertInto(PS_OCPP_TAG)
           .set(PS_OCPP_TAG.OCPP_TAG_PK, ocppTagPk)
           .set(PS_OCPP_TAG.VENDOR_NAME, Constants.CONFIG.getVendorName())
           .execute();
    }

    private int insertLocal(String rfid) {
        return ctx.insertInto(OCPP_TAG)
                  .set(OCPP_TAG.ID_TAG, rfid)
                  .set(OCPP_TAG.BLOCKED, false)
                  .set(OCPP_TAG.IN_TRANSACTION, false)
                  .returning(OCPP_TAG.OCPP_TAG_PK)
                  .fetchOne()
                  .getOcppTagPk();
    }

    private void blockCallInternal(String rfid, boolean blocked, String note) {
        try {
            ctx.update(OCPP_TAG)
               .set(OCPP_TAG.NOTE, note)
               .set(OCPP_TAG.BLOCKED, blocked)
               .where(OCPP_TAG.ID_TAG.equal(rfid))
               .execute();
        } catch (DataAccessException e) {
            throw new SteveException("Execution of updateOcppTag for idTag '%s' FAILED.", rfid, e);
        }
    }

    private PsOcppTagRecord getExternalRecord(String rfid) {
        return ctx.selectFrom(PS_OCPP_TAG)
                  .where(PS_OCPP_TAG.OCPP_TAG_PK.eq(ctx.select(OCPP_TAG.OCPP_TAG_PK)
                                                       .from(OCPP_TAG)
                                                       .where(OCPP_TAG.ID_TAG.eq(rfid))))
                  .fetchOne();
    }

}
