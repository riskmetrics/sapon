package org.apache.axis2.alt;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.axis2.engine.Handler;

public interface ExecutionTracker {

	Flow getFlow();
	void setFlow(Flow flow);

	int getCurrentHandlerIndex();
	void setCurrentHandlerIndex(int currentHandlerIndex);
	int getCurrentPhaseIndex();
	void setCurrentPhaseIndex(int currentPhaseIndex);

	List<Handler> getExecutionChain();

    /**
     * Set the execution chain of Handler in this ExecutionTracker. Doing this
     * causes the current handler/phase indexes to reset to 0, since we have
     * new Handlers to execute (this usually only happens at initialization and
     * when a fault occurs).
     *
     * @param executionChain
     */
	void setExecutionChain(List<? extends Handler> executionChain);


	void addExecutedPhase(Handler phase);
	Handler removeFirstExecutedPhase();
	LinkedList<Handler> getExecutedPhases();
	void setExecutedPhasesExplicit(LinkedList<Handler> inb);
	void resetExecutedPhases();
	boolean isExecutedPhasesReset();

	List<Handler> flattenPhaseListToHandlers(List<Handler> list, Map<String, Handler> map);
	List<Handler> flattenHandlerList(List<Handler> list, Map<String, Handler> map);
}
