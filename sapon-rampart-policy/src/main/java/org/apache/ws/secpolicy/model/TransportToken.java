/*
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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.neethi.PolicyComponent;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SP12Constants;
import org.apache.ws.secpolicy.SPConstants;


public class TransportToken extends AbstractSecurityAssertion implements TokenWrapper {

    private Token transportToken;

    public TransportToken(int version){
        setVersion(version);
    }

    /**
     * @return Returns the transportToken.
     */
    public Token getTransportToken() {
        return transportToken;
    }

    public QName getName() {
        if ( version == SPConstants.SP_V12) {
            return SP12Constants.TRANSPORT_TOKEN;
        } else {
            return SP11Constants.TRANSPORT_TOKEN;
        }
    }

    @Override
	public boolean isOptional() {
        throw new UnsupportedOperationException();
    }

    @Override
	public PolicyComponent normalize() {
        throw new UnsupportedOperationException();
    }

    @Override
	public short getType() {
        return org.apache.neethi.Constants.TYPE_ASSERTION;
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {

        String localName = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);

        if (prefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:TransportToken>

        writer.writeStartElement(namespaceURI, localName);

        String wspPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
        if (wspPrefix == null) {
            writer.setPrefix(SPConstants.WSP_PREFIX, SPConstants.POLICY.getNamespaceURI());
        }

        // <wsp:Policy>
        writer.writeStartElement(SPConstants.POLICY.getNamespaceURI(), SPConstants.POLICY.getLocalPart());

        // serialization of the token ..
        if (transportToken != null) {
            transportToken.serialize(writer);
        }

        // </wsp:Policy>
        writer.writeEndElement();


        writer.writeEndElement();
        // </sp:TransportToken>
    }

    /* (non-Javadoc)
     * @see org.apache.ws.secpolicy.model.TokenWrapper#setToken(org.apache.ws.secpolicy.model.Token)
     */
    public void setToken(Token tok) {
        this.transportToken = tok;
    }


}
