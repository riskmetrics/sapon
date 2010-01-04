package org.apache.axis2.description;

import java.util.Collection;
import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.axis2.alt.ModuleConfigAccessor;
import org.apache.axis2.description.hierarchy.AxisDescription;
import org.apache.axis2.description.hierarchy.ConfigurationDescendant;
import org.apache.axis2.engine.AxisConfiguration;

public interface AxisServiceGroup
	extends AxisDescription, ConfigurationDescendant, ModuleConfigAccessor
{
	void setAxisConfiguration(AxisConfiguration config);

	void addService(AxisService service) throws AxisFault;
	AxisService getService(String serviceName);
	Collection<AxisService> getServices();
	AxisService removeService(String serviceName);

	ClassLoader getServiceGroupClassLoader();
	void setServiceGroupClassLoader(ClassLoader classLoader);

	String getName();
	void setName(String name);

	//TODO: these belong somewhere else.
	List<String> getModuleRefs();
	void addModuleRef(String ref);

}
