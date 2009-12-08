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
package org.apache.axiom.om.ds;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMDataSourceExt;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ByteArrayDataSource is an example implementation of OMDataSourceExt.
 * Use it to insert a (byte[], encoding) into an OM Tree.
 * This data source is useful for placing bytes into an OM
 * tree, instead of having a deeply nested tree.
 */
public class ByteArrayDataSource extends OMDataSourceExtBase<EncodedByteArray> {

    private static final Log log = LogFactory.getLog(ByteArrayDataSource.class);
    private static boolean DEBUG_ENABLED = log.isDebugEnabled();

    private EncodedByteArray byteArray;

    /**
     * Constructor
     * @param bytes
     * @param encoding
     */
    public ByteArrayDataSource(byte[] bytes, String encoding) {
        this(new EncodedByteArray(bytes, encoding));
    }

    public ByteArrayDataSource(EncodedByteArray bytes) {
    	byteArray = bytes;
    }


    public XMLStreamReader getReader() throws XMLStreamException {
        if (DEBUG_ENABLED) {
            log.debug("getReader");
        }
        return StAXUtils.createXMLStreamReader(new ByteArrayInputStream(byteArray.getBytes()),
                                               byteArray.getEncoding());
    }

    public EncodedByteArray getObject() {
       return byteArray;
    }

    public boolean isDestructiveRead() {
        // Reading bytes is not destructive
        return false;
    }

    public boolean isDestructiveWrite() {
        // Writing bytes is not destructive
        return false;
    }

    public byte[] getXMLBytes(String encoding) throws UnsupportedEncodingException {
        if (DEBUG_ENABLED) {
            log.debug("getXMLBytes encoding="+encoding);
        }
        // Return the byte array directly if it is the same encoding
        // Otherwise convert the bytes to the proper encoding
        EncodedByteArray out = byteArray;
        if (!byteArray.getEncoding().equalsIgnoreCase(encoding)) {
            String text = new String(byteArray.getBytes(), byteArray.getEncoding());

            // Convert the internal data structure to the new bytes/encoding
            out = new EncodedByteArray(text.getBytes(encoding), encoding);
        }
        return out.getBytes();
    }

    public void close() {
        byteArray = null;
    }

    public OMDataSourceExt<EncodedByteArray> copy() {
        return new ByteArrayDataSource(byteArray.copy());
    }
}
