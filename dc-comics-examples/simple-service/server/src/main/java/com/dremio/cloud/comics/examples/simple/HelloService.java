package com.dremio.cloud.comics.examples.simple;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloService implements Greeter {

    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom().item(() ->
                HelloReply.newBuilder().setMessage("Hello " + request.getName()).build()
        );
    }

}
