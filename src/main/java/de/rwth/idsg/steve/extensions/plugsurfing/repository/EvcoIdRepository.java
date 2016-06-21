package de.rwth.idsg.steve.extensions.plugsurfing.repository;

import com.google.common.base.Optional;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 21.06.2016
 */
public interface EvcoIdRepository {
    void addEvcoId(String evcoId, String ocppIdTag, String fullHash, String algorithmName);
    Optional<String> getOcppIdTag(String evcoId);
    Optional<String> getEvcoId(String ocppIdTag);
}
