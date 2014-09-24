package org.dspace.content;

import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage   .rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.Date;

/**
 * BitstreamUtil Class is used to hold static lookup methods and avoid overriding Bitstream directly.
 *
 * @author Mark Diggory.
 */
public class BitstreamUtil {

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
}
