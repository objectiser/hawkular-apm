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
package org.hawkular.apm.client.opentracing;

import io.opentracing.APMSpan;

/**
 * This interface represents a trace listener.
 *
 * @author gbrown
 */
public interface TraceListener {

    /**
     * This method is invoked when a new span is created.
     *
     * @param transaction The optional transaction name
     * @param service The optional service name
     * @param traceId The trace id
     * @param span The span
     */
    void spanCreated(String transaction, String service, String traceId, APMSpan span);

    /**
     * This method is invoked when a new span context is extracted.
     *
     * @param transaction The optional transaction name
     * @param service The optional service name
     * @param traceId The trace id
     * @param span The span
     */
    void spanExtracted(String transaction, String service, String traceId, APMSpan span);

    /**
     * This method is invoked when a span is injected.
     *
     * @param transaction The optional transaction name
     * @param service The optional service name
     * @param traceId The trace id
     * @param span The span
     */
    void spanInjected(String transaction, String service, String traceId, APMSpan span);

    /**
     * This method is invoked when a span is finished.
     *
     * @param transaction The optional transaction name
     * @param service The optional service name
     * @param traceId The trace id
     * @param span The span
     */
    void spanFinished(String transaction, String service, String traceId, APMSpan span);

}
