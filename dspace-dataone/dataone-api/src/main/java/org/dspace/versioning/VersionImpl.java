/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;

import java.sql.SQLException;
import java.util.Date;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersionImpl implements Version {

    private int versionId;
    private EPerson eperson;
    private int itemID=-1;
    private Bitstream bitstream;
    private Date versionDate;
    private int versionNumber;
    private String summary;
    private String versionLog;
    private int versionHistoryID;
    private String handle;
    private Context myContext;
    private TableRow myRow;

    protected VersionImpl(Context c, TableRow row)
    {
        myContext = c;
        myRow = row;

        c.cache(this, row.getIntColumn(VersionDAO.VERSION_ID));
    }


    public int getVersionId()
    {
        return myRow.getIntColumn(VersionDAO.VERSION_ID);
    }

    protected void setVersionId(int versionId)
    {
        this.versionId = versionId;
    }

    public EPerson getEperson(){
        try {
            if (eperson == null)
            {
                return EPerson.find(myContext, myRow.getIntColumn(VersionDAO.EPERSON_ID));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return eperson;
    }

    public void setEperson(EPerson ePerson) {
        this.eperson = ePerson;
        myRow.setColumn(VersionDAO.EPERSON_ID, ePerson.getID());
    }

    public int getItemID() {
        return myRow.getIntColumn(VersionDAO.ITEM_ID);
    }


    public Item getItem(){
        try{
            if(getItemID()==-1)
            {
                return null;
            }

            return Item.find(myContext, getItemID());

        }catch(SQLException e){
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Bitstream getAIPBitstream() {
        try {
            if (bitstream == null)
            {
                return Bitstream.find(myContext, myRow.getIntColumn(VersionDAO.BITSTREAM_ID));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return bitstream;
    }

    @Override
    public void setAIPBitstream(int bitstream_id) {
        try {
            this.bitstream = Bitstream.find(myContext, bitstream_id);
            myRow.setColumn(VersionDAO.BITSTREAM_ID, bitstream_id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    @Override
    public Bitstream getOREBitstream() {
        try {
            if (bitstream == null)
            {
                return Bitstream.find(myContext, myRow.getIntColumn(VersionDAO.ORE_BITSTREAM_ID));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return bitstream;
    }

    @Override
    public void setOREBitstream(int bitstream_id) {
        try {
            this.bitstream = Bitstream.find(myContext, bitstream_id);
            myRow.setColumn(VersionDAO.ORE_BITSTREAM_ID, bitstream_id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void setItemID(int itemID)
    {
        this.itemID = itemID;
        if(itemID == -1)
        {
            myRow.setColumnNull(VersionDAO.ITEM_ID);
        }
        else{
            myRow.setColumn(VersionDAO.ITEM_ID, itemID);
        }

    }

    public Date getVersionDate() {
        return myRow.getDateColumn(VersionDAO.VERSION_DATE);
    }

    public void setVersionDate(Date versionDate) {
        this.versionDate = versionDate;
        myRow.setColumn(VersionDAO.VERSION_DATE, versionDate);
    }

    public int getVersionNumber() {
        return myRow.getIntColumn(VersionDAO.VERSION_NUMBER);
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
        myRow.setColumn(VersionDAO.VERSION_NUMBER, versionNumber);
    }

    public String getSummary() {
        return myRow.getStringColumn(VersionDAO.VERSION_SUMMARY);
    }

    public void setSummary(String summary) {
        this.summary = summary;
        myRow.setColumn(VersionDAO.VERSION_SUMMARY, summary);
    }
    public String getVerisonLog() {
        return myRow.getStringColumn(VersionDAO.VERSION_VERSIONlOG);
    }

    public void setVersionLog(String summary) {
        this.versionLog = summary;
        myRow.setColumn(VersionDAO.VERSION_VERSIONlOG, summary);
    }


    public int getVersionHistoryID() {
        return myRow.getIntColumn(VersionDAO.HISTORY_ID);
    }

    public void setVersionHistory(int versionHistoryID) {
        this.versionHistoryID = versionHistoryID;
        myRow.setColumn(VersionDAO.HISTORY_ID, versionHistoryID);
    }


    public Context getMyContext(){
        return myContext;
    }

    protected TableRow getMyRow(){
        return myRow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        VersionImpl version = (VersionImpl) o;

        return getVersionId() == version.getVersionId();

    }

    @Override
    public int hashCode() {
        int hash=7;
        hash=79*hash+(int) (this.getVersionId() ^ (this.getVersionId() >>> 32));
        return hash;
    }

    public String getHandle() {
        return myRow.getStringColumn(VersionDAO.VERSION_HANDLE);
    }

    public void setHandle(String handle) {
        this.handle = handle;
        myRow.setColumn(VersionDAO.VERSION_HANDLE, handle);
    }

    public String[] getRestoreVersionLink(Context context,String knotId,String contextPath,boolean isLatestVersion){
        String[] link = new String[2];
        Bitstream aipBitstream = getAIPBitstream();
        Item item = getItem();

        if(aipBitstream!=null)
        {
            //it is an archived version

            if(!isLatestVersion){
                //cell.addXref(contextPath + "/metadata/internal/bitstream/" + aipBitstream.getID()+"/mets.xml", itemHandle);
                if(item!=null){
                    link[0] = contextPath + "/item/versionhistory?versioning-continue="+knotId+"&versionID="+getVersionId() +"&itemID="+ item.getID()+"&submit_restore";
                    link[1] = "Restore";
                }
                else
                {
                    //should retore the latest version
                    link[0] = contextPath + "/item/versionhistory?versioning-continue="+knotId+"&versionID="+getVersionId() +"&itemID=-1&submit_restore";
                    link[1] = "Restore";
                }

            }
            else if(item!=null)
            {
                //this is the lastest version
                //String itemHandle = item.getHandle();
                //cell.addXref(contextPath + "/handle/" + itemHandle, itemHandle+"*");
            }
            else
            {
                //this item deleted
            }

        }
        else
        {
            //this version is in working status or in an error status , don't show any link for restore
            link[0]="";
            link[1]="";
        }

        return link;
    }
    public String[] getViewVersionLink(Context context,String knotId,String contextPath,boolean isLatestVersion){
        String[] link = new String[2];
        Bitstream aipBitstream = getAIPBitstream();
        Item item = getItem();

        if(aipBitstream!=null)
        {
            //it is an archived version

            if(!isLatestVersion){
                //this is a deleted version
                if(item!=null){
                    link[0] = contextPath + "/item/versionhistory?versioning-continue="+knotId+"&versionID="+getVersionId() +"&itemID="+ item.getID()+"&submit_show";
                    link[1] = "View";
                }
                else
                {
                    link[0] = contextPath + "/item/versionhistory?versioning-continue="+knotId+"&versionID="+getVersionId() +"&itemID="+ getItemID()+"&submit_show";
                    link[1] = "View";
                }
            }
            else if(item!=null)
            {
                //this is the lastest version
//                String itemHandle = item.getHandle();
//                link[0] = contextPath + "/handle/" + itemHandle;
//                link[1] = itemHandle+"*";
            }
            else
            {
                //"Item Deleted"
                link[0] = "";
                link[1] = "Item Deleted";
            }

        }else{
            //this version is in working status
            if(getItem()!=null)
            {
                //item in the working space
                try{
                    WorkspaceItem workspaceItem = WorkspaceItem.find(context,getItem().getID());
                    if(workspaceItem!=null)
                    {
                        link[0] = contextPath + "/submit?workspaceID="+getVersionId();
                        link[1] = "In Submission";
                    }
                    else
                    {
                        WorkflowItem workflowItem = WorkflowItem.find(context,getItem().getID());
                        if(workflowItem!=null)
                        {
                            link[0] = contextPath + "/admin/display-workflowItem?wfiId="+getVersionId();
                            link[1] = "In Workflow";
                        }
                    }

                }catch (Exception e)
                {
                    //log.error(e.getMessage(),e);
                }
            }
            else
            {
                //this is a deleted version or a broken version
                // has no aip bitstream or in the workflow
                //cell.addXref(contextPath + "/version?version="+version.getVersionId(),"*");
                link[0]="";
                link[1]="Deleted";
            }
        }
        return link;


    }
    public boolean canEditSummary(Context context,String knotId,String contextPath,boolean isLatestVersion){
        String[] link = new String[2];
        Bitstream aipBitstream = getAIPBitstream();
        Item item = getItem();

        if(aipBitstream!=null)
        {
            //it is an archived version

            if(!isLatestVersion){
                return true;
            }
            else if(item!=null)
            {
                //this is the lastest version
                return true;
            }
            else
            {
                return false;
            }

        }else{
            //this version is in working status
            if(getItem()!=null)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }
    public boolean isLastestVersion(Context context,VersionHistory history){
        boolean isLatestVersion = false;
        if(history==null)
        {
            VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
            history = versioningService.findVersionByHistoryId(context, getVersionHistoryID());
        }
        if(getVersionNumber()==history.getLatestVersion().getVersionNumber())
        {
            isLatestVersion = true;
        }
        return isLatestVersion;
    }
    public Bitstream[] getBitstreams(Context context){
        return VersionDAO.findAllBitstreams(context,versionId);
    }

}
