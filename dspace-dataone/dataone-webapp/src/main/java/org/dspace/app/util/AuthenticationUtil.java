/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import org.apache.log4j.Logger;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.SQLException;

/**
 * Methods for authenticating the user. This is DSpace platform code, as opposed
 * to the site-specific authentication code, that resides in implementations of
 * the org.dspace.eperson.AuthenticationMethod interface.
 * 
 * @author Scott Phillips
 * @author Robert Tansley
 * @author MArk Diggory
 */

public class AuthenticationUtil
{
    private static final Logger log = Logger.getLogger(AuthenticationUtil.class);

    /**
     * The IP address this user first logged in from, do not allow this session for
     * other IP addresses.
     */
    private static final String CURRENT_IP_ADDRESS = "dspace.user.ip";
    
    /**
     * The effective user id, typically this will never change. However, if an administrator 
     * has assumed login as this user then they will differ.
     */
    private static final String EFFECTIVE_USER_ID = "dspace.user.effective";
    private static final String AUTHENTICATED_USER_ID = "dspace.user.authenticated";

    /**
     * Session attribute name for storing the return URL where the user should
     * be redirected too once successfully authenticated.
     */
    public static final String REQUEST_INTERRUPTED = "dspace.request.interrupted";
    public static final String REQUEST_RESUME = "dspace.request.resume";


    /**
     * Authenticate the current DSpace content based upon given authentication
     * credentials. The AuthenticationManager will consult the configured
     * authentication stack to determine the best method.
     * 
     * @param request
     *            Http Request Object
     * @param email
     *            The email credentials provided by the user.
     * @param password
     *            The password credentials provided by the user.
     * @param realm
     *            The realm credentials provided by the user.
     * @return Return a current context with either the eperson attached if the
     *         authentication was successful or or no eperson attached if the
     *         attempt failed.
     */
    public static Context authenticate(HttpServletRequest request, String email, String password, String realm)
    throws SQLException
    {
        // Get the real HttpRequest
        Context context = ContextUtil.obtainContext(request);

        int implicitStatus = AuthenticationManager.authenticateImplicit(
                context, null, null, null, request);

        if (implicitStatus == AuthenticationMethod.SUCCESS)
        {
            log.info(LogManager.getHeader(context, "login", "type=implicit"));
            AuthenticationUtil.logIn(context, request, context.getCurrentUser());
        }
        else
        {
            // If implicit authentication failed, fall over to explicit.

            int explicitStatus = AuthenticationManager.authenticate(context,
                    email, password, realm, request);

            if (explicitStatus == AuthenticationMethod.SUCCESS)
            {
                // Logged in OK.
                log.info(LogManager
                        .getHeader(context, "login", "type=explicit"));
                AuthenticationUtil.logIn(context, request, context
                        .getCurrentUser());
            }
            else
            {
                log.info(LogManager.getHeader(context, "failed_login", "email="
                        + email + ", realm=" + realm + ", result="
                        + explicitStatus));
            }
        }

        return context;
    }

    /**
     * Perform implicit authentication. The authenticationManager will consult
     * the authentication stack for any methods that can implicitly authenticate
     * this session. If the attempt was successful then the returned context
     * will have an eperson attached other wise the context will not have an
     * eperson attached.
     * 
     * @param request
     *            Http Request
     * @return This requests DSpace context.
     */
    public static Context authenticateImplicit(HttpServletRequest request)
            throws SQLException
    {
        // Get the real HttpRequest
        Context context = ContextUtil.obtainContext(request);

        int implicitStatus = AuthenticationManager.authenticateImplicit(
                context, null, null, null, request);

        if (implicitStatus == AuthenticationMethod.SUCCESS)
        {
            log.info(LogManager.getHeader(context, "login", "type=implicit"));
            AuthenticationUtil.logIn(context, request, context.getCurrentUser());
        }

        return context;
    }

    /**
     * Log the given user in as a real authenticated user. This should only be used after 
     * a user has presented credentials and they have been validated. 
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     * @param eperson
     *            the eperson logged in
     */
    private static void logIn(Context context, HttpServletRequest request,
            EPerson eperson) throws SQLException
    {
        if (eperson == null)
        {
            return;
        }
        
        HttpSession session = request.getSession();

        context.setCurrentUser(eperson);
        
        // Set any special groups - invoke the authentication manager.
        int[] groupIDs = AuthenticationManager.getSpecialGroups(context,
                request);
        for (int groupID : groupIDs)
        {
            context.setSpecialGroup(groupID);
        }

        // and the remote IP address to compare against later requests
        // so we can detect session hijacking.
        session.setAttribute(CURRENT_IP_ADDRESS, request.getRemoteAddr());
        
        // Set both the effective and authenticated user to the same.
        session.setAttribute(EFFECTIVE_USER_ID, eperson.getID());
        session.setAttribute(AUTHENTICATED_USER_ID,eperson.getID());
        
    }
    
    /**
     * Log the given user in as a real authenticated user. This should only be used after 
     * a user has presented credentials and they have been validated. This method 
     * signature is provided to be easier to call from flow scripts.
     * 
     * @param request 
     * 			  HTTP request
     * @param eperson
     *            the eperson logged in
     * 
     */
    public static void logIn(HttpServletRequest request, EPerson eperson) throws SQLException
    {
        Context context = ContextUtil.obtainContext(request);
        
        logIn(context,request,eperson);
    }
    
    
    /**
     * Check to see if there are any session attributes indicating a currently authenticated 
     * user. If there is then log this user in.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP Request
     */
    public static void resumeLogin(Context context, HttpServletRequest request)
            throws SQLException
    {
        HttpSession session = request.getSession(false);

        if (session != null)
        {
            Integer id = (Integer) session.getAttribute(EFFECTIVE_USER_ID);
            Integer realid = (Integer) session.getAttribute(AUTHENTICATED_USER_ID);
            
            if (id != null)
            {
                // Should we check for an ip match from the start of the request to now?
                boolean ipcheck = ConfigurationManager.getBooleanProperty("xmlui.session.ipcheck", true);

                String address = (String)session.getAttribute(CURRENT_IP_ADDRESS);
                if (!ipcheck || (address != null && address.equals(request.getRemoteAddr())))
                {
                    EPerson eperson = EPerson.find(context, id);
                    context.setCurrentUser(eperson);

                    // Set any special groups - invoke the authentication mgr.
                    int[] groupIDs = AuthenticationManager.getSpecialGroups(context, request);
                    for (int groupID : groupIDs)
                    {
                        context.setSpecialGroup(groupID);
                    }
                }
                else
                {
                    // Possible hack attempt or maybe your setup is not providing a consistent end-user IP address.
                    log.warn(LogManager.getHeader(context, "ip_mismatch", "id=" + id + ", request ip=" +
                        request.getRemoteAddr() + ", session ip=" + address));
                }
            } // if id
        } // if session
    }

    
    /**
     * Log the user out.
     * 
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request
     */
    public static void logOut(Context context, HttpServletRequest request) throws SQLException
    {
        HttpSession session = request.getSession();

        if (session.getAttribute(EFFECTIVE_USER_ID) != null &&
        	session.getAttribute(AUTHENTICATED_USER_ID) != null)
        {
    	    Integer effectiveID = (Integer) session.getAttribute(EFFECTIVE_USER_ID); 
    	    Integer authenticatedID = (Integer) session.getAttribute(AUTHENTICATED_USER_ID); 
    	    
    	    if (effectiveID.intValue() != authenticatedID.intValue())
    	    {
    	    	// The user has login in as another user, instead of logging them out, 
    	    	// revert back to their previous login name.
    	    	
    	    	EPerson authenticatedUser = EPerson.find(context, authenticatedID);
    	    	context.setCurrentUser(authenticatedUser);
    	    	session.setAttribute(EFFECTIVE_USER_ID, authenticatedID);
    	    	return;
    	    }
        }
        
        // Otherwise, just log the person out as normal.
        context.setCurrentUser(null);
        session.removeAttribute(EFFECTIVE_USER_ID);
        session.removeAttribute(AUTHENTICATED_USER_ID);
        session.removeAttribute(CURRENT_IP_ADDRESS);
    }

    /**
     * Has this user authenticated?
     * @param request
     * @return true if request is in a session having a user ID.
     */
    public static boolean isLoggedIn(HttpServletRequest request)
    {
        return (null != request.getSession().getAttribute(EFFECTIVE_USER_ID));
    }


    /**
     * Check to see if this request should be resumed.
     *
     * @param realHttpRequest The current real request
     * @return Either the current real request or a stored request that was previously interrupted.
     */
    public static HttpServletRequest resumeRequest(HttpServletRequest realHttpRequest)
    {
        // First check to see if there is a resumed request.
        HttpSession session = realHttpRequest.getSession();
        //session.setMaxInactiveInterval(60);
        Object object = session.getAttribute(REQUEST_RESUME);

        // Next check to make sure it's the right type of object,
        // there should be no condition where it is not - but always
        // safe to check.
        if (object instanceof RequestInfo)
        {
            RequestInfo interruptedRequest = (RequestInfo) object;

            // Next, check to make sure this real request if for the same URL
            // path, if so then resume the previous request.
            String interruptedServletPath = interruptedRequest.getServletPath();
            String realServletPath = realHttpRequest.getServletPath();

            if (realServletPath != null &&
                    realServletPath.equals(interruptedServletPath))
            {
                // Clear the resumed request and send the request back to be resumed.
                session.setAttribute(REQUEST_INTERRUPTED, null);
                session.setAttribute(REQUEST_RESUME, null);

                return interruptedRequest.wrapRequest(realHttpRequest);
            }
        }
        // Otherwise return the real request.
        return realHttpRequest;
    }

}
