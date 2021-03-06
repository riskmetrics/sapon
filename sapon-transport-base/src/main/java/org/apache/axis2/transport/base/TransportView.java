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

package org.apache.axis2.transport.base;

import java.util.Map;

import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.transport.TransportSender;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TransportView implements TransportViewMBean {

    @SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TransportView.class);

    public static final int STOPPED = 0;
    public static final int RUNNING = 1;
    public static final int PAUSED  = 2;
    public static final int SHUTTING_DOWN = 3;

    private TransportListener listener = null;
    private TransportSender sender = null;

    public TransportView(TransportListener listener, TransportSender sender) {
        this.listener = listener;
        this.sender = sender;
    }

    // JMX Attributes
    public long getMessagesReceived() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getMessagesReceived();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getMessagesReceived();
        }
        return -1;
    }

    public long getFaultsReceiving() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getFaultsReceiving();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getFaultsReceiving();
        }
        return -1;
    }

    public long getTimeoutsReceiving() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getTimeoutsReceiving();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getTimeoutsReceiving();
        }
        return -1;
    }

    public long getTimeoutsSending() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getTimeoutsSending();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getTimeoutsSending();
        }
        return -1;
    }

    public long getBytesReceived() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getBytesReceived();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getBytesReceived();
        }
        return -1;
    }

    public long getMessagesSent() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getMessagesSent();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getMessagesSent();
        }
        return -1;
    }

    public long getFaultsSending() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getFaultsSending();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getFaultsSending();
        }
        return -1;
    }

    public long getBytesSent() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getBytesSent();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getBytesSent();
        }
        return -1;
    }

    public long getMinSizeReceived() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getMinSizeReceived();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getMinSizeReceived();
        }
        return -1;
    }

    public long getMaxSizeReceived() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getMaxSizeReceived();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getMaxSizeReceived();
        }
        return -1;
    }

    public double getAvgSizeReceived() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getAvgSizeReceived();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getAvgSizeReceived();
        }
        return -1;
    }

    public long getMinSizeSent() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getMinSizeSent();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getMinSizeSent();
        }
        return -1;
    }

    public long getMaxSizeSent() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getMaxSizeSent();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getMaxSizeSent();
        }
        return -1;
    }

    public double getAvgSizeSent() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getAvgSizeSent();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getAvgSizeSent();
        }
        return -1;
    }

    public Map<Integer, Long> getResponseCodeTable() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getResponseCodeTable();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getResponseCodeTable();
        }
        return null;
    }

    public int getActiveThreadCount() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getActiveThreadCount();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getActiveThreadCount();
        }
        return -1;
    }

    public int getQueueSize() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getQueueSize();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getQueueSize();
        }
        return -1;
    }

    // JMX Operations
    public void start() throws Exception{
        if (listener != null) {
            listener.start();
        }
    }

    public void stop() throws Exception {
        if (listener != null) {
            listener.stop();
        } else if (sender != null) {
            sender.stop();
        }
    }

    public void pause() throws Exception {
        if (listener instanceof ManagementSupport) {
            ((ManagementSupport) listener).pause();
        } else if (sender instanceof ManagementSupport) {
            ((ManagementSupport) sender).pause();
        }
    }

    public void resume() throws Exception {
        if (listener instanceof ManagementSupport) {
            ((ManagementSupport) listener).resume();
        } else if (sender instanceof ManagementSupport) {
            ((ManagementSupport) sender).resume();
        }
    }

    public void maintenenceShutdown(long seconds) throws Exception {
        if (listener instanceof ManagementSupport) {
            ((ManagementSupport) listener).maintenenceShutdown(seconds * 1000);
        } else if (sender instanceof ManagementSupport) {
            ((ManagementSupport) sender).maintenenceShutdown(seconds * 1000);
        }
    }

    public void resetStatistics() {
        if (listener != null && listener instanceof ManagementSupport) {
            ((ManagementSupport) listener).resetStatistics();
        } else if (sender != null && sender instanceof ManagementSupport) {
            ((ManagementSupport) sender).resetStatistics();
        }
    }

    public long getLastResetTime() {
        if (listener != null && listener instanceof ManagementSupport) {
            return ((ManagementSupport) listener).getLastResetTime();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return ((ManagementSupport) sender).getLastResetTime();
        }
        return -1;
    }

    public long getMetricsWindow() {
        if (listener != null && listener instanceof ManagementSupport) {
            return System.currentTimeMillis() - ((ManagementSupport) listener).getLastResetTime();
        } else if (sender != null && sender instanceof ManagementSupport) {
            return System.currentTimeMillis() - ((ManagementSupport) sender).getLastResetTime();
        }
        return -1;
    }
}
