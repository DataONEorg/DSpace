package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.bitstore.ExtendedBitstreamStorageManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage   .rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionDAO;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

/**
 * BitstreamUtil Class is used to hold static lookup methods and avoid overriding Bitstream directly.
 *
 * @author Mark Diggory.
 */
public class BitstreamUtil {

    /** log4j category */
    private static Logger log = Logger.getLogger(BitstreamUtil.class);

    /**
     * Allow access to delete method on Bitstream from code outside the package.
     * @param bitstream
     * @throws SQLException
     */
    public static void delete(Context context, Bitstream bitstream, boolean cleanup) throws SQLException, IOException {
        bitstream.delete();
        if(cleanup)
            ExtendedBitstreamStorageManager.cleanup(context, bitstream.getID(), true, true);
    }

    /**
     * Allow access to delete method on Bitstream from code outside the package.
     * @param bitstream
     * @throws SQLException
     */
    public static void delete(Bitstream bitstream) throws SQLException {
        bitstream.delete();
    }

    public static boolean isDeleted(Bitstream bitstream) throws SQLException {
        return bitstream.isDeleted();
    }

    /**
     * Add support to retrieve the last modified date. Ideally, this should be moved inside Bitstream class in
     * future DSpace version.
     *
     * @param context
     * @param bitstream
     * @return
     * @throws SQLException
     */
    public static Date getLastModifiedDate(Context context,Bitstream bitstream) throws SQLException{

        TableRowIterator row = DatabaseManager.query(context,"select * from bitstream where bitstream_id = "+bitstream.getID());

        if (row.hasNext())
        {
            return row.next().getDateColumn("last_modified_date");
        }
         return null;
    }

    /**
     * Add support to get the date created from the Bitstream. Ideally, this should be moved inside Bitstream class in
     * future DSpace version.
     *
     * @param context
     * @param bitstream
     * @return
     * @throws SQLException
     */
    public static Date getDateCreated(Context context, Bitstream bitstream) throws SQLException {

        TableRowIterator row = DatabaseManager.query(context,"select * from bitstream where bitstream_id = "+bitstream.getID());

        if (row.hasNext())
        {
            return row.next().getDateColumn("create_date");
        }
        return null;
    }



    public static String tableName = "versionitem";
    public static String mapTableName = "Version2Bitstream";

    public static String TYPE_ORE = "ore";
    public static String TYPE_AIP = "aip";
    public static String TYPE_CON = "content";



    public static Version getVersion(Context c, Bitstream b) {
        TableRow row = null;
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        if(getBitstreamType(c,b).equals(TYPE_AIP))
        {

            String query= "select * from "+tableName+" where bitstream_id = ?";
            try{
                row = DatabaseManager.querySingleTable(c, tableName, query,b.getID() );
            }catch (Exception e)
            {
                log.error(e.getMessage(),e);
            }

        }
        else if(getBitstreamType(c,b).equals(TYPE_ORE))
        {
            String query= "select * from "+tableName+" where ore_bitstream_id = ?";
            try{
                row = DatabaseManager.querySingleTable(c, tableName, query,b.getID() );
            }catch (Exception e)
            {
                log.error(e.getMessage(),e);
            }

        }
        else
        {
            String query= "select versionitem.versionitem_id as versionitem_id from versionitem, Version2Bitstream where versionitem.versionitem_id=Version2Bitstream.version_id and Version2Bitstream.bitstream_id = ? order by versionitem.version_number DESC";
            try{
                row = DatabaseManager.querySingle(c, query,b.getID() );
            }catch (Exception e)
            {
                log.error(e.getMessage(),e);
            }

            //this is a content bitstream
            /*
            try{
                DSpaceObject dSpaceObject =  b.getParentObject();
                Item item = (Item) dSpaceObject;
                VersionHistory vh = versioningService.findVersionHistory(c,item.getID());
                Version version = vh.getLatestVersion();
                return version;


            }
            catch (Exception e)
            {

            }
            */
        }
        if(row!=null)
        {
            int versionId = row.getIntColumn("versionitem_id");
            Version version = versioningService.getVersion(c, versionId);
            return version;
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.

    }


    public static String getBitstreamType(Context c, Bitstream b) {

        TableRow row = null;
        String query= "select * from "+tableName+" where ore_bitstream_id = ?";
        try{
            row = DatabaseManager.querySingleTable(c, tableName, query,b.getID() );
        }catch (Exception e)
        {

        }

        if(row==null)
        {
            query= "select * from "+tableName+" where bitstream_id = ?";
            try{
                row = DatabaseManager.querySingleTable(c, tableName, query,b.getID() );
            }catch (Exception e)
            {

            }
            if(row==null)
            {
                return TYPE_CON;
            }
            else
            {
                return TYPE_AIP;
            }

        }
        else
        {
            return TYPE_ORE;
        }
    }


    public static Bitstream getObsoletedBy(Context c, Bitstream b) {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        if(getBitstreamType(c,b).equals(TYPE_AIP))
        {
            TableRow row = null;
            String query= "select * from "+tableName+" where bitstream_id = ?";
            try{
                row = DatabaseManager.querySingleTable(c, tableName, query,b.getID() );
            }catch (Exception e)
            {

            }
            if(row!=null)
            {
                int versionId = row.getIntColumn("versionitem_id");
                Version version = versioningService.getVersion(c, versionId);
                int versionHistoryId = row.getIntColumn("versionhistory_id");
                VersionHistory vh = retrieveVersionHistory(c, versionHistoryId);
                if(vh!=null)
                {
                    if(vh.hasNext(version)){
                        Version nextVerison =  vh.getNext(version);
                        return nextVerison.getAIPBitstream();
                    }
                }
            }

        }
        else if(getBitstreamType(c,b).equals(TYPE_ORE))
        {
            TableRow row = null;
            String query= "select * from "+tableName+" where ore_bitstream_id = ?";
            try{
                row = DatabaseManager.querySingleTable(c, tableName, query,b.getID() );
            }catch (Exception e)
            {

            }
            if(row!=null)
            {
                int versionId = row.getIntColumn("versionitem_id");
                Version version = versioningService.getVersion(c, versionId);
                int versionHistoryId = row.getIntColumn("versionhistory_id");
                VersionHistory vh = retrieveVersionHistory(c, versionHistoryId);
                if(vh!=null)
                {
                    if(vh.hasNext(version)){
                        Version nextVerison =  vh.getNext(version);
                        return nextVerison.getAIPBitstream();
                    }
                }
            }

        }
        else
        {
            //this is a content bitstream
            TableRow row = null;
            String query= "select * from "+mapTableName+" where bitstream_id = ?";
            try{
                row = DatabaseManager.querySingleTable(c, mapTableName, query,b.getID() );
            }catch (Exception e)
            {

            }
            if(row!=null)
            {
                int versionId = row.getIntColumn("version_id");
                Version version = versioningService.getVersion(c, versionId);
                VersionHistory vh = retrieveVersionHistory(c, version.getVersionHistoryID());
                if(vh!=null)
                {
                    if(vh.hasNext(version)){
                        Version nextVerison =  vh.getNext(version);
                        return nextVerison.getAIPBitstream();
                    }
                }
            }
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public static Bitstream getObsoletes(Context c, Bitstream b) {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        if(getBitstreamType(c,b).equals(TYPE_AIP))
        {
            TableRow row = null;
            String query= "select * from "+tableName+" where bitstream_id = ?";
            try{
                row = DatabaseManager.querySingleTable(c, tableName, query,b.getID() );
            }catch (Exception e)
            {

            }
            if(row!=null)
            {
                int versionId = row.getIntColumn("versionitem_id");
                Version version = versioningService.getVersion(c, versionId);
                int versionHistoryId = row.getIntColumn("versionhistory_id");
                VersionHistory vh = retrieveVersionHistory(c,versionHistoryId);
                if(vh!=null)
                {
                    Version preVersion =  vh.getPrevious(version);
                    if(preVersion!=null)
                        return preVersion.getAIPBitstream();
                }
            }

        }
        else if(getBitstreamType(c,b).equals(TYPE_ORE))
        {
            TableRow row = null;
            String query= "select * from "+tableName+" where ore_bitstream_id = ?";
            try{
                row = DatabaseManager.querySingleTable(c, tableName, query,b.getID() );
            }catch (Exception e)
            {

            }
            if(row!=null)
            {
                int versionHistoryId = row.getIntColumn("versionhistory_id");
                int versionId = row.getIntColumn("versionitem_id");
                Version version = versioningService.getVersion(c, versionId);
                VersionHistory vh = retrieveVersionHistory(c,versionHistoryId);
                if(vh!=null)
                {
                    Version preVersion =  vh.getPrevious(version);
                    if(preVersion!=null)
                        return preVersion.getOREBitstream();
                }
            }

        }
        else
        {
            //this is a content bitstream
            TableRow row = null;
            String query= "select * from "+mapTableName+" where bitstream_id = ?";
            try{
                row = DatabaseManager.querySingleTable(c, mapTableName, query,b.getID() );
            }catch (Exception e)
            {

            }
            if(row!=null)
            {
                int versionId = row.getIntColumn("version_id");

                Version version = versioningService.getVersion(c, versionId);
                VersionHistory vh = retrieveVersionHistory(c, version.getVersionHistoryID());
                if(vh!=null)
                {
                    Version preVersion =  vh.getPrevious(version);
                    if(preVersion!=null)
                        return preVersion.getOREBitstream();
                }
            }
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private static VersionHistory retrieveVersionHistory(Context c,Integer versionHistoryId)
    {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        if(versionHistoryId==null)
        {
            return null;
        }
        return versioningService.findVersionByHistoryId(c, versionHistoryId);
    }
}
