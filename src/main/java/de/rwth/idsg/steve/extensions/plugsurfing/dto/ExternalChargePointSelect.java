package de.rwth.idsg.steve.extensions.plugsurfing.dto;

import de.rwth.idsg.steve.ocpp.OcppVersion;
import de.rwth.idsg.steve.repository.dto.ChargePointSelect;
import lombok.Builder;
import lombok.Getter;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 22.01.2016
 */
@Getter
@Builder
public class ExternalChargePointSelect {
    private Integer connectorId;
    private Integer connectorPk;

    private OcppVersion version;
    private ChargePointSelect select;
}
