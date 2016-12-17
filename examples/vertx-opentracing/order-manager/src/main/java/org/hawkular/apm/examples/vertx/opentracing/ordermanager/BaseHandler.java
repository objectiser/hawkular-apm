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
package org.hawkular.apm.examples.vertx.opentracing.ordermanager;

import org.hawkular.apm.client.opentracing.APMTracer;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * @author gbrown
 * @author Juraci Paixão Kröhling
 */
abstract class BaseHandler implements Handler<RoutingContext> {

    private static Tracer tracer = new APMTracer();

    /**
     * @return the tracer
     */
    public static Tracer getTracer() {
        return tracer;
    }

    void sendError(int statusCode, String message, HttpServerResponse response, Span span) {
        response.setStatusCode(statusCode).end(message);
        if (span != null) {
            span.setTag("fault", message == null ? Integer.toString(statusCode) : message);
            span.finish();
        }
    }

}
