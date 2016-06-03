package de.rwth.idsg.steve.extensions.plugsurfing.repository;

import com.google.common.base.Optional;

/**
 * This class corresponds with the RFID value received from PlugSurfing
 *
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 22.01.2016
 */
public interface OcppExternalTagRepository {

    Optional<Integer> getOcppTagPkForRfid(String rfid);

    boolean isExternal(String rfid);

    boolean isExternalOrUnknown(String rfid);

    int addOcppTag(String ocppTagId);

    int addOrIgnoreIfPresent(String rfid);

    void block(String rfid);

    void unblock(String rfid);

    void setInSessionTrue(String rfid);

    void setInSessionFalse(String rfid);
}
