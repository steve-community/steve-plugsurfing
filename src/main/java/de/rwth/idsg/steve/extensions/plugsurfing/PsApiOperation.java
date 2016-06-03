package de.rwth.idsg.steve.extensions.plugsurfing;

import de.rwth.idsg.steve.extensions.plugsurfing.model.receive.request.BaseRequest;
import de.rwth.idsg.steve.extensions.plugsurfing.model.receive.request.SessionStart;
import de.rwth.idsg.steve.extensions.plugsurfing.model.receive.request.SessionStop;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 01.06.2016
 */
@Getter
@RequiredArgsConstructor
public enum PsApiOperation {

    SESSION_START("session-start", SessionStart.class),
    SESSION_STOP("session-stop", SessionStop.class);

    private final String value;
    private final Class<? extends BaseRequest> objectClazz;


    public static PsApiOperation fromValue(String v) {
        for (PsApiOperation c : PsApiOperation.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
