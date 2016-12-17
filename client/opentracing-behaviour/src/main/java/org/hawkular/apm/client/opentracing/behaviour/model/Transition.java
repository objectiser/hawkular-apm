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
package org.hawkular.apm.client.opentracing.behaviour.model;

/**
 * @author gbrown
 */
public class Transition {

    private EventType eventType;

    private String operation;

    private String peer;

    private String nextState;

    /**
     * @return the eventType
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * @param eventType the eventType to set
     */
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    /**
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @param operation the operation to set
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * @return the peer
     */
    public String getPeer() {
        return peer;
    }

    /**
     * @param peer the peer to set
     */
    public void setPeer(String peer) {
        this.peer = peer;
    }

    /**
     * @return the nextState
     */
    public String getNextState() {
        return nextState;
    }

    /**
     * @param nextState the nextState to set
     */
    public void setNextState(String nextState) {
        this.nextState = nextState;
    }

}
