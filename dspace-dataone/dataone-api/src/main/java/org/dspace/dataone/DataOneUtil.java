/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.dataone;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.BitstreamUtil;
import org.dspace.content.OREManifestWriter;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by mdiggory on 10/13/14.
 */
public class DataOneUtil {

    public static String getFormat(Bitstream bitstream)
    {
        BitstreamFormat format = bitstream.getFormat();

        String formatId = format.getMIMEType();

        if(format.getShortDescription().equals(OREManifestWriter.ORE.NS))
            formatId = OREManifestWriter.ORE.NS;
        else if(format.getShortDescription().equals("http://www.loc.gov/METS/"))
            formatId = "http://www.loc.gov/METS/";

        return formatId;
    }

    public static String getPid(Context context, Bitstream bitstream) throws SQLException {
        return "uuid:" + BitstreamUtil.getUuid(context, bitstream);
    }



}
