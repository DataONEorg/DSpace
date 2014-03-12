package org.dataone.rest.api.providers.util;

import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Diptamay Sanyal (diptamay@yahoo.com)
 */
@Provider
@Consumes(MediaType.APPLICATION_XML)
public class JiBXBodyReader extends JiBXBodyHandler implements MessageBodyReader<Object> {

    @Context UriInfo uriInfo;

    public boolean isReadable(Class<?> type, Type genericType,
                              Annotation[] annotations, MediaType mediaType) {
        try {
            getFactory(type);
        } catch (JiBXException e) {
            return false;
        }
        return true;
    }

    public Object readFrom(Class<Object> type, Type genericType,
                           Annotation[] annotations, MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        try {
            IBindingFactory factory = getFactory(uriInfo, type);
            IUnmarshallingContext context = factory.createUnmarshallingContext();
            return context.unmarshalDocument(entityStream, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

}