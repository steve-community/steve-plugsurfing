package de.rwth.idsg.steve.extensions.plugsurfing.repository.impl;

import com.google.common.base.Optional;
import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.CompleteStationInfo;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.ExternalChargePointSelect;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.StationForm;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.StationRepository;
import de.rwth.idsg.steve.ocpp.OcppProtocol;
import de.rwth.idsg.steve.repository.AddressRepository;
import de.rwth.idsg.steve.repository.dto.ChargePointSelect;
import jooq.steve.db.tables.records.AddressRecord;
import jooq.steve.db.tables.records.ChargeBoxRecord;
import jooq.steve.db.tables.records.PsChargeboxRecord;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static jooq.steve.db.tables.ChargeBox.CHARGE_BOX;
import static jooq.steve.db.tables.Connector.CONNECTOR;
import static jooq.steve.db.tables.PsChargebox.PS_CHARGEBOX;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 17.12.2015
 */
@Slf4j
@Repository
public class StationRepositoryImpl implements StationRepository {

    @Autowired private AddressRepository addressRepository;
    @Autowired private DSLContext ctx;

    @Override
    public boolean isExternal(String chargeBoxIdentity) {
        Record1<Integer> record = ctx.select(PS_CHARGEBOX.CHARGE_BOX_PK)
                                     .from(PS_CHARGEBOX)
                                     .join(CHARGE_BOX).on(CHARGE_BOX.CHARGE_BOX_PK.eq(PS_CHARGEBOX.CHARGE_BOX_PK))
                                     .where(CHARGE_BOX.CHARGE_BOX_ID.eq(chargeBoxIdentity))
                                     .and(PS_CHARGEBOX.IS_ENABLED.isTrue())
                                     .fetchOne();

        return record != null;
    }

    @Override
    public boolean isNotExternal(String chargeBoxIdentity) {
        return !isExternal(chargeBoxIdentity);
    }

    @Override
    public PsChargeboxRecord getPlugSurfingStationRecord(int chargeBoxPK) {
        return ctx.selectFrom(PS_CHARGEBOX)
                  .where(PS_CHARGEBOX.CHARGE_BOX_PK.eq(chargeBoxPK))
                  .fetchOne();
    }

    @Override
    public void updateOrAddPlugSurfingStation(StationForm station) {
        ctx.transaction(configuration -> {
            DSLContext ctx = DSL.using(configuration);
            int count = updatePlugSurfingStationInternal(ctx, station);
            if (count == 0) {
                // This means that there are no PlugSurfing Information to update, so add it
                addPlugSurfingStationInternal(ctx, station);
            }
        });
    }

    @Override
    public void disablePlugSurfingStation(int chargeBoxPK) {
        ctx.update(PS_CHARGEBOX)
           .set(PS_CHARGEBOX.POST_TIMESTAMP, (DateTime) null)
           .set(PS_CHARGEBOX.IS_ENABLED, false)
           .where(PS_CHARGEBOX.CHARGE_BOX_PK.eq(chargeBoxPK))
           .execute();
    }

    @Override
    public ExternalChargePointSelect getStationFromConnector(int connectorPK) {

        Result<Record4<String, String, String, Integer>> record =
                ctx.select(CHARGE_BOX.CHARGE_BOX_ID,
                           CHARGE_BOX.ENDPOINT_ADDRESS,
                           CHARGE_BOX.OCPP_PROTOCOL,
                           CONNECTOR.CONNECTOR_ID)
                   .from(CHARGE_BOX)
                   .join(CONNECTOR)
                   .on(CHARGE_BOX.CHARGE_BOX_ID.eq(CONNECTOR.CHARGE_BOX_ID))
                   .where(CONNECTOR.CONNECTOR_PK.eq(connectorPK))
                   .fetch();

        List<ExternalChargePointSelect> bb = record.map(r -> {
            OcppProtocol protocol = OcppProtocol.fromCompositeValue(r.value3());
            ChargePointSelect s = new ChargePointSelect(protocol.getTransport(), r.value1(), r.value2());
            return ExternalChargePointSelect.builder()
                                            .version(protocol.getVersion())
                                            .select(s)
                                            .connectorId(r.value4())
                                            .connectorPk(connectorPK)
                                            .build();
        });

        if (bb == null || bb.isEmpty()) {
            throw new SteveException("EVSE not found");

        } else if (bb.size() == 1) {
            return bb.get(0);
        } else {
            throw new SteveException("EVSE not found");
        }
    }

    @Override
    public Optional<Integer> getConnectorsNumber(String chargeBoxIdentity) {
        Record1<Integer> record = ctx.select(PS_CHARGEBOX.NUMBER_OF_CONNECTORS)
                                     .from(PS_CHARGEBOX)
                                     .join(CHARGE_BOX).on(CHARGE_BOX.CHARGE_BOX_PK.eq(PS_CHARGEBOX.CHARGE_BOX_PK))
                                     .where(CHARGE_BOX.CHARGE_BOX_ID.eq(chargeBoxIdentity))
                                     .fetchOne();
        if (record == null) {
            return Optional.absent();
        } else {
            return Optional.of(record.value1());
        }
    }

    @Override
    public boolean isPosted(String chargeBoxIdentity) {
        Record1<DateTime> dt = ctx.select(PS_CHARGEBOX.POST_TIMESTAMP)
                                  .from(PS_CHARGEBOX)
                                  .join(CHARGE_BOX).on(CHARGE_BOX.CHARGE_BOX_PK.eq(PS_CHARGEBOX.CHARGE_BOX_PK))
                                  .where(CHARGE_BOX.CHARGE_BOX_ID.eq(chargeBoxIdentity))
                                  .fetchOne();

        return dt != null && dt.value1() != null;
    }

    @Override
    public CompleteStationInfo getForStationPost(String chargeBoxIdentity) {
        ChargeBoxRecord cbr = ctx.selectFrom(CHARGE_BOX)
                                 .where(CHARGE_BOX.CHARGE_BOX_ID.equal(chargeBoxIdentity))
                                 .fetchOne();

        AddressRecord ar = addressRepository.get(ctx, cbr.getAddressPk());

        PsChargeboxRecord ps = getPlugSurfingStationRecord(cbr.getChargeBoxPk());

        return CompleteStationInfo.builder()
                                  .chargeBox(cbr)
                                  .address(ar)
                                  .psTable(ps)
                                  .build();
    }

    @Override
    public void setPosted(int chargeBoxPK) {
        ctx.update(PS_CHARGEBOX)
           .set(PS_CHARGEBOX.POST_TIMESTAMP, DateTime.now())
           .where(PS_CHARGEBOX.CHARGE_BOX_PK.eq(chargeBoxPK))
           .execute();
    }


    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void addPlugSurfingStationInternal(DSLContext ctx, StationForm form) {
        int chargeStationPublicKeyId = form.getChargeBoxPk();
        int count = ctx.insertInto(PS_CHARGEBOX)
                       .set(PS_CHARGEBOX.CHARGE_BOX_PK, chargeStationPublicKeyId)
                       .set(PS_CHARGEBOX.IS_OPEN_24, form.getOpen24())
                       .set(PS_CHARGEBOX.NUMBER_OF_CONNECTORS, form.getNumberOfConnectors())
                       // optional arguments
                       .set(PS_CHARGEBOX.IS_FREE_CHARGE, form.getFreeCharge())
                       .set(PS_CHARGEBOX.IS_GREEN_POWER_AVAILABLE, form.getGreenPowerAvailable())
                       .set(PS_CHARGEBOX.IS_PRIVATE, form.getPrivatelyOwned())
                       .set(PS_CHARGEBOX.IS_PLUGIN_CHARGE, form.getPluginCharge())
                       .set(PS_CHARGEBOX.IS_RESERVABLE, form.getReservable())
                       .set(PS_CHARGEBOX.IS_ROOFED, form.getRoofed())
                       .set(PS_CHARGEBOX.TOTAL_PARKING, form.getTotalParking())
                       .set(PS_CHARGEBOX.FLOOR_LEVEL, form.getFloorLevel())
                       .set(PS_CHARGEBOX.PHONE, form.getContact().getPhone())
                       .set(PS_CHARGEBOX.FAX, form.getContact().getFax())
                       .set(PS_CHARGEBOX.WEBSITE, form.getContact().getEmail())
                       .set(PS_CHARGEBOX.EMAIL, form.getContact().getEmail())
                       .set(PS_CHARGEBOX.IS_ENABLED, form.getPlugSurfing())
                       .execute();

        if (count != 1) {
            throw new SteveException("Failed to insert the PlugSurfing Details for Key %s", chargeStationPublicKeyId);
        }
    }

    private int updatePlugSurfingStationInternal(DSLContext ctx, StationForm station) {
        return ctx.update(PS_CHARGEBOX)
                  .set(PS_CHARGEBOX.IS_OPEN_24, station.getOpen24())
                  .set(PS_CHARGEBOX.IS_RESERVABLE, station.getReservable())
                  .set(PS_CHARGEBOX.FLOOR_LEVEL, station.getFloorLevel())
                  .set(PS_CHARGEBOX.IS_FREE_CHARGE, station.getFreeCharge())
                  .set(PS_CHARGEBOX.TOTAL_PARKING, station.getTotalParking())
                  .set(PS_CHARGEBOX.IS_GREEN_POWER_AVAILABLE, station.getGreenPowerAvailable())
                  .set(PS_CHARGEBOX.IS_PLUGIN_CHARGE, station.getPluginCharge())
                  .set(PS_CHARGEBOX.IS_ROOFED, station.getRoofed())
                  .set(PS_CHARGEBOX.IS_PRIVATE, station.getPrivatelyOwned())
                  .set(PS_CHARGEBOX.PHONE, station.getContact().getPhone())
                  .set(PS_CHARGEBOX.FAX, station.getContact().getFax())
                  .set(PS_CHARGEBOX.WEBSITE, station.getContact().getWebsite())
                  .set(PS_CHARGEBOX.EMAIL, station.getContact().getEmail())
                  .set(PS_CHARGEBOX.IS_ENABLED, station.getPlugSurfing())
                  .where(PS_CHARGEBOX.CHARGE_BOX_PK.eq(station.getChargeBoxPk()))
                  .execute();
    }

}
