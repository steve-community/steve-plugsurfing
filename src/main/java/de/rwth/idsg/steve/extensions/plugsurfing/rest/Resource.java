package de.rwth.idsg.steve.extensions.plugsurfing.rest;

import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
public interface Resource {
    DeferredResult<?> dispatch(HttpServletRequest request);
}
