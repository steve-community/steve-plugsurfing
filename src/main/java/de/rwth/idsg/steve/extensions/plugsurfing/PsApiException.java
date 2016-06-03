package de.rwth.idsg.steve.extensions.plugsurfing;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 07.03.2016
 */
public class PsApiException extends RuntimeException {

    private static final long serialVersionUID = 1869614614987376323L;

    @Getter
    private final HttpStatus responseStatus;

    public PsApiException(String message, HttpStatus responseStatus) {
        super(message);
        this.responseStatus = responseStatus;
    }
}
