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

package org.apache.axis2.context;

/**
 * A ContextListener gets informed about new context creations & removal of existing contexts.
 * Register one with a ConfigurationContext and you'll get notifications for every
 * sub-context creation & removal event.
 */
public interface ContextListener {

    /**
     * A context has been added
     *
     * @param context
     */
    void contextCreated(Context<?> context);

    /**
     * A context has been removed
     *
     * @param context
     */
    void contextRemoved(Context<?> context);
}
