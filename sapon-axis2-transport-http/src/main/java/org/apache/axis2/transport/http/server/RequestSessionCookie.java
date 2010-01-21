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

package org.apache.axis2.transport.http.server;

import java.io.IOException;

import org.apache.axis2.Axis2Constants;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

public class RequestSessionCookie implements HttpRequestInterceptor {

    public void process(final HttpRequest request, final HttpContext context)
            throws HttpException, IOException {
        if (request == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }

        String sessionCookie = null;
        Header[] headers = request.getHeaders(HTTPConstants.HEADER_COOKIE);
        for (Header header : headers) {
            HeaderElement[] elements = header.getElements();
            for (HeaderElement element : elements) {
                if (Axis2Constants.SESSION_COOKIE.equalsIgnoreCase(element.getName()) ||
                        Axis2Constants.SESSION_COOKIE_JSESSIONID.equalsIgnoreCase(element.getName())) {
                    sessionCookie = element.getValue();
                }
            }
        }
        context.setAttribute(HTTPConstants.COOKIE_STRING, sessionCookie);
    }

}
