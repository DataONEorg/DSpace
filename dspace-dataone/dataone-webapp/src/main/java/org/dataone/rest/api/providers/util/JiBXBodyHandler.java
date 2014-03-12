package org.dataone.rest.api.providers.util;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.JiBXException;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

/**
 * @author Diptamay Sanyal (diptamay@yahoo.com)
 */
public abstract class JiBXBodyHandler implements JiBXBindingConstants {

    public static IBindingFactory getFactory(String binding, Class klass) throws JiBXException {
        System.out.println("The binding to be loaded is " + binding);
        if (binding == null || binding.equals("")) {
            return BindingDirectory.getFactory(klass);
        } else {
            return BindingDirectory.getFactory(binding, klass);
        }
    }

    public static IBindingFactory getFactory(Class klass) throws JiBXException {
        return getFactory(DEFAULT_BINDING_NAME, klass);
    }

    protected IBindingFactory getFactory(UriInfo uriInfo, Class type) throws JiBXException {
        if (uriInfo != null) {
            MultivaluedMap<String, String> map = uriInfo.getPathParameters();
            //String version = map.getFirst(VERSION).replace(".", "_");
            //System.out.println("Version = " + version);
            String bindingName = new StringBuilder(BINDING).toString();
            return getFactory(bindingName, type);
        } else {
            System.out.println("uriInfo is null");
            return getFactory(type);
        }
    }

}