/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.UUID;

/**
 * The old DSpace uuid identifier service, used to create uuids or retrieve objects based on their uuid
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
@Component
public class UniversallyUniqueIdentifierProvider extends IdentifierProvider {

    /** log4j category */
    private static Logger log = Logger.getLogger(UniversallyUniqueIdentifierProvider.class);

    /** Prefix registered to no one */
    protected static final String EXAMPLE_PREFIX = "123456789";

    protected String[] supportedPrefixes = new String[]{"info:uuid", "uuid:"};

    @Override
    public boolean supports(Class<? extends Identifier> identifier) {
        return UUID.class.isAssignableFrom(identifier);
    }

    public boolean supports(String identifier)
    {
        for(String prefix : supportedPrefixes){
            if(identifier.startsWith(prefix))
            {
                return true;
            }
        }

        return false;
    }

    public String register(Context context, DSpaceObject dso) {
        try{
            return mint(context, dso);
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create uuid", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID(), e);
        }
    }

    public void register(Context context, DSpaceObject dso, String identifier) {
        try{
            createNewIdentifier(context, dso, identifier);
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create uuid", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID(), e);
        }
    }


    public void reserve(Context context, DSpaceObject dso, String identifier) {
        try{
            TableRow uuid = DatabaseManager.create(context, "Uuid");
            modifyUuidRecord(context, dso, uuid, identifier);
        }catch(Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create uuid", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID());
        }
    }


    /**
     * Mint a Identifier for this object if it does not already exist in the Database.
     * Creates a new uuid in the database, but only if it did not previously exi.
     *
     * @param context DSpace context
     * @param dso The DSpaceObject to create a uuid for
     * @return The newly created uuid
     * @exception java.sql.SQLException If a database error occurs
     */
    public String mint(Context context, DSpaceObject dso) {

        try{
            String uuid = this.lookup(context, dso);

            if(uuid != null)
            {
                return uuid;
            }
            else
            {
                return createNewIdentifier(context, dso, null);
            }

        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create uuid", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID());
        }
    }

    public DSpaceObject resolve(Context context, String identifier, String... attributes) {
        // We can do nothing with this, return null
        try
        {
            TableRow dbuuid = findUuidInternal(context, identifier);

            if (dbuuid == null)
            {
                //Check for an url
                identifier = retrieveUuidOutOfUrl(identifier);
                if(identifier != null)
                {
                    dbuuid = findUuidInternal(context, identifier);
                }

                if(dbuuid == null)
                {
                    return null;
                }
            }

            if ((dbuuid.isColumnNull("resource_type_id")) || (dbuuid.isColumnNull("resource_id")))
            {
                throw new IllegalStateException("No associated resource type");
            }

            // What are we looking at here?
            int uuidtypeid = dbuuid.getIntColumn("resource_type_id");
            int resourceID = dbuuid.getIntColumn("resource_id");

            if (uuidtypeid == Constants.ITEM)
            {
                Item item = Item.find(context, resourceID);

                if (log.isDebugEnabled()) {
                    log.debug("Resolved uuid " + identifier + " to item "
                            + ((item == null) ? (-1) : item.getID()));
                }

                return item;
            }
            else if (uuidtypeid == Constants.COLLECTION)
            {
                Collection collection = Collection.find(context, resourceID);

                if (log.isDebugEnabled()) {
                    log.debug("Resolved uuid " + identifier + " to collection "
                            + ((collection == null) ? (-1) : collection.getID()));
                }

                return collection;
            }
            else if (uuidtypeid == Constants.COMMUNITY)
            {
                Community community = Community.find(context, resourceID);

                if (log.isDebugEnabled())
                {
                    log.debug("Resolved uuid " + identifier + " to community "
                            + ((community == null) ? (-1) : community.getID()));
                }

                return community;
            }
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while resolving uuid to item", "uuid: " + identifier), e);
        }
//        throw new IllegalStateException("Unsupported Uuid Type "
//                + Constants.typeText[uuidtypeid]);
        return null;
    }

    @Override
    public String lookup(Context context, DSpaceObject dso) throws IdentifierNotFoundException, IdentifierNotResolvableException {

        try
        {
            TableRow row = getUuidInternal(context, dso.getType(), dso.getID());

            if (row == null)
            {
                return null;
            }
            else
            {
                return row.getStringColumn("uuid");
            }
        }catch(SQLException sqe){
            throw new IdentifierNotResolvableException(sqe.getMessage(),sqe);
        }
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
        delete(context, dso);
    }

    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        try{
        TableRow row = getUuidInternal(context, dso.getType(), dso.getID());
        if (row != null)
        {
            //Only set the "resouce_id" column to null when unbinding a uuid.
            // We want to keep around the "resource_type_id" value, so that we
            // can verify during a restore whether the same *type* of resource
            // is reusing this uuid!
            row.setColumnNull("resource_id");
            DatabaseManager.update(context, row);

            if(log.isDebugEnabled())
            {
                log.debug("Unbound Uuid " + row.getStringColumn("uuid") + " from object " + Constants.typeText[dso.getType()] + " id=" + dso.getID());
            }

        }
        else
        {
            log.warn("Cannot find Uuid entry to unbind for object " + Constants.typeText[dso.getType()] + " id=" + dso.getID());
        }
        }catch(SQLException sqe)
        {
            throw new IdentifierException(sqe.getMessage(),sqe);
        }

    }

    public static String retrieveUuidOutOfUrl(String url)
            throws SQLException {
        // We can do nothing with this, return null
        if (!url.contains("/"))
        {
            return null;
        }

        String[] splitUrl = url.split("/");

        return splitUrl[splitUrl.length - 2] + "/" + splitUrl[splitUrl.length - 1];
    }

    protected String createNewIdentifier(Context context, DSpaceObject dso, String uuidId) throws SQLException {
        TableRow uuid=null;
        if(uuidId != null)
        {
            uuid = findUuidInternal(context, uuidId);


            if(uuid!=null && !uuid.isColumnNull("resource_id"))
            {
                //Check if this uuid is already linked up to this specified DSpace Object
                if(uuid.getIntColumn("resource_id")==dso.getID() &&
                        uuid.getIntColumn("resource_type_id")==dso.getType())
                {
                    //This uuid already links to this DSpace Object -- so, there's nothing else we need to do
                    return uuidId;
                }
                else
                {
                    //uuid found in DB table & already in use by another existing resource
                    throw new IllegalStateException("Attempted to create a uuid which is already in use: " + uuidId);
                }
            }

        }
        else if(uuid!=null && !uuid.isColumnNull("resource_type_id"))
        {
            //If there is a 'resource_type_id' (but 'resource_id' is empty), then the object using
            // this uuid was previously unbound (see unbindUuid() method) -- likely because object was deleted
            int previousType = uuid.getIntColumn("resource_type_id");

            //Since we are restoring an object to a pre-existing uuid, double check we are restoring the same *type* of object
            // (e.g. we will not allow an Item to be restored to a uuid previously used by a Collection)
            if(previousType != dso.getType())
            {
                throw new IllegalStateException("Attempted to reuse a uuid previously used by a " +
                        Constants.typeText[previousType] + " for a new " +
                        Constants.typeText[dso.getType()]);
            }
        }

        if(uuid==null){
            uuid = DatabaseManager.create(context, "Uuid");
            //uuidId = createId(uuid.getIntColumn("uuid_id"));
        }

        modifyUuidRecord(context, dso, uuid, uuidId);
        return uuidId;
    }

    protected String modifyUuidRecord(Context context, DSpaceObject dso, TableRow uuid, String uuidId) throws SQLException {
        uuid.setColumn("uuid", uuidId);
        uuid.setColumn("resource_type_id", dso.getType());
        uuid.setColumn("resource_id", dso.getID());
        DatabaseManager.update(context, uuid);

        if (log.isDebugEnabled())
        {
            log.debug("Created new uuid for "
                    + Constants.typeText[dso.getType()] + " " + uuidId);
        }
        return uuidId;
    }

    /**
     * Return the uuid for an Object, or null if the Object has no uuid.
     *
     * @param context
     *            DSpace context
     * @param type
     *            The type of object
     * @param id
     *            The id of object
     * @return The uuid for object, or null if the object has no uuid.
     * @exception java.sql.SQLException
     *                If a database error occurs
     */
    protected static TableRow getUuidInternal(Context context, int type, int id)
            throws SQLException
    {
        String sql = "SELECT * FROM Uuid WHERE resource_type_id = ? " +
                "AND resource_id = ?";

        return DatabaseManager.querySingleTable(context, "Uuid", sql, type, id);
    }

    /**
     * Find the database row corresponding to uuid.
     *
     * @param context DSpace context
     * @param uuid The uuid to resolve
     * @return The database row corresponding to the uuid
     * @exception java.sql.SQLException If a database error occurs
     */
    protected static TableRow findUuidInternal(Context context, String uuid)
            throws SQLException {
        if (uuid == null)
        {
            throw new IllegalArgumentException("Uuid is null");
        }
        return DatabaseManager.findByUnique(context, "Uuid", "uuid", uuid);
    }



}
