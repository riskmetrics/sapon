package org.apache.synapse.config;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;

import javax.xml.namespace.QName;

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.Startup;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.registry.Registry;

public interface SynapseConfiguration extends ManagedLifecycle {
	
	void addSequence(String key, Mediator mediator);
	void addSequence(String key, Entry entry);  //Why is this called addSequence?
	Map<String, SequenceMediator> getDefinedSequences();
	Mediator getMandatorySequence();
	void setMandatorySequence(Mediator mandatorySequence);
	Mediator getSequence(String key);
	void removeSequence(String key);
	Mediator getMainSequence();
	Mediator getFaultSequence();
	
	void addEntry(String key, Entry entry);
	Map<String, Entry> getCachedEntries();
	Map<String, Entry> getDefinedEntries();
	Object getEntry(String key);  //Why is this Object and not Entry?
	Entry getEntryDefinition(String key);  //How is this different from getEntry()?
	void removeEntry(String key);
	void clearCachedEntry(String key);
	void clearCache(); //Which cache?  (Entry cache, it turns out)
	
	void addEndpoint(String key, Endpoint endpoint);
	Map<String, Endpoint> getDefinedEndpoints();
	Endpoint getEndpoint(String key);
	void removeEndpoint(String key);
	
	void addProxyService(String name, ProxyService proxy);
	ProxyService getProxyService(String name);
	void removeProxyService(String name);
	Collection<ProxyService> getProxyServices();

	Map<String, Object> getLocalRegistry();
	Registry getRegistry();
	void setRegistry(Registry registry);
	
	void setAxisConfiguration(AxisConfiguration axisConfig);
	AxisConfiguration getAxisConfiguration();
	
	String getPathToConfigFile();
	void setPathToConfigFile(String pathToConfigFile);
	
	void setDefaultQName(QName defaultQName);
	QName getDefaultQName();
	
	Timer getSynapseTimer();
	
	Collection<Startup> getStartups();
	Startup getStartup(String id);
	void addStartup(Startup startup);
	void removeStartup(String name);
	
	Properties getProperties();
	void setProperties(Properties properties);
	String getProperty(String propKey, String def);
	long getProperty(String propKey, long def);
	String getProperty(String propKey);
	
}
