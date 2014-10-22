/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dataone.rest.api.providers.util;

import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InsufficientResources;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created with IntelliJ IDEA.
 * User: mdiggory
 * Date: 3/15/14
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
public class D1BaseExceptionMapper implements ExceptionMapper<BaseException> {

    @Context
    private HttpHeaders headers;

    /**
     * Sets Response Status and Response Type, Response is then serialized
     * by the D1BaseExceptionWriter.
     *
     * @param exception
     * @return response
     */
    public Response toResponse(BaseException exception) {

        /*HTTP/1.1 404 Not Found
        Last-Modified: Wed, 16 Dec 2009 13:58:34 GMT
        Content-Length: 1182
        Content-Type: text/xml  */

        return Response
                .status(exception.getCode())
                .entity(exception)
                .type(headers.getMediaType())
                .header("DataONE-Exception-Name", exception.getClass().getSimpleName())
                .header("DataONE-Exception-DetailCode", exception.getDetail_code())
                .header("DataONE-Exception-Description", exception.getDescription())
                .header("DataONE-Exception-PID", exception.getPid())

                .build();
    }
}
