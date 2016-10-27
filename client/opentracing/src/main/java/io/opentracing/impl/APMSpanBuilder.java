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
package io.opentracing.impl;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.client.api.reporter.TraceReporter;

/**
 * This class is used to build the information used to create a Trace node.
 *
 * @author gbrown
 */
public class APMSpanBuilder extends AbstractSpanBuilder {

    private Map<String, Object> state = new HashMap<>();

    private TraceReporter reporter;

    /**
     * @param operationName The operation name
     * @param reporter The trace reporter
     */
    APMSpanBuilder(String operationName, TraceReporter reporter, Clock clock) {
        super(operationName, clock);
        this.reporter = reporter;
    }

    @Override
    protected APMSpan createSpan() {
        return new APMSpan(this, reporter, clock);
    }

    @Override
    AbstractSpanBuilder withStateItem(String key, Object value) {
        state.put(key, value);
        return this;
    }

    @Override
    boolean isTraceState(String key, Object value) {
        return key.startsWith(Constants.HAWKULAR_APM_PREFIX);
    }

    protected Map<String, Object> getState() {
        return state;
    }

}
