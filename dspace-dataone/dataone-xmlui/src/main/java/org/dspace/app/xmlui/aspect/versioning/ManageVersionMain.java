/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.versioning;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DCDate;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionDAO;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;

/**
 * The manage version page is the starting point page for managing 
 * version. From here the user is able to browse or search for version, 
 * once identified the user can selected them for deletion by selecting 
 * the checkboxes and clicking delete or click their name to edit the 
 * version.
 *
 */
public class ManageVersionMain extends AbstractDSpaceTransformer
{
    private static Logger log = Logger.getLogger(VersionHistoryForm.class);
    /** Language Strings */
    private static final Message T_title =
            message("xmlui.administrative.version.ManageVersionMain.title");

    private static final Message T_version_trail =
            message("xmlui.administrative.version.general.version_trail");

    private static final Message T_main_head =
            message("xmlui.administrative.version.ManageVersionMain.main_head");

    private static final Message T_actions_head =
            message("xmlui.administrative.version.ManageVersionMain.actions_head");

    private static final Message T_actions_create =
            message("xmlui.administrative.version.ManageVersionMain.actions_create");

    private static final Message T_actions_create_link =
            message("xmlui.administrative.version.ManageVersionMain.actions_create_link");

    private static final Message T_actions_browse =
            message("xmlui.administrative.version.ManageVersionMain.actions_browse");

    private static final Message T_actions_browse_link =
            message("xmlui.administrative.version.ManageVersionMain.actions_browse_link");

    private static final Message T_actions_search =
            message("xmlui.administrative.version.ManageVersionMain.actions_search");

    private static final Message T_search_help =
            message("xmlui.administrative.version.ManageVersionMain.search_help");

    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_go =
            message("xmlui.general.go");

    private static final Message T_search_head =
            message("xmlui.administrative.version.ManageVersionMain.search_head");
    private static final Message T_search_column0 =
            message("xmlui.administrative.version.ManageVersionMain.search_column0");

    private static final Message T_search_column1 =
            message("xmlui.administrative.version.ManageVersionMain.search_column1");

    private static final Message T_search_column2 =
            message("xmlui.administrative.version.ManageVersionMain.search_column2");

    private static final Message T_search_column3 =
            message("xmlui.administrative.version.ManageVersionMain.search_column3");

    private static final Message T_search_column4 =
            message("xmlui.administrative.version.ManageVersionMain.search_column4");

    private static final Message T_search_column5 =
            message("xmlui.administrative.version.ManageVersionMain.search_column5");

    private static final Message T_search_column6 =
            message("xmlui.administrative.version.ManageVersionMain.search_column6");

    private static final Message T_submit_delete =
            message("xmlui.administrative.version.ManageVersionMain.submit_delete");

    private static final Message T_no_results =
            message("xmlui.administrative.version.ManageVersionMain.no_results");
    private static final Message T_submit_update = message("xmlui.aspect.versioning.VersionHistoryForm.update");
    /**
     * The total number of entries to show on a page
     */
    private static final int PAGE_SIZE = 15;


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null,T_version_trail);
    }


    public void addBody(Body body) throws WingException, SQLException
    {
        /* Get and setup our parameters */
        int page          = parameters.getParameterAsInteger("page",0);
        int highlightID   = parameters.getParameterAsInteger("highlightID",-1);
        String query      = decodeFromURL(parameters.getParameter("query",null));
        String baseURL    = contextPath+"/admin/versions?administrative-continue="+knot.getId();
        int resultCount   = VersionDAO.searchResultCount(context, query);
        Version[] versions = VersionDAO.search(context, query, page*PAGE_SIZE, PAGE_SIZE);
        boolean isAdmin = AuthorizeManager.isAdmin(context);
        // DIVISION: version-main
        Division main = body.addInteractiveDivision("version-main", contextPath
                + "/admin/versions", Division.METHOD_POST,
                "primary administrative version");
        main.setHead(T_main_head);

        // DIVISION: version-actions
        Division actions = main.addDivision("versions-actions");
        actions.setHead(T_actions_head);

        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_browse);
        actionsList.addItemXref(baseURL+"&query&submit_search",
                T_actions_browse_link);

        actionsList.addLabel(T_actions_search);
        org.dspace.app.xmlui.wing.element.Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("query");
        queryField.setAutofocus("autofocus");
        if (query != null)
        {
            queryField.setValue(query);
        }
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_search").setValue(T_go);

        // DIVISION: version-search
        Division search = main.addDivision("version-search");
        search.setHead(T_search_head);

        // If there are more than 10 results the paginate the division.
        if (resultCount > PAGE_SIZE)
        {
            // If there are enough results then paginate the results
            int firstIndex = page*PAGE_SIZE+1;
            int lastIndex = page*PAGE_SIZE + versions.length;

            String nextURL = null, prevURL = null;
            if (page < (resultCount / PAGE_SIZE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }

            search.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
        }

        //Table table = search.addTable("version-search-table", versions.length + 1, 1);
        Table table = search.addTable("versionhistory", 1, 1);
        createTableHeader(table, isAdmin);
        VersionHistory vh = null;
        for (Version version : versions)
        {

            if((vh==null||version.getVersionHistoryID()!=vh.getVersionHistoryId()))
            {
                vh = retrieveVersionHistory(version.getVersionHistoryID());
            }
            boolean isLatestVersion = version.isLastestVersion(context,vh);
            createVersionHistoryRow(table,version,isLatestVersion,isAdmin);
        }

        if (versions.length <= 0)
        {
            Cell cell = table.addRow().addCell(1, 4);
            cell.addHighlight("italic").addContent(T_no_results);
        }
        else
        {
            search.addPara().addButton("submit_delete").setValue(T_submit_delete);
        }

        main.addHidden("administrative-continue").setValue(knot.getId());
    }

    private void createTableHeader(Table table ,Boolean isAdmin) throws WingException
    {
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column0);
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);
        header.addCell().addContent(T_search_column5);
        header.addCell("actions",Row.ROLE_HEADER,"actions").addContent(T_search_column6);
    }

    private VersionHistory retrieveVersionHistory(Integer versionHistoryId) throws WingException
    {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        if(versionHistoryId==null)
        {
            return null;
        }
        return versioningService.findVersionByHistoryId(context, versionHistoryId);
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

        row.addCell().addContent(Integer.toString(version.getVersionHistoryID())+':'+Integer.toString(version.getVersionNumber()));
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
                actionRow.addXref(versionViewLink[0],versionViewLink[1]);
            }

            else
            {
                actionRow.addContent(versionViewLink[1]);
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

            //add the link to restore an old version
            boolean canEditSummary = version.canEditSummary(context,knot.getId(),contextPath,isLatestVersion);

            if(canEditSummary)
            {
                actionRow.addXref(contextPath + "/item/versionhistory?versioning-continue="+knot.getId()+"&versionID="+version.getVersionId() +"&itemID="+ version.getItemID() + "&submit_update", T_submit_update);
            }
            else
            {
                actionRow.addContent("");
            }
        }

    }

}
