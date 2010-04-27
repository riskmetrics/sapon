package org.apache.axiom.attachments;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.mail.MessagingException;

import org.apache.axiom.attachments.lifecycle.LifecycleManager;

public interface Attachments {
	
	LifecycleManager getLifecycleManager();
	void setLifecycleManager(LifecycleManager manager);
	
	String getAttachmentSpecType();
	
	InputStream getSOAPPartInputStream();
	String getSOAPPartContentID();
	String getSOAPPartContentType();
	
	IncomingAttachmentStreams getIncomingAttachmentStreams();
	
	void consumeNext(Map<String, ? extends AttachmentHandler> handlers) 
		throws IOException, MessagingException;
	void consumeRest(Map<String, ? extends AttachmentHandler> handlers) 
		throws IOException, MessagingException;
	
	long getContentLength() throws IOException;
	
	InputStream getIncomingAttachmentsAsSingleStream();

	DataHandler getDataHandler(String blobContentID);
	void addDataHandler(String contentID, DataHandler dataHandler);
	void removeDataHandler(String blobContentID);
	Map<String, DataHandler> getMap();
	
	String[] getAllContentIDs();
	Set<String> getContentIDSet();
	List<String> getContentIDList();
}
