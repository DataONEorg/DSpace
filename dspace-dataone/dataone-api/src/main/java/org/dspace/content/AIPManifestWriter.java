package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.bitstore.BitstreamStorageOutputStream;
import org.dspace.bitstore.ExtendedBitstreamStorageManager;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.*;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: mdiggory
 * Date: 9/19/13
 * Time: 11:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class AIPManifestWriter
{

    /** log4j category */
    private static Logger log = Logger.getLogger(AIPManifestWriter.class);

    private static String PACKAGER_NAME = "INTERNAL-AIP";

    /**
     * Update the entry:  if Internal AIP is out of date or missing,
     * record a new one.  NOT related to the DB-level update() method.
     * @return true if it needed updating.
     */
    public Bitstream updateAIP(Context context, DSpaceObject dso, boolean force)
            throws SQLException, IOException, PackageException,
            CrosswalkException, AuthorizeException, NoSuchAlgorithmException {

        // get ready to write the bitstream
        InternalDSpaceAIPDisseminator dip = (InternalDSpaceAIPDisseminator) PluginManager
                .getNamedPlugin(PackageDisseminator.class, PACKAGER_NAME);

        if (dip == null)
        {
            log.error("Error, Cannot find PackageDisseminator type: " +  PACKAGER_NAME);
            throw new PackageException("Cannot find PackageDisseminator type: "+  PACKAGER_NAME);
        }
        PackageParameters pkgParams = new PackageParameters();
        pkgParams.addProperty("manifestOnly", "true");
        pkgParams.addProperty("internal", "true");
        pkgParams.addProperty("includeBundles", "+all");
        pkgParams.put("context",context);

        BitstreamStorageOutputStream bos = ExtendedBitstreamStorageManager.store(context,
                BitstreamFormat.findByShortDescription(context, "http://www.loc.gov/METS/"));
        // write manifest to bitstream target
        dip.disseminate(context, dso, pkgParams, bos);
        bos.close();

        int bitstreamId = bos.getBitstreamID();
        Bitstream aipBitstream = Bitstream.find(context,bitstreamId);
        if (aipBitstream == null)
            throw new IOException("Writing to AIP bitstream failed, see log.");

        return aipBitstream;
    }

    /**
     * AIP Prototype Code Changes
     */
    /**
     * Returns the Bitstream object containing the file in the asset
     * store indicated by the URI, or null if there is none.
     * See getAbsoluteURI().
     *
     * @param context - the context.
     * @param uri a bitstream absolute URI created by getAbsoluteURI()
     * @return a Bitstream object or null.
     */
    public static Bitstream dereferenceAbsoluteURI(Context context, URI uri)
            throws SQLException
    {
        TableRow row = ExtendedBitstreamStorageManager.dereferenceAbsoluteURI(context, uri);
        if (row == null)
            return null;
        else
            return new Bitstream(context, row);
    }

    /**
     * Returns a URI of the storage occupied by this bitstream in the
     * asset store.  It can be resolved by the dereferenceAbsoluteURI()
     * method.  Note that the "absolute" URI does not depend on the DSpace
     * object model or RDBMS storage, it only depends on the asset store
     * layer.
     *
     * @return external-based URI to bitstream.
     */
    public URI getAbsoluteURI(Context context, Bitstream bitstream)
    {
        try{
            TableRow bRow = DatabaseManager.findByUnique(context,"Bitstream","bitstream_id",bitstream.getID());
            URI result = ExtendedBitstreamStorageManager.getAbsoluteURI(bRow);
            if (log.isDebugEnabled())
                log.debug("Bitstream.getAbsoluteURI returning = \""+result+"\"");
            return result;
        }catch (Exception e)
        {
            return null;
        }
    }


}
