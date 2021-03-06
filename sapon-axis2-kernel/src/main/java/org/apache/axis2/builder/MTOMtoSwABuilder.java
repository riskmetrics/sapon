package org.apache.axis2.builder;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.MTOMConstants;
import org.apache.axiom.om.impl.builder.StAXBuilder;
import org.apache.axiom.om.util.Base64;
import org.apache.axiom.om.util.DetachableInputStream;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axis2.Axis2Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MTOMtoSwABuilder implements Builder, MTOMConstants {

	private static Log log = LogFactory.getLog(MTOMtoSwABuilder.class);
	private static final QName XOP_QNAME = new QName(XOP_NAMESPACE_URI, XOP_INCLUDE);
	private static final QName XOP_HREF_ATTR = new QName("href");

	private Set<QName> qnames = new HashSet<QName>();

	@Override
	public OMElement getDocumentElement(	InputStream inputStream,
											String contentType,
											MessageContext messageContext)
		throws AxisFault
	{
        XMLStreamReader streamReader;
        Attachments attachments = messageContext.getAttachments();
        String charSetEncoding
        	= (String) messageContext.getProperty(Axis2Constants.Configuration.CHARACTER_SET_ENCODING);

        try {

            // Apply a detachable inputstream.  This can be used later
            // to (a) get the length of the incoming message or (b)
            // free transport resources.
            DetachableInputStream is = new DetachableInputStream(inputStream);
            messageContext.setProperty(Axis2Constants.DETACHABLE_INPUT_STREAM, is);

            // Get the actual encoding by looking at the BOM of the InputStream
            PushbackInputStream pis = BuilderUtil.getPushbackInputStream(is);
            String actualCharSetEncoding = BuilderUtil.getCharSetEncoding(pis, charSetEncoding);

            // Get the XMLStreamReader for this input stream
            streamReader = StAXUtils.createXMLStreamReader(pis, actualCharSetEncoding);

            StAXBuilder builder = new StAXSOAPModelBuilder(streamReader);
            SOAPEnvelope envelope = (SOAPEnvelope) builder.getDocumentElement();


//            // Get the actual encoding by looking at the BOM of the InputStream
//            PushbackInputStream pis = BuilderUtil.getPushbackInputStream(inputStream);
//            String actualCharSetEncoding = BuilderUtil.getCharSetEncoding(pis, charSetEncoding);
//
//            // Get the XMLStreamReader for this input stream
//            streamReader = StAXUtils.createXMLStreamReader(pis, actualCharSetEncoding);
//            StAXBuilder builder
//            	= new MTOMStAXSOAPModelBuilder(streamReader, attachments);
//            SOAPEnvelope envelope
//            	= (SOAPEnvelope) builder.getDocumentElement();

            ensureSwA(envelope, attachments);

            //this is a lie.  but a useful one.
            messageContext.setDoingSwA(false);
            messageContext.setDoingMTOM(true);

            BuilderUtil
                    .validateSOAPVersion(BuilderUtil.getEnvelopeNamespace(contentType), envelope);
            BuilderUtil.validateCharSetEncoding(charSetEncoding,
            									builder.getDocument().getCharsetEncoding(),
            									envelope.getNamespace().getNamespaceURI());
            return envelope;
        } catch (IOException e) {
            throw AxisFault.makeFault(e);
        } catch (XMLStreamException e) {
            throw AxisFault.makeFault(e);
        }
	}

	public void addAll(Collection<QName> names)
	{
		qnames.addAll(names);
	}

	private void ensureSwA(SOAPEnvelope envelope, Attachments attachments)
		throws AxisFault
	{
		SOAPBody body = envelope.getBody();
		walk(body, attachments);
	}

	private void walk(OMElement e, Attachments attachments)
		throws AxisFault
	{
		if(qnames.contains(e.getQName())) {
			OMElement child = e.getFirstElement();
			if(child == null) {
				byte[] decoded = Base64.decode(e.getText());
				DataHandler dh
					= new DataHandler(
							new ByteArrayDataSource(decoded, "text/xml"));
				String uuid = UUID.randomUUID().toString();
				attachments.addDataHandler(uuid, dh);
				e.setText("cid:" + uuid);
			} else if(XOP_QNAME.equals(child.getQName())) {
				String attach = desuckify(child.getAttributeValue(XOP_HREF_ATTR));
				if(!attachments.getContentIDSet().contains(attach)) {
					log.warn("Unkwown Content Id in message: " + attach);
				}
				e.setText(CID + attach);
				child.detach();
			}
		}
		for(OMElement child: e.getChildElements()) {
			walk(child, attachments);
		}
	}

	private static final String CID = "cid:";
	private String desuckify(String contentId) {
		if(contentId.startsWith(CID)) {
			contentId = contentId.substring(CID.length());
		}

		try {
			return URLDecoder.decode(contentId, "utf-8");
		} catch(UnsupportedEncodingException e) {
			log.error("Encoding not supported", e);
			return contentId;
		}
	}
}
