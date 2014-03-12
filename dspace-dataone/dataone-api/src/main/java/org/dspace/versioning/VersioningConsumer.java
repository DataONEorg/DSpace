/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;
import org.jaxen.expr.iter.IterableNamespaceAxis;

import java.util.*;

/**
 * Override of DSpaceVersioning Consumer. Adds Support for
 *
 * 1.) Versioning on modification events
 * 2.)
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersioningConsumer implements Consumer {

    /** log4j category */
    private static Logger log = Logger.getLogger(VersioningConsumer.class);

    private MultiValueMap itemsToProcess;

    public void initialize() throws Exception {}

    public void finish(Context ctx) throws Exception {}

    public void consume(Context ctx, Event event) throws Exception {
        if(itemsToProcess == null){
            itemsToProcess = new MultiValueMap();
        }

        int st = event.getSubjectType();
        int et = event.getEventType();
        if(st == Constants.ITEM && (et == Event.INSTALL||et == Event.MODIFY||et == Event.MODIFY_METADATA)){
            Item item = (Item) event.getSubject(ctx);
            itemsToProcess.put(item,event);
        }
        else if(st==Constants.BITSTREAM&&(et==Event.MODIFY||et==Event.MODIFY_METADATA))
        {
            Bitstream bitstream = (Bitstream) event.getSubject(ctx);
            DSpaceObject dso = bitstream.getParentObject();
            if(dso!=null&&dso.getType()==Constants.ITEM)
            {
                Item item = (Item)dso;
                itemsToProcess.put(item,event);
            }

        }
    }

    public void end(Context ctx) throws Exception {
        if(itemsToProcess != null){
            VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);

            // TODO : We should probably use separate context here and don't dirty the existing one with our work.
            for(Object obj : itemsToProcess.keySet()){

                Collection<Event> events = itemsToProcess.getCollection(obj);

                ctx.turnOffAuthorisationSystem();
                try {
                    Item item = (Item) obj;
                    Boolean addBundles = false;
                    int versionId = 0;
                    if (item != null && item.isArchived()) {
                        //case 1: submit a new item without entering workflow
                        //case 2: approve an item in workflow
                        String summary = "";
                        if(eventsToString(events).contains("INSTALL"))
                        {
                            summary = "Create New Item" ;
                        }
                        else
                        {
                            summary = "Modify Item";
                        }
                        VersionImpl version = versioningService.updateVersionHistory(ctx,item,summary,eventsToString(events),new Date());
                        if(version.getAIPBitstream()!=null);
                        {
                           addBundles = true;
                           versionId = version.getVersionId();
                        }
                    }
                    // TODO : Why are you updating the Item here? Leave it upto the service to decide.
                    //item.update();
                    // TODO : We should commit here and catch all exceptions?
                    ctx.getDBConnection().commit();

                } catch (Exception e) {
                    log.error(e.getMessage(),e);
                }
                finally
                {
                    ctx.restoreAuthSystemState();
                }
            }
            ctx.getDBConnection().commit();

        }

        itemsToProcess = null;
    }

    private String eventsToString(Collection<Event> events){
        String s = "";
        for(Event event : events)
        {

            s += event.toString() + "\n";
        }
        return s;
    }

}
