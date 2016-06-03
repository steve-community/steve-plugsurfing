package de.rwth.idsg.steve.extensions.plugsurfing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * To comply with the error response structure of PS, we use the same root and property naming.
 *
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 07.03.2016
 */
@Getter
@RequiredArgsConstructor
@JsonRootName("error")
public class ErrorResponse {

    @JsonProperty(value = "request")
    private final String errorMessage;
}
