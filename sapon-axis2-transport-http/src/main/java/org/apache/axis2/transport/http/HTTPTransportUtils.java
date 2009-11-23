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


package org.apache.axis2.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMException;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPProcessingException;
import org.apache.axiom.soap.impl.llom.soap12.SOAP12Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.apache.axis2.transport.TransportUtils;

public class HTTPTransportUtils {

    private static final int VERSION_UNKNOWN = 0;
    private static final int VERSION_SOAP11 = 1;
    private static final int VERSION_SOAP12 = 2;

    public static InvocationResponse processHTTPPostRequest(MessageContext msgContext,
                                                            InputStream in,
                                                            OutputStream out,
                                                            String contentType,
                                                            String soapActionHeader,
                                                            String requestURI)
    	throws AxisFault
    {
        int soapVersion = VERSION_UNKNOWN;
        try {
            soapVersion = initializeMessageContext(	msgContext,
            										soapActionHeader,
            										requestURI,
            										contentType );
            msgContext.setProperty(MessageContext.TRANSPORT_OUT, out);

            msgContext.setEnvelope(
                    TransportUtils.createSOAPMessage(
                            msgContext,
                            handleGZip(msgContext, in),
                            contentType));
            return AxisEngine.receive(msgContext);
        } catch (SOAPProcessingException e) {
            throw AxisFault.makeFault(e);
        } catch (AxisFault e) {
            throw e;
        } catch (IOException e) {
            throw AxisFault.makeFault(e);
        } catch (OMException e) {
            throw AxisFault.makeFault(e);
        } catch (XMLStreamException e) {
            throw AxisFault.makeFault(e);
        } catch (FactoryConfigurationError e) {
            throw AxisFault.makeFault(e);
        } finally {
            if ((msgContext.getEnvelope() == null) && soapVersion != VERSION_SOAP11) {
                msgContext.setEnvelope(new SOAP12Factory().getDefaultEnvelope());
            }
        }
    }

    public static int initializeMessageContext(MessageContext msgContext,
                                                String soapActionHeader,
                                                String requestURI,
                                                String contentType) {
        int soapVersion = VERSION_UNKNOWN;
        // remove the starting and trailing " from the SOAP Action
        if ((soapActionHeader != null)
                && soapActionHeader.length() > 0
                && soapActionHeader.charAt(0) == '\"'
                && soapActionHeader.endsWith("\"")) {
            soapActionHeader = soapActionHeader.substring(1, soapActionHeader.length() - 1);
        }

        // fill up the Message Contexts
        msgContext.setSoapAction(soapActionHeader);
        msgContext.setTo(new EndpointReference(requestURI));
        msgContext.setServerSide(true);

        // get the type of char encoding
        String charSetEnc = BuilderUtil.getCharSetEncoding(contentType);
        if (charSetEnc == null) {
            charSetEnc = MessageContext.DEFAULT_CHAR_SET_ENCODING;
        }
        msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEnc);

        if (contentType != null) {
            if (contentType.indexOf(SOAP12Constants.SOAP_12_CONTENT_TYPE) > -1) {
                soapVersion = VERSION_SOAP12;
                TransportUtils.processContentTypeForAction(contentType, msgContext);
            } else if (contentType
                    .indexOf(SOAP11Constants.SOAP_11_CONTENT_TYPE) > -1) {
                soapVersion = VERSION_SOAP11;
            } else if (isRESTRequest(contentType)) {
                // If REST, construct a SOAP11 envelope to hold the rest message and
                // indicate that this is a REST message.
                soapVersion = VERSION_SOAP11;
                msgContext.setDoingREST(true);
            }
            if (soapVersion == VERSION_SOAP11 && soapActionHeader == null) {
            	Parameter enableREST = msgContext
            		.getParameter(Constants.Configuration.ENABLE_REST);
            	if(enableREST != null && Constants.VALUE_TRUE.equals(enableREST.getValue())) {
            		// If the content Type is text/xml (BTW which is the
            		// SOAP 1.1 Content type ) and the SOAP Action is
            		// absent it is rest !!
            		msgContext.setDoingREST(true);
                }
            }
        }
        return soapVersion;
    }

    public static InputStream handleGZip(MessageContext msgContext, InputStream in)
            throws IOException {
        Map headers = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);

        if (headers != null) {
            if (HTTPConstants.COMPRESSION_GZIP
                    .equals(headers.get(HTTPConstants.HEADER_CONTENT_ENCODING)) ||
                    HTTPConstants.COMPRESSION_GZIP.equals(headers.get(
                            HTTPConstants.HEADER_CONTENT_ENCODING_LOWERCASE))) {
                in = new GZIPInputStream(in);
            }
        }
        return in;
    }

    /**
     * This will match for content types that will be regarded as REST in WSDL2.0.
     * This contains,
     * 1. application/xml
     * 2. application/x-www-form-urlencoded
     * 3. multipart/form-data
     * <p/>
     * If the request doesnot contain a content type; this will return true.
     *
     * @param contentType content type to check
     * @return Boolean
     */
    public static boolean isRESTRequest(String contentType) {
        return contentType != null &&
               (contentType.indexOf(HTTPConstants.MEDIA_TYPE_APPLICATION_XML) > -1 ||
                contentType.indexOf(HTTPConstants.MEDIA_TYPE_X_WWW_FORM) > -1 ||
                contentType.indexOf(HTTPConstants.MEDIA_TYPE_MULTIPART_FORM_DATA) > -1);
    }
}
