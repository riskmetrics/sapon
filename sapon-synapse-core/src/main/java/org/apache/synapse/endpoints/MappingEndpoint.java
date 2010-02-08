package org.apache.synapse.endpoints;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseMessageContext;

public class MappingEndpoint extends AbstractEndpoint {

	private static final QName FROM_QNAME = new QName("from");
	private static final QName TO_QNAME = new QName("to");

	private final Map<String, String> mappings
		= new HashMap<String, String>();
	private final Map<String, EndpointDefinition> endpointDefs
		= new HashMap<String, EndpointDefinition>();

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

		EndpointDefinition endpointDef = endpointDefs.get(mappedSoapAction);
		if(endpointDef == null) {
			endpointDef = prepEndpointDefinition(mappedSoapAction);
			endpointDefs.put(mappedSoapAction, endpointDef);
		}

		prepareForEndpointStatistics(synCtx);
        // register this as the immediate fault handler for this message.
        synCtx.pushFaultHandler(this);
        // add this as the last endpoint to process this message - used by statistics counting code
        synCtx.setProperty(SynapseConstants.LAST_ENDPOINT, this);
        // set message level metrics collector
        ((Axis2SynapseMessageContext) synCtx).getAxis2MessageContext().setProperty(
            BaseConstants.METRICS_COLLECTOR, metricsMBean);
        // Send the message through this endpoint
        synCtx.getEnvironment().send(endpointDef, synCtx);
	}

	private EndpointDefinition prepEndpointDefinition(String soapAction) {
		EndpointDefinition out = new EndpointDefinition(getDefinition());
		out.setAddress(soapAction);
		return out;
	}

}
