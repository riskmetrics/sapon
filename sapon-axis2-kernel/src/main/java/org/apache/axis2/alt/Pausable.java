package org.apache.axis2.alt;

public interface Pausable {
	void pause();
	boolean isPaused();
	void setPaused(boolean paused);
}
