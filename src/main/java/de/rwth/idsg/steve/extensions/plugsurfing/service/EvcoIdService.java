package de.rwth.idsg.steve.extensions.plugsurfing.service;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import de.rwth.idsg.steve.extensions.plugsurfing.repository.EvcoIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 21.06.2016
 */
@Service
public class EvcoIdService {

    // Do not touch, do not change!
    private static final String HASH_FUNCTION_NAME = "SHA-256";
    private static final HashFunction HASH_FUNCTION = Hashing.sha256();

    @Autowired private EvcoIdRepository evcoIdRepository;

    public String getOcppIdTag(String evcoId) {
        Optional<String> optional = evcoIdRepository.getOcppIdTag(evcoId);

        // This is not the first time we encounter this evco-id, and have the relevant data already in db
        if (optional.isPresent()) {
            return optional.get();
        }

        String hash = stringToHash(evcoId);
        String ocppIdTag = toUpper(getFirst15(hash));
        evcoIdRepository.addEvcoId(evcoId, ocppIdTag, hash, HASH_FUNCTION_NAME);

        return ocppIdTag;
    }

    public Optional<String> getEvcoId(String rfid) {
        return evcoIdRepository.getEvcoId(rfid);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String stringToHash(String arg) {
        HashCode hc = HASH_FUNCTION.hashString(arg, Charsets.UTF_8);
        return hc.toString();
    }

    /**
     * OCPP 1.2 supports max id tag length of 15, OCPP 1.5 max id tag length of 20. To be compatible with both,
     * we have to use 15.
     *
     * With using only the the first 15 characters of the hash, we are being optimistic that there will not be
     * collisions in practice: How many users (evco-ids) are there to cause a collision?
     *
     * But in theory...
     */
    private static String getFirst15(String arg) {
        return arg.substring(0, 15);
    }

    private static String toUpper(String arg) {
        return arg.toUpperCase();
    }
}
