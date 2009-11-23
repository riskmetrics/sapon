/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.builtin;

import java.util.ArrayList;
import java.util.List;

import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;


/**
 * Logs the specified message into the configured logger. The log levels specify
 * which attributes would be logged, and is configurable. Additionally custom
 * properties may be defined to the logger, where literal values or expressions
 * could be specified for logging. The custom properties are printed into the log
 * using the defined separator (\n, "," etc)
 */
public class LogMediator extends AbstractMediator {

    /** Only properties specified to the Log mediator */
    public static final int CUSTOM  = 0;
    /** To, From, WSAction, SOAPAction, ReplyTo, MessageID and any properties */
    public static final int SIMPLE  = 1;
    /** All SOAP header blocks and any properties */
    public static final int HEADERS = 2;
    /** all attributes of level 'simple' and the SOAP envelope and any properties */
    public static final int FULL    = 3;

    public static final String DEFAULT_SEP = ", ";

    /** The default log level is set to SIMPLE */
    private int logLevel = SIMPLE;
    /** The separator for which used to separate logging information */
    private String separator = DEFAULT_SEP;
    /** The holder for the custom properties */
    private final List<MediatorProperty> properties = new ArrayList<MediatorProperty>();

    /**
     * Logs the current message according to the supplied semantics
     *
     * @param synCtx (current) message to be logged
     * @return true always
     */
    public boolean mediate(SynapseMessageContext synCtx) {

    	log.debug("Start : Log mediator");

    	if (log.isTraceEnabled()) {
    		log.trace("Message : " + synCtx.getEnvelope());
        }

        log.info(getLogMessage(synCtx));

        log.debug("End : Log mediator");
        return true;
    }

    private String getLogMessage(SynapseMessageContext synCtx) {
        switch (logLevel) {
            case CUSTOM:
                return getCustomLogMessage(synCtx);
            case SIMPLE:
                return getSimpleLogMessage(synCtx);
            case HEADERS:
                return getHeadersLogMessage(synCtx);
            case FULL:
                return getFullLogMessage(synCtx);
            default:
                return "Invalid log level specified";
        }
    }

    private String getCustomLogMessage(SynapseMessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        setCustomProperties(sb, synCtx);
        return trimLeadingSeparator(sb);
    }

    private String getSimpleLogMessage(SynapseMessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        if (synCtx.getTo() != null) {
			sb.append("To: ").append(synCtx.getTo().getAddress());
		} else {
			sb.append("To: ");
		}
        if (synCtx.getFrom() != null) {
			sb.append(separator).append("From: ").append(synCtx.getFrom().getAddress());
		}
        if (synCtx.getWSAAction() != null) {
			sb.append(separator).append("WSAction: ").append(synCtx.getWSAAction());
		}
        if (synCtx.getSoapAction() != null) {
			sb.append(separator).append("SOAPAction: ").append(synCtx.getSoapAction());
		}
        if (synCtx.getReplyTo() != null) {
			sb.append(separator).append("ReplyTo: ").append(synCtx.getReplyTo().getAddress());
		}
        if (synCtx.getMessageID() != null) {
			sb.append(separator).append("MessageID: ").append(synCtx.getMessageID());
		}
        sb.append(separator).append("Direction: ").append(
                synCtx.isResponse() ? "response" : "request");
        setCustomProperties(sb, synCtx);
        return trimLeadingSeparator(sb);
    }

    private String getHeadersLogMessage(SynapseMessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        if (synCtx.getEnvelope() != null) {
            SOAPHeader header = synCtx.getEnvelope().getHeader();
            if (header != null) {
                for(SOAPHeaderBlock headerBlk: header.examineAllHeaderBlocks()) {
                	sb.append(separator).append(headerBlk.getLocalName()).
                		append(" : ").append(headerBlk.getText());
                }
            }
        }
        setCustomProperties(sb, synCtx);
        return trimLeadingSeparator(sb);
    }

    private String getFullLogMessage(SynapseMessageContext synCtx) {
        StringBuffer sb = new StringBuffer();
        sb.append(getSimpleLogMessage(synCtx));
        if (synCtx.getEnvelope() != null) {
			sb.append(separator).append("Envelope: ").append(synCtx.getEnvelope());
		}
        return trimLeadingSeparator(sb);
    }

    private void setCustomProperties(StringBuffer sb, SynapseMessageContext synCtx) {
        if (properties != null && !properties.isEmpty()) {
            for (MediatorProperty property : properties) {
                if(property != null){
                sb.append(separator).append(property.getName()).append(" = ").append(property.getValue()
                        != null ? property.getValue() :
                        property.getEvaluatedExpression(synCtx));
                }
            }
        }
    }

    public int getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public void addProperty(MediatorProperty p) {
        properties.add(p);
    }

    public void addAllProperties(List<MediatorProperty> list) {
        properties.addAll(list);
    }

    public List<MediatorProperty> getProperties() {
        return properties;
    }

    private String trimLeadingSeparator(StringBuffer sb) {
        String retStr = sb.toString();
        if (retStr.startsWith(separator)) {
            return retStr.substring(separator.length());
        } else {
            return retStr;
        }
    }
}
