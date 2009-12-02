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

package org.apache.axis2.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This will make a hash map which can contain multiple entries for the same hash value.
 */
public class MultipleEntryHashMap<K, V> {

    private Map<K, List<V>> table;

    public MultipleEntryHashMap() {
        this.table = new Hashtable<K, List<V>>(1);
    }

    /**
     * Removes and returns the first value associated with the given key.
     *
     * @param key
     * @return
     */
    public V get(K key) {
        List<V> list = table.get(key);
        if (list != null && list.size() > 0) {
            V value = list.get(0);
            list.remove(0);
            return value;
        }
        return null;
    }

    public V put(K key, V value) {
        List<V> list = table.get(key);
        if (list == null) {
        	list = new ArrayList<V>();
            table.put(key, list);
        }
        list.add(value);
        return value;
    }

    public Set<K> keySet() {
        return table.keySet();
    }
}
