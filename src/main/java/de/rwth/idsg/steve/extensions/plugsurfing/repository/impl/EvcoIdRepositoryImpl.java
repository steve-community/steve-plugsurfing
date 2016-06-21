package de.rwth.idsg.steve.extensions.plugsurfing.repository.impl;

import com.google.common.base.Optional;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.EvcoIdRepository;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static jooq.steve.db.Tables.PS_EVCO_ID;
import static jooq.steve.db.tables.OcppTag.OCPP_TAG;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 21.06.2016
 */
@Slf4j
@Repository
public class EvcoIdRepositoryImpl implements EvcoIdRepository {

    @Autowired private DSLContext ctx;

    @Override
    public void addEvcoId(String evcoId, String ocppIdTag, String fullHash, String algorithmName) {
        ctx.insertInto(PS_EVCO_ID)
           .set(PS_EVCO_ID.EVCO_ID, evcoId)
           .set(PS_EVCO_ID.OCPP_ID_TAG, ocppIdTag)
           .set(PS_EVCO_ID.HASH_VALUE, fullHash)
           .set(PS_EVCO_ID.HASH_ALGORITHM_NAME, algorithmName)
           .execute();
    }

    @Override
    public Optional<String> getOcppIdTag(String evcoId) {
        Record1<String> record = ctx.select(OCPP_TAG.ID_TAG)
                                    .from(PS_EVCO_ID)
                                    .join(OCPP_TAG).on(PS_EVCO_ID.OCPP_ID_TAG.eq(OCPP_TAG.ID_TAG))
                                    .where(PS_EVCO_ID.EVCO_ID.eq(evcoId))
                                    .fetchOne();
        return getOptional(record);
    }

    @Override
    public Optional<String> getEvcoId(String ocppIdTag) {
        Record1<String> record = ctx.select(PS_EVCO_ID.EVCO_ID)
                                    .from(PS_EVCO_ID)
                                    .join(OCPP_TAG).on(PS_EVCO_ID.OCPP_ID_TAG.eq(OCPP_TAG.ID_TAG))
                                    .where(OCPP_TAG.ID_TAG.eq(ocppIdTag))
                                    .fetchOne();
        return getOptional(record);
    }

    private static Optional<String> getOptional(Record1<String> record) {
        if (record == null) {
            return Optional.absent();
        } else {
            return Optional.fromNullable(record.value1());
        }
    }
}
