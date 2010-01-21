/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.axis2.Axis2Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Encapsulates all server information
 */
public class SynapseServerInfoImpl implements SynapseServerInfo {

    private static final Log log = LogFactory.getLog(SynapseServerInfoImpl.class);

    /* The properties map */
    private final Map<String, String> properties = new HashMap<String, String>();
    /*The Axis2 repository location */
    private String axis2RepoLocation;
    /* The path to the axis2.xml file  */
    private String axis2XmlLocation;
    /* The synapse home is the home directory of the Synapse installation   */
    private String synapseHome;
    /* The path to the synapse.xml file */
    private String synapseXMLLocation;
    /* The root directory to resolve paths for registry, default to synapse.home/repository  */
    private String resolveRoot;
    /* An optional server name to activate pinned services, tasks etc.. and to differentiate instances on a cluster */
    private String serverName = "localhost";
    /* Server controller provider */
    private String serverControllerProvider = DEFAULT_SERVER_CONTROLLER_PROVIDER;
    /* The default synapse server controller*/
    private static final String DEFAULT_SERVER_CONTROLLER_PROVIDER = "org.apache.synapse.Axis2SynapseController";
    /* whether it is needed to create a new server instance*/
    private boolean createNewInstance = true;
    /* Server host name */
    private String hostName;
    /* Server IP address*/
    private String ipAddress;
    /* Deployment mode*/
    private String deploymentMode;

    public SynapseServerInfoImpl() {
        initServerHostAndIP();
    }

    public void setAxis2RepoLocation(String axis2RepoLocation) {
        assertNotNullOrEmpty(axis2RepoLocation, Axis2Constants.AXIS2_REPO);
        if (!new File(axis2RepoLocation).isAbsolute() && synapseHome != null) {
            this.axis2RepoLocation = synapseHome + File.separator + axis2RepoLocation;
        } else {
            this.axis2RepoLocation = axis2RepoLocation;
        }
    }

    public void setAxis2XmlLocation(String axis2Xml) {
        assertNotNullOrEmpty(axis2Xml, Axis2Constants.AXIS2_CONF);
        if (!new File(axis2Xml).isAbsolute() && synapseHome != null) {
            this.axis2XmlLocation = synapseHome + File.separator + axis2Xml;
        } else {
            this.axis2XmlLocation = axis2Xml;
        }
    }

    public void setSynapseHome(String synapseHome) {
        assertNotNullOrEmpty(synapseHome, SynapseConstants.SYNAPSE_HOME);
        this.synapseHome = synapseHome;
    }

    public void setResolveRoot(String resolveRoot) {
        if (resolveRoot == null || "".equals(resolveRoot)) {
            return;
        }
        if (!new File(resolveRoot).isAbsolute() && synapseHome != null) {
            this.resolveRoot = synapseHome + File.separator + resolveRoot;
        } else {
            this.resolveRoot = resolveRoot;
        }
    }

    public void setSynapseXMLLocation(String synapseXMLLocation) {
        assertNotNullOrEmpty(synapseXMLLocation, SynapseConstants.SYNAPSE_XML);
        if (!new File(synapseXMLLocation).isAbsolute() && synapseHome != null) {
            this.synapseXMLLocation = synapseHome + File.separator + synapseXMLLocation;
        } else {
            this.synapseXMLLocation = synapseXMLLocation;
        }
    }

    public String getAxis2RepoLocation() {
        return axis2RepoLocation;
    }


    public String getAxis2XmlLocation() {
        return axis2XmlLocation;
    }


    public String getSynapseHome() {
        return synapseHome;
    }


    public String getSynapseXMLLocation() {
        return synapseXMLLocation;
    }


    public String getResolveRoot() {
        return resolveRoot;
    }


    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerControllerProvider() {
        return serverControllerProvider;
    }

    public void setServerControllerProvider(String serverControllerProvider) {
        this.serverControllerProvider = serverControllerProvider;
    }

    public boolean isCreateNewInstance() {
        return createNewInstance;
    }

    public void setCreateNewInstance(boolean createNewInstance) {
        this.createNewInstance = createNewInstance;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeploymentMode() {
        return deploymentMode;
    }

    public void setDeploymentMode(String deploymentMode) {
        this.deploymentMode = deploymentMode;
    }

    public void addConfigurationProperty(String key, String value) {
        assertNotNullOrEmpty(key);
        assertNotNullOrEmpty(value, key);
        properties.put(key.trim(), value.trim());
    }

    public String getConfigurationProperty(String key) {
        assertNotNullOrEmpty(key);
        return properties.get(key.trim());
    }

    private void assertNotNullOrEmpty(String value, String paramter) {
        if (value == null || "".equals(value)) {
            handleFatal("The parameter - " + paramter + "  must be provided.");
        }
    }

    private void assertNotNullOrEmpty(String key) {
        if (key == null || "".equals(key)) {
            handleFatal("A configuration parameter is null or empty.");
        }
    }

    private void handleFatal(String msg) {
        log.fatal(msg);
        throw new SynapseException(msg);
    }

    private void initServerHostAndIP() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            if (addr != null) {
                ipAddress = addr.getHostAddress();
                hostName = addr.getHostName();
                if (hostName == null) {
                    hostName = ipAddress;
                }
            }
        } catch (UnknownHostException e) {
            log.warn("Unable to get the hostName or IP address of the server", e);
        }
    }

    @Override
	public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[ Server Name : ").append(serverName).append(" ]");
        sb.append("[ Synapse Home : ").append(synapseHome).append(" ]");
        sb.append("[ Synapse XML : ").append(synapseXMLLocation).append(" ]");
        sb.append("[ Server Host : ").append(hostName).append(" ]");
        sb.append("[ Server IP : ").append(ipAddress).append(" ]");
        sb.append("[ Axis2 Repository : ").append(axis2RepoLocation).append(" ]");
        sb.append("[ Axis2 XML : ").append(axis2XmlLocation).append(" ]");
        return sb.toString();
    }
}
