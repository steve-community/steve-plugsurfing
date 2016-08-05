package de.rwth.idsg.steve.extensions.plugsurfing.repository.impl;

import com.google.common.base.Optional;
import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.ConnectorStatus;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.ConnectorPostStatus;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.ConnectorRepositroy;
import de.rwth.idsg.steve.extensions.plugsurfing.utils.ConnectorStatusConverter;
import de.rwth.idsg.steve.extensions.plugsurfing.utils.StationUtils;
import de.rwth.idsg.steve.repository.impl.ChargePointRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.TableLike;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static jooq.steve.db.tables.ChargeBox.CHARGE_BOX;
import static jooq.steve.db.tables.Connector.CONNECTOR;
import static jooq.steve.db.tables.ConnectorStatus.CONNECTOR_STATUS;
import static jooq.steve.db.tables.PsChargebox.PS_CHARGEBOX;
import static jooq.steve.db.tables.Reservation.RESERVATION;
import static jooq.steve.db.tables.Transaction.TRANSACTION;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 28.12.2015
 */
@Slf4j
@Repository
public class ConnectorRepositoryImpl implements ConnectorRepositroy {

    @Autowired private DSLContext ctx;

    /**
     * Returns Optional, because we need to check whether this connector is related to a PS station or not.
     */
    @Override
    public Optional<Integer> getConnectorPk(String chargeBoxId, int connectorId) {
        Record1<Integer> recordId = ctx.select(CONNECTOR.CONNECTOR_PK)
                                       .from(CONNECTOR)
                                       // to guarantee that this is a PS station we need the joins
                                       .join(CHARGE_BOX).on(CHARGE_BOX.CHARGE_BOX_ID.eq(CONNECTOR.CHARGE_BOX_ID))
                                       .join(PS_CHARGEBOX).on(PS_CHARGEBOX.CHARGE_BOX_PK.eq(CHARGE_BOX.CHARGE_BOX_PK))
                                       .where(CONNECTOR.CONNECTOR_ID.eq(connectorId))
                                       .and(CONNECTOR.CHARGE_BOX_ID.eq(chargeBoxId))
                                       .fetchOne();
        if (recordId == null) {
            return Optional.absent();
        } else {
            return Optional.fromNullable(recordId.value1());
        }
    }

    @Override
    public int getConnectorPkFromTransactionPk(int transactionPK) {
        Record1<Integer> recordId = ctx.select(CONNECTOR.CONNECTOR_PK)
                                       .from(CONNECTOR)
                                       .join(TRANSACTION)
                                       .on(CONNECTOR.CONNECTOR_PK.eq(TRANSACTION.CONNECTOR_PK))
                                       .where(TRANSACTION.TRANSACTION_PK.eq(transactionPK))
                                       .fetchOne();
        if (recordId == null) {
            throw new SteveException("No such PK for given transactionPK : %s", transactionPK);
        }
        return recordId.value1();
    }

    @Override
    public List<Integer> getDiscoveredConnPks(String chargeBoxId) {
        return ctx.selectDistinct(CONNECTOR.CONNECTOR_PK)
                  .from(CONNECTOR)
                  .join(CHARGE_BOX).on(CHARGE_BOX.CHARGE_BOX_ID.eq(CONNECTOR.CHARGE_BOX_ID))
                  .join(PS_CHARGEBOX).on(PS_CHARGEBOX.CHARGE_BOX_PK.eq(CHARGE_BOX.CHARGE_BOX_PK))
                  .where(CONNECTOR.CHARGE_BOX_ID.eq(chargeBoxId))
                  .and(CONNECTOR.CONNECTOR_ID.notEqual(0))
                  .fetch()
                  .map(Record1::value1);
    }


    @Override
    public List<Integer> getDiscoveredConnPks(int chargeBoxPk) {
        return ctx.selectDistinct(CONNECTOR.CONNECTOR_PK)
                  .from(CONNECTOR)
                  .join(CHARGE_BOX).on(CHARGE_BOX.CHARGE_BOX_ID.eq(CONNECTOR.CHARGE_BOX_ID))
                  .join(PS_CHARGEBOX).on(PS_CHARGEBOX.CHARGE_BOX_PK.eq(CHARGE_BOX.CHARGE_BOX_PK))
                  .where(PS_CHARGEBOX.CHARGE_BOX_PK.eq(chargeBoxPk))
                  .and(CONNECTOR.CONNECTOR_ID.notEqual(0))
                  .fetch()
                  .map(Record1::value1);
    }

    /**
     * A customized version of {@link ChargePointRepositoryImpl#getChargePointConnectorStatus()}
     * for this specific use case.
     */
    @Override
    public List<ConnectorPostStatus> getChargePointConnectorStatus(String chargeBoxId) {
        // Prepare for the inner select of the second join
        Field<Integer> t1Pk = CONNECTOR_STATUS.CONNECTOR_PK.as("t1_pk");
        Field<DateTime> t1Max = DSL.max(CONNECTOR_STATUS.STATUS_TIMESTAMP).as("t1_max");
        TableLike<?> t1 = ctx.select(t1Pk, t1Max)
                             .from(CONNECTOR_STATUS)
                             .groupBy(CONNECTOR_STATUS.CONNECTOR_PK)
                             .asTable("t1");

        return ctx.select(CONNECTOR.CONNECTOR_PK,
                          CONNECTOR_STATUS.STATUS)
                  .from(CONNECTOR_STATUS)
                  .join(CONNECTOR)
                    .onKey()
                  .join(CHARGE_BOX)
                    .on(CHARGE_BOX.CHARGE_BOX_ID.eq(CONNECTOR.CHARGE_BOX_ID))
                  .join(PS_CHARGEBOX)
                    .on(PS_CHARGEBOX.CHARGE_BOX_PK.eq(CHARGE_BOX.CHARGE_BOX_PK))
                  .join(t1)
                    .on(CONNECTOR_STATUS.CONNECTOR_PK.equal(t1.field(t1Pk)))
                    .and(CONNECTOR_STATUS.STATUS_TIMESTAMP.equal(t1.field(t1Max)))
                  .where(CONNECTOR.CONNECTOR_ID.notEqual(0))
                  .and(CHARGE_BOX.CHARGE_BOX_ID.eq(chargeBoxId))
                  .fetch()
                  .map(r -> buildStatus(r.value1(), r.value2()));
    }

    @Override
    public int getConnectorIdFromReservation(int reservationPk) {
        return ctx.select(CONNECTOR.CONNECTOR_ID)
                  .from(CONNECTOR)
                  .join(RESERVATION)
                  .on(CONNECTOR.CONNECTOR_PK.eq(RESERVATION.CONNECTOR_PK))
                  .where(RESERVATION.CONNECTOR_PK.eq(reservationPk))
                  .fetchOne()
                  .value1();
    }

    private static ConnectorPostStatus buildStatus(int connectorPk, String status) {
        ConnectorStatus cs = ConnectorStatusConverter.getConnectorStatus(status);
        return StationUtils.buildConnectorPostStatus(connectorPk, cs);
    }
}
