package de.rwth.idsg.steve.extensions.plugsurfing.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
public interface Resource {
    void dispatch(HttpServletRequest request, HttpServletResponse response);
}
