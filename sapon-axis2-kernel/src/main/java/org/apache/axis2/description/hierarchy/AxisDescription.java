package org.apache.axis2.description.hierarchy;

import java.util.Collection;

import org.apache.axiom.om.OMNode;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.DescriptionConstants;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.description.PolicySubject;


public interface AxisDescription
	extends ParameterInclude, DescriptionConstants, ConfigurationDescendant,
	        PolicySubject
{
	String getDocumentation();
	OMNode getDocumentationNode();
	void setDocumentation(OMNode documentation);
	void setDocumentation(String documentation);

//	void setParent(AxisDescription parent);
//	AxisDescription getParent();
//
//	void addChild(AxisDescription child);
//	void addChild(Object key, AxisDescription child);
//	Iterable<AxisDescription> getChildren();
//	AxisDescription getChild(Object key);
//	void removeChild(Object key);

	void engageModule(AxisModule module) throws AxisFault;
	void engageModule(AxisModule module, AxisDescription source)
		throws AxisFault;
	Collection<AxisModule> getEngagedModules();
	boolean isEngaged(String moduleName);
	boolean isEngaged(AxisModule axisModule);
	void disengageModule(AxisModule module) throws AxisFault;
}
