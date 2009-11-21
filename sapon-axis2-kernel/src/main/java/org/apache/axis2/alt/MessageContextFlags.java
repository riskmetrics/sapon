package org.apache.axis2.alt;

public interface MessageContextFlags {

	boolean isDoingMTOM();
	void setDoingMTOM(boolean b);

	boolean isDoingREST();
	void setDoingREST(boolean b);

	boolean isDoingSwA();
	void setDoingSwA(boolean b);

	boolean isNewThreadRequired();
	void setNewThreadRequired(boolean b);

	boolean isOutputWritten();
	void setOutputWritten(boolean b);

	boolean isResponseWritten();
	void setResponseWritten(boolean b);

	boolean isProcessingFault();
	void setProcessingFault(boolean b);

	boolean isSOAP11();
	void setSOAP11(boolean t);

	boolean isServerSide();
	void setServerSide(boolean b);

	boolean isFault();
	void setFault(boolean b);
}
