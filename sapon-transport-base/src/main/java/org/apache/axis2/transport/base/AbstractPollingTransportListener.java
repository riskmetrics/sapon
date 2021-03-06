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

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.AxisFault;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.Timer;

public abstract class AbstractPollingTransportListener<T extends AbstractPollTableEntry>
        extends AbstractTransportListener {

    /** The main timer. */
    private Timer timer;
    /** Keep the list of endpoints and poll durations */
    private final List<T> pollTable = new ArrayList<T>();

    @Override
    public void init(ConfigurationContext cfgCtx,
            TransportInDescription transportIn) throws AxisFault {

        timer = new Timer("PollTimer");
        super.init(cfgCtx, transportIn);
        T entry = createPollTableEntry(transportIn);
        if (entry != null) {
            entry.setPollInterval(getPollInterval(transportIn));
            schedulePoll(entry);
            pollTable.add(entry);
        }
    }

    @Override
    public void destroy() {
        // Explicitly cancel all polls not predispatched to services. All other polls will
        // be canceled by stopListeningForService. Pay attention to the fact the cancelPoll
        // modifies pollTable.
        List<T> entriesToCancel = new ArrayList<T>();
        for (T entry : pollTable) {
            if (entry.getService() == null) {
                entriesToCancel.add(entry);
            }
        }
        for (T entry : entriesToCancel) {
            cancelPoll(entry);
        }
        
        super.destroy();
        timer.cancel();
        timer = null;
    }

    /**
     * Schedule a repeated poll at the specified interval for a given service.
     * The method will schedule a single-shot timer task with executes a work
     * task on the worker pool. At the end of this work task, a new timer task
     * is scheduled for the next poll (except if the polling for the service
     * has been canceled). This effectively schedules the poll repeatedly
     * with fixed delay.
     * @param entry the poll table entry with the configuration for the service
     * @param pollInterval the interval between successive polls in milliseconds
     */
    void schedulePoll(final T entry) {
        final long pollInterval = entry.getPollInterval();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                workerPool.execute(new Runnable() {
                    public void run() {
                        if (state == BaseConstants.PAUSED) {
                            if (log.isDebugEnabled()) {
                                log.debug("Transport " + getTransportName() +
                                        " poll trigger : Transport is currently paused..");
                            }
                        } else {
                            poll(entry);
                        }
                    }
                });
            }
        };
        entry.timerTask = timerTask;
        if (entry.isConcurrentPollingAllowed()) {
            timer.scheduleAtFixedRate(timerTask, pollInterval, pollInterval);
        } else {
            timer.schedule(timerTask, pollInterval);
        }
    }

    private void cancelPoll(T entry) {
        synchronized (entry) {
            entry.timerTask.cancel();
            entry.canceled = true;
        }
        pollTable.remove(entry);
    }

    protected abstract void poll(T entry);

    protected void onPollCompletion(T entry) {
        if (!entry.isConcurrentPollingAllowed()) {
            synchronized (entry) {
                if (!entry.canceled) {
                    schedulePoll(entry);
                }
            }
        }
    }

    /**
     * method to log a failure to the log file and to update the last poll status and time
     * @param msg text for the log message
     * @param e optional exception encountered or null
     * @param entry the PollTableEntry
     */
    protected void processFailure(String msg, Exception e, T entry) {
        if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }
        long now = System.currentTimeMillis();
        entry.setLastPollState(AbstractPollTableEntry.FAILED);
        entry.setLastPollTime(now);
        entry.setNextPollTime(now + entry.getPollInterval());
        onPollCompletion(entry);
    }

    private long getPollInterval(ParameterInclude params) {
        Parameter param = params.getParameter(BaseConstants.TRANSPORT_POLL_INTERVAL);
        long pollInterval = BaseConstants.DEFAULT_POLL_INTERVAL;
        if (param != null && param.getValue() instanceof String) {
            String s = (String)param.getValue();
            int multiplier;
            if (s.endsWith("ms")) {
                s = s.substring(0, s.length()-2);
                multiplier = 1;
            } else {
                multiplier = 1000;
            }
            try {
                pollInterval = Integer.parseInt(s) * multiplier;
            } catch (NumberFormatException e) {
                log.error("Invalid poll interval : " + param.getValue() + ",  default to : "
                        + (BaseConstants.DEFAULT_POLL_INTERVAL / 1000) + "sec", e);
            }
        }
        return pollInterval;
    }
    
    @Override
    protected void startListeningForService(AxisService service) throws AxisFault {
        T entry = createPollTableEntry(service);
        if (entry == null) {
            throw new AxisFault("The service has no configuration for the transport");
        }
        entry.setService(service);
        entry.setPollInterval(getPollInterval(service));
        schedulePoll(entry);
        pollTable.add(entry);
    }
    
    /**
     * Create a poll table entry based on the provided parameters.
     * If no relevant parameters are found, the implementation should
     * return null. An exception should only be thrown if there is an
     * error or inconsistency in the parameters.
     * 
     * @param params The source of the parameters to construct the
     *               poll table entry. If the parameters were defined on
     *               a service, this will be an {@link AxisService}
     *               instance.
     * @return
     */
    protected abstract T createPollTableEntry(ParameterInclude params) throws AxisFault;

    /**
     * Get the EPR for the given service
     * 
     * @param serviceName service name
     * @param ip          ignored
     * @return the EPR for the service
     * @throws AxisFault not used
     */
    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        for (T entry : pollTable) {
            AxisService service = entry.getService();
            if (service != null) {
                String candidateName = service.getName();
                if (candidateName.equals(serviceName) ||
                        serviceName.startsWith(candidateName + ".")) {
                    return new EndpointReference[]{ entry.getEndpointReference() };
                }
            }
        }
        return null;
    }

    @Override
    protected void stopListeningForService(AxisService service) {
        for (T entry : pollTable) {
            if (service == entry.getService()) {
                cancelPoll(entry);
                break;
            }
        }
    }

    // -- jmx/management methods--
    /**
     * Pause the listener - Stop accepting/processing new messages, but continues processing existing
     * messages until they complete. This helps bring an instance into a maintenence mode
     * @throws org.apache.axis2.AxisFault on error
     */
    public void pause() throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        state = BaseConstants.PAUSED;
        log.info("Listener paused");
    }

    /**
     * Resume the lister - Brings the lister into active mode back from a paused state
     * @throws AxisFault on error
     */
    public void resume() throws AxisFault {
        if (state != BaseConstants.PAUSED) return;
        state = BaseConstants.STARTED;
        log.info("Listener resumed");
    }

    /**
     * Stop processing new messages, and wait the specified maximum time for in-flight
     * requests to complete before a controlled shutdown for maintenence
     *
     * @param millis a number of milliseconds to wait until pending requests are allowed to complete
     * @throws AxisFault on error
     */
    public void maintenenceShutdown(long millis) throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        stop();
        state = BaseConstants.STOPPED;
        log.info("Listener shutdown");
    }
}
