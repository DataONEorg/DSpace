package org.dspace.app.util;

import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.util.Properties;

/**
 * Created by mdiggory on 10/2/14.
 */
public class DSpacePropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer
{

    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Autowired
    private ConfigurationService configurationService;


    @Override protected Properties mergeProperties()
    {
        return configurationService.getProperties();
    }
}