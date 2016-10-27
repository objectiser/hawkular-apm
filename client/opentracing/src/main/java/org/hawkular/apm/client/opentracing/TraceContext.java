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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.hawkular.apm.api.model.events.EndpointRef;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.client.api.reporter.TraceReporter;

import io.opentracing.impl.APMSpan;

/**
 * This class represents the context associated with a trace instance.
 *
 * @author gbrown
 */
public class TraceContext {

    private Trace trace;

    private APMSpan topSpan;

    private NodeBuilder rootNode;

    private String businessTransaction;

    private String reportingLevel;

    private AtomicInteger nodeCount = new AtomicInteger(0);

    private TraceReporter reporter;

    private static List<NodeProcessor> nodeProcessors = new ArrayList<>();

    static {
        nodeProcessors.add(new DefaultNodeProcessor());
    }

    /**
     * This constructor initialises the trace context.
     *
     * @param topSpan The top level span in the trace
     * @param rootNode The builder for the root node of the trace fragment
     * @param reporter The trace reporter
     */
    public TraceContext(APMSpan topSpan, NodeBuilder rootNode, TraceReporter reporter) {
        this.topSpan = topSpan;
        this.rootNode = rootNode;
        this.reporter = reporter;

        trace = new Trace();
        trace.setId(UUID.randomUUID().toString());
        trace.setHostName(PropertyUtil.getHostName());
        trace.setHostAddress(PropertyUtil.getHostAddress());

        // Initialise the root node's path
        rootNode.setNodePath(String.format("%s:0", trace.getId()));
    }

    /**
     * This method indicates the start of processing a node within the trace
     * instance.
     */
    public void startProcessingNode() {
        nodeCount.incrementAndGet();
    }

    /**
     * This method indicates the end of processing a node within the trace
     * instance. Once all nodes for a trace have completed being processed,
     * the trace will be reported.
     */
    public void endProcessingNode() {
        if (nodeCount.decrementAndGet() == 0 && reporter != null) {
            Node node = rootNode.build();

            trace.setTimestamp(node.getTimestamp());
            trace.setBusinessTransaction(getBusinessTransaction());
            trace.getNodes().add(node);

            reporter.report(trace);
        }
    }

    /**
     * @return the businessTransaction
     */
    public String getBusinessTransaction() {
        return businessTransaction;
    }

    /**
     * @param businessTransaction the businessTransaction to set
     */
    public void setBusinessTransaction(String businessTransaction) {
        this.businessTransaction = businessTransaction;
    }

    /**
     * @return the reportingLevel
     */
    public String getReportingLevel() {
        return reportingLevel;
    }

    /**
     * @param reportingLevel the reportingLevel to set
     */
    public void setReportingLevel(String reportingLevel) {
        this.reportingLevel = reportingLevel;
    }

    /**
     * This method returns the node processors.
     *
     * @return The node processors
     */
    public List<NodeProcessor> getNodeProcessors() {
        return nodeProcessors;
    }

    /**
     * This method returns the top span associated with the traces involved in the current
     * service invocation.
     *
     * @return The top span
     */
    public APMSpan getTopSpan() {
        return topSpan;
    }

    /**
     * This method returns the source endpoint for the root trace fragment generated
     * by the service invocation.
     *
     * @return The source endpoint
     */
    public EndpointRef getSourceEndpoint() {
        return new EndpointRef(TagUtil.getUriPath(topSpan.getTags()), topSpan.getOperationName(), false);
    }

}
