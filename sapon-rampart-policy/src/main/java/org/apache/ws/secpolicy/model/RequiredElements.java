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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.neethi.PolicyComponent;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SP12Constants;
import org.apache.ws.secpolicy.SPConstants;

public class RequiredElements extends AbstractSecurityAssertion {

    private List<String> xPathExpressions = new ArrayList<String>();

    private Map<String, String> declaredNamespaces = new HashMap<String, String>();

    private String xPathVersion;

    public RequiredElements(int version) {
        setVersion(version);
    }

    /**
     * @return Returns the xPathExpressions.
     */
    public List<String> getXPathExpressions() {
        return xPathExpressions;
    }

    public void addXPathExpression(String expr) {
        this.xPathExpressions.add(expr);
    }

    /**
     * @return Returns the xPathVersion.
     */
    public String getXPathVersion() {
        return xPathVersion;
    }

    /**
     * @param pathVersion
     *            The xPathVersion to set.
     */
    public void setXPathVersion(String pathVersion) {
        xPathVersion = pathVersion;
    }

    public Map<String, String> getDeclaredNamespaces () {
        return declaredNamespaces;
    }

    public void addDeclaredNamespaces(String uri, String prefix ) {
        declaredNamespaces.put(prefix, uri);
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {

        String localName = getName().getLocalPart();
        String namespaceURI = getName().getNamespaceURI();

        String prefix;
        String writerPrefix = writer.getPrefix(namespaceURI);

        if (writerPrefix == null) {
            prefix = getName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        } else {
            prefix = writerPrefix;
        }

        //  <sp:RequiredElements>
        writer.writeStartElement(prefix, localName, namespaceURI);

        // xmlns:sp=".."
        writer.writeNamespace(prefix, namespaceURI);

        if (writerPrefix == null) {
            // xmlns:sp=".."
            writer.writeNamespace(prefix, namespaceURI);
        }

        if (xPathVersion != null) {
            writer.writeAttribute(prefix, namespaceURI, SPConstants.XPATH_VERSION, xPathVersion);
        }

        String xpathExpression;

        for (Object element : xPathExpressions) {
         xpathExpression = (String) element;
         // <sp:XPath ..>
         writer.writeStartElement(prefix, SPConstants.XPATH_EXPR, namespaceURI);
         writer.writeCharacters(xpathExpression);
         writer.writeEndElement();
      }

        //</sp:RequiredElements>
        writer.writeEndElement();
    }

    public QName getName() {
        if (version == SPConstants.SP_V12) {
            return SP12Constants.REQUIRED_ELEMENTS;
        } else {
            return SP11Constants.REQUIRED_ELEMENTS;
        }
    }

    @Override
	public PolicyComponent normalize() {
        return this;
    }
}
