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
package org.apache.axiom.om;

import javax.xml.stream.XMLStreamReader;

/**
 * Objects returned by OMElement.getXMLStreamReader may implement this interface
 */
/**
 * @author scheu
 *
 */
public interface OMXMLStreamReader extends XMLStreamReader, OMAttachmentAccessor {
    
    /**
     * By default, an OMText item that has an MTOM datahandler 
     * will be rendered as a inlined text event.
     * @return true if inlined as TEXT, false if XOP_INCLUDE is used
     */
    public boolean isInlineMTOM();
    
    /**
     * @param value set to true if inlining of text is desired (default)
     * throw OMException if not the value is not supported.
     */
    public void setInlineMTOM(boolean value);
    
}
