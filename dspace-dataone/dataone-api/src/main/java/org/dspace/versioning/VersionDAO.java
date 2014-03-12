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
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersionDAO
{

    protected final static String TABLE_NAME = "versionitem";
    protected final static String VERSION_ID = "versionitem_id";
    protected final static String ITEM_ID = "item_id";
    protected final static String VERSION_NUMBER = "version_number";
    protected final static String EPERSON_ID = "eperson_id";
    protected final static String VERSION_DATE = "version_date";
    protected final static String VERSION_SUMMARY = "version_summary";
    protected final static String VERSION_VERSIONlOG = "version_log";
    protected final static String HISTORY_ID = "versionhistory_id";
    protected final static String BITSTREAM_ID = "bitstream_id";
    protected final static String ORE_BITSTREAM_ID = "ore_bitstream_id";
    protected final static String VERSION_HANDLE = "handle";
    public VersionImpl find(Context context, int id) {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context, TABLE_NAME, VERSION_ID, id);

            if (row == null)
            {
                return null;
            }

            return new VersionImpl(context, row);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    public VersionImpl findByItem(Context c, Item item) {
        return findByItemId(c, item.getID());
    }

    /**
     * This method should always return the latest version if there are more than one.
     *
     * @param context
     * @param itemId
     * @return
     */
    public VersionImpl findByItemId(Context context, int itemId) {
        try {
            if (itemId == 0 || itemId == -1)
            {
                return null;
            }

            TableRowIterator tri = DatabaseManager.queryTable(context,TABLE_NAME, "SELECT * FROM " + TABLE_NAME + " where " + ITEM_ID + "=" + itemId + " order by " + VERSION_NUMBER + " desc");
            if(tri.hasNext())
            {
                TableRow tr = tri.next();

                VersionImpl fromCache = (VersionImpl) context.fromCache(VersionImpl.class, tr.getIntColumn(VERSION_ID));

                if (fromCache != null)
                {
                    return fromCache;
                }else{
                    return new VersionImpl(context, tr);
                }
            }
            else
            {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public VersionImpl findByItemIdInprogress(Context context,int itemId){
        try {
            //find the -1 version for this item first
            TableRowIterator tri = DatabaseManager.queryTable(context,TABLE_NAME, "SELECT * FROM " + TABLE_NAME + " where " + ITEM_ID + "=" + itemId + " and version_number = -1 order by " + VERSION_ID + " desc");
            if(tri.hasNext())
            {
                TableRow tr = tri.next();

                VersionImpl fromCache = (VersionImpl) context.fromCache(VersionImpl.class, tr.getIntColumn(VERSION_ID));

                if (fromCache != null)
                {
                    return fromCache;
                }else{
                    return new VersionImpl(context, tr);
                }
            }
            else
                return null;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    public List<Version> findByVersionHistory(Context context, int versionHistoryId) {
        TableRowIterator tri = null;
        try {
            tri = DatabaseManager.queryTable(context,TABLE_NAME, "SELECT * FROM " + TABLE_NAME + " where " + HISTORY_ID + "=" + versionHistoryId + " order by " + VERSION_NUMBER + " desc");

            List<Version> versions = new ArrayList<Version>();
            while (tri.hasNext())
            {
                TableRow tr = tri.next();

                VersionImpl fromCache = (VersionImpl) context.fromCache(VersionImpl.class, tr.getIntColumn(VERSION_ID));

                if (fromCache != null)
                {
                    versions.add(fromCache);
                }else{
                    versions.add(new VersionImpl(context, tr));
                    context.cache(versions,versionHistoryId);
                }
            }
            return versions;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (tri != null)
            {
                tri.close();
            }
        }

    }


    public VersionImpl create(Context context,int itemId, boolean isWorkspace) {
        try {

            // if workspace, versionnumber = -1

            // If archived, version number is next version
            // move item_id if from previous to new version.
            // update previous version
            TableRow row = DatabaseManager.create(context, TABLE_NAME);
            VersionImpl v = new VersionImpl(context, row);
            if(isWorkspace)
            {
                v.setVersionNumber(-1);
            }
            else
            {
                VersionImpl preVersion = findByItemId(context, itemId);
                if(preVersion!=null)
                {
                    //preVersion.setItemID(-1);
                    // TODO : DOESN'T THIS NEED TO BE UPDATED TO HAVE PRESERVED IN NEXT COMMIT?  YES
                    //todo:we need update it here, but with a different method because the old version item has already been set to be -1
                    //this.update(preVersion);
                    // TODO : SHOULDN'T THIS HAVE +1 ADDED TO IT?
                    v.setVersionNumber(preVersion.getVersionNumber() + 1);
                }
                else
                {
                    v.setVersionNumber(0);
                }

            }

            return v;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    public void delete(Context c, int versionID) {
        try {
            // Remove ourself
            VersionImpl version = find(c, versionID);
            if(version!=null){
                //Remove ourself from our cache first !
                c.removeCached(version, version.getVersionId());

                DatabaseManager.delete(c, version.getMyRow());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    public void update(VersionImpl version) {
        try {
            Item item = version.getItem();
            if(item==null)
            {
                DatabaseManager.update(version.getMyContext(), version.getMyRow());
            }
            else{
//            if(item.isArchived()&&version.getVersionNumber()==-1)
//            {
//                //don't update
//               throw new RuntimeException("Archived item should not have a version number equals -1");
//            }
//            else
                if(!item.isArchived()&&version.getVersionNumber()!=-1)
                {
                    //don't update
                    throw new RuntimeException("None Archived item should have a version number euqals -1");
                }
                else
                {
                    //update
                    DatabaseManager.update(version.getMyContext(), version.getMyRow());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    public static Version[] search(Context context, String query, int offset, int limit)
            throws SQLException
    {
        if(query==null)
        {
            query="";
        }
        String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
        queryBuf.append("SELECT * FROM versionitem WHERE versionitem_id = ? OR ");
        queryBuf.append("LOWER(handle) LIKE LOWER(?) OR item_id = ? OR to_char(version_date, 'YYYY-MM-DD') like ? ORDER BY item_id,version_number ASC ");


        if (limit > 0)
        {
            queryBuf.append(" LIMIT ? ");
        }

        if (offset > 0)
        {
            queryBuf.append(" OFFSET ? ");
        }


        String dbquery = queryBuf.toString();

        // When checking against the version-id, make sure the query can be made into a number
        Integer int_param;
        try {
            int_param = Integer.valueOf(query);
        }
        catch (NumberFormatException e) {
            int_param = Integer.valueOf(-1);
        }

        // Create the parameter array, including limit and offset if part of the query
        Object[] paramArr = new Object[] {int_param,params,int_param,params};
        if (limit > 0 && offset > 0)
        {
            paramArr = new Object[]{int_param, params,int_param,params, limit, offset};
        }
        else if (limit > 0)
        {
            paramArr = new Object[]{int_param, params,int_param,params, limit};
        }
        else if (offset > 0)
        {
            paramArr = new Object[]{int_param,params,int_param, params,offset};
        }

        // Get all the epeople that match the query
        TableRowIterator rows = DatabaseManager.query(context,
                dbquery, paramArr);
        try
        {
            List<TableRow> versionRows = rows.toList();
            Version[] versions = new Version[versionRows.size()];

            for (int i = 0; i < versionRows.size(); i++)
            {
                TableRow row = (TableRow) versionRows.get(i);

                // First check the cache
                Version fromCache = (Version) context.fromCache(Version.class, row
                        .getIntColumn("eperson_id"));

                if (fromCache != null)
                {
                    versions[i] = fromCache;
                }
                else
                {
                    versions[i] = new VersionImpl(context, row) {
                    };
                }
            }

            return versions;
        }
        finally
        {
            if (rows != null)
            {
                rows.close();
            }
        }
    }

    public static int searchResultCount(Context context, String query)
            throws SQLException
    {
        if(query==null)
        {
            query="";
        }
        String dbquery = "%"+query.toLowerCase()+"%";
        Long count;

        // When checking against the eperson-id, make sure the query can be made into a number
        Integer int_param;
        try {
            int_param = Integer.valueOf(query);
        }
        catch (NumberFormatException e) {
            int_param = Integer.valueOf(-1);
        }

        // Get all the epeople that match the query
        TableRow row = DatabaseManager.querySingle(context,
                "SELECT count(*) as epcount FROM versionitem WHERE versionitem_id = ? OR " +
                        "LOWER(handle) LIKE LOWER(?) OR item_id = ? OR to_char(version_date, 'YYYY-MM-DD') like ?",
                new Object[] {int_param,dbquery,int_param,dbquery});

        // use getIntColumn for Oracle count data
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            count = Long.valueOf(row.getIntColumn("epcount"));
        }
        else  //getLongColumn works for postgres
        {
            count = Long.valueOf(row.getLongColumn("epcount"));
        }

        return count.intValue();
    }

    public static Bitstream[] findAllBitstreams(Context context,int verisonId) {

        ArrayList<Bitstream> bitstreamsList = new ArrayList<Bitstream>();
        try {
            TableRowIterator tri = DatabaseManager.queryTable(context,TABLE_NAME, "SELECT * FROM version2bitstream where version_id=" + verisonId);
            if(tri.hasNext())
            {
                TableRow tr = tri.next();

                Bitstream fromCache = (Bitstream) context.fromCache(Bitstream.class, tr.getIntColumn("bitsream_id"));

                if (fromCache != null)
                {
                    bitstreamsList.add(fromCache);
                }else{
                    bitstreamsList.add(Bitstream.find(context,tr.getIntColumn("bitstream_id")));
                }
            }
            else
            {
                return null;
            }
            Bitstream[] bitstreams = new Bitstream[bitstreamsList.size()];
            bitstreams = bitstreamsList.toArray(bitstreams);
            return bitstreams;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static void addBitstreams(Context context,int versionId, Bundle[] bundles)throws SQLException
    {
        for(Bundle bundle:bundles)
        {
            for (Bitstream bitstream : bundle.getBitstreams())
            {
                // Insert the mapping
                TableRow mappingRow = DatabaseManager.row("version2bitstream");
                mappingRow.setColumn("version_id", versionId);
                mappingRow.setColumn("bitstream_id", bitstream.getID());
                DatabaseManager.insert(context, mappingRow);
            }
        }
    }
}
