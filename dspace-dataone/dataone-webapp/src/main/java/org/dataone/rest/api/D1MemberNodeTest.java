/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dataone.rest.api;

import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created with IntelliJ IDEA.
 * User: mdiggory
 * Date: 3/16/14
 * Time: 9:34 AM
 * To change this template use File | Settings | File Templates.
 */

@Path("/test")
public class D1MemberNodeTest {

    @GET
    @Path("/exception/notimplemented")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void doTestNotImplemented() throws NotImplemented
    {
        throw new NotImplemented("1234","Exception Occured");
    }

    @GET
    @Path("/exception/servicefailure")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void doTestServiceFailure() throws ServiceFailure
    {
        throw new ServiceFailure("1234","Exception Occured");
    }

    @GET
    @Path("/exception/insufficientresources")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void doTestInsufficientResources() throws InsufficientResources
    {
        throw new InsufficientResources("1234","Exception Occured");
    }

}
