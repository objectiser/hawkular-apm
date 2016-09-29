/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hawkular.apm.example.swarm.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.hawkular.apm.example.swarm.dao.User;
import org.hawkular.apm.example.swarm.dao.UserDAO;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

/**
 * @author Pavol Loffay
 * @author gbrown
 */
@Path("/")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class UserHandler {

    @Inject
    private UserDAO userDAO;

    @Inject
    private Tracer tracer;

    @GET
    @Path("/users")
    public Response getAll(@Context HttpHeaders headers) {
        SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                new HttpHeadersExtractAdapter(headers.getRequestHeaders()));

        try (Span serverSpan = tracer.buildSpan("GET")
                .asChildOf(spanCtx)
                .withTag("http.url", "/wildfly-swarm/users")
                .withTag("service", "WildflySwarm")
                .start()) {
            Collection<User> users = userDAO.getAllUsers(serverSpan);
            return Response.ok().entity(users).build();
        }
    }

    @GET
    @Path("/users/{id}")
    public Response getOne(@Context HttpHeaders headers, @PathParam("id") String id) {
        SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                new HttpHeadersExtractAdapter(headers.getRequestHeaders()));

        try (Span serverSpan = tracer.buildSpan("GET")
                .asChildOf(spanCtx)
                .withTag("http.url", "/wildfly-swarm/users/{id}")
                .withTag("service", "WildflySwarm")
                .withTag("user.id", id)
                .start()) {
            
            User user = userDAO.getUser(serverSpan, id);
            serverSpan.setTag("user.name", user.getName());
            return Response.ok().entity(user).build();
        }
    }

    @POST
    @Path("/users")
    public Response getOne(@Context HttpHeaders headers, User user) {
        SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                new HttpHeadersExtractAdapter(headers.getRequestHeaders()));

        try (Span serverSpan = tracer.buildSpan("POST")
                .asChildOf(spanCtx)
                .withTag("http.url", "/wildfly-swarm/users")
                .withTag("service", "WildflySwarm")
                .withTag("user.name", user.getName())
                .start()) {
            
            user = userDAO.createUser(serverSpan, user);
            serverSpan.setTag("user.id", user.getId());
            return Response.ok().entity(user).build();
        }
    }

}
