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
package io.opentracing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.client.api.recorder.BatchTraceRecorder;
import org.hawkular.apm.client.api.recorder.TraceRecorder;
import org.hawkular.apm.client.api.sampler.ContextSampler;
import org.hawkular.apm.client.api.sampler.Sampler;
import org.hawkular.apm.client.opentracing.APMTracer;
import org.hawkular.apm.client.opentracing.DeploymentMetaData;
import org.hawkular.apm.client.opentracing.TraceListener;

import io.opentracing.propagation.Format;

/**
 * @author gbrown
 */
public abstract class AbstractAPMTracer extends AbstractTracer {

    private static final Logger log = Logger.getLogger(APMTracer.class.getName());

    private TraceRecorder recorder;
    private ContextSampler sampler;

    private List<TraceListener> traceListeners = new ArrayList<>();

    public AbstractAPMTracer() {
        this(new BatchTraceRecorder(), null);
    }

    public AbstractAPMTracer(TraceRecorder recorder, Sampler sampler) {
        this.recorder = recorder;
        this.sampler = new ContextSampler(sampler);

        this.traceListeners.addAll(ServiceResolver.getServices(TraceListener.class));
    }

    public void setTraceRecorder(TraceRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    APMSpanBuilder createSpanBuilder(String operationName) {
        return new APMSpanBuilder(operationName, recorder, sampler, traceListeners);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        if (spanContext instanceof APMSpan) {
            APMSpan span = (APMSpan) spanContext;
            span.setInteractionId(UUID.randomUUID().toString());
            span.getNodeBuilder().setNodeType(NodeType.Producer);

            // Notify listeners that span was injected
            traceListeners.forEach(l -> l.spanInjected(span.getTraceContext().getTransaction(),
                    DeploymentMetaData.getInstance().getServiceName(), span.getTraceContext().getTraceId(), span));
        }
        super.inject(spanContext, format, carrier);
    }

    @Override
    Map<String, Object> getTraceState(SpanContext spanContext) {
        Map<String, Object> ret = new HashMap<>();

        if (spanContext instanceof APMSpan) {
            APMSpan span = (APMSpan) spanContext;
            if (span.getInteractionId() != null) {
                ret.put(Constants.HAWKULAR_APM_ID, span.getInteractionId());
            } else {
                // Not sure if issue - but just logging as warning for now
                log.warning("No id available to include in trace state for context = " + spanContext);
            }
            ret.putAll(span.state());
        }

        return ret;
    }

    public void addTraceListener(TraceListener l) {
        traceListeners.add(l);
    }

    public void removeTraceListener(TraceListener l) {
        traceListeners.remove(l);
    }
}
