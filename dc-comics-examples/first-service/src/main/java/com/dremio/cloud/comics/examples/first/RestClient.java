package com.dremio.cloud.comics.examples.first;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public interface RestClient {

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    String hello();

}
