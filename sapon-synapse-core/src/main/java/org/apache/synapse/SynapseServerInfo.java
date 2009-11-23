package org.apache.synapse;

public interface SynapseServerInfo
{
	String getAxis2RepoLocation();
    String getAxis2XmlLocation();
    String getSynapseHome();
    String getSynapseXMLLocation();
    String getResolveRoot();
    String getServerName();
    String getServerControllerProvider();
    boolean isCreateNewInstance();
    String getHostName();
    String getDeploymentMode();
    String getConfigurationProperty(String key);
    String getIpAddress();
}
