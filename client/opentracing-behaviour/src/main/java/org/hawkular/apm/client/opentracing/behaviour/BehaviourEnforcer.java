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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.client.opentracing.TraceListener;
import org.hawkular.apm.client.opentracing.behaviour.model.EventType;
import org.hawkular.apm.client.opentracing.behaviour.model.FiniteStateMachine;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentracing.impl.APMSpan;

/**
 * @author gbrown
 */
public class BehaviourEnforcer implements TraceListener {

    private static final Logger log = Logger.getLogger(BehaviourEnforcer.class);

    private Map<String, Session> sessions = new HashMap<>();
    private Map<String, FiniteStateMachine> fsms = new HashMap<>();

    private static ObjectMapper mapper = new ObjectMapper();

    public BehaviourEnforcer() {
    }

    protected FiniteStateMachine getFiniteStateMachine(String transaction, String service) {
        String fsmName = transaction + "-" + service + ".fsm";
        FiniteStateMachine fsm = fsms.get(fsmName);


        if (fsm == null) {
            // Load FSM
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fsmName);
            if (is != null) {
                try {
                    fsm = mapper.readValue(is, FiniteStateMachine.class);
                    fsms.put(fsmName, fsm);
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Failed to load FSM", e);
                }
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("getFiniteStateMachine: fsmName="+fsmName+" fsm = "+fsm);
        }

        return fsm;
    }

    protected Session getSession(String transaction, String service, String traceId) {
        synchronized (sessions) {
            Session session = sessions.get(traceId);

            if (session == null) {
                // Create new session
                FiniteStateMachine fsm = getFiniteStateMachine(transaction, service);
                if (fsm != null) {
                    session = new Session(fsm);
                    sessions.put(traceId, session);
                }
            }

            return session;
        }
    }

    @Override
    public void spanCreated(String transaction, String service, String traceId, APMSpan span) {
    }

    @Override
    public void spanExtracted(String transaction, String service, String traceId, APMSpan span) {
        Session session = getSession(transaction, service, traceId);
        if (session != null) {
            boolean valid = session.isValid(EventType.Receive, span.getOperationName(), null);
            span.setTag("fsm.request", valid);
            span.setTag("fsm.nodeType", NodeType.Consumer.name());
            if (!valid) {
                span.addProperty(new Property("fault", "UnexpectedBehaviour"));
            }
        }
    }

    @Override
    public void spanInjected(String transaction, String service, String traceId, APMSpan span) {
        Session session = getSession(transaction, service, traceId);
        if (session != null) {
            boolean valid = session.isValid(EventType.Send, span.getOperationName(), null);
            span.setTag("fsm.request", valid);
            span.setTag("fsm.nodeType", NodeType.Producer.name());
            if (!valid) {
                span.addProperty(new Property("fault", "UnexpectedBehaviour"));
            }
        }
    }

    @Override
    public void spanFinished(String transaction, String service, String traceId, APMSpan span) {
        if (span.getTags().containsKey("fsm.request")) {
            // Check if status has been defined, indicating a response
            String op = getResponseOperation(span);
            if (op != null) {
                Session session = getSession(transaction, service, traceId);
                if (session != null) {
                    NodeType nodeType = NodeType.valueOf(span.getTags().get("fsm.nodeType").toString());
                    boolean valid = session.isValid(nodeType == NodeType.Consumer
                            ? EventType.Send : EventType.Receive, op, null);
                    span.setTag("fsm.response", valid);
                    if (!valid) {
                        span.addProperty(new Property("fault", "UnexpectedBehaviour"));
                    }
                }
            }
        }
    }

    protected String getResponseOperation(APMSpan span) {
        String op = (String) span.getTags().get("fault");
        if (op == null && span.getTags().keySet().stream().filter(k -> k.endsWith("status_code")).count() > 0) {
            op = span.getOperationName();
        }
        return op;
    }
}
