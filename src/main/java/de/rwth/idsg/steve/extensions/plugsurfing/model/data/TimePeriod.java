package de.rwth.idsg.steve.extensions.plugsurfing.model.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.joda.time.DateTime;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Getter
@Setter
@ToString
public class TimePeriod {
    private DateTime start;
    private DateTime stop;
}
