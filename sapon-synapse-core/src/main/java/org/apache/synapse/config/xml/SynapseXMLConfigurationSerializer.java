package org.apache.synapse.config.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.endpoints.EndpointSerializer;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.Endpoint;

public class SynapseXMLConfigurationSerializer implements ConfigurationSerializer {

    private static final Log log = LogFactory
            .getLog(XMLConfigurationSerializer.class);

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    private static final OMNamespace synNS = fac.createOMNamespace(
            XMLConfigConstants.SYNAPSE_NAMESPACE, "syn");

    /**
     * Order of entries is irrelevant, however its nice to have some order.
     *
     * @param synCfg
     * @throws XMLStreamException
     */

    public OMElement serializeConfiguration(SynapseConfiguration synCfg) {

        OMElement definitions = fac.createOMElement("definitions", synNS);

        // first process a remote registry if present
        if (synCfg.getRegistry() != null) {
            RegistrySerializer.serializeRegistry(definitions, synCfg
                    .getRegistry());
        }

        // add proxy services
        for(ProxyService service: synCfg.getProxyServices()) {
            ProxyServiceSerializer.serializeProxy(definitions, service);
        }

//        // Add Event sources
//        for (SynapseEventSource eventSource : synCfg.getEventSources()) {
//            EventSourceSerializer.serializeEventSource(definitions, eventSource);
//        }

        Map<String, Entry> entries = new HashMap<String, Entry>();
        Map<String, Endpoint> endpoints = new HashMap<String, Endpoint>();
        Map<String, Mediator> sequences = new HashMap<String, Mediator>();

        for(Map.Entry<String, Object> e: synCfg.getLocalRegistry().entrySet()) {
            String key = e.getKey();
            if (SynapseConstants.SERVER_IP.equals(key) || SynapseConstants.SERVER_HOST.equals(key)) {
                continue;
            }
            Object o = e.getValue();
            if (o instanceof Mediator) {
                sequences.put(key, (Mediator)o);
            } else if (o instanceof Endpoint) {
                endpoints.put(key.toString(), (Endpoint) o);
            } else if (o instanceof Entry) {
                entries.put(key, (Entry)o);
            } else {
                handleException("Unknown object : " + o.getClass()
                        + " for serialization into Synapse configuration");
            }
        }

        // process entries
        serializeEntries(definitions, entries);

        // process endpoints
        serializeEndpoints(definitions, endpoints);

        // process sequences
        serializeSequences(definitions, sequences);

        // handle startups
        serializeStartups(definitions, synCfg.getStartups());

        return definitions;
    }

    private static void serializeEntries(OMElement definitions, Map<String, Entry> entries) {
        for (Entry e: entries.values()) {
        	EntrySerializer.serializeEntry(e, definitions);
        }
    }

    private static void serializeStartups(OMElement definitions, Collection<Startup> startups) {
        for(Startup s : startups) {
        	StartupFinder.getInstance().serializeStartup(definitions, s);
        }
    }

    private static void serializeEndpoints(OMElement definitions, Map<String, Endpoint> endpoints) {
        for (Endpoint endpoint: endpoints.values()) {
            definitions.addChild(EndpointSerializer.getElementFromEndpoint(endpoint));
        }
    }

    private static void serializeSequences(OMElement definitions, Map<String, Mediator> sequences) {
        for (Mediator m: sequences.values()) {
        	MediatorSerializerFinder.getInstance().getSerializer(m)
        		.serializeMediator(definitions, m);
        }
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public QName getTagQName() {
        return XMLConfigConstants.DEFINITIONS_ELT;
	}

}
