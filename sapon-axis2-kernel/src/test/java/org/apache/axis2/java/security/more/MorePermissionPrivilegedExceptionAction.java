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

package org.apache.axis2.java.security.more;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.apache.axis2.java.security.interf.Actor;

/**
 * MorePermission has read permission to both public.txt and private.txt
 */

public class MorePermissionPrivilegedExceptionAction implements Actor {

    private Actor _actor;
    private boolean _usingDoPrivilege;

    // Constructor
    public MorePermissionPrivilegedExceptionAction(Actor a, boolean usingDoPrivilege) {
        _actor = a;
        _usingDoPrivilege = usingDoPrivilege;

    }

    // Implementing Actor's takeAction method
    public void takeAction() {
        try {
            if (_usingDoPrivilege) {
                // Demostrate the usage of AccessController.doPrivileged(PrivilegedExceptionAction action)
                AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Object>() {
                            public Object run() {
                                _actor.takeAction();
                                return null;
                            }
                        });
            } else {
                // Use no doPrivilege
                _actor.takeAction();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}

