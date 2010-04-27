package org.apache.axiom.attachments.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.Header;
import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StreamPart extends AbstractPart {
	
	private static Log log = LogFactory.getLog(StreamPart.class);
	
	public StreamPart(Map<String, Header> headers) {
		super(headers);
	}

	@Override
	public DataHandler getDataHandler() throws MessagingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getFileName() throws MessagingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getInputStream() throws IOException, MessagingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getSize() throws MessagingException {
		throw new UnsupportedOperationException();
	}

}
