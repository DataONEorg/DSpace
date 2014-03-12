/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.versioning;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersionHistoryForm extends AbstractDSpaceTransformer {
    /** Language strings */

    private static final Message T_head2 = message("xmlui.aspect.versioning.VersionHistoryForm.head2");
    private static final Message T_column1 = message("xmlui.aspect.versioning.VersionHistoryForm.column1");
    private static final Message T_column2 = message("xmlui.aspect.versioning.VersionHistoryForm.column2");
    private static final Message T_column3 = message("xmlui.aspect.versioning.VersionHistoryForm.column3");
    private static final Message T_column4 = message("xmlui.aspect.versioning.VersionHistoryForm.column4");
    private static final Message T_column5 = message("xmlui.aspect.versioning.VersionHistoryForm.column5");
    private static final Message T_column6 = message("xmlui.aspect.versioning.VersionHistoryForm.column6");
    private static final Message T_column7 = message("xmlui.aspect.versioning.VersionHistoryForm.column7");
    private static final Message T_submit_update = message("xmlui.aspect.versioning.VersionHistoryForm.update");
    private static final Message T_submit_cancel = message("xmlui.aspect.versioning.VersionHistoryForm.return");
    private static final Message T_submit_delete = message("xmlui.aspect.versioning.VersionHistoryForm.delete");
    private static final Message T_legend = message("xmlui.aspect.versioning.VersionHistoryForm.legend");
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");
    private static final Message T_trail =
            message("xmlui.Version.versionhistory.trail");





    /** Language Strings */
    private static final Message T_title =
            message("xmlui.administrative.version.ManageVersionMain.title");

    private static final Message T_version_trail =
            message("xmlui.administrative.version.general.version_trail");

    private static final Message T_go =
            message("xmlui.general.go");



    /**
     * The total number of entries to show on a page
     */
    private static final int PAGE_SIZE = 15;

    /** log4j category */
    private static Logger log = Logger.getLogger(VersionHistoryForm.class);
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        boolean isItemView=parameters.getParameterAsInteger("itemID",-1) != -1;
        if(isItemView){
        Item item = getItem();
        VersionHistory versionHistory = retrieveVersionHistory(item);
        if(versionHistory!=null&&!versionHistory.getLatestVersion().getItem().equals(item))
        {  //always use the latest item
            item = versionHistory.getLatestVersion().getItem();
        }

        if(item!=null){
            String title = item.getMetadata("dc.title")[0].value;
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(item,pageMeta,contextPath);
            if(title!=null&&title.length()>0)
            pageMeta.addTrailLink(contextPath+"/handle/"+item.getHandle(),title);
            else
            pageMeta.addTrail().addContent("Item");
        pageMeta.addTrail().addContent(T_trail);
        }
        else
        {
            pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
            pageMeta.addTrail().addContent(T_trail);
        }
        }
        else
        {
            pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
            pageMeta.addTrail().addContent("Versions");
        }
    }
    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {

        boolean isItemView=parameters.getParameterAsInteger("itemID",-1) != -1;
        boolean isAdmin = AuthorizeManager.isAdmin(context);
        if(isItemView)
        {
        Item item = getItem();
        VersionHistory versionHistory = retrieveVersionHistory(item);
        if(versionHistory!=null&&!versionHistory.getLatestVersion().getItem().equals(item))
        {  //always use the latest item
            item = versionHistory.getLatestVersion().getItem();
        }

        if(item==null || !AuthorizeManager.isAdmin(this.context, item.getOwningCollection()))
        {

                //Check if only administrators can view the item history
                if(new DSpace().getConfigurationService().getPropertyAsType("versioning.item.history.view.admin", false))
                {
                    return;
                }

        }




        if(versionHistory!=null)
        {
            Division main = createMain(body);
            createTableForItem(main, versionHistory, isAdmin);

            if(isAdmin){
                addButtons(main);
                main.addHidden("versioning-continue").setValue(knot.getId());
            }

            Para note = main.addPara();
            note.addContent(T_legend);
        }
        }
    }


    private Item getItem() throws WingException
    {
        try
        {
            if(parameters.getParameterAsInteger("itemID",-1) == -1)
            {
                DSpaceObject dso;
                dso = HandleUtil.obtainHandle(objectModel);
                if (!(dso instanceof Item))
                {
                    return null;
                }
                return (Item) dso;
            }else{
                return Item.find(context, parameters.getParameterAsInteger("itemID", -1));
            }
        } catch (SQLException e) {
            throw new WingException(e);
        }


    }

    private VersionHistory retrieveVersionHistory(Item item) throws WingException
    {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        if(item==null)
        {
            return null;
        }
        return versioningService.findVersionHistory(context, item.getID());
    }


    private Division createMain(Body body) throws WingException
    {
        Division main = body.addInteractiveDivision("view-verion-history", contextPath+"/item/versionhistory", Division.METHOD_POST, "view version history");
        main.setHead(T_head2);
        return main;
    }
    private void createVersionHistoryRow(Table table,Version version,boolean isLatestVersion,boolean isAdmin)throws WingException
    {
        Row row = table.addRow(null, Row.ROLE_DATA,"metadata-value");
        if(isAdmin)
        {
            CheckBox remove = row.addCell().addCheckBox("remove");
            remove.setLabel("remove");
            remove.addOption(version.getVersionId());
        }

        row.addCell().addXref(contextPath + "/item/versionhistory?versioning-continue="+knot.getId()+"&versionID="+version.getVersionId() +"&itemID="+ version.getItemID()+"&submit_show", Integer.toString(version.getVersionId()));
        row.addCell().addContent(Integer.toString(version.getVersionNumber()));
        if(version.getItem()!=null&&isLatestVersion)
        {
            row.addCell().addXref("/handle/"+version.getHandle(),version.getHandle()+"*");
        }
        else
        {
            row.addCell().addContent(version.getHandle());
        }

        row.addCell().addContent(new DCDate(version.getVersionDate()).toString());
        row.addCell("summary",Cell.ROLE_DATA,"version-summary").addContent(version.getSummary());
        if(isAdmin)
        {
            //ACTION SECTION
            Cell actionRow = row.addCell("actions",Row.ROLE_DATA,"actions");
            //add the link of the version information detail page
            String[] versionViewLink = version.getViewVersionLink(context,knot.getId(),contextPath,isLatestVersion);
            if(versionViewLink[0]!=null&&versionViewLink[0].length()>0)
            {
                actionRow.addXref(versionViewLink[0],versionViewLink[1]);//,item,version,isLatestVersion);
            }

            else
            {
                actionRow.addContent(versionViewLink[1]);//,item,version,isLatestVersion);
            }
            //add the link to restore an old version
            String[] restorelink = version.getRestoreVersionLink(context,knot.getId(),contextPath,isLatestVersion);

            if(restorelink[0]!=null&&restorelink[0].length()>0)
            {
                actionRow.addXref(restorelink[0],restorelink[1]);
            }
            else
            {
                actionRow.addContent(restorelink[1]);
            }
            actionRow.addXref(contextPath + "/item/versionhistory?versioning-continue="+knot.getId()+"&versionID="+version.getVersionId() +"&itemID="+ version.getItemID() + "&submit_update", T_submit_update);

        }

    }
    private void createTableForItem(Division main, VersionHistory history, boolean isAdmin) throws WingException, SQLException
    {
        Table table = main.addTable("versionhistory", 1, 1);

        createTableHeader(table,isAdmin);

        if(history != null)
        {
            for(Version version : history.getVersions())
            {

                //Skip items currently in submission
                if(version.getItem()!=null&&isItemInSubmission(version.getItem()))
                {
                    continue;
                }
                boolean isLatestVersion = false;
                if(version.getVersionNumber()==history.getLatestVersion().getVersionNumber())
                {
                    isLatestVersion = true;
                }
                createVersionHistoryRow(table,version,isLatestVersion,isAdmin);
            }
        }
    }

    private void createTableHeader(Table table ,Boolean isAdmin) throws WingException
    {
        Row header = table.addRow(Row.ROLE_HEADER);
        if(isAdmin)
        {
            header.addCell().addContent("");
        }
        header.addCell().addContent(T_column1);
        header.addCell().addContent(T_column2);
        header.addCell().addContent(T_column3);
        header.addCell().addContent(T_column4);
        header.addCell().addContent(T_column5);
        header.addCell("actions",Row.ROLE_HEADER,"actions").addContent(T_column6);
    }
    private boolean isItemInSubmission(Item item) throws SQLException
    {
        WorkspaceItem workspaceItem = WorkspaceItem.findByItem(context, item);
        InProgressSubmission workflowItem;
        if(ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow"))
        {
            workflowItem = XmlWorkflowItem.findByItem(context, item);
        }else{
            workflowItem = WorkflowItem.findByItem(context, item);
        }

        return workspaceItem != null || workflowItem != null;
    }

    private void addButtons(Division main) throws WingException {
        Para actions = main.addPara();
        actions.addButton("submit_delete").setValue(T_submit_delete);
        actions.addButton("submit_cancel").setValue(T_submit_cancel);
    }

}
