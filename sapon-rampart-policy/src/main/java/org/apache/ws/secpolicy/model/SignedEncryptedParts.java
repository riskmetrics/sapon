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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.neethi.PolicyComponent;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SP12Constants;
import org.apache.ws.secpolicy.SPConstants;

public class SignedEncryptedParts extends AbstractSecurityAssertion {

    private boolean body;

    private boolean attachments;

    private List<Header> headers = new ArrayList<Header>();

    private boolean signedParts;

    public SignedEncryptedParts(boolean signedParts, int version) {
        this.signedParts = signedParts;
        setVersion(version);
    }

    /**
     * @return Returns the body.
     */
    public boolean isBody() {
        return body;
    }

    /**
     * @param body The body to set.
     */
    public void setBody(boolean body) {
        this.body = body;
    }

    /**
     * @return Returns the attachments.
     */
    public boolean isAttachments() {
        return attachments;
    }

    /**
     * @param attachments The attachments to set.
     */
    public void setAttachments(boolean attachments) {
        this.attachments = attachments;
    }

    /**
     * @return Returns the headers.
     */
    public List<Header> getHeaders() {
        return this.headers;
    }

    /**
     * @param headers The headers to set.
     */
    public void addHeader(Header header) {
        this.headers.add(header);
    }

    /**
     * @return Returns the signedParts.
     */
    public boolean isSignedParts() {
        return signedParts;
    }

    public QName getName() {
        if (signedParts) {
            if ( version == SPConstants.SP_V12) {
                return SP12Constants.SIGNED_PARTS;
            } else {
                return SP11Constants.SIGNED_PARTS;
            }
        }

        if ( version == SPConstants.SP_V12) {
            return SP12Constants.ENCRYPTED_PARTS;
        } else {
            return SP11Constants.ENCRYPTED_PARTS;
        }

    }

    @Override
	public PolicyComponent normalize() {
        return this;
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        String localName = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);

        if (prefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:SignedParts> | <sp:EncryptedParts>
        writer.writeStartElement(prefix, localName, namespaceURI);

        // xmlns:sp=".."
        writer.writeNamespace(prefix, namespaceURI);

        if (isBody()) {
            // <sp:Body />
            writer.writeStartElement(prefix, SPConstants.BODY, namespaceURI);
            writer.writeEndElement();
        }

        Header header;
        for (Object element : headers) {
            header = (Header) element;
            // <sp:Header Name=".." Namespace=".." />
            writer.writeStartElement(prefix, SPConstants.HEADER, namespaceURI);
            // Name attribute is optional
            if (header.getName() != null) {
                writer.writeAttribute("Name", header.getName());
            }
            writer.writeAttribute("Namespace", header.getNamespace());

            writer.writeEndElement();
        }

        if (isAttachments() && version == SPConstants.SP_V12) {
            // <sp:Attachments />
            writer.writeStartElement(prefix, SPConstants.ATTACHMENTS, namespaceURI);
            writer.writeEndElement();
        }

        // </sp:SignedParts> | </sp:EncryptedParts>
        writer.writeEndElement();
    }


}
