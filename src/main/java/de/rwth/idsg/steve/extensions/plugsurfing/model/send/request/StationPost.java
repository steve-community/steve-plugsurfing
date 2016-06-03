package de.rwth.idsg.steve.extensions.plugsurfing.model.send.request;

import com.fasterxml.jackson.annotation.JsonRootName;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.Station;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Getter
@Setter
@ToString(callSuper = true)
@JsonRootName("station-post")
public class StationPost extends AbstractRequest {
    private Station station;
}
