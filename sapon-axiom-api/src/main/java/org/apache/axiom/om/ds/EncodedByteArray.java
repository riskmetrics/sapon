package org.apache.axiom.om.ds;

import java.util.Arrays;

public class EncodedByteArray {

	private final byte[] bytes;
	private final String encoding;

	public EncodedByteArray(final byte[] bytes, final String encoding) {
		this.bytes = bytes;
		this.encoding = encoding;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public String getEncoding() {
		return encoding;
	}

	public EncodedByteArray copy() {
		return new EncodedByteArray(Arrays.copyOf(bytes, bytes.length),
									encoding);
	}
}
