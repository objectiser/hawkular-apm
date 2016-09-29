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

package org.hawkular.apm.example.swarm.dao;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * @author Pavol Loffay
 */
@Singleton
public class UserDAO {

    private static final String KEYSPACE = "wildfly_swarm";
    private static final String TABLE = "users";

    private final Session session;
    private Tracer tracer;

    @Inject
    public UserDAO(Tracer tracer) {
    	this.tracer = tracer;
        Cluster cluster = Cluster.builder().addContactPoint("localhost").build(); // WAS "cassandra"
        this.session = cluster.connect();
        init();
    }

    private void init() {
        try (Span parentSpan = tracer.buildSpan("DatabaseInit")
                .withTag("service", "WildflySwarm")
                .start()) {
            
            BoundStatement boundStatement = session.prepare("DROP KEYSPACE IF EXISTS " + KEYSPACE).bind();
            executeWithClientSpan(parentSpan, boundStatement);

            boundStatement = session.prepare("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE + " WITH REPLICATION = " +
                    "{'class' : 'SimpleStrategy', 'replication_factor' : 1}").bind();
            executeWithClientSpan(parentSpan, boundStatement);

            boundStatement = session.prepare("USE " + KEYSPACE).bind();
            executeWithClientSpan(parentSpan, boundStatement);

            boundStatement = session.prepare("CREATE TABLE " + KEYSPACE + "." + TABLE +
                    "(id text PRIMARY KEY, name text)").bind();
            executeWithClientSpan(parentSpan, boundStatement);
        }
    }

    public User createUser(Span parentSpan, User user) {
        user.setId(UUID.randomUUID().toString());

        /**
         * With query builder tracing is not reported in C*, but
         * it shows complete query with parameters when calling statement.toString();
         */
//        Statement statement = QueryBuilder.insertInto(KEYSPACE, TABLE)
//                .value("id", user.getId())
//                .value("name", user.getName())
//                .enableTracing();

        BoundStatement boundStatement = session.prepare("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, name)" +
                " VALUES(?, ?)").bind(user.getId(), user.getName());

        executeWithClientSpan(parentSpan, boundStatement);

        return user;
    }

    public User getUser(Span parentSpan, String id) {
//        Statement statement = QueryBuilder.select()
//                .from(KEYSPACE, TABLE)
//                .where(QueryBuilder.eq("id", id))
//                .enableTracing();

        BoundStatement boundStatement = session.prepare("SELECT * FROM " + KEYSPACE + "." + TABLE +
                " WHERE id = ?").bind(id);

        ResultSet resultSet = executeWithClientSpan(parentSpan, boundStatement);

        User user = null;
        for (Row row: resultSet) {
            user = new User(row.getString("id"), row.getString("name"));
        }

        return user;
    }

    public Collection<User> getAllUsers(Span parentSpan) {
//        Statement statement = QueryBuilder.select()
//                .from(KEYSPACE, TABLE)
//                .enableTracing();

        BoundStatement boundStatement = session.prepare("SELECT * FROM " + KEYSPACE + "." + TABLE).bind();

        ResultSet resultSet = executeWithClientSpan(parentSpan, boundStatement);

        List<User> users = new ArrayList<>();

        for (Row row: resultSet) {
            users.add(new User(row.getString("id"), row.getString("name")));
        }

        return users;
    }

    private ResultSet executeWithClientSpan(Span parentSpan, BoundStatement boundStatement) {

        try (Span clientSpan = tracer.buildSpan("Cassandra")
                .asChildOf(parentSpan)
                .withTag("database.url", "cassandra")
                .withTag("database.query", boundStatement.preparedStatement().getQueryString())
                .start()) {
            ResultSet resultSet = session.execute(boundStatement.enableTracing());
            return resultSet;
        }
    }
}
