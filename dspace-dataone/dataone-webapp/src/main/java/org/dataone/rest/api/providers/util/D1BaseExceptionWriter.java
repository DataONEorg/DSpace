/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dataone.rest.api.providers.util;

import org.apache.commons.io.IOUtils;
import org.dataone.service.exceptions.BaseException;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created with IntelliJ IDEA.
 * User: mdiggory
 * Date: 3/16/14
 * Time: 10:35 AM
 * To change this template use File | Settings | File Templates.
 */

@Provider
@Produces({MediaType.APPLICATION_XML})
public class D1BaseExceptionWriter extends JiBXBodyHandler implements MessageBodyWriter<BaseException> {

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return BaseException.class.isAssignableFrom(type);
    }

    public long getSize(BaseException e, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeTo(BaseException e, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        IOUtils.write(e.serialize(BaseException.FMT_XML), entityStream);
    }
}
