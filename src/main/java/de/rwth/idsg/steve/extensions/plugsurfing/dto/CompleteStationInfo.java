package de.rwth.idsg.steve.extensions.plugsurfing.dto;

import jooq.steve.db.tables.records.AddressRecord;
import jooq.steve.db.tables.records.ChargeBoxRecord;
import jooq.steve.db.tables.records.PsChargeboxRecord;
import lombok.Builder;
import lombok.Getter;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 23.02.2016
 */
@Getter
@Builder
public class CompleteStationInfo {
    private final ChargeBoxRecord chargeBox;
    private final AddressRecord address;
    private final PsChargeboxRecord psTable;
}
