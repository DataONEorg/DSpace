/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dataone.rest.api.providers.util;

import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

/**
 * @author Diptamay Sanyal (diptamay@yahoo.com)
 */
@Provider
@Produces(MediaType.APPLICATION_XML)
public class JiBXBodyWriter extends JiBXBodyHandler implements MessageBodyWriter<Object> {

    @Context UriInfo uriInfo;

    public boolean isWriteable(Class<?> type, Type genericType, java.lang.annotation.Annotation[] annotations, MediaType mediaType) {
        try {
            getFactory(type);
        } catch (JiBXException e) {
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    public long getSize(Object o, Class<?> type, Type genericType, java.lang.annotation.Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    public void writeTo(Object o, Class<?> type, Type genericType, java.lang.annotation.Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        try {
            IBindingFactory factory = getFactory(uriInfo, type);
            IMarshallingContext context = factory.createMarshallingContext();
            context.setIndent(2);
            context.marshalDocument(o, "UTF-8", true, entityStream);
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}