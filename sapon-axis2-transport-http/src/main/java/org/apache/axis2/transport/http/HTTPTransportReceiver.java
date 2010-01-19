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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;

/**
 * Class HTTPTransportReceiver
 */
public class HTTPTransportReceiver {
    public static Map<String, String> getGetRequestParameters(String requestURI) {

        Map<String, String> map = new HashMap<String, String>();
        if (requestURI == null || "".equals(requestURI)) {
            return map;
        }
        char[]       chars = requestURI.toCharArray();
        final int NOT_BEGUN = 1500;
        final int INSIDE_NAME = 1501;
        final int INSIDE_VALUE = 1502;
        int state = NOT_BEGUN;
        StringBuffer name = new StringBuffer();
        StringBuffer value = new StringBuffer();

        for (char c : chars) {
            if (state == NOT_BEGUN) {
                if (c == '?') {
                    state = INSIDE_NAME;
                }
            } else if (state == INSIDE_NAME) {
                if (c == '=') {
                    state = INSIDE_VALUE;
                } else {
                    name.append(c);
                }
            } else if (state == INSIDE_VALUE) {
                if (c == ',') {
                    state = INSIDE_NAME;
                    map.put(name.toString(), value.toString());
                    name.delete(0, name.length());
                    value.delete(0, value.length());
                } else {
                    value.append(c);
                }
            }
        }

        if (name.length() + value.length() > 0) {
            map.put(name.toString(), value.toString());
        }

        return map;
    }

    /**
     * Returns the HTML text for the list of services deployed.
     * This can be delegated to another Class as well
     * where it will handle more options of GET messages.
     *
     * @return Returns String.
     */
    public static String getServicesHTML(ConfigurationContext configurationContext) {
        final StringBuilder temp = new StringBuilder();
        Map<String, AxisService> services
        	= configurationContext.getAxisConfiguration().getServices();
        Map<String, String> erroneousServices =
                configurationContext.getAxisConfiguration().getFaultyServices();
        boolean status = false;

        if ((services != null) && !services.isEmpty()) {
            status = true;
            temp.append("<h2>" + "Deployed services" + "</h2>");

            for (final AxisService axisService: services.values()) {
                temp.append("<h3><a href=\"" + axisService.getName() + "?wsdl\">" +
                		axisService.getName() + "</a></h3>");

                Iterator<AxisOperation> iterator
                	= axisService.getOperations().iterator();

                if (iterator.hasNext()) {
                    temp.append("Available operations <ul>");

                    for (; iterator.hasNext();) {
                        AxisOperation axisOperation = iterator.next();

                        temp.append("<li>" + axisOperation.getName().getLocalPart() + "</li>");
                    }

                    temp.append("</ul>");
                } else {
                    temp.append("No operations specified for this service");
                }
            }
        }

        if ((erroneousServices != null) && !erroneousServices.isEmpty()) {
            temp.append("<hr><h2><font color=\"blue\">Faulty Services</font></h2>");
            status = true;
            for(String faultyServiceName: erroneousServices.keySet()) {
                temp.append("<h3><font color=\"blue\">" + faultyServiceName + "</font></h3>");
            }
        }

        if (!status) {
            temp.append("<h2>There are no services deployed</h2>");
        }

        return "<html><head><title>Axis2: Services</title></head><body>"
        		+ temp.toString()
                + "</body></html>";
    }

    // NOTE: This method is no longer used by the standard Axis2 HTTP transport (see WSCOMMONS-405).
    //       However it is still used by Synapse's NIO HTTP transport.
    public static String printServiceHTML(String serviceName,
                                          ConfigurationContext configurationContext) {
        StringBuilder temp = new StringBuilder("");
        try {
            AxisConfiguration axisConfig = configurationContext.getAxisConfiguration();
            AxisService axisService = axisConfig.getService(serviceName);

            temp.append("<h3>" + axisService.getName() + "</h3>");
            temp.append("<a href=\"" + axisService.getName() + "?wsdl\">wsdl</a> <br/> ");
            temp.append("<i>Service Description :  " + axisService.getDocumentation() +
            			"</i><br/><br/>");
            Iterator<AxisOperation> iterator = axisService.getOperations().iterator();
            if (iterator.hasNext()) {
                temp.append("Available operations <ul>");
                for (; iterator.hasNext();) {
                    AxisOperation axisOperation = iterator.next();
                    temp.append("<li>" + axisOperation.getName().getLocalPart() + "</li>");
                }
                temp.append("</ul>");
            } else {
                temp.append("No operations specified for this service");
            }
        }
        catch (AxisFault axisFault) {
        	//TODO: uhh, who sees this?  why are we writing stack traces out to
        	//html?
            return "<html><head><title>Service has a fualt</title></head>" + "<body>"
                    + "<hr><h2><font color=\"blue\">" + axisFault.getMessage() +
                    "</font></h2></body></html>";
        }
        return "<html><head><title>Axis2: Services</title></head><body>"
        		+ temp.toString()
        		+ "</body></html>";
    }
}
