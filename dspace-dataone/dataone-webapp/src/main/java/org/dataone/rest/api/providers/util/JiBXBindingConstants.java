package org.dataone.rest.api.providers.util;

/**
 * Created with IntelliJ IDEA.
 * User: mdiggory
 * Date: 3/12/14
 * Time: 12:17 PM
 * To change this template use File | Settings | File Templates.
 */
public interface JiBXBindingConstants {
    String VERSION = "version";
    String BINDING = "binding";
    String DEFAULT_VERSION_STRING = "";
    String DEFAULT_BINDING_NAME = new StringBuilder(BINDING).append(DEFAULT_VERSION_STRING).toString();
}