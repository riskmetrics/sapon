package org.apache.synapse.endpoints;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.SynapseMessageContext;

public class MappingEndpoint extends AbstractEndpoint {

	private static final QName FROM_QNAME = new QName("from");
	private static final QName TO_QNAME = new QName("to");

	private Map<String, String> mappings = new HashMap<String, String>();

	public void extractMappings(final OMElement elem )
    {
    	for(OMElement el: elem.getChildrenWithLocalName("map")) {
    		final String from = el.getAttributeValue(FROM_QNAME);
    		final String to = el.getAttributeValue(TO_QNAME);
    		mappings.put(from, to);
    	}
    }

	@Override
	public void send(SynapseMessageContext synCtx)
	{
		final String clientSoapAction = synCtx.getSoapAction();
		final String mappedSoapAction = mappings.get(clientSoapAction);
		if(mappedSoapAction == null) {
			//we obviously can't deliver it, so blow up.
			throw new RuntimeException(
					"No known mapping for the given client SOAPAction: " +
					clientSoapAction
					);
		}
		final EndpointDefinition def = getDefinition();
		def.setAddress("bluebox:" + mappedSoapAction);
		super.send(synCtx);
	}

}
