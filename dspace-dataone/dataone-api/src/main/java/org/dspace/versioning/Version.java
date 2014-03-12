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
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;
import java.util.Date;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface Version
{
    public EPerson getEperson();
    public int getItemID();
    public Date getVersionDate();
    public int getVersionNumber();
    public void setVersionNumber(int versionNumber);
    public String getSummary();
    public int getVersionHistoryID();
    public String getHandle();
    public int getVersionId();
    public Item getItem();
    public Bitstream getAIPBitstream();
    public void setAIPBitstream(int bitstream_id);
    public void setItemID(int item_id);
    public String getVerisonLog();
    public void setVersionLog(String log);
    public String[] getRestoreVersionLink(Context context,String knotId,String contextPath,boolean isLatestVersion);
    public String[] getViewVersionLink(Context context,String knotId,String contextPath,boolean isLatestVersion);
    public boolean canEditSummary(Context context,String knotId,String contextPath,boolean isLatestVersion);
    public boolean isLastestVersion(Context context,VersionHistory history);
    public Bitstream[] getBitstreams(Context context);
    public Bitstream getOREBitstream() ;
    public void setOREBitstream(int bitstream_id) ;
}

