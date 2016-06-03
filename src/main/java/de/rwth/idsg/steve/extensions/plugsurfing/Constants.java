package de.rwth.idsg.steve.extensions.plugsurfing;

import com.google.common.base.Optional;
import de.rwth.idsg.steve.utils.PropertiesFileLoader;
import lombok.Getter;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Getter
public final class Constants {
    public static final Constants CONFIG = new Constants();

    private final String path;
    private final String apiKey;
    private final Optional<String> stevePsApiKey;
    private final String partnerIdentifier;
    private final String cpoId;
    private final String vendorName;

    private Constants() {
        PropertiesFileLoader prop = new PropertiesFileLoader("plugsurfing.properties");
        path = prop.getString("path");
        apiKey = prop.getString("api-key");
        stevePsApiKey = Optional.fromNullable(prop.getOptionalString("steve-ps-api-key"));
        partnerIdentifier = prop.getString("partner-identifier");
        cpoId = prop.getString("cpo-id");
        vendorName = prop.getString("roaming-vendor-name");
    }
}
