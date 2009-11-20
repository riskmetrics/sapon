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

package org.apache.axiom.attachments.utils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

public final class BAAInputStreamTestCase extends junit.framework.TestCase {

    public void testRead() throws Exception {
        byte [] data = new byte [] { 5, 10, -10, -5, 0 };
        ArrayList<byte[]> list = new ArrayList<byte[]>();
        list.add(data);
        BAAInputStream in = new BAAInputStream(list, 5);
        ByteArrayInputStream expected = new ByteArrayInputStream(data);

        assertEquals(expected.read(), in.read());
        assertEquals(expected.read(), in.read());
        assertEquals(expected.read(), in.read());
        assertEquals(expected.read(), in.read());
        assertEquals(expected.read(), in.read());
        assertEquals(expected.read(), in.read());
    }

}
