package org.dspace.bitstore;

import org.apache.log4j.Logger;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.event.Event;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Bitstream Storage Output Stream provides a means to create a new Bitstream by handing an
 * OutputStream to another part of the application. This is useful for when a bitstream
 * is generated during a conversion process such as an XSL transformation,
 *
 * One example of usage is to serialize METS and ORE represetnation of Items into the assetstore.
 *
 * @author Mark Diggory
 */
public class BitstreamStorageOutputStream extends DigestOutputStream {


    /** log4j log */
    private static Logger log = Logger.getLogger(BitstreamStorageOutputStream.class);

    private TableRow bitstream = null;
    private Context context = null;

    /** The count of bytes that have passed. */
    private long count = 0;

    // Checksum algorithm
    private static final String CSA = "MD5";

    boolean closed;

    private Map attrs = new HashMap();

    /**
     * Constructs a new ProxyOutputStream.
     *
     * @param proxy the OutputStream to delegate to
     */
    public BitstreamStorageOutputStream(Context context, TableRow bitstream, OutputStream proxy) throws NoSuchAlgorithmException {
        super(proxy, MessageDigest.getInstance(CSA));
        this.bitstream = bitstream;
        this.context = context;
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.closed = true;
        // update DB

        attrs.put("size_bytes", getByteCount());
        attrs.put("checksum", Utils.toHex(this.getMessageDigest().digest()));
        attrs.put("checksum_algorithm", CSA);

        updateBitstream(bitstream, this.attrs);

        bitstream.setColumn("deleted", false);

        try {
            DatabaseManager.update(context, bitstream);
        } catch (SQLException e) {
            throw new IOException(e.getMessage(),e);
        }

        int bitstream_id = bitstream.getIntColumn("bitstream_id");

        if (log.isDebugEnabled())
        {
            log.debug("Stored bitstream " + bitstream_id + " under id " + bitstream.getStringColumn("internal_id") );
        }

        log.info(LogManager.getHeader(context, "create_bitstream", "bitstream_id=" + bitstream_id));

        context.addEvent(new Event(Event.CREATE, Constants.BITSTREAM, bitstream_id, null));

    }

    public int getBitstreamID()
    {
        return bitstream.getIntColumn("bitstream_id");
    }

    public void setAttribute(String name, Object value)
    {
        attrs.put(name, value);
    }

    //-----------------------------------------------------------------------
    @Override
    public void write(int idx) throws IOException {
        count += 1;
        super.write(idx);
    }

    @Override
    public void write(byte[] bts) throws IOException {
        count += bts != null ? bts.length : 0;
        super.write(bts);
    }

    @Override
    public void write(byte[] bts, int st, int end) throws IOException {
        count += end;
        super.write(bts, st, end);
    }

    public synchronized long getByteCount() {
        return this.count;
    }

    protected static void updateBitstream(TableRow bitstream, Map attrs)
            throws IOException
    {
        Iterator iter = attrs.keySet().iterator();
        while (iter.hasNext())
        {
            String column = (String)iter.next();
            Object val = attrs.get(column);
            if (val != null)
            {
                if (val instanceof String) {
                    String value = (String) val;
                    bitstream.setColumn(column, value);
                }
                if (val instanceof Long) {
                    Long value = (Long) val;
                    bitstream.setColumn(column, value);
                }
                if (val instanceof Integer) {
                    Integer value = (Integer) val;
                    bitstream.setColumn(column, value);
                }
                if (val instanceof Boolean) {
                    Boolean value = (Boolean) val;
                    bitstream.setColumn(column, value);
                }
                if (val instanceof Date) {
                    Date value = (Date) val;
                    bitstream.setColumn(column, value);
                }
            }
        }
    }

}
