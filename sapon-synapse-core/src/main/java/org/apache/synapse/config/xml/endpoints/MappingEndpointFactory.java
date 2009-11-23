package org.apache.synapse.config.xml.endpoints;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.MappingEndpoint;

/**
 * Creates {@link MappingEndpoint} using a XML configuration.
 * <p/>
 * Configuration syntax:
 * <pre>
 * &lt;endpoint [name="<em>name</em>"]&gt;
 *   &lt;mapping sender="<em>sender</em>" [format="soap11|soap12|pox|get"]
 *            [optimize="mtom|swa"]
 *            [encoding="<em>charset encoding</em>"]
 *            [statistics="enable|disable"] [trace="enable|disable"]&gt;
 *     .. extensibility ..
 *	   
 *	   &lt;map from="from uri" to="to uri"/&gt;+
 *
 *     &lt;enableRM [policy="<em>key</em>"]/&gt;?
 *     &lt;enableSec [policy="<em>key</em>"]/&gt;?
 *     &lt;enableAddressing [version="final|submission"] [separateListener="true|false"]/&gt;?
 *
 *     &lt;timeout&gt;
 *       &lt;duration&gt;<em>timeout duration in seconds</em>&lt;/duration&gt;
 *       &lt;action&gt;discard|fault&lt;/action&gt;
 *     &lt;/timeout&gt;?
 *
 *     &lt;suspendDurationOnFailure&gt;
 *       <em>suspend duration in seconds</em>
 *     &lt;/suspendDurationOnFailure&gt;?
 *   &lt;/address&gt;
 * &lt;/endpoint&gt;
 * </pre>
 */
public class MappingEndpointFactory extends DefaultEndpointFactory {
	
	private static MappingEndpointFactory instance = new MappingEndpointFactory();
	
	private MappingEndpointFactory() {
	}
	
	public static MappingEndpointFactory getInstance() {
		return instance;
	}
	
	@Override
	protected Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint)
	{
		MappingEndpoint mappingEndpoint = new MappingEndpoint();
		
        OMAttribute name = epConfig.getAttribute(
                new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));

        if (name != null) {
            mappingEndpoint.setName(name.getAttributeValue());
        }
        
        OMElement mappingElement = epConfig.getFirstChildWithName(
        		new QName(SynapseConstants.SYNAPSE_NAMESPACE, "mapping"));
        if(mappingElement != null) {
        	EndpointDefinition definition = createEndpointDefinition(mappingElement);
            mappingEndpoint.setDefinition(definition);
            processAuditStatus(definition, mappingEndpoint.getName(), epConfig);	
        }
        
        mappingEndpoint.extractMappings(mappingElement);
        return mappingEndpoint;
	}

    @Override
    public EndpointDefinition createEndpointDefinition(OMElement elem) {

        OMAttribute sender = elem.getAttribute(new QName("sender"));
        EndpointDefinition endpointDefinition = new EndpointDefinition();

        if (sender != null) {
        	//we use setAddress because it's what's there already, and
        	//it still works, meaning we still get resolved to the right
        	//sender assuming our axis2.xml is configured correctly.
        	endpointDefinition.setAddress(sender.getAttributeValue());
        }

        extractCommonEndpointProperties(endpointDefinition, elem);
        extractSpecificEndpointProperties(endpointDefinition, elem);
        return endpointDefinition;
    }



}
