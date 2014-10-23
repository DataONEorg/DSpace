/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.apache.log4j.Logger;
import org.dspace.app.packager.Packager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.bitstore.BitstreamStorageOutputStream;
import org.dspace.bitstore.ExtendedBitstreamStorageManager;
import org.dspace.content.*;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.utils.DSpace;
import org.jaxen.expr.iter.IterableNamespaceAxis;
import org.springframework.beans.factory.annotation.Required;

import java.io.*;
import java.util.Date;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersioningServiceImpl implements VersioningService{

    /** log4j category */


    private static final Logger log = Logger.getLogger(VersioningServiceImpl.class);

    private VersionHistoryDAO versionHistoryDAO;
    private VersionDAO versionDAO;
    private DefaultItemVersionProvider provider;


    /** Service Methods */
//    public Version createNewVersion(Context c, int itemId){
//        return createNewVersion(c, itemId);
//    }


    public Version createNewWorkingVersionInSubmission(Context c, int itemId, String summary) {
        try{
            Item item = Item.find(c, itemId);

            Date date = new Date();
            if(item==null)
            {
                VersionHistory history = findVersionHistory(c, itemId);
                item = history.getLatestVersion().getItem();

            }

            // Create new Workspace Item
            Item itemNew = provider.createNewItemAndAddItInWorkspace(c, item);

            // Complete any update of the Item and new Identifier generation that needs to happen
            provider.updateItemState(c, itemNew, item);

            Version version = this.updateVersionHistory(c, itemNew, item, summary,"create new version in submission", date);

            return version;

        }catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void removeVersion(Context c, int versionID) {
        Version version = versionDAO.find(c, versionID);
        if(version!=null){
            removeVersion(c, version);
        }
    }

    public void removeVersion(Context c, Item item) {

        //comment it out because we want to keep the version history so we can restore the deleted item later
//        Version version = versionDAO.findByItem(c, item);
//        if(version!=null){
//            removeVersion(c, version);
//        }
    }

    protected void removeVersion(Context c, Version version) {
        try{
            VersionHistory history = versionHistoryDAO.findById(c, version.getVersionHistoryID(), versionDAO);
            provider.deleteVersionedItem(c, version, history);
            versionDAO.delete(c, version.getVersionId());

            history.remove(version);

            if(history.isEmpty()){
                versionHistoryDAO.delete(c, version.getVersionHistoryID(), versionDAO);
            }
            //Delete the item linked to the version
            Item item = version.getItem();

            if(item !=null)
            {
                Collection[] collections = item.getCollections();

                // Remove item from all the collections it's in (so our item is also deleted)
                for (Collection collection : collections)
                {
                    collection.removeItem(item);
                }
            }

            Bitstream bit = version.getAIPBitstream();

            if(bit != null)
            {
                BitstreamUtil.delete(bit);
            }

        }catch (Exception e) {
            c.abort();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public VersionHistory findVersionByHistoryId(Context c, int versionHistoryId) {
        try{
            VersionHistory history = versionHistoryDAO.findById(c,versionHistoryId, versionDAO);
            return history;
        }catch (Exception e) {
            c.abort();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Version getVersion(Context c, int versionID) {
        return versionDAO.find(c, versionID);
    }


    public Item restoreVersion(Context c, int versionID){
        return restoreVersion(c, versionID, null);
    }

    public Item restoreVersion(Context c, int versionID, String summary)  {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        Version version = versioningService.getVersion(c,versionID);
        Integer versionHistoryId = version.getVersionHistoryID();
        VersionHistory versionHistory = versioningService.findVersionByHistoryId(c,version.getVersionHistoryID());
        String packageType = "INTERNAL-AIP";
        Bitstream bitstream = version.getAIPBitstream();
        Item newItem = version.getItem();
        if(bitstream!=null)
        {
            Version latestVersionversion = versionHistory.getLatestVersion();
            Item itemToReplace = latestVersionversion.getItem();
            {
                if(!version.equals(latestVersionversion))
                {
                    //todo:replace item like aip replace mode
                    //create a new version??
                    PackageIngester sip = (PackageIngester) PluginManager
                            .getNamedPlugin(PackageIngester.class, "INTERNAL-AIP");
                    if (sip == null)
                    {
                        //todo: better handle errors
                        return null;
                    }

                    // todo:don't replace , create a new item instead


                    try
                    {
                        PackageParameters pkgParams =new PackageParameters();

                        File sourceFile = new File("/tmp/version_"+version.getVersionId());
                        sourceFile.createNewFile();

                        String metsFile = "";
                        InputStream inputStream =bitstream.retrieve();
                        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

                        StringBuilder sb = new StringBuilder();

                        // write the inputStream to a FileOutputStream
                        OutputStream outputStream =new FileOutputStream(sourceFile);

                        int read = 0;
                        byte[] bytes = new byte[1024];

                        while ((read = inputStream.read(bytes)) != -1) {
                            outputStream.write(bytes, 0, read);
                        }
                        pkgParams.setProperty("manifestOnly","true");
                        pkgParams.setProperty("internal","true");
                        //replace the object from the source file
                        newItem = (Item)sip.replace(c, itemToReplace, sourceFile, pkgParams);

                        Date date = new Date();
                        //update the version history with the new item we just created or updated
                        VersionImpl newVerison = this.updateVersionHistory(c,newItem,summary,"", date,true);

                        //set the new version number to be -1 so we can update this number in consumer
                        //commit all changes & exit successfully
                        c.getDBConnection().commit();


                    }
                    catch (Exception e)
                    {
                        // abort all operations
                        e.printStackTrace();
                        c.abort();
                        System.out.println(e);
                    }
                }

            }
        }
        return newItem;
    }

    public VersionHistory findVersionHistory(Context c, int itemId){
        return versionHistoryDAO.find(c, itemId, versionDAO);
    }
    public Version updateVersion(Context c, int itemId, String summary) {
        VersionImpl version = versionDAO.findByItemId(c, itemId);
        version.setSummary(summary);
        versionDAO.update(version);
        return version;
    }
    public Version updateVersionByVersionId(Context c, int itemId, int versionId,String summary) {
        VersionImpl version = versionDAO.find(c,versionId);
        version.setSummary(summary);
        versionDAO.update(version);
        return version;
    }

    public Version getVersion(Context c, Item item){
        return versionDAO.findByItemId(c, item.getID());
    }

    /**
     * Update version history will be called when
     * a.) Manually Created New Revision is added to submitters Workspace (version number = -1)
     * b.) Consumer generates new revision (version number = latest +1)
     * c.) InstallItem Creates New Version Record (new Item submitted from Workspace)
     *
     * @param c
     * @param item
     * @param summary
     * @param date
     */
    public VersionImpl updateVersionHistory(Context c, Item item, String summary,String log, Date date) {
        return updateVersionHistory(c, item, null, summary, log ,date,false);
    }
    public VersionImpl updateVersionHistory(Context c, Item item, String summary,String log, Date date,boolean restoreMod) {
        return updateVersionHistory(c, item, null, summary, log ,date,restoreMod);
    }
    public VersionImpl updateVersionHistory(Context c, Item newItem, Item previousItem, String summary,String log, Date date) {
        return updateVersionHistory( c, newItem, previousItem,  summary, log,  date,false);
    }
    public VersionImpl updateVersionHistory(Context c, Item newItem, Item previousItem, String summary,String logs, Date date,boolean restoreMode) {
        VersionImpl versionImpl=null;
        try {

            VersionHistory history = null;
            history = findVersionHistory(c, newItem.getID());
            if(history==null&&previousItem != null)
            {
                history = findVersionHistory(c, previousItem.getID());

                if(history == null)      {
                    Version origVersion =  updateVersionHistory(c, previousItem, "Original Version", "",date);
                    history = findVersionHistory(c, previousItem.getID());
                }
            }

            if(history==null)
            {
                history = versionHistoryDAO.create(c);
            }

            // findByItem should return the either
            // (a) No version because it does not yet exist
            // (b) latest Version in history with same item id (should always be just one)
            // (c) the only working Version for this item (version number = -1)
            versionImpl = versionDAO.findByItem(c,newItem);

            //if there is no version or it's not a working version or it is a restore mode, create a new version
            if(versionImpl == null||versionImpl.getVersionNumber()!=-1||restoreMode)
            {
                if(previousItem==null&&history!=null)
                {
                    previousItem = history.getLatestVersion().getItem();
                }
                // if version item is null , create a new version for the item
                // A submission or workflow Item is neither archived nor withdrawn
                // if so, its a "working version", create and assign -1 to version
                versionImpl = versionDAO.create(c,newItem.getID(),!newItem.isArchived() && !newItem.isWithdrawn());
                versionImpl.setVersionDate(date);
                versionImpl.setEperson(newItem.getSubmitter());
                versionImpl.setItemID(newItem.getID());
                if(newItem.getHandle()!=null)
                    versionImpl.setHandle(newItem.getHandle());
                versionImpl.setSummary(summary);
                versionImpl.setVersionLog(logs);
                if(restoreMode)
                {
                    versionImpl.setVersionNumber(-1);
                }
                history.add(versionImpl);
                versionImpl.setVersionHistory(history.getVersionHistoryId());
                versionDAO.update(versionImpl);
            }
            else
            {
                //it is a promotion action
                if(!newItem.isArchived() && !newItem.isWithdrawn())
                {
                    // If Item is in Workspace or Workflow, keep the version -1, don't write AIP yet
                    versionImpl.setVersionNumber(-1);
                    //versionImpl.setSummary("add new work space item");
                }
                else
                {
                    // If it is a working version that has been archived, get next number, write AIP
                    if(previousItem==null)
                    {
                        previousItem = history.getLatestVersion().getItem();
                    }
                    try{
                        if(previousItem!=null&&previousItem.getID()!=newItem.getID()){

                            // Removed, because of this causes a recursive delete of the item and version
                            // in collection.removeItem(...);
                            //Collection[] collections= previousItem.getCollections();
                            //for(Collection collection:collections)
                            //{
                            //    collection.removeItem(previousItem);
                            //}
                            // Instead, just set the Item to not be discoverable
                            previousItem.setDiscoverable(false);
                            previousItem.update();
                        }
                    }catch (Exception e)
                    {
                        log.error(e.getMessage(),e);
                    }
                    versionImpl.setVersionNumber(getNextVersionNumer(history.getLatestVersion()));
                    //versionImpl.setSummary("archive new version");



                }

            }

            if(newItem.isArchived()
                    && !newItem.isWithdrawn()
                    && versionImpl.getAIPBitstream()==null
                    && !restoreMode){
                //the aip bitstream is null
                AIPManifestWriter aipManifestWriter = new AIPManifestWriter();
                Bitstream bitstream = aipManifestWriter.updateAIP(c,newItem,true);
                versionImpl.setAIPBitstream(bitstream.getID());
                AuthorizeManager.inheritPolicies(c, newItem, bitstream);

                OREManifestWriter oreManifestWriter = new OREManifestWriter();
                Bitstream b = oreManifestWriter.updateORE(c,newItem,versionImpl,true);
                versionImpl.setOREBitstream(b.getID());
                AuthorizeManager.inheritPolicies(c, newItem, b);

                VersionDAO.addBitstreams(c,versionImpl.getVersionId(),newItem.getBundles());
            }
            if(summary!=null&&summary.length()>0&&(versionImpl.getSummary()==null||versionImpl.getSummary().length()==0))
            {
                //only update the version summary when it is empty
                versionImpl.setSummary(summary);
            }
            if(logs!=null&&summary.length()>0&&(versionImpl.getVerisonLog()==null||versionImpl.getVerisonLog().length()==0))
            {
                //only update the version summary when it is empty
                versionImpl.setVersionLog(logs);
            }
            if(newItem.getHandle()!=null)
                versionImpl.setHandle(newItem.getHandle());
            else if(previousItem.getHandle()!=null)
            {
                versionImpl.setHandle(previousItem.getHandle());
            }
            versionDAO.update(versionImpl);

            return versionImpl;

        }catch (Exception e)
        {
            log.error(e.getMessage(),e);
            return versionImpl;
        }

    }


    protected int getNextVersionNumer(Version latest){
        if(latest==null) return 1;

        return latest.getVersionNumber()+1;
    }

    public VersionHistoryDAO getVersionHistoryDAO() {
        return versionHistoryDAO;
    }

    public void setVersionHistoryDAO(VersionHistoryDAO versionHistoryDAO) {
        this.versionHistoryDAO = versionHistoryDAO;
    }

    public VersionDAO getVersionDAO() {
        return versionDAO;
    }

    public void setVersionDAO(VersionDAO versionDAO) {
        this.versionDAO = versionDAO;
    }

    @Required
    public void setProvider(DefaultItemVersionProvider provider) {
        this.provider = provider;
    }


    public boolean canVersion(Context c, Item item){
        boolean canVersion = false;
        try{
            if(AuthorizeManager.isAdmin(c, item.getOwningCollection())||AuthorizeManager.authorizeActionBoolean(c,item, Constants.WRITE,false)||item.getSubmitter().equals(c.getCurrentUser()))
            {
                canVersion = true;
            }
            else
            {
                DSpaceObject parentObject = item.getParentObject();
                while(parentObject!=null&&parentObject.getType()!=Constants.SITE)
                {
                    //todo:add reviwers and editers check
                    parentObject = parentObject.getParentObject();
                }
            }
        }catch (Exception e)
        {

        }
        return canVersion;
    }
    public void setVersionLog(VersionImpl version,String versionLog){

        version.setVersionLog(versionLog);
    }
}
