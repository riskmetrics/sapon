/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axiom.soap.impl.dom;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.serialize.StreamWriterToContentHandlerConverter;
import org.apache.axiom.om.impl.util.OMSerializerUtil;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultDetail;
import org.apache.axiom.soap.SOAPProcessingException;

public abstract class SOAPFaultDetailImpl extends SOAPElement implements SOAPFaultDetail {


    protected SOAPFaultDetailImpl(SOAPFault parent,
                                  boolean extractNamespaceFromParent, SOAPFactory factory)
            throws SOAPProcessingException {
        super(parent,
              factory.getSOAPVersion().getFaultDetailQName().getLocalPart(),
              extractNamespaceFromParent, factory);
    }

    protected SOAPFaultDetailImpl(SOAPFactory factory) {
        super(factory);
    }

    protected SOAPFaultDetailImpl(SOAPFault parent,
                                  OMXMLParserWrapper builder,
                                  SOAPFactory factory) {
        super(parent, factory.getSOAPVersion().getFaultDetailQName().getLocalPart(), builder,
              factory);
    }

    public void addDetailEntry(OMElement detailElement) {
        this.addChild(detailElement);
    }

    public Iterable<OMElement> getAllDetailEntries() {
        return getChildElements();
    }

    @Override
	protected void internalSerialize(XMLStreamWriter writer, boolean cache)
            throws XMLStreamException {
        // select the builder
        short builderType = PULL_TYPE_BUILDER;    // default is pull type
        if (builder != null) {
            builderType = this.builder.getBuilderType();
        }
        if ((builderType == PUSH_TYPE_BUILDER)
                && (builder.getRegisteredContentHandler() == null)) {
            builder.registerExternalContentHandler(
                    new StreamWriterToContentHandlerConverter(writer));
        }

        if (!cache) {
            //No caching
            if (this.firstChild != null) {
                OMSerializerUtil.serializeStartpart(this, writer);
                firstChild.internalSerializeAndConsume(writer);
                OMSerializerUtil.serializeEndpart(writer);
            } else if (!this.done) {
                if (builderType == PULL_TYPE_BUILDER) {
                    OMSerializerUtil.serializeByPullStream(this, writer);
                } else {
                    OMSerializerUtil.serializeStartpart(this, writer);
                    builder.setCache(cache);
                    builder.next();
                    OMSerializerUtil.serializeEndpart(writer);
                }
            } else {
                OMSerializerUtil.serializeNormal(this, writer, cache);
            }
            // do not serialise the siblings


        } else {
            //Cached
            OMSerializerUtil.serializeNormal(this, writer, cache);

            // do not serialise the siblings
        }


    }
}
