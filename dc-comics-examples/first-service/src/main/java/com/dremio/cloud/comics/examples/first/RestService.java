package com.dremio.cloud.comics.examples.first;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class RestService {

    private static Logger logger = LoggerFactory.getLogger(RestService.class);

    // @Context
    UriInfo uriInfo;

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        logger.info("hello");
        return "hello";
    }

    @GET
    @Path("/chain")
    @Produces(MediaType.TEXT_PLAIN)
    public String chain() {
        RestClient restClient = RestClientBuilder.newBuilder()
                .baseUri(uriInfo.getBaseUri())
                .build(RestClient.class);
        return ("chain -> " + restClient.hello());
    }

}
