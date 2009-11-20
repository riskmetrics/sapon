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

package org.apache.axiom.om.impl;

import javax.xml.stream.Location;

public class EmptyOMLocation implements Location {


    public int getLineNumber() {
        return -1;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getColumnNumber() {
        return -1;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getCharacterOffset() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getPublicId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getSystemId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
