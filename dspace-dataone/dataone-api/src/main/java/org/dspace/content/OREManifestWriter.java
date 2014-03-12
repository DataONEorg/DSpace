package org.dspace.content;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import org.dspace.bitstore.BitstreamStorageOutputStream;
import org.dspace.bitstore.ExtendedBitstreamStorageManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.versioning.Version;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * A Simple Jena based ORE Bitstream writer for DataONE.
 * @author Mark Diggory
 */
public class OREManifestWriter {

    static SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected String retrieveIdentifier(Bitstream b)
    {
        return retrieveIdentifier(String.valueOf(b.getID()));
    }

    protected String retrieveIdentifier(String id)
    {
        try {
            return URLEncoder.encode("dc:bitstream/" + id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return URLEncoder.encode("dc:bitstream/" + id);
        }
    }



    public Bitstream updateORE(Context c, Item newItem, Version version, boolean b) throws NoSuchAlgorithmException, SQLException, IOException {

        // create an empty Model
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("cito",CITO.NS);
        model.setNsPrefix("ore",ORE.NS);
        model.setNsPrefix("dc",DC.NS);
        model.setNsPrefix("dcterms",DCTerms.NS);

        BitstreamStorageOutputStream bos = ExtendedBitstreamStorageManager.store(c, BitstreamFormat.findByShortDescription(c, ORE.NS));

        int id = bos.getBitstreamID();

        Bitstream mets = version.getAIPBitstream();

        String rem_id = this.retrieveIdentifier(String.valueOf(id));

        Resource aggregation = model.createResource("https://cn.dataone.org/cn/v1/resolve/" + rem_id + "#aggregation")
                .addProperty(RDF.type, ORE.Aggregation)
                .addProperty(DCTerms.title, newItem.getMetadata("dc.title")[0].value);

        Resource rem = model.createResource("https://cn.dataone.org/cn/v1/resolve/" + rem_id)
                .addProperty(RDF.type, ORE.ResourceMap)
                .addProperty(DC.format,"application/rdf+xml")
                .addProperty(DCTerms.created, dateFormatUTC.format(new Date()))
                .addProperty(DCTerms.creator, ConfigurationManager.getProperty("dspace.url"))
                .addProperty(DCTerms.identifier, rem_id)
                .addProperty(DCTerms.modified, dateFormatUTC.format(new Date()))
                .addProperty(ORE.describes, aggregation);

        Resource scimeta = model.createResource("https://cn.dataone.org/cn/v1/resolve/" + this.retrieveIdentifier(mets))
                .addProperty(DCTerms.identifier, this.retrieveIdentifier(mets));

        for(Bundle bundle : newItem.getBundles())
        {
            for(Bitstream bits : bundle.getBitstreams())
            {
                Resource scidata = model.createResource("https://cn.dataone.org/cn/v1/resolve/" + this.retrieveIdentifier(bits));

                if(bits.getDescription() != null)
                {
                    scidata.addProperty(DCTerms.description,bits.getDescription());
                }

                if(bits.getName() != null)
                {
                    scidata.addProperty(DCTerms.title,bits.getName());
                }

                scidata.addProperty(DCTerms.identifier, this.retrieveIdentifier(bits));
                scidata.addProperty(CITO.isDocumentedBy, scimeta);

                scimeta.addProperty(CITO.documents, scidata);

                aggregation.addProperty(ORE.aggregates, scidata);
            }
        }


        model.write(bos);
        bos.close();
        return Bitstream.find(c,bos.getBitstreamID());
    }

    public static class ORE {

        /** <p>The RDF model that holds the vocabulary terms</p> */
        private static Model m_model = ModelFactory.createDefaultModel();
        /** <p>The namespace of the vocabulary as a string</p> */
        public static final String NS = "http://www.openarchives.org/ore/terms/";
        public static final Property describes = m_model.createProperty(NS + "describes");
        public static final Property aggregates = m_model.createProperty(NS + "aggregates");
        public static final Resource Aggregation = m_model.createProperty(NS + "Aggregation");
        public static final Resource ResourceMap = m_model.createProperty(NS + "ResourceMap");
    }

    public static class CITO {

        /** <p>The RDF model that holds the vocabulary terms</p> */
        private static Model m_model = ModelFactory.createDefaultModel();
        /** <p>The namespace of the vocabulary as a string</p> */
        public static final String NS = "http://purl.org/spar/cito/";
        public static final Property isDocumentedBy = m_model.createProperty(NS + "isDocumentedBy");
        public static final Property documents = m_model.createProperty(NS + "documents");
    }
}