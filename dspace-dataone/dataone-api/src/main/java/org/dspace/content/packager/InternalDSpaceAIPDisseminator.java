/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.*;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import edu.harvard.hul.ois.mets.*;
import edu.harvard.hul.ois.mets.helper.*;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.bitstore.ExtendedBitstreamStorageManager;
import org.dspace.content.*;
import org.dspace.content.crosswalk.*;
import org.dspace.core.*;
import org.dspace.license.CreativeCommons;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.util.Date;

/**
 * Subclass of the METS packager framework to disseminate a DSpace
 * Archival Information Package (AIP).  The AIP is intended to be, foremost,
 * a _complete_ and _accurate_ representation of one object in the DSpace
 * object model.  An AIP contains all of the information needed to restore
 * the object precisely in another DSpace archive instance.
 * <p>
 * Configuration keys:
 * <p>
 * The following take as values a space-and-or-comma-separated list
 * of plugin names that name *either* a DisseminationCrosswalk or
 * StreamDisseminationCrosswalk plugin.  Shown are the default values.
 * The value may be a simple crosswalk name, or a METS MDsec-name followed by
 * a colon and the crosswalk name e.g. "DSpaceDepositLicense:DSPACE_DEPLICENSE"
 *
 *    # MD types to put in the sourceMD section of the object.
 *    aip.disseminate.sourceMD = AIP-TECHMD
 *
 *    # MD types to put in the techMD section of the object (and member Bitstreams if an Item)
 *    aip.disseminate.techMD = PREMIS
 *
 *    # MD types to put in digiprovMD section of the object.
 *    #aip.disseminate.digiprovMD =
 *
 *    # MD types to put in the rightsMD section of the object.
 *    aip.disseminate.rightsMD = DSpaceDepositLicense:DSPACE_DEPLICENSE, \
 *       CreativeCommonsRDF:DSPACE_CCRDF, CreativeCommonsText:DSPACE_CCTXT, METSRights
 *
 *    # MD types to put in dmdSec's corresponding  the object.
 *    aip.disseminate.dmd = MODS, DIM
 *
 * @author Larry Stone
 * @author Tim Donohue
 * @author Mark Diggory
 * @uthor Lantian Gai
 * @version $Revision: 1.1 $
 * @see AbstractMETSDisseminator
 * @see AbstractPackageDisseminator
 */
public class InternalDSpaceAIPDisseminator extends AbstractMETSDisseminator
{
    private static final Logger log = Logger.getLogger(InternalDSpaceAIPDisseminator.class);

    /**
     * Unique identifier for the profile of the METS document.
     * To ensure uniqueness, it is the URL that the XML schema document would
     * have _if_ there were to be one.  There is no schema at this time.
     */
    public static final String PROFILE_1_0 =
            "http://www.dspace.org/schema/aip/mets_aip_1_0.xsd";

    /** TYPE of the div containing AIP's parent handle in its mptr. */
    public static final String PARENT_DIV_TYPE = "AIP Parent Link";

    // Default MDTYPE value for deposit license -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    private static final String DSPACE_DEPOSIT_LICENSE_MDTYPE =
            "DSpaceDepositLicense:DSPACE_DEPLICENSE";

    // Default MDTYPE value for CC license in RDF -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    private static final String CREATIVE_COMMONS_RDF_MDTYPE =
            "CreativeCommonsRDF:DSPACE_CCRDF";

    // Default MDTYPE value for CC license in Text -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    private static final String CREATIVE_COMMONS_TEXT_MDTYPE =
            "CreativeCommonsText:DSPACE_CCTXT";

    // dissemination parameters passed to the AIP Disseminator
    private PackageParameters disseminateParams = null;

    // List of Bundles to filter on, when building AIP
    private List<String> filterBundles = new ArrayList<String>();
    // Whether 'filterBundles' specifies an exclusion list (default) or inclusion list.
    private boolean excludeBundles = true;

    // JDOM xml output writer - indented format for readability.
    private static XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

    /**
     * Get the URL by which the METS manifest refers to a Bitstream
     * member of an Item the "package".  Note that this ONLY has to work
     * for the Bitstreams belonging to a Bunde in an Item, NOT for the
     * other associated Bitstreams containing metadata streams, or logo
     * of a Community/Collection, etc.
     * <p>
     * For an internal AIP, this is a reference to a file
     * in the asset store.  An external AIP names a file in the package
     * with a relative URL, that is, relative pathname.
     * <p>
     * @return String in URL format naming path to bitstream.
     */
    public String makeBitstreamURL(Bitstream bitstream, PackageParameters params)
    {
        // if bare manifest, use external "persistent" URI for bitstreams
        if (params != null && (params.getBooleanProperty("manifestOnly", false) ||
                params.getBooleanProperty("internal", false)))
        {
            Context context = (Context)params.get("context");
            if(context==null)
            {
                return null;
            }
            return ExtendedBitstreamStorageManager.getAbsoluteURI(context, bitstream).toString();
        }
        else
        {
            String base = "bitstream_"+String.valueOf(bitstream.getID());
            String ext[] = bitstream.getFormat().getExtensions();
            return (ext.length > 0) ? base+"."+ext[0] : base;
        }
    }


    /**
     * Export the object (Item, Collection, or Community) as a
     * "package" on the indicated OutputStream.  Package is any serialized
     * representation of the item, at the discretion of the implementing
     * class.  It does not have to include content bitstreams.
     * <p>
     * Use the <code>params</code> parameter list to adjust the way the
     * package is made, e.g. including a "<code>metadataOnly</code>"
     * parameter might make the package a bare manifest in XML
     * instead of a Zip file including manifest and contents.
     * <p>
     * Throws an exception of the chosen object is not acceptable or there is
     * a failure creating the package.
     *
     * @param context  DSpace context.
     * @param dso  DSpace object (item, collection, etc)
     * @param params Properties-style list of options specific to this packager
     * @param outStream File where export package should be written
     * @throws PackageValidationException if package cannot be created or there
     * is a fatal error in creating it.
     */
    public void disseminate(Context context, DSpaceObject dso,
                            PackageParameters params, OutputStream outStream)
            throws PackageValidationException, CrosswalkException, AuthorizeException, SQLException, IOException
    {
        // Reset our 'unique' ID counter back to 1 (in case a previous dissemination was run)
        // This ensures that the @ID attributes of METS tags always begin at '1', which
        // also ensures that the Checksums don't change because of accidental @ID value changes.
        disseminateParams = params;
        resetCounter();

        try
        {
            Mets manifest = makeManifest(context, dso, params, null);
            //only validate METS if specified (default = true)
            if(params.getBooleanProperty("validate", true))
            {
                manifest.validate(new MetsValidator());
            }
            manifest.write(new MetsWriter(outStream));

        }//end try
        catch (MetsException e)
        {
            String errorMsg = "Error exporting METS for DSpace Object, type="
                    + Constants.typeText[dso.getType()] + ", handle="
                    + dso.getHandle() + ", dbID="
                    + String.valueOf(dso.getID());

            // We don't pass up a MetsException, so callers don't need to
            // know the details of the METS toolkit
            log.error(errorMsg,e);
            throw new PackageValidationException(errorMsg, e);
        }
        finally
        {
            //Close stream / stop writing to file
            if (outStream != null)
            {
                outStream.close();
            }
        }
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso,
                            PackageParameters params, File pkgFile)
            throws PackageValidationException, CrosswalkException, AuthorizeException, SQLException, IOException
    {
        //Before disseminating anything, save the passed in PackageParameters, so they can be used by all methods
        disseminateParams = params;

        boolean disseminate = true; //by default, always disseminate

        //if user specified to only disseminate objects updated *after* a specific date
        // (Note: this only works for Items right now, as DSpace doesn't store a
        //  last modified date for Collections or Communities)
        if(disseminateParams.containsKey("updatedAfter") && dso.getType()==Constants.ITEM)
        {
            Date afterDate = Utils.parseISO8601Date(disseminateParams.getProperty("updatedAfter"));

            //if null is returned, we couldn't parse the date!
            if(afterDate==null)
            {
                throw new IOException("Invalid date passed in via 'updatedAfter' option. Date must be in ISO-8601 format, and include both a day and time (e.g. 2010-01-01T00:00:00).");
            }

            //check when this item was last modified.
            Item i = (Item) dso;
            if(i.getLastModified().after(afterDate))
            {
                disseminate = true;
            }
            else
            {
                disseminate = false;
            }
        }

        if(disseminate)
        {
            //just do a normal dissemination as specified by AbstractMETSDisseminator
            super.disseminate(context, dso, params, pkgFile);
        }
    }

    /**
     * Return identifier string for the METS profile this produces.
     *
     * @return string name of profile.
     */
    @Override
    public String getProfile()
    {
        return PROFILE_1_0;
    }

    /**
     * Returns name of METS fileGrp corresponding to a DSpace bundle name.
     * For AIP the mapping is direct.
     *
     * @param bname name of DSpace bundle.
     * @return string name of fileGrp
     */
    @Override
    public String bundleToFileGrp(String bname)
    {
        return bname;
    }

    /**
     * Create the metsHdr element for the AIP METS Manifest.
     * <p>
     * CREATEDATE is time at which the package (i.e. this manifest) was created.
     * LASTMODDATE is last-modified time of the target object, if available.
     * Agent describes the archive this belongs to.
     *
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public MetsHdr makeMetsHdr(Context context, DSpaceObject dso,
                               PackageParameters params)
    {
        MetsHdr metsHdr = new MetsHdr();

        // Note: we specifically do not add a CREATEDATE to <metsHdr>
        // as for AIPs we want md5 checksums to be identical if no content
        // has changed.  Adding a CREATEDATE changes checksum each time.

        // Add a LASTMODDATE for items
        if (dso.getType() == Constants.ITEM)
        {
            metsHdr.setLASTMODDATE(((Item) dso).getLastModified());
        }

        // Agent Custodian - name custodian, the DSpace Archive, by handle.
        Agent agent = new Agent();
        agent.setROLE(Role.CUSTODIAN);
        agent.setTYPE(Type.OTHER);
        agent.setOTHERTYPE("DSpace Archive");
        Name name = new Name();
        name.getContent()
                .add(new PCData(Site.getSiteHandle()));
        agent.getContent().add(name);
        metsHdr.getContent().add(agent);

        // Agent Creator - name creator, which is a specific version of DSpace.
        Agent agentCreator = new Agent();
        agentCreator.setROLE(Role.CREATOR);
        agentCreator.setTYPE(Type.OTHER);
        agentCreator.setOTHERTYPE("DSpace Software");
        Name creatorName = new Name();
        creatorName.getContent()
                .add(new PCData("DSpace " + Util.getSourceVersion()));
        agentCreator.getContent().add(creatorName);
        metsHdr.getContent().add(agentCreator);

        return metsHdr;
    }

    /**
     * Return the name of all crosswalks to run for the dmdSec section of
     * the METS Manifest.
     * <p>
     * Default is DIM (DSpace Internal Metadata) and MODS.
     *
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public String [] getDmdTypes(Context context, DSpaceObject dso, PackageParameters params)
            throws SQLException, IOException, AuthorizeException
    {
        String dmdTypes = ConfigurationManager.getProperty("aip.disseminate.dmd");
        if (dmdTypes == null)
        {
            String result[] = new String[2];
            result[0] = "MODS";
            result[1] = "DIM";
            result[2] = "QDC";
            return result;
        }
        else
        {
            return dmdTypes.split("\\s*,\\s*");
        }
    }

    /**
     * Return the name of all crosswalks to run for the techMD section of
     * the METS Manifest.
     * <p>
     * Default is PREMIS.
     *
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public String[] getTechMdTypes(Context context, DSpaceObject dso, PackageParameters params)
            throws SQLException, IOException, AuthorizeException
    {
        String techTypes = ConfigurationManager.getProperty("aip.disseminate.techMD");
        if (techTypes == null)
        {
            if (dso.getType() == Constants.BITSTREAM)
            {
                String result[] = new String[1];
                result[0] = "PREMIS";
                return result;
            }
            else
            {
                return new String[0];
            }
        }
        else
        {
            return techTypes.split("\\s*,\\s*");
        }
    }

    /**
     * Return the name of all crosswalks to run for the sourceMD section of
     * the METS Manifest.
     * <p>
     * Default is AIP-TECHMD.
     * <p>
     * In an AIP, the sourceMD element MUST include the original persistent
     * identifier (Handle) of the object, and the original persistent ID
     * (Handle) of its parent in the archive, so that it can be restored.
     *
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public String[] getSourceMdTypes(Context context, DSpaceObject dso, PackageParameters params)
            throws SQLException, IOException, AuthorizeException
    {
        String sourceTypes = ConfigurationManager.getProperty("aip.disseminate.sourceMD");
        if (sourceTypes == null)
        {
            String result[] = new String[1];
            result[0] = "AIP-TECHMD";
            return result;
        }
        else
        {
            return sourceTypes.split("\\s*,\\s*");
        }
    }

    /**
     * Return the name of all crosswalks to run for the digiprovMD section of
     * the METS Manifest.
     * <p>
     * By default, none are returned
     *
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public String[] getDigiprovMdTypes(Context context, DSpaceObject dso, PackageParameters params)
            throws SQLException, IOException, AuthorizeException
    {
        String dpTypes = ConfigurationManager.getProperty("aip.disseminate.digiprovMD");
        if (dpTypes == null)
        {
            return new String[0];
        }
        else
        {
            return dpTypes.split("\\s*,\\s*");
        }
    }

    /**
     * Return the name of all crosswalks to run for the rightsMD section of
     * the METS Manifest.
     * <p>
     * By default, Deposit Licenses and CC Licenses will be added for Items.
     * Also, by default METSRights info will be added for all objects.
     *
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public String[] getRightsMdTypes(Context context, DSpaceObject dso, PackageParameters params)
            throws SQLException, IOException, AuthorizeException
    {

        List<String> result = new ArrayList<String>();
        String rTypes = ConfigurationManager.getProperty("aip.disseminate.rightsMD");

        //If unspecified in configuration file, add default settings
        if (rTypes == null)
        {
            // Licenses only apply to an Item
            if (dso.getType() == Constants.ITEM)
            {
                //By default, disseminate Deposit License, and any CC Licenses
                // to an item's rightsMD section
                if (PackageUtils.findDepositLicense(context, (Item)dso) != null)
                {
                    result.add(DSPACE_DEPOSIT_LICENSE_MDTYPE);
                }

                if (CreativeCommons.getLicenseRdfBitstream((Item) dso) != null)
                {
                    result.add(CREATIVE_COMMONS_RDF_MDTYPE);
                }
                else if (CreativeCommons.getLicenseTextBitstream((Item)dso) != null)
                {
                    result.add(CREATIVE_COMMONS_TEXT_MDTYPE);
                }
            }

            //By default, also add METSRights info to the rightsMD
            result.add("METSRights");
        }
        else
        {
            return rTypes.split("\\s*,\\s*");
        }

        return result.toArray(new String[result.size()]);
    }


    /**
     * Adds another structMap element to contain the "parent link" that
     * is an essential part of every AIP.  This is a structmap of one
     * div, which contains an mptr indicating the Handle of the parent
     * of this object in the archive.  The div has a unique TYPE attribute
     * value, "AIP Parent Link", and the mptr has a LOCTYPE of "HANDLE"
     * and an xlink:href containing the raw Handle value.
     * <p>
     * Note that the parent Handle has to be stored here because the
     * parent is needed to create a DSpace Object when restoring the
     * AIP; it cannot be determined later once the ingester parses it
     * out of the metadata when the crosswalks are run.  So, since the
     * crosswalks require an object to operate on, and creating the
     * object requires a parent, we cannot depend on metadata processed
     * by crosswalks (e.g.  AIP techMd) for the parent, it has to be at
     * a higher level in the AIP manifest.  The structMap is an obvious
     * and standards-compliant location for it.
     *
     * @param context DSpace context
     * @param dso Current DSpace object
     * @param params Packager Parameters
     * @param mets METS manifest
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     * @throws MetsException
     */
    @Override
    public void addStructMap(Context context, DSpaceObject dso,
                             PackageParameters params, Mets mets)
            throws SQLException, IOException, AuthorizeException, MetsException
    {
        // find parent Handle
        String parentHandle = null;
        switch (dso.getType())
        {
            case Constants.ITEM:
                parentHandle = ((Item)dso).getOwningCollection().getHandle();
                break;

            case Constants.COLLECTION:
                parentHandle = (((Collection)dso).getCommunities())[0].getHandle();
                break;

            case Constants.COMMUNITY:
                Community parent = ((Community)dso).getParentCommunity();
                if (parent == null)
                {
                    parentHandle = Site.getSiteHandle();
                }
                else
                {
                    parentHandle = parent.getHandle();
                }
            case Constants.SITE:
                break;
        }

        // Parent Handle should only be null if we are creating a site-wide AIP
        if(parentHandle!=null)
        {
            // add a structMap to contain div pointing to parent:
            StructMap structMap = new StructMap();
            structMap.setID(gensym("struct"));
            structMap.setTYPE("LOGICAL");
            structMap.setLABEL("Parent");
            Div div0 = new Div();
            div0.setID(gensym("div"));
            div0.setTYPE(PARENT_DIV_TYPE);
            div0.setLABEL("Parent of this DSpace Object");
            Mptr mptr = new Mptr();
            mptr.setID(gensym("mptr"));
            mptr.setLOCTYPE(Loctype.HANDLE);
            mptr.setXlinkHref(parentHandle);
            div0.getContent().add(mptr);
            structMap.getContent().add(div0);
            mets.getContent().add(structMap);
        }
    }

    /**
     * By default, include all bundles in AIP as content.
     * <P>
     * However, if the user specified a comma separated list of bundle names
     * via the "filterBundles" (or "includeBundles")  option, then check if this
     * bundle is in that list.  If it is, return true.  If it is not, return false.
     *
     * @param bundle Bundle to check for
     * @return true if bundle should be disseminated when disseminating Item AIPs
     */
    @Override
    public boolean includeBundle(Bundle bundle)
    {
        List<String> bundleList = getBundleList();

        //Check if we are disseminating all bundles
        if(bundleList.size()==1 && bundleList.get(0).equalsIgnoreCase("all") && !this.excludeBundles)
        {
            return true; //all bundles should be disseminated
        }
        else
        {
            //Check if bundle name is in our list of filtered bundles
            boolean inList = filterBundles.contains(bundle.getName());
            //Based on whether this is an inclusion or exclusion filter,
            //return whether this bundle should be included.
            return this.excludeBundles ? !inList : inList;
        }
    }

    /**
     * Get our list of bundles to include/exclude in this AIP,
     * based on the passed in parameters
     * @return List of bundles to filter on
     */
    protected List<String> getBundleList()
    {
        // Check if we already have our list of bundles to filter on, if so, just return it.
        if(this.filterBundles!=null && !this.filterBundles.isEmpty())
            return this.filterBundles;

        // Check for 'filterBundles' option, as this allows for inclusion/exclusion of bundles.
        String bundleList = this.disseminateParams.getProperty("filterBundles");

        if(bundleList==null || bundleList.isEmpty())
        {
            //For backwards compatibility with DSpace 1.7.x, check the
            //'includeBundles' option to see if a list of bundles was provided
            bundleList = this.disseminateParams.getProperty("includeBundles", "+all");
            //if we are taking the 'includeBundles' value, prepend "+" to specify that this is an inclusion
            bundleList = bundleList.startsWith("+") ? bundleList : "+".concat(bundleList);
        }
        // At this point, 'bundleList' will be *non-null*. If neither option was passed in,
        // then 'bundleList' defaults to "+all" (i.e. include all bundles).

        //If our filter list of bundles begins with a '+', then this list
        // specifies all the bundles to *include*. Otherwise all
        // bundles *except* the listed ones are included
        if(bundleList.startsWith("+"))
        {
            this.excludeBundles = false;
            //remove the preceding '+' from our bundle list
            bundleList = bundleList.substring(1);
        }

        //Split our list of bundles to filter on commas
        this.filterBundles = Arrays.asList(bundleList.split(","));


        return this.filterBundles;
    }


    /**
     * Returns a user help string which should describe the
     * additional valid command-line options that this packager
     * implementation will accept when using the <code>-o</code> or
     * <code>--option</code> flags with the Packager script.
     *
     * @return a string describing additional command-line options available
     * with this packager
     */
    @Override
    public String getParameterHelp()
    {
        String parentHelp = super.getParameterHelp();

        //Return superclass help info, plus the extra parameter/option that this class supports
        return parentHelp +
                "\n\n" +
                "* filterBundles=[bundleList]      " +
                "List of bundles specifying which Bundles should be included in an AIP. If this list starts with a '+' symbol," +
                " then it represents a list of bundles to *include* in the AIP.  By default, the list represents a list of bundles" +
                " to *exclude* from the AIP.";
    }

    /**
     * Create an element wrapped around a metadata reference (either mdWrap
     * or mdRef); i.e. dmdSec, techMd, sourceMd, etc.  Checks for
     * XML-DOM oriented crosswalk first, then if not found looks for
     * stream crosswalk of the same name.
     *
     * @param context DSpace Context
     * @param dso DSpace Object we are generating METS manifest for
     * @param mdSecClass class of mdSec (TechMD, RightsMD, DigiProvMD, etc)
     * @param typeSpec Type of metadata going into this mdSec (e.g. MODS, DC, PREMIS, etc)
     * @param params the PackageParameters
     * @param extraStreams list of extra files which need to be added to final dissemination package
     *
     * @return mdSec element or null if xwalk returns empty results.
     *
     * @throws SQLException
     * @throws PackageValidationException
     * @throws CrosswalkException
     * @throws IOException
     * @throws AuthorizeException
     */
    protected MdSec makeMdSec(Context context, DSpaceObject dso, Class mdSecClass,
                              String typeSpec, PackageParameters params,
                              MdStreamCache extraStreams)
            throws SQLException, PackageValidationException, CrosswalkException,
            IOException, AuthorizeException
    {
        try
        {
            //create our metadata element (dmdSec, techMd, sourceMd, rightsMD etc.)
            MdSec mdSec = (MdSec) mdSecClass.newInstance();
            mdSec.setID(gensym(mdSec.getLocalName()));
            String parts[] = typeSpec.split(":", 2);
            String xwalkName, metsName;

            //determine the name of the crosswalk to use to generate metadata
            // for dmdSecs this is the part *after* the colon in the 'type' (see getDmdTypes())
            // for all other mdSecs this is usually just corresponds to type name.
            if (parts.length > 1)
            {
                metsName = parts[0];
                xwalkName = parts[1];
            }
            else
            {
                metsName = typeSpec;
                xwalkName = typeSpec;
            }

            // First, check to see if the crosswalk we are using is a normal DisseminationCrosswalk
            boolean xwalkFound = PluginManager.hasNamedPlugin(DisseminationCrosswalk.class, xwalkName);

            if(xwalkFound)
            {
                // Find the crosswalk we will be using to generate the metadata for this mdSec
                DisseminationCrosswalk xwalk = (DisseminationCrosswalk)
                        PluginManager.getNamedPlugin(DisseminationCrosswalk.class, xwalkName);

                if (xwalk.canDisseminate(dso))
                {
                    // Check if our Crosswalk actually wraps another Packager Plugin
                    if(xwalk instanceof AbstractPackagerWrappingCrosswalk)
                    {
                        // If this crosswalk wraps another Packager Plugin, we can pass it our Packaging Parameters
                        // (which essentially allow us to customize the output of the crosswalk)
                        AbstractPackagerWrappingCrosswalk wrapper = (AbstractPackagerWrappingCrosswalk) xwalk;
                        wrapper.setPackagingParameters(params);
                    }

                    //For a normal DisseminationCrosswalk, we will be expecting an XML (DOM) based result.
                    // So, we are going to wrap this XML result in an <mdWrap> element
                    MdWrap mdWrap = new MdWrap();
                    setMdType(mdWrap, metsName);
                    XmlData xmlData = new XmlData();
                    if (crosswalkToMetsElement(xwalk, dso, xmlData) != null)
                    {
                        mdWrap.getContent().add(xmlData);
                        mdSec.getContent().add(mdWrap);
                        return mdSec;
                    }
                    else
                    {
                        return null;
                    }
                }
                else
                {
                    return null;
                }
            }
            // If we didn't find the correct crosswalk, we will check to see if this is
            // a StreamDisseminationCrosswalk -- a Stream crosswalk disseminates to an OutputStream
            else
            {
                StreamDisseminationCrosswalk sxwalk = (StreamDisseminationCrosswalk)
                        PluginManager.getNamedPlugin(StreamDisseminationCrosswalk.class, xwalkName);
                if (sxwalk != null)
                {
                    if (sxwalk.canDisseminate(context, dso))
                    {
                        // Check if our Crosswalk actually wraps another Packager Plugin
                        if(sxwalk instanceof AbstractPackagerWrappingCrosswalk)
                        {
                            // If this crosswalk wraps another Packager Plugin, we can pass it our Packaging Parameters
                            // (which essentially allow us to customize the output of the crosswalk)
                            AbstractPackagerWrappingCrosswalk wrapper = (AbstractPackagerWrappingCrosswalk) sxwalk;
                            wrapper.setPackagingParameters(params);
                        }

                        // Disseminate crosswalk output to an outputstream
                        ByteArrayOutputStream disseminateOutput = new ByteArrayOutputStream();
                        sxwalk.disseminate(context, dso, disseminateOutput);
                        // Convert output to an inputstream, so we can write to manifest or Zip file
                        ByteArrayInputStream crosswalkedStream = new ByteArrayInputStream(disseminateOutput.toByteArray());

                        //If we are capturing extra files to put into a Zip package
                        if(extraStreams!=null)
                        {
                            //Create an <mdRef> -- we'll just reference the file by name in Zip package
                            MdRef mdRef = new MdRef();
                            //add the crosswalked Stream to list of files to add to Zip package later
                            extraStreams.addStream(mdRef, crosswalkedStream);

                            //set properties on <mdRef>
                            // Note, filename will get set on this <mdRef> later,
                            // when we process all the 'extraStreams'
                            mdRef.setMIMETYPE(sxwalk.getMIMEType());
                            setMdType(mdRef, metsName);
                            mdRef.setLOCTYPE(Loctype.URL);
                            mdSec.getContent().add(mdRef);
                        }
                        else
                        {
                            //If we are *not* capturing extra streams to add to Zip package later,
                            // that means we are likely only generating a METS manifest
                            // (i.e. manifestOnly = true)
                            // In this case, the best we can do is take the crosswalked
                            // Stream, base64 encode it, and add in an <mdWrap> field

                            // First, create our <mdWrap>
                            MdWrap mdWrap = new MdWrap();
                            mdWrap.setMIMETYPE(sxwalk.getMIMEType());
                            setMdType(mdWrap, metsName);

                            // Now, create our <binData> and add base64 encoded contents to it.
                            BinData binData = new BinData();
                            Base64 base64 = new Base64(crosswalkedStream);
                            binData.getContent().add(base64);
                            mdWrap.getContent().add(binData);
                            mdSec.getContent().add(mdWrap);
                        }

                        return mdSec;
                    }
                    else
                    {
                        return null;
                    }
                }
                else
                {
                    throw new PackageValidationException("Cannot find " + xwalkName + " crosswalk plugin, either DisseminationCrosswalk or StreamDisseminationCrosswalk");
                }
            }
        }
        catch (InstantiationException e)
        {
            throw new PackageValidationException("Error instantiating Mdsec object: "+ e.toString(), e);
        }
        catch (IllegalAccessException e)
        {
            throw new PackageValidationException("Error instantiating Mdsec object: "+ e.toString(), e);
        }
        catch (java.io.FileNotFoundException fe)
        {
            log.error(fe.getMessage(),fe);
        }
        return null;
    }

    // Get result from crosswalk plugin and add it to the document,
    // including namespaces and schema.
    // returns the new/modified element upon success.
    private MetsElement crosswalkToMetsElement(DisseminationCrosswalk xwalk,
                                               DSpaceObject dso, MetsElement me)
            throws CrosswalkException,
            IOException, SQLException, AuthorizeException
    {
        try
        {
            // add crosswalk's namespaces and schemaLocation to this element:
            String raw = xwalk.getSchemaLocation();
            String sloc[] = raw == null ? null : raw.split("\\s+");
            Namespace ns[] = xwalk.getNamespaces();
            for (int i = 0; i < ns.length; ++i)
            {
                String uri = ns[i].getURI();
                if (sloc != null && sloc.length > 1 && uri.equals(sloc[0]))
                {
                    me.setSchema(ns[i].getPrefix(), uri, sloc[1]);
                }
                else
                {
                    me.setSchema(ns[i].getPrefix(), uri);
                }
            }

            // add result of crosswalk
            PreformedXML pXML = null;
            if (xwalk.preferList())
            {
                List<Element> res = xwalk.disseminateList(dso);
                if (!(res == null || res.isEmpty()))
                {
                    pXML = new PreformedXML(outputter.outputString(res));
                }
            }
            else
            {
                Element res = xwalk.disseminateElement(dso);
                if (res != null)
                {
                    pXML = new PreformedXML(outputter.outputString(res));
                }
            }
            if (pXML != null)
            {
                me.getContent().add(pXML);
                return me;
            }
            return null;
        }
        catch (CrosswalkObjectNotSupported e)
        {
            // ignore this xwalk if object is unsupported.
            if (log.isDebugEnabled())
            {
                log.debug("Skipping MDsec because of CrosswalkObjectNotSupported: dso=" + dso.toString() + ", xwalk=" + xwalk.getClass().getName());
            }
            return null;
        }
    }
}
