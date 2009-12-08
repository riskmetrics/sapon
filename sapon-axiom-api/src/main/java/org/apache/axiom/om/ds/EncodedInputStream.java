package org.apache.axiom.om.ds;

import java.io.InputStream;

public class EncodedInputStream {
	private final InputStream is;
	private final String encoding;

	public EncodedInputStream(final InputStream is, final String encoding) {
		this.is = is;
		this.encoding = encoding;
	}

	public InputStream getInputStream() {
		return is;
	}

	public String getEncoding() {
		return encoding;
	}
}
