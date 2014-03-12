package org.dspace.app.dataone.impl;

import org.apache.solr.client.solrj.SolrQuery;
import org.dataone.service.exceptions.*;
import org.dataone.service.mn.tier1.v1.MNCore;
import org.dataone.service.types.v1.*;
import org.dspace.content.DSpaceObject;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.storage.rdbms.DatabaseManager;

import java.sql.Connection;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: mdiggory
 * Date: 3/12/14
 * Time: 11:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class DSpaceMNCoreImpl implements MNCore {

    /**
     * Capabilities (to be set off of Spring Service Manager
     */
    private Node capabilities = null;

    public Date ping() throws NotImplemented, ServiceFailure, InsufficientResources {
        SolrQuery queryArgs = new SolrQuery();
        if(queryArgs==null)
        {
            throw new ServiceFailure("solr","solr is null");
        }
        try{
            Connection connection = DatabaseManager.getConnection();
            if(connection==null)
            {
                throw new ServiceFailure("sql","database connection failed");
            }
        }   catch  (Exception e)
        {
            throw new ServiceFailure("2042",e.getMessage());
        }
        return new Date();
    }

    public Log getLogRecords(Date fromDate, Date toDate, Event event, String pidFilter, Integer start, Integer count) throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure {

        Log log = new Log();

        LogEntry entry = new LogEntry();
        entry.setEntryId(UUID.randomUUID().toString());
        entry.setDateLogged(toDate != null ? toDate : new Date());

        Identifier id = new Identifier();
        id.setValue("http:/dx.doi.org/10.5061/dryad.571?ver=2011-07-28T15:02:29.334-0400");
        entry.setIdentifier(id);
        entry.setIpAddress("152.3.68.115");

        Subject subject = new Subject();
        subject.setValue("DC=dataone, DC=org");
        entry.setSubject(subject);

        entry.setUserAgent("Agent");
        entry.setEvent(event != null ? event : Event.READ);

        NodeReference ref = new NodeReference();
        ref.setValue("urn:node:DSPACE");
        entry.setNodeIdentifier(ref);

        log.addLogEntry(entry);

        return log;

    }


    public void setCapabilities(Node capabilities){
         this.capabilities = capabilities;
    }
    public Node getCapabilities() throws NotImplemented, ServiceFailure
    {
        return capabilities;
    }

    public Log getLogRecords(Session session, Date fromDate, Date toDate, Event event, String pidFilter, Integer start, Integer count) throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure {
        return getLogRecords(fromDate, toDate, event, pidFilter, start, count);
    }

}
