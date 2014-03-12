package org.dspace.identifier;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 * User: mdiggory
 * Date: 6/11/14
 * Time: 11:19 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class InternalIdentifierProvider extends IdentifierProvider {

    /** log4j category */
    private static Logger log = Logger.getLogger(UniversallyUniqueIdentifierProvider.class);

    protected String[] supportedPrefixes = new String[]{"info:ds", "ds"};

    @Override
    public boolean supports(Class<? extends Identifier> identifier) {
        return InternalIdentifier.class.isAssignableFrom(identifier);
    }

    @Override
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

    @Override
    /**
     * https://host/dataone/object/ds:bitstream/1234
     */
    public DSpaceObject resolve(Context context, String identifier, String... attributes) throws IdentifierNotFoundException, IdentifierNotResolvableException {

        // https://host/dataone/object/ds:bitstream/1234
        if(identifier.startsWith("ds:bitstream/"))
        {
            try {
                return Bitstream.find(context, Integer.parseInt(identifier.replace("ds:bitstream/","")));
            } catch (Exception e) {
                log.error(e.getMessage(),e);
            }
        }
        else if(identifier.startsWith("ds:item/"))
        {
            try {
                return Item.find(context, Integer.parseInt(identifier.replace("ds:item/", "")));
            } catch (Exception e) {
                log.error(e.getMessage(),e);
            }
        }

        return null;
    }

    @Override
    public String lookup(Context context, DSpaceObject object) throws IdentifierNotFoundException, IdentifierNotResolvableException {

        if(object instanceof Bitstream)
        {
            return "ds:bitstream/" + object.getID();
        }

        return null;
    }

    @Override
    public String register(Context context, DSpaceObject item) throws IdentifierException {
        return null; //Not Implemented
    }

    @Override
    public String mint(Context context, DSpaceObject dso) throws IdentifierException {
        return null; //Not Implemented
    }

    @Override
    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        //Not Implemented
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
        //Not Implemented
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
        //Not Implemented
    }

    @Override
    public void register(Context context, DSpaceObject object, String identifier) throws IdentifierException {
        //Not Implemented
    }
}
