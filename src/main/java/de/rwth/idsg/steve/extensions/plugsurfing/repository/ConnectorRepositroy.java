package de.rwth.idsg.steve.extensions.plugsurfing.repository;


import com.google.common.base.Optional;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.ConnectorPostStatus;

import java.util.List;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 28.12.2015
 */
public interface ConnectorRepositroy {

    Optional<Integer> getConnectorPk(String chargeBoxId, int connectorId);

    int getConnectorPkFromTransactionPk(int transactionPK);

    List<Integer> getDiscoveredConnPks(String chargeBoxId);

    List<Integer> getDiscoveredConnPks(int chargeBoxPk);

    List<ConnectorPostStatus> getChargePointConnectorStatus(String chargeBoxId);

    int getConnectorIdFromReservation(int reservationPk);
}
