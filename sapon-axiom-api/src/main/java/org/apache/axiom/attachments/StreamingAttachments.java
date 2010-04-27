package org.apache.axiom.attachments;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.apache.axiom.attachments.impl.PartFactory;
import org.apache.axiom.attachments.impl.StreamPart;
import org.apache.axiom.attachments.lifecycle.LifecycleManager;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.util.UUIDGenerator;

public class StreamingAttachments implements Attachments {
	
	private static final int PUSHBACK_SIZE = 4 * 1024;
	
	private LifecycleManager manager;
	private int contentLength;
	private ContentType contentType;
	private byte[] boundary;
	private PushbackInputStream pushbackInStream;
	
	private String soapPartId;
	private DataHandler soapPartDataHandler;
	
	public StreamingAttachments() 
	{}

	public StreamingAttachments(	LifecycleManager manager, 
									InputStream inStream, 
									String contentTypeString, 
									boolean fileCacheEnable,
									String attachmentRepoDir, 
									String fileThreshold, 
									int contentLength ) 
    	throws OMException 
    {
		this.manager = manager;
        this.contentLength = contentLength;

        try {
            contentType = new ContentType(contentTypeString);
        } catch (ParseException e) {
            throw new OMException(
                    "Invalid Content Type Field in the Mime Message"
                    , e);
        }

        // Boundary always have the prefix "--".
        try {
            String encoding = contentType.getParameter("charset");
            if(encoding == null || encoding.length()==0){
                encoding = "UTF-8";
            }
            String boundaryParam = contentType.getParameter("boundary");
            if (boundaryParam == null) {
                throw new OMException("Content-type has no 'boundary' parameter");
            }
            this.boundary = ("--" + boundaryParam).getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new OMException(e);
        }

        pushbackInStream = new PushbackInputStream(inStream, PUSHBACK_SIZE);

        // Move the read pointer to the beginning of the first part
        // read till the end of first boundary
        while (true) {
            int value;
            try {
                value = pushbackInStream.read();
                if ((byte) value == boundary[0]) {
                    int boundaryIndex = 0;
                    while ((boundaryIndex < boundary.length)
                            && ((byte) value == boundary[boundaryIndex])) {
                        value = pushbackInStream.read();
                        if (value == -1) {
                            throw new OMException(
                                    "Unexpected End of Stream while searching for first Mime Boundary");
                        }
                        boundaryIndex++;
                    }
                    if (boundaryIndex == boundary.length) { // boundary found
                        pushbackInStream.read();
                        break;
                    }
                } else if ((byte) value == -1) {
                    throw new OMException(
                            "Mime parts not found. Stream ended while searching for the boundary");
                }
            } catch (IOException e1) {
                throw new OMException("Stream Error" + e1.toString(), e1);
            }
        }

        Part soapPart = getSoapPart();
        try {
        	long size = soapPart.getSize();

            soapPartId = soapPart.getContentID();
            if (soapPartId == null) {
            	soapPartId = "soapPart_" + UUIDGenerator.getUUID();
            }

            if (size > 0) {
            	soapPartDataHandler = soapPart.getDataHandler();
            } else {
            	// Either the mime part is empty or the stream ended 
            	// without having a MIME message terminator
            	soapPartDataHandler 
            		= new DataHandler(new ByteArrayDataSource(new byte[]{}));
            }
        } catch (MessagingException e) {
        	throw new OMException("Error reading Soap Part." + e);
		}
    }

	@Override
	public void addDataHandler(String contentID, DataHandler dataHandler) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] getAllContentIDs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAttachmentSpecType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> getContentIDList() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> getContentIDSet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getContentLength() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public DataHandler getDataHandler(String blobContentID) {
		if(soapPartId.equals(blobContentID)) {
			return soapPartDataHandler;
		}
		throw new IllegalArgumentException();
	}

	@Override
	public IncomingAttachmentStreams getIncomingAttachmentStreams() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream getIncomingAttachmentsAsSingleStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LifecycleManager getLifecycleManager() {
		return manager;
	}

	@Override
	public Map<String, DataHandler> getMap() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSOAPPartContentID() {
		String out = soapPartId;
        if (out.length() > 4
                && "cid:".equalsIgnoreCase(out.substring(0, 4))) {
            out = out.substring(4);
        }
        return out;
	}

	@Override
	public String getSOAPPartContentType() {
		return soapPartDataHandler.getContentType();
	}

	@Override
	public InputStream getSOAPPartInputStream() {
		try {
			return soapPartDataHandler.getInputStream();
		} catch(IOException e) {
			throw new OMException("Problem with DataHandler of the SOAP Mime Part. ", e);
		}
	}
	
	@Override
	public void removeDataHandler(String blobContentID) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLifecycleManager(LifecycleManager manager) {
		throw new UnsupportedOperationException();
	}
	
    /**
     * @return This will return the next available MIME part in the stream.
     * @throws OMException if Stream ends while reading the next part...
     */
    private Part getSoapPart() throws OMException {

        // Create a MIMEBodyPartInputStream that simulates a single stream for this MIME body part
        MIMEBodyPartInputStream partStream =
            new MIMEBodyPartInputStream(pushbackInStream,
                                        boundary,
                                        PUSHBACK_SIZE);

        // The PartFactory will determine which Part implementation is most appropriate.
        Part part = PartFactory.createPart(	getLifecycleManager(), 
        									partStream,
        									true,
        									0,
        									null,//attachmentRepoDir,
        									contentLength);  // content-length for the whole message
        return part;
    }

	@Override
	public void consumeNext(Map<String, ? extends AttachmentHandler> handlers)
		throws IOException, MessagingException
	{
		Map<String, Header> headers = new HashMap<String, Header>();
		readHeaders(pushbackInStream, headers);
		Part part = new StreamPart(headers);
		
		MIMEBodyPartInputStream nextStream
			= new MIMEBodyPartInputStream(	pushbackInStream, 
											boundary, 
											PUSHBACK_SIZE );
		
		String cid = part.getContentID();
		AttachmentHandler handler = handlers.get(cid);
		if(handler == null) {
			handler = handlers.get(AttachmentHandler.DEFAULT_KEY);
		}
		
		if(handler != null) {
			handler.handleAttachment(cid, nextStream);
		} 
		
		// skip must be implemented properly on MIMEBodyPartInputStream, so 
		// that we'll only consume and disregard the remainder of the MIME part 
		// and not the entire underlying stream.
		nextStream.skip(Long.MAX_VALUE);
	}

	@Override
	public void consumeRest(Map<String, ? extends AttachmentHandler> handlers)
		throws IOException, MessagingException
	{
		int sentinel = pushbackInStream.read();
		while(sentinel != -1) {
			pushbackInStream.unread(sentinel);
			consumeNext(handlers);
			sentinel = pushbackInStream.read();
		}
	} 
	
    /**
     * The implementing class must call initHeaders prior to using
     * any of the Part methods.
     * @param is
     * @param headers
     */
    private static void readHeaders(	PushbackInputStream in, 
    									Map<String, Header> headers ) 
    	throws IOException 
    {
        boolean done = false;

        final int BUF_SIZE = 1024;
        byte[] headerBytes = new byte[BUF_SIZE];

        int size = in.read(headerBytes);
        int index = 0;
        StringBuilder sb = new StringBuilder(50);

        while (!done && index < size) {

            // Get the next byte
            int ch = headerBytes[index++];
            if (index == size) {
                size = in.read(headerBytes);
                index =0;
            }

            if (ch == 13) {

                // Get the next byte
                ch = headerBytes[index++];
                if (index == size) {
                    size = in.read(headerBytes);
                    index =0;
                }

                if (ch == 10) {
                    // 13, 10 indicates we are starting a new line...thus a new header
                    // Get the next byte
                    ch = headerBytes[index++];
                    if (index == size) {
                        size = in.read(headerBytes);
                        index =0;
                    }

                    if (ch == 13) {

                        // Get the next byte
                        ch = headerBytes[index++];
                        if (index == size) {
                            size = in.read(headerBytes);
                            index =0;
                        }

                        if (ch == 10) {
                            // Blank line indicates we are done.
                            readHeader(sb, headers);
                            sb.delete(0, sb.length()); // Clear the buffer for reuse
                            done = true;
                        }
                    } else {

                        // Semicolon is a continuation character
                        String check = sb.toString().trim();
                        if (!check.endsWith(";")) {
                            // now parse and add the header String
                            readHeader(sb, headers);
                            sb.delete(0, sb.length()); // Clear the buffer for reuse
                        }
                        sb.append((char) ch);
                    }
                } else {
                    sb.append(13);
                    sb.append((char) ch);
                }
            } else {
                sb.append((char) ch);
            }
        }
        
        if (index < size) {  
        	in.unread(headerBytes, index, size-index);
        }        
    }
    
    private static void readHeader(	StringBuilder header, 
    								Map<String, Header> headers	) 
    {
    	int delimiter = header.indexOf(":");
    	String name = header.substring(0, delimiter).trim();
    	String value = header.substring(delimiter + 1, header.length()).trim();

    	Header headerObj = new Header(name, value);

    	// Use the lower case name as the key
    	String key = name.toLowerCase();
    	headers.put(key, headerObj);
    }
}
