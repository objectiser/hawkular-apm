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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.client.opentracing.APMTracerTest.TestTraceReporter;
import org.junit.Test;

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;

/**
 * @author gbrown
 */
public class APMTracerReferenceTest {

    private static final String TEST_TXN = "TestBTxn";
    private static final String TEST_APM_ID0 = "id0";
    private static final String TEST_APM_ID1 = "id1";
    private static final String TEST_APM_ID2 = "id2";
    private static final String TEST_APM_TRACEID = "xyz";

    @Test
    public void testNoReferences() {
        TestTraceReporter reporter = new TestTraceReporter();
        Tracer tracer = new APMTracer(reporter);

        Span span = tracer.buildSpan("NoReferences")
                .start();
        span.finish();

        assertEquals(1, reporter.getTraces().size());

        Trace trace = reporter.getTraces().get(0);
        assertEquals(1, trace.getNodes().size());
        assertEquals(Component.class, trace.getNodes().get(0).getClass());
        assertTrue(trace.getNodes().get(0).getCorrelationIds().isEmpty());
        assertEquals(0, ((Component) trace.getNodes().get(0)).getNodes().size());
    }

    @Test
    public void testSingleExtractedChildOf() {
        TestTraceReporter reporter = new TestTraceReporter();
        Tracer tracer = new APMTracer(reporter);

        SpanContext spanCtx = extractedTraceState(tracer, TEST_APM_ID1);

        Span span = tracer.buildSpan("SingleChildOfSpanContext")
                .asChildOf(spanCtx)
                .start();
        span.finish();

        assertEquals(1, reporter.getTraces().size());

        Trace trace = reporter.getTraces().get(0);
        assertEquals(1, trace.getNodes().size());
        assertEquals(Consumer.class, trace.getNodes().get(0).getClass());
        assertEquals(((Consumer) trace.getNodes().get(0)).getCorrelationIds().get(0),
                new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID1));
        assertEquals(0, ((Consumer) trace.getNodes().get(0)).getNodes().size());
    }

    @Test
    public void testSingleExtractedFollowsFrom() {
        TestTraceReporter testTraceReporter = new TestTraceReporter();
        Tracer tracer = new APMTracer(testTraceReporter);

        SpanContext spanCtx = extractedTraceState(tracer, TEST_APM_ID1);

        Span span = tracer.buildSpan("root")
                .addReference(References.FOLLOWS_FROM, spanCtx)
                .start();
        span.finish();

        assertEquals(1, testTraceReporter.getTraces().size());

        Trace trace = testTraceReporter.getTraces().get(0);
        assertEquals(1, trace.getNodes().size());
        assertEquals(Consumer.class, trace.getNodes().get(0).getClass());
        assertEquals(((Consumer) trace.getNodes().get(0)).getCorrelationIds().get(0),
                new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID1));
        assertEquals(0, ((Consumer) trace.getNodes().get(0)).getNodes().size());
    }

    @Test
    public void testSingleChildOfSpan() {
        TestTraceReporter reporter = new TestTraceReporter();
        Tracer tracer = new APMTracer(reporter);

        Span parentSpan = tracer.buildSpan("ParentSpan")
                .start();

        Span childSpan = tracer.buildSpan("ChildSpan")
                .asChildOf(parentSpan)
                .start();
        childSpan.finish();

        parentSpan.finish();

        assertEquals(1, reporter.getTraces().size());

        Trace trace = reporter.getTraces().get(0);
        assertEquals(1, trace.getNodes().size());
        assertEquals(Component.class, trace.getNodes().get(0).getClass());

        Component parentComponent = (Component) trace.getNodes().get(0);

        assertTrue(parentComponent.getCorrelationIds().isEmpty());
        assertEquals(1, parentComponent.getNodes().size());
        assertEquals(Component.class, parentComponent.getNodes().get(0).getClass());

        Component childComponent = (Component) parentComponent.getNodes().get(0);

        assertTrue(childComponent.getCorrelationIds().isEmpty());
        assertEquals(0, childComponent.getNodes().size());
    }

    @Test
    public void testSingleChildOfSpanUsingContext() {
        TestTraceReporter reporter = new TestTraceReporter();
        Tracer tracer = new APMTracer(reporter);

        Span parentSpan = tracer.buildSpan("ParentSpan")
                .start();

        Span childSpan = tracer.buildSpan("ChildSpan")
                .asChildOf(parentSpan.context())
                .start();
        childSpan.finish();

        parentSpan.finish();

        assertEquals(1, reporter.getTraces().size());

        Trace trace = reporter.getTraces().get(0);
        assertEquals(1, trace.getNodes().size());
        assertEquals(Component.class, trace.getNodes().get(0).getClass());

        Component parentComponent = (Component) trace.getNodes().get(0);

        assertTrue(parentComponent.getCorrelationIds().isEmpty());
        assertEquals(1, parentComponent.getNodes().size());
        assertEquals(Component.class, parentComponent.getNodes().get(0).getClass());

        Component childComponent = (Component) parentComponent.getNodes().get(0);

        assertTrue(childComponent.getCorrelationIds().isEmpty());
        assertEquals(0, childComponent.getNodes().size());
    }

    @Test
    public void testSingleFollowsFromRef() {
        TestTraceReporter reporter = new TestTraceReporter();
        Tracer tracer = new APMTracer(reporter);

        Span parentSpan = tracer.buildSpan("ParentSpan")
                .start();

        parentSpan.finish();

        Span followsFromSpan = tracer.buildSpan("FollowsFromSpan")
                .addReference(References.FOLLOWS_FROM, parentSpan.context())
                .start();
        followsFromSpan.finish();

        assertEquals(2, reporter.getTraces().size());

        Trace parentTrace = reporter.getTraces().get(0);

        assertEquals(1, parentTrace.getNodes().size());
        assertEquals(Component.class, parentTrace.getNodes().get(0).getClass());

        Component parentComponent = (Component) parentTrace.getNodes().get(0);

        assertTrue(parentComponent.getCorrelationIds().isEmpty());
        assertEquals(0, parentComponent.getNodes().size());

        Trace followsFromTrace = reporter.getTraces().get(1);

        assertEquals(parentTrace.getTraceId(), followsFromTrace.getTraceId());

        assertEquals(1, followsFromTrace.getNodes().size());

        // 'Consumer' introduced to link the followsFrom component to the referenced Span/Node
        assertEquals(Consumer.class, followsFromTrace.getNodes().get(0).getClass());

        Consumer followsFromConsumer = (Consumer) followsFromTrace.getNodes().get(0);

        assertEquals(followsFromConsumer.getCorrelationIds().get(0),
                new CorrelationIdentifier(Scope.CausedBy, parentTrace.getFragmentId() + ":0"));
        assertEquals(1, followsFromConsumer.getNodes().size());
        assertEquals(Component.class, followsFromConsumer.getNodes().get(0).getClass());

        Component followsFromComponent = (Component) followsFromConsumer.getNodes().get(0);

        assertTrue(followsFromComponent.getCorrelationIds().isEmpty());
        assertEquals(0, followsFromComponent.getNodes().size());
    }

    /**
     * This test shows multiple FollowsFrom references with just a single ChildOf(SpanContext).
     */
    @Test
    public void testSingleChildOfSpanContextWithOtherFollowsFromRefs() {
        TestTraceReporter reporter = new TestTraceReporter();
        Tracer tracer = new APMTracer(reporter);

        Span refdSpan1 = tracer.buildSpan("ref1")
                .start();
        refdSpan1.finish();

        Span refdSpan2 = tracer.buildSpan("ref2")
                .start();
        refdSpan2.finish();

        SpanContext spanCtx = extractedTraceState(tracer, TEST_APM_ID1);

        Span span = tracer.buildSpan("SingleChildOfSpanContext")
                .addReference(References.FOLLOWS_FROM, refdSpan1.context())
                .asChildOf(spanCtx)
                .addReference(References.FOLLOWS_FROM, refdSpan2.context())
                .start();
        span.finish();

        assertEquals(3, reporter.getTraces().size());

        Trace trace1 = reporter.getTraces().get(0);
        Trace trace2 = reporter.getTraces().get(1);
        Trace trace3 = reporter.getTraces().get(2);

        assertEquals(1, trace3.getNodes().size());
        assertEquals(Consumer.class, trace3.getNodes().get(0).getClass());
        assertTrue(((Consumer) trace3.getNodes().get(0)).getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID1)));
        assertTrue(((Consumer) trace3.getNodes().get(0)).getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.CausedBy, trace1.getFragmentId() + ":0")));
        assertTrue(((Consumer) trace3.getNodes().get(0)).getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.CausedBy, trace2.getFragmentId() + ":0")));
        assertEquals(0, ((Consumer) trace3.getNodes().get(0)).getNodes().size());
    }

    /**
     * This test shows multiple FollowsFrom references with just a single ChildOf(Span).
     */
    @Test
    public void testSingleChildOfSpanWithOtherFollowsFromRefs() {
        TestTraceReporter reporter = new TestTraceReporter();
        Tracer tracer = new APMTracer(reporter);

        Span refdSpan1 = tracer.buildSpan("ref1")
                .start();
        refdSpan1.finish();

        Span refdSpan2 = tracer.buildSpan("ref2")
                .start();
        refdSpan2.finish();

        Span parentSpan = tracer.buildSpan("parentSpan")
                .start();

        Span span = tracer.buildSpan("SingleChildOfSpan")
                .addReference(References.FOLLOWS_FROM, refdSpan1.context())
                .asChildOf(parentSpan)
                .addReference(References.FOLLOWS_FROM, refdSpan2.context())
                .start();
        span.finish();

        parentSpan.finish();

        assertEquals(3, reporter.getTraces().size());

        Trace trace1 = reporter.getTraces().get(0);
        Trace trace2 = reporter.getTraces().get(1);
        Trace trace3 = reporter.getTraces().get(2);

        assertEquals(1, trace3.getNodes().size());
        assertEquals(Component.class, trace3.getNodes().get(0).getClass());

        Component parentComponent = (Component) trace3.getNodes().get(0);

        assertEquals(1, parentComponent.getNodes().size());
        assertEquals(Component.class, parentComponent.getNodes().get(0).getClass());

        Component childComponent = (Component) parentComponent.getNodes().get(0);

        assertTrue(childComponent.getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.CausedBy, trace1.getFragmentId() + ":0")));
        assertTrue(childComponent.getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.CausedBy, trace2.getFragmentId() + ":0")));
        assertEquals(0, childComponent.getNodes().size());
    }

    /**
     * This test defines three references, 'refdSpan1' which is the FollowFrom,
     * 'refdSpan2' which is the ChildOf ref (so real parent), but it is overridden
     * by the extracted 'spanCtx' - and so the real parent/child relationship is
     * changed to a casual link.
     */
    @Test
    public void testSingleChildOfSpanContextWithOtherChildOfSpanAndFollowsFromRefs() {
        TestTraceReporter reporter = new TestTraceReporter();
        Tracer tracer = new APMTracer(reporter);

        Span refdSpan1 = tracer.buildSpan("ref1")
                .start();
        refdSpan1.finish();

        Span refdSpan2 = tracer.buildSpan("ref2")
                .start();

        SpanContext spanCtx = extractedTraceState(tracer, TEST_APM_ID1);

        Span span = tracer.buildSpan("SingleChildOfSpanContext")
                .asChildOf(refdSpan2)
                .asChildOf(spanCtx)
                .addReference(References.FOLLOWS_FROM, refdSpan1.context())
                .start();

        refdSpan2.finish();

        span.finish();

        assertEquals(3, reporter.getTraces().size());

        Trace trace1 = reporter.getTraces().get(0);
        Trace trace2 = reporter.getTraces().get(1);
        Trace trace3 = reporter.getTraces().get(2);

        assertEquals(1, trace3.getNodes().size());
        assertEquals(Consumer.class, trace3.getNodes().get(0).getClass());
        assertTrue(((Consumer) trace3.getNodes().get(0)).getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID1)));
        assertTrue(((Consumer) trace3.getNodes().get(0)).getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.CausedBy, trace2.getFragmentId() + ":0")));
        assertTrue(((Consumer) trace3.getNodes().get(0)).getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.CausedBy, trace1.getFragmentId() + ":0")));
        assertEquals(0, ((Consumer) trace3.getNodes().get(0)).getNodes().size());
    }

    @Test
    public void testMultipleVariousChildOfAndFollowsFromRefsSameTraceInstance() {
        testJoin(true);
    }

    @Test
    public void testMultipleVariousChildOfAndFollowsFromRefsDiffTraceInstances() {
        testJoin(false);
    }

    protected void testJoin(boolean sameTraceId) {
        TestTraceReporter reporter = new TestTraceReporter();
        Tracer tracer = new APMTracer(reporter);

        Map<String, String> rootheaders = new HashMap<>();
        rootheaders.put(Constants.HAWKULAR_APM_TRACEID, sameTraceId ? TEST_APM_TRACEID : "AnotherTraceId");
        rootheaders.put(Constants.HAWKULAR_APM_ID, TEST_APM_ID0);

        SpanContext parentSpanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                new TextMapExtractAdapter(rootheaders));

        Span refdParent = tracer.buildSpan("refParent")
                .asChildOf(parentSpanCtx)
                .start();

        Span refdSpan1 = tracer.buildSpan("ref1")
                .asChildOf(refdParent)
                .start();
        refdSpan1.finish();

        Span refdSpan2 = tracer.buildSpan("ref2")
                .asChildOf(refdParent)
                .start();

        refdParent.finish();

        SpanContext spanCtx1 = extractedTraceState(tracer, TEST_APM_ID1);

        SpanContext spanCtx2 = extractedTraceState(tracer, TEST_APM_ID2);

        Span span = tracer.buildSpan("JoinForSameTraceInstance")
                .asChildOf(refdSpan2)
                .asChildOf(spanCtx1)
                .addReference(References.FOLLOWS_FROM, refdSpan1.context())
                .asChildOf(spanCtx2)
                .start();

        refdSpan2.finish();

        span.finish();

        assertEquals(2, reporter.getTraces().size());

        Trace trace1 = reporter.getTraces().get(0);
        Trace trace2 = reporter.getTraces().get(1);

        assertNotNull(trace1.getTraceId());
        assertNotNull(trace2.getTraceId());
        assertEquals(sameTraceId, trace1.getTraceId().equals(trace2.getTraceId()));

        // Verify trace1 structure of Consumer node with two Component children
        assertEquals(1, trace1.getNodes().size());
        assertEquals(Consumer.class, trace1.getNodes().get(0).getClass());
        assertTrue(((Consumer) trace1.getNodes().get(0)).getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID0)));
        assertEquals(2, ((Consumer) trace1.getNodes().get(0)).getNodes().size());
        assertEquals(Component.class, ((Consumer) trace1.getNodes().get(0)).getNodes().get(0).getClass());
        assertEquals(Component.class, ((Consumer) trace1.getNodes().get(0)).getNodes().get(1).getClass());

        // Verify trace2 structure as a 'join' of various ref types
        assertEquals(1, trace2.getNodes().size());
        assertEquals(Component.class, trace2.getNodes().get(0).getClass());
        assertTrue(((Component) trace2.getNodes().get(0)).getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID1)));
        assertTrue(((Component) trace2.getNodes().get(0)).getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID2)));
        assertTrue(((Component) trace2.getNodes().get(0)).getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.CausedBy, trace1.getFragmentId() + ":0:0")));
        assertTrue(((Component) trace2.getNodes().get(0)).getCorrelationIds().contains(
                new CorrelationIdentifier(Scope.CausedBy, trace1.getFragmentId() + ":0:1")));
        assertEquals(0, ((Component) trace2.getNodes().get(0)).getNodes().size());
    }

    protected SpanContext extractedTraceState(Tracer tracer, String id) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HAWKULAR_APM_TRACEID, TEST_APM_TRACEID);
        headers.put(Constants.HAWKULAR_APM_ID, id);
        headers.put(Constants.HAWKULAR_APM_TXN, TEST_TXN);

        return tracer.extract(Format.Builtin.TEXT_MAP,
                new TextMapExtractAdapter(headers));
    }

}
