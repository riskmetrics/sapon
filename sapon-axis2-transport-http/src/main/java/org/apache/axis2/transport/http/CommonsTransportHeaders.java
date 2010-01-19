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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.Header;

public class CommonsTransportHeaders implements Map<String, Object> {
    private Header[] headers;

    Map<String, Object> headerMap = null;

    public CommonsTransportHeaders(Header[] headers) {
        this.headers = headers;
    }

    private void init() {
        headerMap = new HashMap<String, Object>();
        for (Header header : headers) {
            headerMap.put(header.getName(), header.getValue());
        }
    }

    private void ensureInit() {
    	if(headerMap == null) {
    		init();
    	}
    }

    public int size() {
    	ensureInit();
        return headerMap.size();
    }

    public void clear() {
        if (headerMap != null) {
            headerMap.clear();
        }
    }

    public boolean isEmpty() {
    	ensureInit();
        return headerMap.isEmpty();
    }

    public boolean containsKey(Object key) {
    	ensureInit();
        return headerMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
        ensureInit();
        return headerMap.containsValue(value);
    }

    public Collection<Object> values() {
    	ensureInit();
        return headerMap.values();
    }

    public void putAll(Map<? extends String, ?> t) {
    	ensureInit();
        headerMap.putAll(t);
    }

    public Set<Map.Entry<String, Object>> entrySet() {
    	ensureInit();
        return headerMap.entrySet();
    }

    public Set<String> keySet() {
    	ensureInit();
        return headerMap.keySet();
    }

    public Object get(Object key) {
    	ensureInit();
        return headerMap.get(key);
    }

    public Object remove(Object key) {
    	ensureInit();
        return headerMap.remove(key);
    }

    public Object put(String key, Object value) {
    	ensureInit();
        return headerMap.put(key, value);
    }
}
