/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.util.Date;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface VersioningService {

    //Version createNewVersion(Context c, int itemId);

    void removeVersion(Context c, int versionID);

    void removeVersion(Context c, Item item);

    Version getVersion(Context c, int versionID);

    Item restoreVersion(Context c, int versionID);

    Item restoreVersion(Context c, int versionID, String summary);

    VersionHistory findVersionHistory(Context c, int itemId);

    Version updateVersion(Context c, int itemId, String summary);

    Version updateVersionByVersionId(Context c, int itemId, int versionId, String summary);

    Version getVersion(Context c, Item item);

    boolean canVersion(Context c, Item item);

    Version createNewWorkingVersionInSubmission(Context context, int itemID, String summary);
    VersionImpl updateVersionHistory(Context c, Item item, String summary,String log, Date date);
    VersionImpl updateVersionHistory(Context c, Item item, String summary,String log, Date date,boolean restoreMode);
    VersionImpl updateVersionHistory(Context c, Item newItem, Item previousItem, String summary,String log, Date date);

    public VersionHistory findVersionByHistoryId(Context c, int versionHistoryId) ;
}
