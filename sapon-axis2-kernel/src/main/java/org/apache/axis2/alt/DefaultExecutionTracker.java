package org.apache.axis2.alt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.util.LoggingControl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultExecutionTracker implements ExecutionTracker
{
	private static final Log log
		= LogFactory.getLog(DefaultExecutionTracker.class);

	private Flows flow = Flows.IN;

	/**
     * @serial The chain of Handlers/Phases for processing this message
     */
    private final List<Handler> executionChain = new ArrayList<Handler>();

    /**
     * @serial The chain of executed Handlers/Phases from processing
     */
    private LinkedList<Handler> executedPhases;

    /**
     * @serial Index into the executuion chain of the currently executing handler
     */
    private int currentHandlerIndex;

    /**
     * @serial Index into the current Phase of the currently executing handler (if any)
     */
    private int currentPhaseIndex;

    /**
     * Indicates whether the executed phase list
     * was reset before the restored list has been reconciled
     */
    private transient boolean executedPhasesReset = false;


	@Override
	public void addExecutedPhase(Handler phase) {
        if (executedPhases == null) {
            executedPhases = new LinkedList<Handler>();
        }
        executedPhases.addFirst(phase);
	}

	@Override
	public int getCurrentHandlerIndex() {
		return currentHandlerIndex;
	}

	@Override
	public int getCurrentPhaseIndex() {
		return currentPhaseIndex;
	}

	@Override
	public LinkedList<Handler> getExecutedPhases() {
        if (executedPhases == null) {
            executedPhases = new LinkedList<Handler>();
        }
        return executedPhases;
	}

	@Override
	public List<Handler> getExecutionChain() {
        return executionChain;
	}

	@Override
	public Flows getFlow() {
		return this.flow;
	}

	@Override
	public Handler removeFirstExecutedPhase() {
        if (executedPhases != null) {
            return executedPhases.removeFirst();
        }
        return null;
	}

	@Override
	public void resetExecutedPhases() {
        executedPhasesReset = true;
        executedPhases = new LinkedList<Handler>();
    }

	public boolean isExecutedPhasesReset() {
		return executedPhasesReset;
	}

	@Override
	public void setCurrentHandlerIndex(int currentHandlerIndex) {
		this.currentHandlerIndex = currentHandlerIndex;
	}

	@Override
	public void setCurrentPhaseIndex(int currentPhaseIndex) {
		this.currentPhaseIndex = currentPhaseIndex;
	}

	@Override
	public void setExecutedPhasesExplicit(LinkedList<Handler> inb) {
		this.executedPhases = inb;
	}

	@Override
	public void setExecutionChain(List<? extends Handler> executionChain) {
        this.executionChain.clear();
        this.executionChain.addAll(executionChain);
        currentHandlerIndex = -1;
        currentPhaseIndex = 0;
	}

	@Override
	public void setFlow(Flows flow) {
		this.flow = flow;
	}

    /**
     * Flatten the phase list into a list of just unique handler instances
     *
     * @param list the list of handlers
     * @param map  users should pass null as this is just a holder for the recursion
     * @return a list of unigue object instances
     */
    public List<Handler> flattenPhaseListToHandlers(	List<Handler> list,
    													Map<String, Handler> map )
    {
        if (map == null) {
            map = new LinkedHashMap<String, Handler>();
        }
        for(final Handler handler: list) {
        	if(handler == null) {
        		continue;
        	}
        	String key = handler.getClass().getName() + "@" + handler.hashCode();

            if (handler instanceof Phase) {
                flattenHandlerList(((Phase) handler).getHandlers(), map);
            } else {
                map.put(key, handler);
            }
        }

        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            for(Map.Entry<String, Handler> e: map.entrySet()) {
                log.trace("flattenPhaseListToHandlers():  key [" + e.getKey() +
                        "]    handler name [" + e.getValue().getName() + "]");
            }
        }

        return new ArrayList<Handler>(map.values());
    }

    /**
     * Flatten the handler list into just unique handler instances
     * including phase instances.
     *
     * @param list the list of handlers/phases
     * @param map  users should pass null as this is just a holder for the recursion
     * @return a list of unigue object instances
     */
    public List<Handler> flattenHandlerList(	List<Handler> list,
    											Map<String, Handler> map )
    {
        if (map == null) {
            map = new LinkedHashMap<String, Handler>();
        }

        for(Handler handler: list) {
        	if(handler == null) {
        		continue;
        	}
            String key = handler.getClass().getName() + "@" + handler.hashCode();
            map.put(key, handler);
            if (handler instanceof Phase) {
                flattenHandlerList(((Phase) handler).getHandlers(), map);
            }
        }
        return new ArrayList<Handler>(map.values());
    }

}
