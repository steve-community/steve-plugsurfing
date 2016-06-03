package de.rwth.idsg.steve.extensions.plugsurfing.utils;

import de.rwth.idsg.steve.extensions.plugsurfing.model.data.ConnectorStatus;
import ocpp.cp._2010._08.AvailabilityType;
import ocpp.cs._2010._08.ChargePointStatus;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 11.02.2016
 */
public final class ConnectorStatusConverter {
    private ConnectorStatusConverter() { }

    public static ConnectorStatus getConnectorStatus(ChargePointStatus chargePointStatus) {
        switch (chargePointStatus) {
            case AVAILABLE:     return ConnectorStatus.Available;
            case UNAVAILABLE:   return ConnectorStatus.Offline;
            case OCCUPIED:      return ConnectorStatus.Occupied;
            case FAULTED:       return ConnectorStatus.Offline;
            default:            return ConnectorStatus.Unknown;
        }
    }

    public static ConnectorStatus getConnectorStatus(ocpp.cs._2012._06.ChargePointStatus chargePointStatus) {
        switch (chargePointStatus) {
            case AVAILABLE:     return ConnectorStatus.Available;
            case UNAVAILABLE:   return ConnectorStatus.Offline;
            case OCCUPIED:      return ConnectorStatus.Occupied;
            case RESERVED:      return ConnectorStatus.Reserved;
            case FAULTED:       return ConnectorStatus.Offline;
            default:            return ConnectorStatus.Unknown;
        }
    }

    public static ConnectorStatus getConnectorStatus(AvailabilityType chargePointStatus) {
        switch (chargePointStatus) {
            case OPERATIVE:     return ConnectorStatus.Available;
            case INOPERATIVE:   return ConnectorStatus.Offline;
            default:            return ConnectorStatus.Unknown;

        }
    }

    public static ConnectorStatus getConnectorStatus(ocpp.cp._2012._06.AvailabilityType chargePointStatus) {
        switch (chargePointStatus) {
            case OPERATIVE:     return ConnectorStatus.Available;
            case INOPERATIVE:   return ConnectorStatus.Offline;
            default:            return ConnectorStatus.Unknown;

        }
    }

    public static ConnectorStatus getConnectorStatus(String dbValue) {
        // We map db string to ocpp 1.5 status, because 1.5 values are superset of 1.2
        ocpp.cs._2012._06.ChargePointStatus v = ocpp.cs._2012._06.ChargePointStatus.fromValue(dbValue);
        return getConnectorStatus(v);
    }
}
