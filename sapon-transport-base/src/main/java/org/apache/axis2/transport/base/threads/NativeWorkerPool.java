/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.axis2.transport.base.threads;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Worker pool implementation based on java.util.concurrent in JDK 1.5 or later.
 */
public class NativeWorkerPool implements WorkerPool {

    static final Log log = LogFactory.getLog(NativeWorkerPool.class);

    private final ThreadPoolExecutor executor;
    private final LinkedBlockingQueue<Runnable> blockingQueue;

    public NativeWorkerPool(int core, int max, int keepAlive,
        int queueLength, String threadGroupName, String threadGroupId) {

        if (log.isDebugEnabled()) {
            log.debug("Using native util.concurrent package..");
        }
        blockingQueue =
            (queueLength == -1 ? new LinkedBlockingQueue<Runnable>()
                               : new LinkedBlockingQueue<Runnable>(queueLength));
        executor = new ThreadPoolExecutor(
            core, max, keepAlive,
            TimeUnit.SECONDS,
            blockingQueue,
            new NativeThreadFactory(new ThreadGroup(threadGroupName), threadGroupId));
    }

    public void execute(final Runnable task) {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    task.run();
                } catch (Throwable t) {
                    log.error("Uncaught exception", t);
                }
            }
        });
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public int getQueueSize() {
        return blockingQueue.size();
    }

    public void shutdown(int timeout) throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
    }
}
