package org.dspace.dataone.statistics;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.core.ConfigurationManager;


/**
 * This class maintains the database of DataOne log events - logging requests as they occur and
 * generating log records when requested.
 *
 * @author Peter E. Midford
 * @author Mark Diggory
 **/
public class DataOneSolrLogger {

    private final HttpSolrServer solr;

    static Logger log = Logger.getLogger(DataOneSolrLogger.class);

    static SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static final String LUCENE_ESCAPE_CHARS = "[\\\\+\\-\\!\\(\\)\\:\\^\\]\\{\\}\\~\\*\\?\\/]";
    private static final Pattern LUCENE_PATTERN = Pattern.compile(LUCENE_ESCAPE_CHARS);
    private static final String REPLACEMENT_STRING = "\\\\$0";

    static {
        dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public DataOneSolrLogger() {

        final String serverString = ConfigurationManager.getProperty("dataone","server");

        log.info("dataone.server from configuration :" + serverString);

        HttpSolrServer server = null;

        try{
            server = new HttpSolrServer(serverString);
            log.info("Solr server for dataone logging initialized from " + serverString);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            server = null;
        }
        finally{
            solr = server;
        }

    }

    /**
     * Store the DataONE Event into Solr
     * @param request
     * @param identifier
     * @param event
     * @param dateLogged
     * @param nodeIdentifier
     */
    public void logEvent(
            HttpServletRequest request,
            String identifier,
            String event,
            Date dateLogged,
            String nodeIdentifier)
    {
        try
        {
            SolrInputDocument doc1 = new SolrInputDocument();



            String ip = request.getRemoteAddr();

            if(request.getHeader("X-Forwarded-For") != null) {
                /* This header is a comma delimited list */
                for (String xfip : request.getHeader("X-Forwarded-For").split(",")) {
                    /* proxy itself will sometime populate this header with the same value in
                    remote address. ordering in spec is vague, we'll just take the last
                    not equal to the proxy
                    */
                    if (!request.getHeader("X-Forwarded-For").contains(ip)) {
                        ip = xfip.trim();
                    }
                }
            }
            doc1.addField("identifier", identifier != null ? identifier : "null");
            doc1.addField("ipAddress", ip != null ? ip : "null");
            doc1.addField("userAgent",
                    request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "null");
            doc1.addField("subject", "public");
            doc1.addField("event" , event != null ? event : "null");
            doc1.addField("dateLogged", dateFormatUTC.format(dateLogged != null ? dateLogged : new Date()) );
            doc1.addField("nodeIdentifier", nodeIdentifier != null ? nodeIdentifier : "null");

            solr.add(doc1);

            solr.commit();

        }
        catch (RuntimeException re)
        {
            throw re;
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }


    /**
     * Ideally use the LogEntry class to avoid all these strings
     */
    public QueryResponse getLogRecords(Date fromDate, Date toDate, String event, String pidFilter, int start, int count ) throws ParseException, SolrServerException, org.apache.lucene.queryparser.surround.parser.ParseException {

        final SolrQuery solrQuery = new SolrQuery("nodeIdentifier:urn*");

        solrQuery.setSort("dateLogged", SolrQuery.ORDER.asc);

        if (event != null)
        {
            solrQuery.addFilterQuery("event:" + event);
            log.info("Adding event Filter: " + event);
        }

        if(fromDate != null && toDate != null)
        {
            solrQuery.addFilterQuery("dateLogged:[" + dateFormatUTC.format(fromDate) + " TO " +  dateFormatUTC.format(toDate) + "]");
        }
        else if( fromDate != null)
        {
            solrQuery.addFilterQuery("dateLogged:[" +  dateFormatUTC.format(fromDate) + " TO NOW]");
        }
        else if( toDate != null)
        {
            solrQuery.addFilterQuery("dateLogged:[* TO " + dateFormatUTC.format(toDate) + "]");
        }

        if (pidFilter != null)
        {
            String escaped = LUCENE_PATTERN.matcher(pidFilter).replaceAll(REPLACEMENT_STRING);
            solrQuery.addFilterQuery("identifier:" + escaped);
            log.info("Adding pid filter: " + escaped);
        }

        solrQuery.setStart(start);
        solrQuery.setRows(count);

        return solr.query(solrQuery);
    }



}