package de.rwth.idsg.steve.extensions.plugsurfing.repository;

import com.google.common.base.Optional;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.CompleteStationInfo;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.ExternalChargePointSelect;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.StationForm;
import jooq.steve.db.tables.records.PsChargeboxRecord;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 17.12.2015
 */
public interface StationRepository {

    boolean isExternal(String chargeBoxIdentity);

    boolean isNotExternal(String chargeBoxIdentity);

    PsChargeboxRecord getPlugSurfingStationRecord(int chargeBoxPK);

    void updateOrAddPlugSurfingStation(StationForm station);

    void disablePlugSurfingStation(int chargeBoxPK);

    ExternalChargePointSelect getStationFromConnector(int connectorPK);

    Optional<Integer> getConnectorsNumber(String chargeBoxIdentity);

    boolean isPosted(String chargeBoxIdentity);

    CompleteStationInfo getForStationPost(String chargeBoxIdentity);

    void setPosted(int chargeBoxPK);
}
