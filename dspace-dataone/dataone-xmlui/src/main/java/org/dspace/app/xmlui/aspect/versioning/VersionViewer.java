package org.dspace.app.xmlui.aspect.versioning;

import org.apache.cocoon.ProcessingException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: lantian @ atmire . com
 * Date: 9/27/13
 * Time: 3:52 PM
 */
public class VersionViewer extends AbstractDSpaceTransformer{

    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");
    private static final Message T_trail =
            message("xmlui.Version.versionhistory.trail");

    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        boolean isItemView=parameters.getParameterAsInteger("itemID",-1) == -1;

        // retrieve version ID from Request:
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);

        // Get our parameters and state
        int versionId = parameters.getParameterAsInteger("versionID",-1);

        Version version = versioningService.getVersion(context, versionId);

        VersionHistory vh = versioningService.findVersionByHistoryId(context, version.getVersionHistoryID());
        Version latest = vh.getLatestVersion();
        Item item = latest.getItem();

        if(item!=null){
            String title = item.getMetadata("dc.title")[0].value;
            pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
            HandleUtil.buildHandleTrail(item, pageMeta, contextPath);
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

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {

        //todo:add login check for version
        // retrieve version ID from Request:
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);

        // Get our parameters and state
        int versionId = parameters.getParameterAsInteger("versionID",-1);

        Version version = versioningService.getVersion(context, versionId);

        VersionHistory vh = versioningService.findVersionByHistoryId(context, version.getVersionHistoryID());
        Version latest = vh.getLatestVersion();
        Item item = latest.getItem();

        String title = "";

        if(item != null)
        {
            title += "Version for Item: " + item.getHandle();
        }
        else
        {
            title +="Version for deleted Item : "+version.getHandle();
        }

        // Build the item viewer division.
        Division division = body.addDivision("version-view","primary");
        division.setHead(title);

        //metadata/internal/*/*/mets.xml
        Bitstream bitstream = version.getAIPBitstream();
        if(bitstream!=null){
            ReferenceSet referenceSet = division.addReferenceSet("version-viewer", ReferenceSet.TYPE_DETAIL_VIEW);

            referenceSet.addReference(bitstream);

            Division restoreForm = division.addInteractiveDivision("restore-version",contextPath+"/restore-version",Division.METHOD_POST);
            List list = restoreForm.addList("version",List.TYPE_FORM);
            list.addItem().addHidden("versionId");
            //list.addItem().addButton("revision","revision").setValue("Restore this version");
        }
        else
        {
            division.addPara("No revision found");
        }
        Division id = division.addDivision("version-id");
        id.setHead("Version Id");
        id.addPara().addContent(version != null ? Integer.toString(version.getVersionId()) : "No ID");

        Division number = division.addDivision("version-number");
        number.setHead("Version Number");
        number.addPara().addContent(version != null ? Integer.toString(version.getVersionNumber()) : "No Number");

        Division itemDiv = division.addDivision("version-item");
        itemDiv.setHead("Item Id");
        itemDiv.addPara().addContent(version != null ? Integer.toString(version.getItemID()) : "No Item Id");

        Division summary = division.addDivision("version-summary");
        summary.setHead("Version Summary");
        summary.addPara().addContent(version != null ? version.getSummary() : "No summary");
        Division date = division.addDivision("version-date");
        date.setHead("Version Create Date");
        date.addPara().addContent(version != null ? version.getVersionDate().toString() : "No Date");
        Division editor = division.addDivision("version-editor");
        editor.setHead("Version Editor");
        editor.addPara().addXref(version != null ? "mailto:"+version.getEperson().getEmail():"",version != null ? version.getEperson().getFullName() : "No Editor");
        Division log = division.addDivision("version-log");
        log.setHead("Version Log");
        log.addPara().addContent(version != null ? version.getVerisonLog() : "No log");

    }
}
