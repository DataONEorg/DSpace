package org.dspace.versioning;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

import java.util.Date;

/**
 * Service that will update existing Versions that have their own items to also have
 * METS and ORE manifests. To be run as part of an upgrade process after installing
 * DataONE addon services.
 *
 * @author Mark Diggory
 * @author Lantian Gai
 */
public class ImportVersion2Bitstream {

    private  static VersionDAO versionDAO = new VersionDAO();

    private static VersionHistoryDAO versionHistoryDAO = new VersionHistoryDAO();

    private static final Logger log = Logger.getLogger(ImportVersion2Bitstream.class);

    public static void main(String[] argv)
    {
        Context context = null;
        try{
            context = new Context();
            context.turnOffAuthorisationSystem();
            ItemIterator items = Item.findAll(context);
            VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
            Date date = new Date();
            while(items.hasNext())
            {
                Item item = items.next();
                if(item.isArchived()&&!item.isWithdrawn())
                {
                    debug("processing item:" + item.getID());

                    VersionHistory vh = versioningService.findVersionHistory(context,item.getID());
                    if(vh==null){

                        vh=versionHistoryDAO.create(context);
                        VersionImpl versionImpl = versionDAO.create(context,item.getID(),!item.isArchived() && !item.isWithdrawn());

                        log.debug("create version:"+versionImpl.getVersionId());

                        versionImpl.setVersionDate(date);
                        versionImpl.setEperson(item.getSubmitter());
                        versionImpl.setItemID(item.getID());
                        if(item.getHandle()!=null)
                            versionImpl.setHandle(item.getHandle());
                        versionImpl.setSummary("initial version");
                        versionImpl.setVersionLog("initial import");
                        versionImpl.setVersionNumber(0);

                        vh.add(versionImpl);
                        versionImpl.setVersionHistory(vh.getVersionHistoryId());

                        AIPManifestWriter aipManifestWriter = new AIPManifestWriter();
                        Bitstream bitstream = aipManifestWriter.updateAIP(context,item,true);
                        versionImpl.setAIPBitstream(bitstream.getID());
                        AuthorizeManager.inheritPolicies(context, item, bitstream);

                        OREManifestWriter oreManifestWriter = new OREManifestWriter();
                        Bitstream b = oreManifestWriter.updateORE(context,item,versionImpl, true);
                        versionImpl.setOREBitstream(b.getID());
                        AuthorizeManager.inheritPolicies(context, item, b);

                        VersionDAO.addBitstreams(context,versionImpl.getVersionId(),item.getBundles());

                        versionDAO.update(versionImpl);
                        context.commit();
                    }
                    else
                    {
                        debug("found version history:" + vh.getVersionHistoryId());

                        for(Version version : vh.getVersions())
                        {
                            Item vItem = version.getItem();

                            debug("found version:" + version.getVersionId());

                            if(version.getVersionNumber() > -1 && vItem != null)
                            {
                                boolean updated = false;

                                if(version.getAIPBitstream() == null)
                                {
                                    AIPManifestWriter aipManifestWriter = new AIPManifestWriter();
                                    Bitstream bitstream = aipManifestWriter.updateAIP(context, vItem, true);
                                    version.setAIPBitstream(bitstream.getID());
                                    AuthorizeManager.inheritPolicies(context, item, bitstream);
                                    updated = true;
                                }
                                else
                                {
                                    AuthorizeManager.removePoliciesActionFilter(context, version.getAIPBitstream(), Constants.READ);
                                    AuthorizeManager.inheritPolicies(context, item, version.getAIPBitstream());
                                    version.getAIPBitstream().setFormat(BitstreamFormat.findByShortDescription(context, "http://www.loc.gov/METS/"));

                                    debug("has AIP Bitstream:" + version.getAIPBitstream().getID());
                                    debug("has AIP Bitstream Format:" + version.getAIPBitstream().getFormat().getShortDescription());

                                    for(ResourcePolicy policy : AuthorizeManager.getPoliciesActionFilter(context, version.getAIPBitstream(), Constants.READ))
                                    {
                                        debug("has AIP Bitstream READ Policy:" + policy.getID() + " " + policy.getGroupID() + " " + policy.getEPersonID());
                                    }



                                }

                                if(version.getOREBitstream() == null)
                                {
                                    OREManifestWriter oreManifestWriter = new OREManifestWriter();
                                    Bitstream b = oreManifestWriter.updateORE(context,vItem,version, true);
                                    version.setOREBitstream(b.getID());
                                    AuthorizeManager.inheritPolicies(context, item, b);
                                    updated = true;
                                }
                                else
                                {
                                    AuthorizeManager.removePoliciesActionFilter(context, version.getOREBitstream(), Constants.READ);
                                    AuthorizeManager.inheritPolicies(context, item, version.getOREBitstream());
                                    version.getOREBitstream().setFormat(BitstreamFormat.findByShortDescription(context, OREManifestWriter.ORE.NS));

                                    debug("has ORE Bitstream:" + version.getOREBitstream().getID());
                                    debug("has ORE Bitstream Format:" + version.getOREBitstream().getFormat().getShortDescription());

                                    for(ResourcePolicy policy : AuthorizeManager.getPoliciesActionFilter(context, version.getAIPBitstream(), Constants.READ))
                                    {
                                        debug("has ORE Bitstream READ Policy:" + policy.getID() + " " + policy.getGroupID() + " " + policy.getEPersonID());
                                    }
                                }

                                if(version.getBitstreams(context) == null || version.getBitstreams(context).length < 1)
                                {
                                    VersionDAO.addBitstreams(context,version.getVersionId(),vItem.getBundles());
                                    updated = true;
                                }
                                else
                                {
                                    String result =  "has Attached Content Bitstreams:";

                                    for(Bitstream b : version.getBitstreams(context))
                                    {
                                        result += " " + b.getID();
                                    }

                                    debug(result);
                                }

                                if(updated)
                                {
                                    //TODO : DAO should not be exposed outside of service
                                    versionDAO.update((VersionImpl)version);
                                }


                            }

                        }
                    }

                }
                context.clearCache();
            }
            context.restoreAuthSystemState();
            context.complete();
        }catch (Exception e)
        {
            if(context!=null)
            {
                context.restoreAuthSystemState();
                context.abort();
            }
           System.out.println(e.getMessage());
            System.out.println(e.fillInStackTrace());
            log.error(e.getMessage(),e);
        }

    }

    private static void debug(String message)
    {
        System.out.println(message);
        log.debug(message);
    }

}
