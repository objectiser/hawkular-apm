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
package org.hawkular.apm.client.opentracing.behaviour;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.client.api.recorder.TraceRecorder;
import org.hawkular.apm.client.opentracing.APMTracer;
import org.hawkular.apm.client.opentracing.DeploymentMetaData;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.propagation.TextMapInjectAdapter;

/**
 * @author gbrown
 */
public class BehaviourEnforcerTest {

    @Test
    public void testConsumerOneWay() {
        DeploymentMetaData.getInstance().setServiceName("consumer1way");

        TraceRecorder recorder = Mockito.mock(TraceRecorder.class);
        Tracer tracer = new APMTracer(recorder);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HAWKULAR_APM_TXN, "test");

        SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                new TextMapExtractAdapter(headers));

        Span span = tracer.buildSpan("hello")
                .asChildOf(spanCtx)
                .start();

        span.setTag("http.status_code", "200");

        span.finish();

        ArgumentCaptor<Trace> traceCaptor = ArgumentCaptor.forClass(Trace.class);
        Mockito.verify(recorder).record(traceCaptor.capture());

        Trace result = traceCaptor.getValue();
        assertNotNull(result);
        assertEquals(1, result.getNodes().size());
        assertEquals(Consumer.class, result.getNodes().get(0).getClass());

        Consumer consumer = (Consumer) result.getNodes().get(0);
        assertEquals("true", consumer.getProperties("fsm.request").iterator().next().getValue());

        // FSM didn't expect a response
        assertEquals("false", consumer.getProperties("fsm.response").iterator().next().getValue());
    }

    @Test
    public void testConsumerTwoWay() {
        DeploymentMetaData.getInstance().setServiceName("consumer2way");

        TraceRecorder recorder = Mockito.mock(TraceRecorder.class);
        Tracer tracer = new APMTracer(recorder);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HAWKULAR_APM_TXN, "test");

        SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                new TextMapExtractAdapter(headers));

        Span span = tracer.buildSpan("hello")
                .asChildOf(spanCtx)
                .start();

        span.setTag("http.status_code", "200");

        span.finish();

        ArgumentCaptor<Trace> traceCaptor = ArgumentCaptor.forClass(Trace.class);
        Mockito.verify(recorder).record(traceCaptor.capture());

        Trace result = traceCaptor.getValue();
        assertNotNull(result);
        assertEquals(1, result.getNodes().size());
        assertEquals(Consumer.class, result.getNodes().get(0).getClass());

        Consumer consumer = (Consumer) result.getNodes().get(0);
        assertEquals("true", consumer.getProperties("fsm.request").iterator().next().getValue());
        assertEquals("true", consumer.getProperties("fsm.response").iterator().next().getValue());
    }

    @Test
    public void testConsumerWithFault() {
        DeploymentMetaData.getInstance().setServiceName("consumerfault");

        TraceRecorder recorder = Mockito.mock(TraceRecorder.class);
        Tracer tracer = new APMTracer(recorder);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HAWKULAR_APM_TXN, "test");

        SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                new TextMapExtractAdapter(headers));

        Span span = tracer.buildSpan("hello")
                .asChildOf(spanCtx)
                .start();

        span.setTag("fault", "myfault");

        span.finish();

        ArgumentCaptor<Trace> traceCaptor = ArgumentCaptor.forClass(Trace.class);
        Mockito.verify(recorder).record(traceCaptor.capture());

        Trace result = traceCaptor.getValue();
        assertNotNull(result);
        assertEquals(1, result.getNodes().size());
        assertEquals(Consumer.class, result.getNodes().get(0).getClass());

        Consumer consumer = (Consumer) result.getNodes().get(0);
        assertEquals("true", consumer.getProperties("fsm.request").iterator().next().getValue());
        assertEquals("true", consumer.getProperties("fsm.response").iterator().next().getValue());
    }

    @Test
    public void testConsumerProducer() {
        DeploymentMetaData.getInstance().setServiceName("consumerproducer");

        TraceRecorder recorder = Mockito.mock(TraceRecorder.class);
        Tracer tracer = new APMTracer(recorder);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HAWKULAR_APM_TXN, "test");

        SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                new TextMapExtractAdapter(headers));

        Span spanHello = tracer.buildSpan("hello")
                .asChildOf(spanCtx)
                .start();

        Span spanWorld = tracer.buildSpan("world")
                .asChildOf(spanHello)
                .start();

        Map<String, String> outheaders = new HashMap<>();
        tracer.inject(spanWorld.context(), Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(outheaders));

        spanWorld.setTag("http.status_code", "200");

        spanWorld.finish();

        spanHello.setTag("http.status_code", "200");

        spanHello.finish();

        ArgumentCaptor<Trace> traceCaptor = ArgumentCaptor.forClass(Trace.class);
        Mockito.verify(recorder).record(traceCaptor.capture());

        Trace result = traceCaptor.getValue();
        assertNotNull(result);
        assertEquals(1, result.getNodes().size());
        assertEquals(Consumer.class, result.getNodes().get(0).getClass());

        Consumer consumer = (Consumer) result.getNodes().get(0);
        assertEquals("true", consumer.getProperties("fsm.request").iterator().next().getValue());
        assertEquals("true", consumer.getProperties("fsm.response").iterator().next().getValue());

        assertEquals(1, consumer.getNodes().size());
        assertEquals(Producer.class, consumer.getNodes().get(0).getClass());

        Producer producer = (Producer) consumer.getNodes().get(0);
        assertEquals("true", producer.getProperties("fsm.request").iterator().next().getValue());
        assertEquals("true", producer.getProperties("fsm.response").iterator().next().getValue());
    }

}
