package org.apache.axiom.attachments;

import java.io.InputStream;

public interface AttachmentHandler {
	
	public static final String DEFAULT_KEY 
		= "org.apache.axiom.attachments.AttachmentHandler.DEFAULT_KEY";
	
	void handleAttachment(String cid, InputStream contents);

}
