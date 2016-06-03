package de.rwth.idsg.steve.extensions.plugsurfing.utils;

import com.neovisionaries.i18n.CountryCode;
import de.rwth.idsg.steve.extensions.plugsurfing.Constants;
import de.rwth.idsg.steve.extensions.plugsurfing.dto.CompleteStationInfo;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.Address;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.Connector;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.ConnectorName;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.ConnectorStatus;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.Contact;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.Station;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.ConnectorPostStatus;
import de.rwth.idsg.steve.extensions.plugsurfing.model.send.request.StationPost;
import jooq.steve.db.tables.records.AddressRecord;
import jooq.steve.db.tables.records.PsChargeboxRecord;
import jooq.steve.db.tables.records.TransactionRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 08.03.2016
 */
public final class StationUtils {

    private StationUtils() { }

    /**
     * Default OCPP values are in Wh, but PS wants kWh!
     */
    public static double calculateEnergy(TransactionRecord transaction) {
        String start = transaction.getStartValue();
        String stop = transaction.getStopValue();

        BigDecimal startBD = new BigDecimal(start);
        BigDecimal stopBD = new BigDecimal(stop);
        BigDecimal diffInWH = stopBD.subtract(startBD);

        BigDecimal diffInKWH = diffInWH.divide(new BigDecimal(1000));
        return diffInKWH.doubleValue();
    }

    public static ConnectorPostStatus buildConnectorPostStatus(int connectorPk, ConnectorStatus status) {
        ConnectorPostStatus connectorRequest = new ConnectorPostStatus();
        connectorRequest.setConnectorPrimaryKey(String.valueOf(connectorPk));
        connectorRequest.setStatus(status);
        return connectorRequest;
    }

    public static StationPost buildStationPost(Station station) {
        StationPost post = new StationPost();
        post.setStation(station);
        return post;
    }

    public static Station buildStation(CompleteStationInfo info, List<Integer> discoveredConns) {
        Station station = new Station();

        station.setAddress(StationUtils.getAddress(info.getAddress()));
        station.setContact(StationUtils.getContact(info.getPsTable()));
        station.setConnectors(StationUtils.getConnectors(discoveredConns));

        station.setId(info.getChargeBox().getChargeBoxId());
        station.setName(info.getChargeBox().getChargeBoxId());
        station.setDescription(info.getChargeBox().getDescription());
        station.setLatitude(info.getChargeBox().getLocationLatitude());
        station.setLongitude(info.getChargeBox().getLocationLongitude());

        station.setCpoId(Constants.CONFIG.getCpoId());
        station.setIsOpen24(info.getPsTable().getIsOpen_24());
        station.setNotes(info.getChargeBox().getNote());
        station.setIsReservable(info.getPsTable().getIsReservable());
        station.setFloorLevel(info.getPsTable().getFloorLevel());
        station.setIsFreeCharge(info.getPsTable().getIsFreeCharge());
        station.setTotalParking(info.getPsTable().getTotalParking());
        station.setIsGreenPowerAvailable(info.getPsTable().getIsGreenPowerAvailable());
        station.setIsPluginCharge(info.getPsTable().getIsPluginCharge());
        station.setIsRoofed(info.getPsTable().getIsRoofed());
        station.setIsPrivate(info.getPsTable().getIsPrivate());

        return station;
    }

    private static Address getAddress(AddressRecord record) {
        Address addr = new Address();
        addr.setStreet(record.getStreet());
        addr.setStreetNumber(record.getHouseNumber());
        addr.setCity(record.getCity());
        addr.setZip(record.getZipCode());
        addr.setCountry(CountryCode.getByCode(record.getCountry()));
        return addr;
    }

    private static Contact getContact(PsChargeboxRecord psTable) {
        Contact c = new Contact();
        c.setEmail(psTable.getEmail());
        c.setWeb(psTable.getWebsite());
        c.setFax(psTable.getFax());
        c.setPhone(psTable.getPhone());
        return c;
    }

    private static List<Connector> getConnectors(List<Integer> discoveredConns) {
        List<Connector> connectors = new ArrayList<>();
        for (Integer connectorPk : discoveredConns) {
            Connector c = new Connector();
            c.setPrimaryKey(String.valueOf(connectorPk));
            c.setName(ConnectorName.Unknown);
            c.setSpeed(0); // PS requires a value for speed, which cannot be obtained from OCPP
            connectors.add(c);
        }
        return connectors;
    }
}
