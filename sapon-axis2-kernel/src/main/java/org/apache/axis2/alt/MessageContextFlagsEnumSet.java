package org.apache.axis2.alt;

import java.util.EnumSet;

public class MessageContextFlagsEnumSet implements MessageContextFlags {
	private static enum Flag {
		DOING_MTOM,
		DOING_REST,
		DOING_SWA,
		FAULT,
		NEW_THREAD,
		OUTPUT_WRITTEN,
		PROCESSING_FAULT,
		RESPONSE_WRITTEN,
		SERVER_SIDE,
		SOAP_11
	}

	private final EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);

	private final boolean is(Flag f) {
		return flags.contains(f);
	}

	private final void set(Flag f, boolean b) {
		if(b){ flags.add(f); }
		else{ flags.remove(f); }
	}

	@Override
	public boolean isDoingMTOM() { return is(Flag.DOING_MTOM); }

	@Override
	public void setDoingMTOM(boolean b) { set(Flag.DOING_MTOM, b); }

	@Override
	public boolean isDoingREST() { return is(Flag.DOING_REST); }

	@Override
	public void setDoingREST(boolean b) { set(Flag.DOING_REST, b); }

	@Override
	public boolean isDoingSwA() { return is(Flag.DOING_SWA); }

	@Override
	public void setDoingSwA(boolean b) { set(Flag.DOING_SWA, b); }

	@Override
	public boolean isFault() { return is(Flag.FAULT); }

	@Override
	public void setFault(boolean b) { set(Flag.FAULT, b); }

	@Override
	public boolean isNewThreadRequired() { return is(Flag.NEW_THREAD); }

	@Override
	public void setNewThreadRequired(boolean b) { set(Flag.NEW_THREAD, b); }

	@Override
	public boolean isOutputWritten() { return is(Flag.OUTPUT_WRITTEN); }

	@Override
	public void setOutputWritten(boolean b) { set(Flag.OUTPUT_WRITTEN, b); }

	@Override
	public boolean isProcessingFault() { return is(Flag.PROCESSING_FAULT); }

	@Override
	public void setProcessingFault(boolean b) { set(Flag.PROCESSING_FAULT, b); }

	@Override
	public boolean isResponseWritten() { return is(Flag.RESPONSE_WRITTEN); }

	@Override
	public void setResponseWritten(boolean b) { set(Flag.RESPONSE_WRITTEN, b); }

	@Override
	public boolean isSOAP11() { return is(Flag.SOAP_11); }

	@Override
	public void setSOAP11(boolean b) { set(Flag.SOAP_11, b); }

	@Override
	public boolean isServerSide() { return is(Flag.SERVER_SIDE); }

	@Override
	public void setServerSide(boolean b) { set(Flag.SERVER_SIDE, b); }
}
