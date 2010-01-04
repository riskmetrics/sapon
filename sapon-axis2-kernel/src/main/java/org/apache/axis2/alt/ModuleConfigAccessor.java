package org.apache.axis2.alt;

import org.apache.axis2.description.ModuleConfiguration;

//TODO:  this probably shouldn't be an interface on its own.
public interface ModuleConfigAccessor {
	ModuleConfiguration getModuleConfig(String moduleName);
	void addModuleConfig(ModuleConfiguration moduleConfiguration);
}
