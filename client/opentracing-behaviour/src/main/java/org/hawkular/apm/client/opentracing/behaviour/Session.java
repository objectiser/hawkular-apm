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

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.client.opentracing.behaviour.model.EventType;
import org.hawkular.apm.client.opentracing.behaviour.model.FiniteStateMachine;
import org.hawkular.apm.client.opentracing.behaviour.model.State;
import org.hawkular.apm.client.opentracing.behaviour.model.Transition;

/**
 * @author gbrown
 */
public class Session {

    private static final Logger log = Logger.getLogger(Session.class);

    private FiniteStateMachine fsm;
    private State currentState;
    private long lastUpdated;

    public Session(FiniteStateMachine fsm) {
        this.fsm = fsm;
        currentState = fsm.getStates().get(fsm.getInitialState());
    }

    public boolean isValid(EventType type, String operation, String peer) {
        boolean ret = false;

        // Check if transition exists
        // TODO: Maybe support endpoint name mapping at some point, allowing service to use its
        // own names, as long as they are used consistently in line with the peer names in the
        // fsm
        Transition next = currentState.getTransitions().stream()
                .filter(transition -> transition.getEventType() == type && transition.getOperation().equals(operation)
                    && (peer == null || peer.equals(transition.getPeer())))
                .findFirst().orElse(null);

        if (next != null) {
            currentState = fsm.getStates().get(next.getNextState());
            lastUpdated = System.currentTimeMillis();
            ret = true;
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Session["+this+"] type="+type+" operation="+operation+" peer="+peer+" isValid="+ret);
        }

        return ret;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

}
