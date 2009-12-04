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

package org.apache.axis2.wsdl.util;

import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Message;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides support for processing a WSDL4J defintion which includes imports.
 * It allows the imports to be processed into a single WSDL4J Definition object
 */
public class WSDL4JImportedWSDLHelper {

    protected static final Log log = LogFactory.getLog(WSDL4JImportedWSDLHelper.class);

    /**
     * The intention of this procedure is to process the imports. When
     * processing the imports the imported documents will be populating the
     * items in the main document recursivley
     *
     * @param wsdl4JDefinition
     */
    public static void processImports(Definition wsdl4JDefinition,
                                      List<String> processedDocuments)
    {
        Map<String, List<Import>> wsdlImports = wsdl4JDefinition.getImports();

        if (wsdlImports != null) {
            for (List<Import> imports: wsdlImports.values()) {
                for(Import wsdlImport: imports) {
                    if (wsdlImport.getDefinition() != null) {
                        Definition importedDef = wsdlImport.getDefinition();
                        if(importedDef == null) {
                        	continue;
                        }
                        String key = importedDef.getDocumentBaseURI();
                        if (key == null) {
                            key = importedDef.getTargetNamespace();
                        }
                        // stop recursive imports!
                        if (processedDocuments.contains(key)) {
                            return;
                        }
                        processedDocuments.add(key);

                        processImports(importedDef,
                                processedDocuments);

                        Map<String, String> namespaces = importedDef.getNamespaces();
                        for(Map.Entry<String, String> e: namespaces.entrySet()) {
                            if (!wsdl4JDefinition.getNamespaces().containsValue(e.getValue())) {
                                wsdl4JDefinition.getNamespaces().put(e.getKey(), e.getValue());
                            }
                        }

                        wsdl4JDefinition.getNamespaces().putAll(namespaces);
                        Types t = importedDef.getTypes();
                        if (t != null) {
                            for (ExtensibilityElement extElem: t.getExtensibilityElements()) {
                                Types types = wsdl4JDefinition.getTypes();
                                if (types == null) {
                                    types = wsdl4JDefinition.createTypes();
                                    wsdl4JDefinition.setTypes(types);
                                }
                                types.addExtensibilityElement(extElem);
                            }
                        }

                        Map<QName, Message> messagesMap = importedDef.getMessages();
                        wsdl4JDefinition.getMessages().putAll(messagesMap);

                        Map<QName, PortType> porttypeMap = importedDef.getPortTypes();
                        wsdl4JDefinition.getPortTypes().putAll(porttypeMap);

                        Map<QName, Binding> bindingMap = importedDef.getBindings();
                        wsdl4JDefinition.getBindings().putAll(bindingMap);

                        Map<QName, Service> serviceMap = importedDef.getServices();
                        wsdl4JDefinition.getServices().putAll(serviceMap);

                        List<ExtensibilityElement> extElementList
                        	= importedDef.getExtensibilityElements();
                        wsdl4JDefinition.getExtensibilityElements().addAll(
                                extElementList);
                    }
                }
            }
        }
    }
}
