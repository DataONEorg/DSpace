/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.versioning;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;

import java.io.IOException;
import java.sql.SQLException;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class FlowVersionUtils {


    /** log4j category */
    private static Logger log = Logger.getLogger(FlowVersionUtils.class);

    private static final Message T_version_created = new Message("default", "The new version has been created.");
    private static final Message T_version_delete = new Message("default", "The selected version(s) have been deleted.");
    private static final Message T_version_updated = new Message("default", "The version has been updated.");
    private static final Message T_version_restored = new Message("default", "The version has been restored.");


    /**
     * Create a new version of the specified item
     *
     * @param context The DSpace context
     * @param itemID  The id of the to-be-versioned item
     * @return A result object
     */
    // Versioning
    public static FlowResult processCreateNewVersion(Context context, int itemID, String summary) throws SQLException, AuthorizeException, IOException {
        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            Item item = Item.find(context, itemID);
            VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
            if (AuthorizeManager.isAdmin(context, item) || item.canEdit() || versioningService.canVersion(context, item)) {
                Version version = versioningService.createNewWorkingVersionInSubmission(context, itemID, summary);
                if(version!=null){
                    WorkspaceItem wsi = WorkspaceItem.findByItem(context, version.getItem());
                    //set the stage to be upload stage
                    wsi.setStageReached(3);
                    wsi.setPageReached(1);


                    wsi.update();
                    context.commit();
                    result.setParameter("id", wsi.getItem().getID());
                    result.setParameter("wsid", wsi.getID());
                    result.setParameter("handle",item.getCollections()[0].getHandle());
                    String handle = item.getCollections()[0].getHandle();
                    result.setOutcome(true);
                    result.setContinue(true);
                    result.setMessage(T_version_created);
                    result.setParameter("summary", summary);
                }
                else
                {
                    result.setOutcome(false);
                    result.setContinue(false);
                }
            }
        } catch (Exception ex) {
            context.abort();
            throw new RuntimeException(ex);
        }
        return result;
    }

    /**
     * Modify latest version
     *
     * @param context The DSpace context
     * @param itemID  The id of the to-be-versioned item
     * @return A result object
     */
    // Versioning
    public static FlowResult processUpdateVersion(Context context, int itemID, int versionId, String summary) throws SQLException, AuthorizeException, IOException {

        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            Item item = Item.find(context, itemID);

            if (AuthorizeManager.isAdmin(context, item)) {
                VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
                versioningService.updateVersionByVersionId(context,itemID, versionId, summary);

                context.commit();

                result.setOutcome(true);
                result.setContinue(true);
                result.setMessage(T_version_updated);
                result.setParameter("summary", summary);
            }
        } catch (Exception ex) {
            context.abort();
            throw new RuntimeException(ex);
        }
        return result;
    }


    /**
     * Restore a version
     *
     * @param versionID id of the version to restore
     * @param context   The DSpace context
     * @return A result object
     */
    // Versioning
    public static FlowResult processRestoreVersion(Context context, int versionID, String summary) throws SQLException, AuthorizeException, IOException {
        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
            DSpaceObject dso = versioningService.restoreVersion(context, versionID, summary);

            result.setOutcome(true);
            result.setContinue(true);
            result.setMessage(T_version_restored);
        } catch (Exception ex) {
            context.abort();
            throw new RuntimeException(ex);
        }
        return result;
    }


    /**
     * Delete version(s)
     *
     * @param context    The DSpace context
     * @param versionIDs list of versionIDs to delete
     * @return A result object
     */
    // Versioning
    public static FlowResult processDeleteVersions(Context context, int itemId, String[] versionIDs) throws SQLException, AuthorizeException, IOException, UIException {
        FlowResult result = new FlowResult();
        try {
            result.setContinue(false);

            VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);

            VersionHistory versionHistory = versioningService.findVersionHistory(context, itemId);

            for (String id : versionIDs) {
                versioningService.removeVersion(context, Integer.parseInt(id));
            }
            context.commit();

            //Retrieve the latest version of our history (IF any is even present)
            Version latestVersion = versionHistory.getLatestVersion();
            if(latestVersion == null){
                result.setParameter("itemID", null);
            }else{
                result.setParameter("itemID", latestVersion.getItemID());
            }
            result.setContinue(true);
            result.setOutcome(true);
            result.setMessage(T_version_delete);

        } catch (Exception ex) {
            log.error(ex.getMessage(),ex);
            if(context != null && context.getDBConnection() != null)
                context.abort();
            throw new RuntimeException(ex.getMessage(),ex);
        }
        return result;
    }
}
