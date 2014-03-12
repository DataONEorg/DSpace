/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This is a wrapper filter that assures DSpace ContextUtil.context
 * objects are properly closed
 * 
 * @author Mark Diggory
 */
public class DSpaceContextServletFilter implements Filter
{
    private static final Logger LOG = Logger.getLogger(DSpaceContextServletFilter.class);

	private static final long serialVersionUID = 1L;

    
    /**
     * Before this servlet will become functional replace 
     */
    public void init(FilterConfig arg0) throws ServletException {

    }
    
	
	/**
     * Before passing off a request to the servlet check to see if there is a request that
     * should be resumed? If so replace the real request with a faked request and pass that off to 
     * cocoon.
     */
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain arg2) throws IOException, ServletException { 

        HttpServletRequest realRequest = (HttpServletRequest)request;
        HttpServletResponse realResponse = (HttpServletResponse) response;

        try {

            arg2.doFilter(realRequest, realResponse);

        } catch (IOException e) {
            ContextUtil.abortContext(realRequest);
            if (LOG.isDebugEnabled()) {
                  LOG.debug("The connection was reset", e);
                }
            else {
                LOG.error("Client closed the connection before file download was complete");
            }
        } catch (RuntimeException e) {
            ContextUtil.abortContext(realRequest);
            LOG.error("Serious Runtime Error Occurred Processing Request!", e);
            throw e;
        } catch (Exception e) {
            ContextUtil.abortContext(realRequest);
                LOG.error("Serious Error Occurred Processing Request!", e);
        } finally {
            // Close out the DSpace context no matter what.
            ContextUtil.completeContext(realRequest);
        }
    }

	public void destroy() {

	}



}
