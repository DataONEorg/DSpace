package org.dataone.rest.api;

import com.sun.jersey.multipart.FormDataParam;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dataone.service.exceptions.*;
import org.dataone.service.mn.tier1.v1.MNCore;
import org.dataone.service.types.v1.*;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dspace.app.util.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Email;
import org.dspace.dataone.statistics.DataOneSolrLogger;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierService;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.utils.DSpace;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * DataONE Member Node Services.
 * @author Mark Diggory
 * @author Lantian Gai
 * @author Mini Pillai
 */
@Path("/")
@Component
public class D1MemberNode {

    @Autowired
    public void setNode(Node node) {
        this.node = node;
    }

    Node node;

    DataOneSolrLogger dataOneSolrLogger = new DataOneSolrLogger();

    Logger log = Logger.getLogger(D1MemberNode.class);

    private static final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    /**
     *
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws InsufficientResources
     */
    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_XML})
    public Node doRootV1Ping() throws NotImplemented, ServiceFailure, InsufficientResources
    {
        return doPing();
    }

    /**
     *
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws InsufficientResources
     */
    @GET
    @Path("/v1")
    @Produces({MediaType.APPLICATION_XML})
    public Node doRootPing() throws NotImplemented, ServiceFailure, InsufficientResources
    {
        return doPing();
    }

    /**
     *
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws InsufficientResources
     */
    @GET
    @Path("/v1/monitor/ping")
    @Produces({MediaType.APPLICATION_XML})
    public Node doPing() throws NotImplemented, ServiceFailure, InsufficientResources
    {
        try{
            Connection connection = DatabaseManager.getConnection();
            if(connection==null)
            {
                throw new ServiceFailure("sql","database connection failed");
            }
        }   catch  (Exception e)
        {
            throw new ServiceFailure("2042",e.getMessage());
        }

        node.getPing().setLastSuccess(new Date());

        return node;
    }

    /**
     *
     * @return
     * @throws NotImplemented
     * @throws ServiceFailure
     */
    @GET
    @Path("/v1/node")
    @Produces({MediaType.APPLICATION_XML})
    public Node getCapabilities() throws NotImplemented, ServiceFailure
    {
        if(node != null)
        {
            return node;
        }
        else
        {
            throw new ServiceFailure("2042","mnCore is null");
        }
    }

    /**
     *
     *  Represents a collection of :class:`Types.LogEntry`
     *  elements, used to transfer log information between DataONE
     *  components.
     *
     * Schema fragment(s) for this class:
     * <pre>
     * &lt;xs:complexType xmlns:ns="http://ns.dataone.org/service/types/v1" xmlns:xs="http://www.w3.org/2001/XMLSchema" name="Log">
     *   &lt;xs:complexContent>
     *     &lt;xs:extension base="ns:Slice">
     *       &lt;xs:sequence>
     *         &lt;xs:element type="ns:LogEntry" name="logEntry" minOccurs="0" maxOccurs="unbounded"/>
     *       &lt;/xs:sequence>
     *     &lt;/xs:extension>
     *   &lt;/xs:complexContent>
     * &lt;/xs:complexType>
     * </pre>
     *
     * A single log entry as reported by a Member Node or
     * Coordinating Node through the :func:`MNCore.getLogRecords` or :func:`CNCore.getLogRecords` methods.
     *
     * Schema fragment(s) for this class:
     * <pre>
     * &lt;xs:complexType xmlns:ns="http://ns.dataone.org/service/types/v1" xmlns:xs="http://www.w3.org/2001/XMLSchema" name="LogEntry">
     *   &lt;xs:sequence>
     *     &lt;xs:element type="xs:string" name="entryId" minOccurs="1" maxOccurs="1"/>
     *     &lt;xs:element type="ns:Identifier" name="identifier" minOccurs="1" maxOccurs="1"/>
     *     &lt;xs:element type="xs:string" name="ipAddress" minOccurs="1" maxOccurs="1"/>
     *     &lt;xs:element type="xs:string" name="userAgent" minOccurs="1" maxOccurs="1"/>
     *     &lt;xs:element type="ns:Subject" name="subject" minOccurs="1" maxOccurs="1"/>
     *     &lt;xs:element type="ns:Event" name="event" minOccurs="1" maxOccurs="1"/>
     *     &lt;xs:element type="xs:dateTime" name="dateLogged" minOccurs="1" maxOccurs="1"/>
     *     &lt;xs:element type="ns:NodeReference" name="nodeIdentifier" minOccurs="1" maxOccurs="1"/>
     *   &lt;/xs:sequence>
     * &lt;/xs:complexType>
     * </pre>
     *
     * Example Response:
     *
     * <d1:log xmlns:d1="http://ns.dataone.org/service/types/v1" count="3" start="0" total="1273">
     *   <logEntry>
     *       <entryId>1</entryId>
     *       <identifier>MNodeTierTests.201260152556757.</identifier>
     *       <ipAddress>129.24.0.17</ipAddress>
     *       <userAgent>null</userAgent>
     *       <subject>CN=testSubmitter,DC=dataone,DC=org</subject>
     *       <event>create</event>
     *       <dateLogged>2012-02-29T23:25:58.104+00:00</dateLogged>
     *       <nodeIdentifier>urn:node:DEMO2</nodeIdentifier>
     *   </logEntry>
     *  </d1:log>
     *
     * Retrieve log information from the Member Node for the specified slice parameters. Log entries will only return PIDs.
     * This method is used primarily by the log aggregator to generate aggregate statistics for nodes, objects, and the methods of access.
     * The response MUST contain only records for which the requestor has permission to read.
     * Note that date time precision is limited to one millisecond. If no timezone information is provided UTC will be assumed.
     * Access control for this method MUST be configured to allow calling by Coordinating Nodes and MAY be configured to allow more general access.
     *
     * Rest URL:
     * GET /log?[fromDate={fromDate}][&toDate={toDate}][&event={event}][&idFilter={idFilter}][&start={start}][&count={count}]
     *
     *
     * @param fromDate fromDate (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.DateTime">Types.DateTime</a>) – Records with time stamp greater than or equal to (>=) this value
     *                 will be returned. Transmitted as a URL query parameter, and so must be escaped accordingly.
     *
     * @param toDate toDate (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.DateTime">Types.DateTime</a>) – Records with a time stamp less than (<) this value will be returned.
     *               If not specified, then defaults to now. Transmitted as a URL query parameter, and so must be
     *               escaped accordingly.
     *
     * @param event event (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.Event">Types.Event</a>) – Return only log records for the specified type of event. Default is all.
     *              Transmitted as a URL query parameter, and so must be escaped accordingly.
     *
     * @param pidFilter pidFilter (string) – Return only log records for identifiers that start with the supplied
     *                  identifier string. Support for this parameter is optional and MAY be ignored by the Member
     *                  Node implementation with no warning. Accepts PIDs and SIDs Transmitted as a URL query parameter,
     *                  and so must be escaped accordingly.
     *
     * @param start start=0 (integer) – Optional zero based offset from the first record in the set of matching log
     *              records. Used to assist with paging the response. Transmitted as a URL query parameter, and so
     *              must be escaped accordingly.
     *
     * @param count count=1000 (integer) – The maximum number of log records that should be returned in the response.
     *              The Member Node may return fewer and the caller should check the total in the response to determine
     *              if further pages may be retrieved. Transmitted as a URL query parameter, and so must be escaped
     *              accordingly.
     *
     * @return Types.Log
     * @throws InvalidRequest (errorCode=400, detailCode=1480) The request parameters were malformed or an invalid date range was specified.
     * @throws InvalidToken   (errorCode=401, detailCode=1470)
     * @throws NotAuthorized  (errorCode=401, detailCode=1460) Raised if the user making the request is not authorized to access the log records. This is determined by the policy of the Member Node.
     * @throws NotImplemented (errorCode=501, detailCode=1461)
     * @throws ServiceFailure (errorCode=500, detailCode=1490)
     */
    @GET
    @Path("/v1/log")
    @Produces({MediaType.APPLICATION_XML})
    public Log getLogRecords(
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate") String toDate,
            @QueryParam("event") String event,
            @QueryParam("pidFilter") String pidFilter,
            @DefaultValue("0") @QueryParam("start") Integer start,
            @DefaultValue("1000") @QueryParam("count") Integer count) throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure
    {

        try{

            Log log = new Log();

            QueryResponse result = dataOneSolrLogger.getLogRecords(
                    fromDate != null ? new DateTime(fromDate).toDate() : null,
                    toDate != null ? new DateTime(toDate).toDate() : null,
                    event,
                    pidFilter,
                    start,
                    count);

            SolrDocumentList list = result.getResults();

            log.setCount(list.size());
            log.setStart((int)list.getStart());
            log.setTotal((int)list.getNumFound());

            for(SolrDocument doc : list)
            {
                LogEntry entry = new LogEntry();
                Identifier identifier = new Identifier();
                identifier.setValue(doc.get("identifier").toString());
                entry.setIdentifier(identifier);
                entry.setDateLogged((Date)doc.get("dateLogged"));
                entry.setEntryId(doc.get("entryId").toString());
                entry.setEvent(Event.valueOf(doc.get("event").toString()));
                entry.setIpAddress(doc.get("ipAddress").toString());
                // TODO: Set from Configuration
                NodeReference ref = new NodeReference();
                ref.setValue("urn:node:DSPACE");
                entry.setNodeIdentifier(ref);
                Subject subject = new Subject();
                subject.setValue(doc.get("subject").toString());
                entry.setSubject(subject);
                entry.setUserAgent(doc.get("userAgent").toString());
                log.addLogEntry(entry);
            }

            return log;

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ServiceFailure("1490", e.getMessage());
        }



    }

    /**
     * Lookup DSpaceObject and Return in specific format. Default format will be used for standard DSpace PID. Alternate
     * Formats may be provided in the future as alternate PID.
     *
     * PID May be any identifier configured in IdentifierService that may be used to resolve the object, examples may
     * include DSpace handle id, CNRI Handles, DOI or other implemented identifiers.
     *
     *
     * Retrieve an object identified by id from the node. Supports both PIDs and SIDs. SID will return HEAD PID.
     *
     * The response MUST contain the bytes of the indicated object, and the checksum of the bytes retrieved
     * SHOULD match the SystemMetadata.checksum recorded in the Types.SystemMetadata when calling with PID.
     *
     * If the object does not exist on the node servicing the request, then Exceptions.NotFound must be raised even
     * if the object exists on another node in the DataONE system.
     *
     * Also implmented by Coordinating Nodes as CNRead.get().
     *
     * Use Cases: UC01, UC06, UC16
     *
     * Rest URL:
     * GET /object/nnnbhj{id}
     *hghb bmnb, b,b,bmb
     * @param pid persistent identifier, must be resolve able in IdentifierService. (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.Identifier">Types.Identifier</a>) – The identifier
     *            for the object to be retrieved. May be a PID or a SID. Transmitted as part of the URL path and must
     *            be escaped accordingly.
     * @return Bytes of the specified object. Types.OctetStream
     * @throws InvalidToken (errorCode=401, detailCode=1010)
     * @throws NotAuthorized (errorCode=401, detailCode=1000)
     * @throws NotImplemented (errorCode=501, detailCode=1001)
     * @throws ServiceFailure (errorCode=500, detailCode=1030)
     * @throws NotFound (errorCode=404, detailCode=1020) The object specified by id does not exist at this node.
     * The description should include a reference to the resolve method.
     * @throws InsufficientResources (errorCode=413, detailCode=1002) The node is unable to service the request
     * due to insufficient resources such as CPU, memory, or bandwidth being over utilized.

     */
    @GET
    @Path("/v1/object/{pid: (.*)+}")
    public Response getObject(@Context HttpServletRequest request, @PathParam("pid") String pid) throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, NotFound, InsufficientResources {

        try {

            DSpace dspace = new DSpace();
            org.dspace.core.Context context = ContextUtil.obtainContext(request);

            IdentifierService identifierService = dspace.getSingletonService(IdentifierService.class);
            DSpaceObject object = identifierService.resolve(context,pid);

            if(object != null && object instanceof Bitstream)
            {
                Bitstream bitstream = (Bitstream)object;

                if(BitstreamUtil.isDeleted(bitstream))
                {
                    throw new NotFound("1020", pid + " Not Found (Deleted)");
                }

                if(!AuthorizeManager.authorizeActionBoolean(context, bitstream, Constants.READ))
                {
                    throw new NotAuthorized("1000","Not Authorized to Access");
                }

                Response response = Response
                        .ok(bitstream.retrieve())
                        .type(bitstream.getFormat().getMIMEType())
                        .header("Content-Disposition", createDispositionHeader(bitstream, pid))
                        .build();

                logEvent(request, pid, Event.READ);

                return response;

            }
            else if(object != null)
            {
                throw new NotImplemented("1001", "Unsupported Object Type " + object.getClass().getName());
            }

            throw new NotFound("1020", pid + " Not Found");

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new ServiceFailure("1030", e.getMessage());
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new ServiceFailure("1030", e.getMessage());
        } catch (AuthorizeException e) {
            log.error(e.getMessage());
            throw new NotAuthorized("1000","Not Authorized to Access " + e.getMessage());
        } catch (IdentifierNotResolvableException e) {
            log.error(e.getMessage());
            throw new NotFound("1020", pid + " Not Found: " + e.getMessage());
        } catch (IdentifierNotFoundException e) {
            log.error(e.getMessage());
            throw new NotFound("1020", pid + " Not Found: " + e.getMessage());
        } finally {

        }
    }

    /**
     *
     *  Describes the object identified by id by returning the associated system metadata object. If the object does
     *  not exist on the node servicing the request, then Exceptions.NotFound MUST be raised even if the object exists
     *  on another node in the DataONE system.
     *
     *  Use Cases: UC06, UC37, UC16
     *
     *  Rest URL:
     *  GET /meta/{pid}
     *
     * Schema fragment(s) for this class:
     * <pre>
     * &lt;xs:complexType xmlns:ns="http://ns.dataone.org/service/types/v1" xmlns:xs="http://www.w3.org/2001/XMLSchema" name="SystemMetadata">
     *   &lt;xs:sequence>
     *     &lt;xs:element type="xs:long" name="serialVersion" minOccurs="0" maxOccurs="1"/>
     *     &lt;xs:element type="ns:Identifier" name="identifier"/>
     *     &lt;xs:element type="ns:ObjectFormatIdentifier" name="formatId"/>
     *     &lt;xs:element type="xs:long" name="size"/>
     *     &lt;xs:element type="ns:Checksum" name="checksum"/>
     *     &lt;xs:element type="ns:Subject" name="submitter" minOccurs="0" maxOccurs="1"/>
     *     &lt;xs:element type="ns:Subject" name="rightsHolder"/>
     *     &lt;xs:element type="ns:AccessPolicy" name="accessPolicy" minOccurs="0" maxOccurs="1"/>
     *     &lt;xs:element type="ns:ReplicationPolicy" name="replicationPolicy" minOccurs="0" maxOccurs="1"/>
     *     &lt;xs:element type="ns:Identifier" name="obsoletes" minOccurs="0" maxOccurs="1"/>
     *     &lt;xs:element type="ns:Identifier" name="obsoletedBy" minOccurs="0" maxOccurs="1"/>
     *     &lt;xs:element type="xs:boolean" name="archived" minOccurs="0" maxOccurs="1"/>
     *     &lt;xs:element type="xs:dateTime" name="dateUploaded" minOccurs="0" maxOccurs="1"/>
     *     &lt;xs:element type="xs:dateTime" name="dateSysMetadataModified" minOccurs="0" maxOccurs="1"/>
     *     &lt;xs:element type="ns:NodeReference" name="originMemberNode" minOccurs="0" maxOccurs="1"/>
     *     &lt;xs:element type="ns:NodeReference" name="authoritativeMemberNode" minOccurs="0" maxOccurs="1"/>
     *     &lt;xs:element type="ns:Replica" name="replica" minOccurs="0" maxOccurs="unbounded"/>
     *   &lt;/xs:sequence>
     * &lt;/xs:complexType>
     * </pre>
     *
     * @param pid (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.Identifier">Types.Identifier</a>) – Identifier for the science data or science metedata object of interest.
     *            May be either a PID or a SID. Transmitted as part of the URL path and must be escaped accordingly.
     * @return Types.SystemMetadata. System metadata object describing the object.
     * @throws InvalidToken (errorCode=401, detailCode=1050)
     * @throws NotAuthorized (errorCode=401, detailCode=1040)
     * @throws NotImplemented (errorCode=501, detailCode=1041)
     * @throws ServiceFailure (errorCode=500, detailCode=1090)
     * @throws NotFound (errorCode=404, detailCode=1060) There is no data or science metadata identified by the given
     * pid on the node where the request was serviced. The error message should provide a hint to use the
     * CNRead.resolve() mechanism.
     *
     */
    @GET
    @Path("/v1/meta/{pid: (.*)+}")
    @Produces({MediaType.APPLICATION_XML})
    public SystemMetadata getSystemMetadata(@Context HttpServletRequest request, @PathParam("pid") String pid) throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, NotFound {

        try {


            DSpace dspace = new DSpace();
            org.dspace.core.Context context = ContextUtil.obtainContext(request);

            IdentifierService identifierService = dspace.getSingletonService(IdentifierService.class);
            DSpaceObject object = identifierService.resolve(context,pid);

            if(object != null && object instanceof Bitstream)
            {
                Bitstream bitstream = (Bitstream)object;

                //boolean isAuthorized = AuthorizeManager.authorizeActionBoolean(context, bitstream, Constants.READ);

                //if(!isAuthorized)
                //{
                //    throw new NotAuthorized("1040","Not Authorized to Access");
                //}

                // send it to output stream
                String mimeType = bitstream.getFormat().getMIMEType();
                //response.setContentType(mimeType);
                log.debug("Setting data file MIME type to: " + mimeType);

                String name = bitstream.getName();

                SystemMetadata  systemMetadata = new SystemMetadata();
                systemMetadata.setArchived(BitstreamUtil.isDeleted(bitstream));
                systemMetadata.setSize(BigInteger.valueOf(bitstream.getSize()));

                Checksum checksum = new Checksum();
                checksum.setValue(bitstream.getChecksum());
                checksum.setAlgorithm(bitstream.getChecksumAlgorithm());
                systemMetadata.setChecksum(checksum);

                // TODO Verify State of Bitstream in Item (Bitstreams that are part of
                // Versions or withdrawn items shouldbe considered archived)
                DSpaceObject parent = bitstream.getParentObject();
                if(parent == null  || (parent instanceof Item && ((Item) parent).isWithdrawn()))
                    systemMetadata.setArchived(true);
                else
                    systemMetadata.setArchived(false);

                Identifier identifier = new Identifier();
                identifier.setValue(pid);
                systemMetadata.setIdentifier(identifier);

                Subject subject = new Subject();
                subject.setValue("test");
                systemMetadata.setRightsHolder(subject);

                AccessPolicy ap = new AccessPolicy();

                for(ResourcePolicy rp : AuthorizeManager.getPoliciesActionFilter(context, bitstream, Constants.READ))
                {
                    if(rp != null && rp.getGroupID() == 0)
                    {
                        AccessRule ar = new AccessRule();
                        Subject pub =  new Subject();
                        pub.setValue("public");
                        ar.setSubjectList(Collections.singletonList(pub));
                        ar.setPermissionList(Collections.singletonList(Permission.READ));
                        ap.setAllowList(Collections.singletonList(ar));
                    }
                }

                systemMetadata.setAccessPolicy(ap);

                Bitstream obsoletedByBitstream = BitstreamUtil.getObsoletedBy(context,bitstream);
                if(obsoletedByBitstream!=null)
                {
                    Identifier obsoletedByIdentifier = new Identifier();
                    obsoletedByIdentifier.setValue("ds:bitstream/"+obsoletedByBitstream.getID());
                    systemMetadata.setObsoletedBy(obsoletedByIdentifier);
                }

                Bitstream obsoleteBitstream = BitstreamUtil.getObsoletes(context,bitstream);
                if(obsoleteBitstream!=null){
                    Identifier obsoleteIdentifier = new Identifier();
                    obsoleteIdentifier.setValue("ds:bitstream/"+obsoleteBitstream.getID());
                    systemMetadata.setObsoletes(obsoleteIdentifier);
                }
                 /*
                <allow>
                <subject>public</subject>
                <permission>read</permission>
                </allow>
                   */
                ObjectFormatIdentifier objectFormatIdentifier = new ObjectFormatIdentifier();
                objectFormatIdentifier.setValue(bitstream.getFormat().getMIMEType());
                systemMetadata.setFormatId(objectFormatIdentifier);

                systemMetadata.setDateSysMetadataModified(BitstreamUtil.getLastModifiedDate(context, bitstream));
                systemMetadata.setDateUploaded(BitstreamUtil.getDateCreated(context, bitstream));

                return systemMetadata;
            }
            else if(object != null)
            {
                throw new NotImplemented("1041", "Unsupported Object Type " + object.getClass().getName());
            }

            throw new NotFound("1060", pid + " Not Found");

        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new ServiceFailure("1090", e.getMessage());
        } catch (IdentifierNotResolvableException e) {
            log.error(e.getMessage());
            throw new NotFound("1060", pid + " Not Found: " + e.getMessage());
        } catch (IdentifierNotFoundException e) {
            log.error(e.getMessage());
            throw new NotFound("1060", pid + " Not Found: " + e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new NotFound("1060", pid + " Not Found: " + e.getMessage());
        }

    }

    /**
     * This method provides a lighter weight mechanism than MNRead.getSystemMetadata() for a client to determine basic
     * properties of the referenced object. The response should indicate properties that are typically returned in a
     * HTTP HEAD request: the date late modified, the size of the object, the type of the object
     * (the SystemMetadata.formatId).
     *
     * The principal indicated by token must have read privileges on the object, otherwise Exceptions.NotAuthorized
     * is raised.
     *
     * If the object does not exist on the node servicing the request, then Exceptions.NotFound must be raised even
     * if the object exists on another node in the DataONE system.
     *
     * Note that this method is likely to be called frequently and so efficiency should be taken into consideration
     * during implementation.
     *
     * Use Cases: UC16
     *
     * Rest URL:
     * HEAD /object/{pid}
     *
     * curl -I http://mn1.dataone.org/mn/v1/object/ABC123
     *
     * HTTP/1.1 200 OK
     * Last-Modified: Wed, 16 Dec 2009 13:58:34 GMT
     * Content-Length: 10400
     * Content-Type: application/octet-stream
     * DataONE-formatId: eml://ecoinformatics.org/eml-2.0.1
     * DataONE-Checksum: SHA-1,2e01e17467891f7c933dbaa00e1459d23db3fe4f
     * DataONE-SerialVersion: 1234
     *
     * @param request
     * @param pid (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.Identifier">Types.Identifier</a>) – Identifier for the object in question. May be either a PID or a SID.
     *            Transmitted as part of the URL path and must be escaped accordingly.
     * @return Types.DescribeResponse A set of values providing a basic description of the object.
     * @throws InvalidRequest
     * @throws InvalidToken (errorCode=401, detailCode=1370)
     * @throws NotAuthorized (errorCode=401, detailCode=1360)
     * @throws NotImplemented (errorCode=501, detailCode=1361)
     * @throws ServiceFailure (errorCode=500, detailCode=1390)
     * @throws NotFound (errorCode=404, detailCode=1380)
     */
    @HEAD
    @Path("/v1/object/{pid: (.*)+}")
    @Produces({MediaType.APPLICATION_XML})
    public Response getDescribeObject(@Context HttpServletRequest request, @PathParam("pid") String pid)  throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, NotFound {

        try{

            SystemMetadata systemMetadata = this.getSystemMetadata(request, pid);

            Response.ResponseBuilder rb = Response.ok();

            rb = rb.header("Content-Length",systemMetadata.getSize().toString());
            rb = rb.header("Content-Type",systemMetadata.getFormatId().getValue());

            if(systemMetadata.getDateUploaded() != null)
            {
                rb = rb.header("Last-Modified",format.format(systemMetadata.getDateUploaded()));
            }

            rb = rb.header("DataONE-formatId",systemMetadata.getFormatId().getValue());
            rb = rb.header("DataONE-Checksum",systemMetadata.getChecksum().getAlgorithm() + "," + systemMetadata.getChecksum().getValue());
            rb = rb.header("DataONE-SerialVersion",systemMetadata.getSerialVersion() != null ? systemMetadata.getSerialVersion().toString() : "1");

            return rb.build();

        } catch (NotFound notFound) {


            throw new NotFound("1380", notFound.getMessage());
        } catch (InvalidToken invalidToken) {
            throw new InvalidToken("1370", invalidToken.getMessage());
        } catch (NotAuthorized notAuthorized) {
            throw new NotAuthorized("1360", notAuthorized.getMessage());
        } catch (NotImplemented notImplemented) {
            throw new NotImplemented("1361", notImplemented.getMessage());
        } catch (ServiceFailure serviceFailure) {
            throw new ServiceFailure("1390", serviceFailure.getMessage());
        }
    }

    /**
     *
     * Represents the value of a computed :term:`checksum`
     * expressed as a hexadecimal formatted version of the message digest. Note
     * that these hex values should be treated as case-insensitive strings, in
     * that leading zeros must be preserved, and digests can use a mixture of
     * upper and lower case letters to represent the hex values. Comparison
     * algorithms MUST be able to handle any variant of these representations
     * (e.g., by performing a case-insensitive string match on hex digests from
     * the same algorithm).
     *
     * Rest URL:
     * GET /checksum/{pid}[?checksumAlgorithm={checksumAlgorithm}]
     *
     * Schema fragment(s) for this class:
     * <pre>
     * &lt;xs:complexType xmlns:ns="http://ns.dataone.org/service/types/v1" xmlns:xs="http://www.w3.org/2001/XMLSchema" name="Checksum">
     *   &lt;xs:simpleContent>
     *     &lt;xs:extension base="xs:string">
     *       &lt;xs:attribute type="xs:string" use="required" name="algorithm"/>
     *     &lt;/xs:extension>
     *   &lt;/xs:simpleContent>
     * &lt;/xs:complexType>
     * </pre>
     *
     * @param pid (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.Identifier">Types.Identifier</a>) – The identifier of the object the operation is being performed on. Transmitted as
     *            part of the URL path and must be escaped accordingly.
     * @param checksumAlgorithm (string) – The name of an algorithm that will be used to compute a checksum of the
     *                          bytes of the object. This value is drawn from a DataONE controlled list of values as
     *                          indicted in the Types.SystemMetadata. If not specified, then the system wide default
     *                          checksum algorithm should be used. Transmitted as a URL query parameter, and so must
     *                          be escaped accordingly.

     * @return Returns a Types.Checksum for the specified object using an accepted hashing algorithm. The result is
     * used to determine if two instances referenced by a PID are identical, hence it is necessary that MNs can
     * ensure that the returned checksum is valid for the referenced object either by computing it on the fly or
     * by using a cached value that is certain to be correct.
     *
     * @throws InvalidRequest (errorCode=400, detailCode=1402) A supplied parameter was invalid, most likely an
     * unsupported checksum algorithm was specified, in which case the error message should include an enumeration
     * of supported checksum algorithms.
     * @throws InvalidToken (errorCode=401, detailCode=1430)
     * @throws NotAuthorized (errorCode=401, detailCode=1400)
     * @throws NotImplemented (errorCode=501, detailCode=1401)
     * @throws ServiceFailure (errorCode=500, detailCode=1410)
     * @throws NotFound (errorCode=404, detailCode=1420)
     */
    @GET
    @Path("/v1/checksum/{pid: (.*)+}")
    @Produces({MediaType.APPLICATION_XML})
    public Checksum getChecksum(@Context HttpServletRequest request, @PathParam("pid") String pid, @QueryParam("checksumAlgorithm") String checksumAlgorithm) throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, NotFound {

        try {

            DSpace dspace = new DSpace();
            org.dspace.core.Context context = ContextUtil.obtainContext(request);

            IdentifierService identifierService = dspace.getSingletonService(IdentifierService.class);
            DSpaceObject object = identifierService.resolve(context,pid);

            if(object != null && object instanceof Bitstream)
            {
                Bitstream bitstream = (Bitstream)object;

                boolean isAuthorized = AuthorizeManager.authorizeActionBoolean(context, bitstream, Constants.READ);

                if(!isAuthorized)
                {
                    throw new NotAuthorized("1400","Not Authorized to Access");
                }

                // TODO Provide Check for appropriate Algorithms
                Checksum checksum = new Checksum();
                checksum.setValue(bitstream.getChecksum());
                checksum.setAlgorithm(bitstream.getChecksumAlgorithm());

                return checksum;
            }
            else if(object != null)
            {
                throw new NotImplemented("1401", "Unsupported Object Type " + object.getClass().getName());
            }

            throw new NotFound("1420", pid + " Not Found");

        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new ServiceFailure("1410", e.getMessage());
        } catch (IdentifierNotResolvableException e) {
            log.error(e.getMessage());
            throw new NotFound("1420", pid + " Not Found: " + e.getMessage());
        } catch (IdentifierNotFoundException e) {
            log.error(e.getMessage());
            throw new NotFound("1420", pid + " Not Found: " + e.getMessage());
        }

    }

    /**
     *
     * Retrieve the list of objects present on the MN that match the calling parameters. This method is required to
     * support the process of Member Node synchronization. At a minimum, this method MUST be able to return a list
     * of objects that match: fromDate < SystemMetadata.dateSysMetadataModified but is expected to also support date
     * range (by also specifying toDate), and should also support slicing of the matching set of records by indicating
     * the starting index of the response (where 0 is the index of the first item) and the count of elements to be
     * returned.
     *
     * Note that date time precision is limited to one millisecond. If no timezone information is provided, the UTC
     * will be assumed.
     *
     * Use Cases: UC06, UC16
     *
     * Rest URL:
     * GET /object[?fromDate={fromDate}&toDate={toDate}&identifier={identifier}&formatId={formatId}&replicaStatus={replicaStatus} &start={start}&count={count}]
     *
     * Schema fragment(s) for this class:
     * <pre>
     * &lt;xs:complexType xmlns:ns="http://ns.dataone.org/service/types/v1" xmlns:xs="http://www.w3.org/2001/XMLSchema" name="ObjectInfo">
     *   &lt;xs:sequence>
     *     &lt;xs:element type="ns:Identifier" name="identifier" minOccurs="1" maxOccurs="1"/>
     *     &lt;xs:element type="ns:ObjectFormatIdentifier" name="formatId"/>
     *     &lt;xs:element type="ns:Checksum" name="checksum" minOccurs="1" maxOccurs="1"/>
     *     &lt;xs:element type="xs:dateTime" name="dateSysMetadataModified"/>
     *     &lt;xs:element type="xs:long" name="size"/>
     *   &lt;/xs:sequence>
     * &lt;/xs:complexType>
     * </pre>
     *
     * @param identifier (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.Identifier">Types.Identifier</a>) – Restrict results to the specified identifier. May be a PID or a SID.
     *                   In the case of the latter, returns a listing of all PIDs that share the given SID.
     *                   Transmitted as a URL query parameter, and so must be escaped accordingly.
     *
     * @param fromDate (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.DateTime">Types.DateTime</a>) – Entries with SystemMetadata.dateSysMetadataModified greater than (>) fromDate
     *                 must be returned. Transmitted as a URL query parameter, and so must be escaped accordingly.
     *
     * @param toDate (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.DateTime">Types.DateTime</a>) – Entries with SystemMetadata.dateSysMetadataModified less than (<) toDate must
     *               be returned. Transmitted as a URL query parameter, and so must be escaped accordingly.
     *
     * @param formatId (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.ObjectFormatIdentifier">Types.ObjectFormatIdentifier</a>) – Restrict results to the specified object format identifier.
     *                 Transmitted as a URL query parameter, and so must be escaped accordingly.
     *
     * @param replicaStatus TBD
     *
     * @param start start=0 (integer) – The zero-based index of the first value, relative to the first record of the
     *              resultset that matches the parameters. Transmitted as a URL query parameter, and so must be escaped
     *              accordingly.
     *
     * @param count count=1000 (integer) – The maximum number of entries that should be returned in the response.
     *              The Member Node may return fewer and the caller should check the total in the response to determine
     *              if further pages may be retrieved. Transmitted as a URL query parameter, and so must be escaped
     *              accordingly.
     *
     * @return Types.ObjectList The list of PIDs that match the query criteria. If none match, an empty list is returned.
     *
     * @throws InvalidRequest (errorCode=400, detailCode=1540)
     * @throws InvalidToken (errorCode=401, detailCode=1530)
     * @throws NotAuthorized (errorCode=401, detailCode=1520)
     * @throws NotImplemented (errorCode=501, detailCode=1560) Raised if some functionality requested is not implemented.
     * In the case of an optional request parameter not being supported, the errorCode should be 400. If the requested
     * format (through HTTP Accept headers) is not supported, then the standard HTTP 406 error code should be returned.
     * @throws ServiceFailure (errorCode=500, detailCode=1580)
     */
    @GET
    @Path("/v1/object")
    @Produces({MediaType.APPLICATION_XML})
    public ObjectList listObjects(
            @QueryParam("identifier") String identifier,
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate") String toDate,
            @QueryParam("formatId") String formatId,
            @QueryParam("replicaStatus") Boolean replicaStatus,
            @QueryParam("start") Integer start,
            @QueryParam("count") Integer count) throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure {

        QueryResponse results = null;

        try{
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery("*:*");
            solrQuery.addFilterQuery("search.resourcetype:" + Constants.BITSTREAM);
            solrQuery.addFilterQuery("deleted_b:" + false );
            solrQuery.addSort(new SolrQuery.SortClause("dateSysMetadataModified_dt",SolrQuery.ORDER.desc));

            if(identifier != null)
                solrQuery.addFilterQuery("identifier_s:" + ClientUtils.escapeQueryChars(identifier));

            if(formatId != null)
            {
                if(OREManifestWriter.ORE.NS.contains(formatId))
                {
                    formatId = OREManifestWriter.ORE.NS;
                }

                solrQuery.addFilterQuery("formatId_s:" + ClientUtils.escapeQueryChars(formatId));
            }
            if(replicaStatus != null)
                solrQuery.addFilterQuery("replicaStatus:" + replicaStatus);

            if(fromDate!=null&&toDate!=null)
            {
                solrQuery.addFilterQuery("dateSysMetadataModified_dt:["+parseDate(fromDate)+" TO "+parseDate(toDate)+"}");
            }
            else if(fromDate!=null)
            {
                solrQuery.addFilterQuery("dateSysMetadataModified_dt:["+parseDate(fromDate)+" TO *}");
            }
            else if(toDate!=null)
            {
                solrQuery.addFilterQuery("dateSysMetadataModified_dt:[* TO "+parseDate(toDate)+"}");
            }

            if(start==null||start<0){
                solrQuery.setStart(0);
            }
            else
            {
                solrQuery.setStart(start);
            }
            if(count==null||count<0){
                solrQuery.setRows(100);
            }
            else {
                solrQuery.setRows(count);
            }

            results = getSolr().query(solrQuery);

        } catch (SolrServerException e) {
            log.error(e.getMessage(),e);
            throw new InvalidRequest ("1540", e.getMessage());
        }

        ObjectList objectList = new ObjectList();

        if(results!=null){

            SolrDocumentList solrDocuments =  results.getResults();

            objectList.setTotal((int) solrDocuments.getNumFound());
            objectList.setCount(solrDocuments.size());
            objectList.setStart((int) solrDocuments.getStart());

            for(SolrDocument solrDocument : solrDocuments)
            {
                String id = (String) solrDocument.getFirstValue("identifier_s");
                String format = (String) solrDocument.getFirstValue("formatId_s");
                Date dateSysMetadataModified = (Date) solrDocument.getFirstValue("dateSysMetadataModified_dt");
                Long size = (Long) solrDocument.getFirstValue("size_l");
                String checksum = (String)solrDocument.getFirstValue("checksum_s");

                ObjectInfo objectInfo = new ObjectInfo();
                Identifier identifierObj = new Identifier();
                identifierObj.setValue(id != null ? id : "null");
                objectInfo.setIdentifier(identifierObj);

                ObjectFormatIdentifier objectFormat = new ObjectFormatIdentifier();
                objectFormat.setValue(format != null ? format : "octet/stream");
                objectInfo.setFormatId(objectFormat);

                try {
                    objectInfo.setDateSysMetadataModified(dateSysMetadataModified != null ? dateSysMetadataModified : new Date());
                }catch (Exception e)
                {
                    log.debug(e.getMessage(),e);
                }

                BigInteger sizeObject = new BigInteger(size != null && size.longValue() > 0 ? size.toString() : "0");
                objectInfo.setSize(sizeObject);

                Checksum checksumObject = new Checksum();
                checksumObject.setValue(checksum == null || "0".equals(size) ? "d41d8cd98f00b204e9800998ecf8427e" : checksum);
                checksumObject.setAlgorithm("MD5");
                objectInfo.setChecksum(checksumObject);



                objectList.addObjectInfo(objectInfo);
            }
        }

        return objectList;
    }

    static SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static DateTimeFormatter formatter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    private String parseDate(String date)
    {
          return formatter.print(new DateTime(date));
    }

    /**
     * This is a callback method used by a CN to indicate to a MN that it cannot complete synchronization of the
     * science metadata identified by pid. When called, the MN should take steps to record the problem description
     * and notify an administrator or the data owner of the issue.
     *
     * A successful response is indicated by a HTTP status of 200. An unsuccessful call is indicated by a returned
     * exception and associated HTTP status code.
     *
     * Access control for this method MUST be configured to allow calling by Coordinating Nodes and MAY be configured
     * to allow more general access.
     *
     * Use Cases: UC06
     *
     * Rest URL:
     * POST /error
     *
     * Optionally raised by the receiving MN, depending on implementation.
     *
     * @param message message (Types.Exception) – An instance of the Exceptions.SynchronizationFailed exception
     *                with body appropriately filled. Transmitted as an UTF-8 encoded XML structure for the
     *                respective type as defined in the DataONE types schema, as a File part of the MIME
     *                multipart/mixed message.

     * @return Types.Boolean A successful response is indicated by a HTTP 200 status. An unsuccessful call is indicated
     * by returning the appropriate exception.
     *
     * @throws InvalidToken     (errorCode=401, detailCode=2164)
     * @throws NotAuthorized    (errorCode=401, detailCode=2162)
     * @throws NotImplemented   (errorCode=501, detailCode=2160)
     * @throws ServiceFailure   (errorCode=500, detailCode=2161)
     */
    @POST
    @Path("/v1/error")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_XML})
    public boolean synchronizationFailed(
            @FormDataParam("message") String message) throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure {

        try {

            log.error(message);

            Email email = Email.getEmail("dataone_error");
            email.addRecipient(ConfigurationManager.getProperty("alert.recipient"));
            email.addArgument(format.format(new Date()));
            email.addArgument(message);
            email.send();

            return true;

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new ServiceFailure("2161", e.getMessage());
        } catch (MessagingException e) {
            log.error(e.getMessage(), e);
            throw new ServiceFailure("2161", e.getMessage());
        }

    }

    /**
     * Called by a target Member Node to fullfill the replication request originated by a Coordinating Node calling
     * MNReplication.replicate(). This is a request to make a replica copy of the object, and differs from a call to
     * GET /object in that it should be logged as a replication event rather than a read event on that object.
     * If the object being retrieved is restricted access, then a Tier 2 or higher Member Node MUST make a call to
     * CNReplication.isNodeAuthorized() to verify that the Subject of the caller is authorized to retrieve the content.
     *
     * A successful operation is indicated by a HTTP status of 200 on the response.
     *
     * Failure of the operation MUST be indicated by returning an appropriate exception.
     *
     * Use Cases: UC09
     *
     * Rest URL:
     * GET /replica/{pid}
     *
     * @param pid pid (<a href="http://mule1.dataone.org/ArchitectureDocs-current/apis/Types.html#Types.Identifier">Types.Identifier</a>) – The identifier of the object to get as a replica Transmitted as part
     *            of the URL path and must be escaped accordingly.
     *
     * @return Types.OctetStream Bytes of the specified object.
     * @throws InvalidToken   (errorCode=401, detailCode=2183)
     * @throws NotAuthorized  (errorCode=401, detailCode=2182)
     * @throws NotImplemented (errorCode=501, detailCode=2180)
     * @throws ServiceFailure (errorCode=500, detailCode=2181)
     * @throws NotFound       (errorCode=404, detailCode=2185)
     * @throws InsufficientResources (errorCode=413, detailCode=2184) The node is unable to service the request due
     * to insufficient resources such as CPU, memory, or bandwidth being over utilized.
     */
    @GET
    @Path("/v1/replica/{pid: (.*)+}")
    public Response getReplica(@Context HttpServletRequest request, @PathParam("pid") String pid) throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, NotFound, InsufficientResources {

        try {

            DSpace dspace = new DSpace();
            org.dspace.core.Context context = ContextUtil.obtainContext(request);

            IdentifierService identifierService = dspace.getSingletonService(IdentifierService.class);
            DSpaceObject object = identifierService.resolve(context,pid);

            if(object != null && object instanceof Bitstream)
            {
                Bitstream bitstream = (Bitstream)object;

                if(!AuthorizeManager.authorizeActionBoolean(context, bitstream, Constants.READ))
                {
                    throw new NotAuthorized("2182","Not Authorized to Access");
                }

                Response response = Response
                        .ok(bitstream.retrieve())
                        .type(bitstream.getFormat().getMIMEType())
                        .header("Content-Disposition", createDispositionHeader(bitstream, pid))
                        .build();

                logEvent(request, pid, Event.REPLICATE);

                return response;

            }
            else if(object != null)
            {
                throw new NotImplemented("2180", "Unsupported Object Type " + object.getClass().getName());
            }

            throw new NotFound("2185", pid + " Not Found");

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new ServiceFailure("2181", e.getMessage());
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new ServiceFailure("2181", e.getMessage());
        } catch (AuthorizeException e) {
            log.error(e.getMessage());
            throw new NotAuthorized("2182","Not Authorized to Access " + e.getMessage());
        } catch (IdentifierNotResolvableException e) {
            log.error(e.getMessage());
            throw new NotFound("2185", pid + " Not Found: " + e.getMessage());
        } catch (IdentifierNotFoundException e) {
            log.error(e.getMessage());
            throw new NotFound("2185", pid + " Not Found: " + e.getMessage());
        }
    }

    /**
     * Submit a query against the specified queryEngine and return the response as formatted by the queryEngine.
     * The MNQuery.query() operation may be implemented by more than one type of search engine and the queryEngine
     * parameter indicates which search engine is targeted. The value and form of query is determined by the specific
     * query engine.
     *
     * For example, the SOLR search engine will accept many of the standard parameters of SOLR, including field
     * restrictions and faceting.
     *
     * This method is optional for Member Nodes, but if implemented, both getQueryEngineDescription and listQueryEngines
     * must also be implemented.
     *
     * @param queryEngine specify the engine will be used for this query
     * @param query query string.
     * @return Jersey Response Object, including the list of objects return from query engine.
     * @throws InvalidRequest
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws NotFound
     */
    @GET

    @Path("/v1/query/{queryEngine}/{query}")
    @Produces({MediaType.APPLICATION_XML})
    public InputStream query(
            @PathParam("queryEngine") String queryEngine,
            @PathParam("query") String query
    ) throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented, NotFound {
        throw new NotImplemented("","");
    }

    @GET
    @Path("/v1/query/{queryType}")
    @Produces({MediaType.APPLICATION_XML})
    public QueryEngineDescription getQueryEngineDescription(
            @PathParam("queryType") String queryEngine) throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, NotFound {
        throw new NotImplemented("","");
    }

    @GET
    @Path("/v1/query")
    @Produces({MediaType.APPLICATION_XML})
    public QueryEngineList listQueryEngines() throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented {
        throw new NotImplemented("","");
    }

    @GET
    @Path("/v1/isAuthorized/{pid}")
    @Produces({MediaType.APPLICATION_XML})
    public boolean isAuthorized(
            @PathParam("pid") String pid,
            @QueryParam("action") String action
    ) throws ServiceFailure, InvalidRequest, InvalidToken, NotFound, NotAuthorized, NotImplemented {
        throw new NotImplemented("","");
    }


    private HttpSolrServer solr = null;

    private HttpSolrServer getSolr() throws ServiceFailure {

        if ( solr == null)
        {
            String solrService = new DSpace().getConfigurationService().getProperty("discovery.search.server");

            UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);

            if (urlValidator.isValid(solrService))
            {
                try {
                    log.debug("Solr URL: " + solrService);
                    solr = new HttpSolrServer(solrService);

                    solr.setBaseURL(solrService);

                    SolrQuery solrQuery = new SolrQuery()
                            .setQuery("search.resourcetype:2 AND search.resourceid:1");

                    solr.query(solrQuery);

                } catch (SolrServerException e) {
                    log.error("Error while initializing solr server", e);
                    throw new ServiceFailure("1580", "Solr Is Not Available. " + e.getMessage());
                }
            }
            else
            {
                log.error("Error while initializing solr, invalid url: " + solrService);
                throw new ServiceFailure("1580", "Solr Is Not Available. Invalid URL: " + solrService);
            }
        }

        return solr;
    }

    private String createDispositionHeader(Bitstream bitstream, String pid)
    {
        String name = bitstream.getName();

        if(name == null || name.trim().equals(""))
        {
            //flatten identifier
            name = pid.replace("/","_").replace(".","_");
        }

        String ext = "";

        // Some rough checking for file extension possibilities
        if(!name.contains(".")
                && bitstream.getFormat() != null
                && bitstream.getFormat().getExtensions() != null
                && bitstream.getFormat().getExtensions().length > 0)
        {
            ext = "." + bitstream.getFormat().getExtensions()[0];
        }

        return "attachment; filename=\"" + name + ext;
    }

    private void logEvent(HttpServletRequest request, String pid, Event event)
    {
        try {
            this.dataOneSolrLogger.logEvent(request, pid, event.name(), new Date(), this.getCapabilities().getIdentifier().getValue());
        } catch(Exception e){
            log.error(e.getMessage(),e);
        }
    }

}
