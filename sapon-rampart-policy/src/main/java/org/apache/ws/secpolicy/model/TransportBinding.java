/**
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ws.secpolicy.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.neethi.All;
import org.apache.neethi.Assertion;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SP12Constants;
import org.apache.ws.secpolicy.SPConstants;

public class TransportBinding extends Binding {

    private TransportToken transportToken;

    private List<TransportBinding> transportBindings;

    public TransportBinding(int version) {
        super(version);
    }
    /**
     * @return Returns the transportToken.
     */
    public TransportToken getTransportToken() {
        return transportToken;
    }

    /**
     * @param transportToken
     *            The transportToken to set.
     */
    public void setTransportToken(TransportToken transportToken) {
        this.transportToken = transportToken;
    }

    public List<TransportBinding> getConfigurations() {
        return transportBindings;
    }

    public TransportBinding getDefaultConfiguration() {
        if (transportBindings != null) {
            return transportBindings.get(0);
        }
        return null;
    }

    public void addConfiguration(TransportBinding transportBinding) {
        if (transportBindings == null) {
            transportBindings = new ArrayList<TransportBinding>();
        }
        transportBindings.add(transportBinding);
    }

    public QName getName() {
        if (version == SPConstants.SP_V12) {
            return SP12Constants.TRANSPORT_BINDING;
        } else {
            return SP11Constants.TRANSPORT_BINDING;
        }
    }

    @Override
	public PolicyComponent normalize() {
        if (isNormalized()) {
            return this;
        }

        AlgorithmSuite algorithmSuite = getAlgorithmSuite();
        List<Assertion> configurations = algorithmSuite.getConfigurations();

        if (configurations != null && configurations.size() == 1) {
            setNormalized(true);
            return this;
        }

        Policy policy = new Policy();
        ExactlyOne exactlyOne = new ExactlyOne();

        All wrapper;
        TransportBinding transportBinding;

        for (Assertion assertion: configurations) {
            wrapper = new All();
            transportBinding = new TransportBinding(this.getVersion());

            algorithmSuite = (AlgorithmSuite)assertion;
            transportBinding.setAlgorithmSuite(algorithmSuite);
            transportBinding.setIncludeTimestamp(isIncludeTimestamp());
            transportBinding.setLayout(getLayout());
            transportBinding
                    .setSignedEndorsingSupportingTokens(getSignedEndorsingSupportingTokens());
            transportBinding
                    .setSignedSupportingToken(getSignedSupportingToken());
            transportBinding.setTransportToken(getTransportToken());

            wrapper.addPolicyComponent(transportBinding);
            exactlyOne.addPolicyComponent(wrapper);
        }

        policy.addPolicyComponent(exactlyOne);
        return policy;
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        String localName = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);

        if (prefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:TransportBinding>
        writer.writeStartElement(namespaceURI, localName);
        writer.writeNamespace(prefix, namespaceURI);

        String pPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
        if (pPrefix == null) {
            writer.setPrefix(SPConstants.WSP_PREFIX, SPConstants.POLICY.getNamespaceURI());
        }

        // <wsp:Policy>
        writer.writeStartElement(SPConstants.POLICY.getNamespaceURI(), SPConstants.POLICY.getLocalPart());


        if (transportToken == null) {
            // TODO more meaningful exception
            throw new RuntimeException("no TransportToken found");
        }

        // <sp:TransportToken>
        transportToken.serialize(writer);
        // </sp:TransportToken>

        AlgorithmSuite algorithmSuite = getAlgorithmSuite();
        if (algorithmSuite == null) {
            throw new RuntimeException("no AlgorithmSuite found");
        }

        // <sp:AlgorithmSuite>
        algorithmSuite.serialize(writer);
        // </sp:AlgorithmSuite>

        Layout layout = getLayout();
        if (layout != null) {
            // <sp:Layout>
            layout.serialize(writer);
            // </sp:Layout>
        }

        if (isIncludeTimestamp()) {
            // <sp:IncludeTimestamp>
            writer.writeStartElement(namespaceURI, SPConstants.INCLUDE_TIMESTAMP);
            writer.writeEndElement();
            // </sp:IncludeTimestamp>
        }

        // </wsp:Policy>
        writer.writeEndElement();

        // </sp:TransportBinding>
        writer.writeEndElement();

    }

}
