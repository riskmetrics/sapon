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
import java.util.concurrent.CountDownLatch;

import org.apache.axis2.Axis2Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the class invoked by the command line scripts synapse.sh and
 * synapse-daemon.sh to start an instance of Synapse. This class calls on the
 * ServerManager to start up the instance
 *
 * TODO Switch to using commons-cli and move all command line parameter
 * processing etc from the .sh and .bat into this.. for 1.3 release :)
 */
public class SynapseShellStarter {

    private static final Log log = LogFactory.getLog(SynapseShellStarter.class);

	private static final String USAGE_TXT = "Usage: SynapseServer " +
		"<axis2_repository> <axis2_xml> <synapse_home> <synapse_xml> " +
		"<resolve_root> <deployment mode>" +
		"\n Opts: -? this message";

    public static void printUsage() {
        System.out.println(USAGE_TXT);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {

        // first check if we should print usage
        if (args.length <= 0 || args.length == 2 || args.length == 3 || args.length >= 8) {
            printUsage();
        }

        log.info("Starting Apache Synapse...");

        // create the server configuration using the commandline arguments
        SynapseServerInfo synapseInfo = create(args);

        ServerManager serverManager = ServerManager.getInstance();
        serverManager.init(synapseInfo, null);

        try {
            serverManager.start();
            addShutdownHook();
            log.info("Apache Synapse started successfully");

			// Put the main thread into wait state. This makes sure that the
			// Synapse server doesn't stop immediately if ServerManager#start
            // doesn't create any non daemon threads (see also SYNAPSE-425).
            new CountDownLatch(1).await();

        } catch (SynapseException e) {
            log.error("Error starting Apache Synapse, trying a clean shutdown...", e);
            serverManager.shutdown();
        }
    }

    private static void addShutdownHook() {
        Thread shutdownHook = new Thread() {
            @Override
			public void run() {
                log.info("Shutting down Apache Synapse...");
                try {
                    ServerManager.getInstance().shutdown();
                    log.info("Apache Synapse shutdown complete");
                    log.info("Halting JVM");
                } catch (Exception e) {
                    log.error("Error occurred while shutting down Apache Synapse, " +
                            "it may not be a clean shutdown", e);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Creates a ServerConfigurationInformation based on command line arguments
     *
     * @param args Command line arguments
     * @return ServerConfigurationInformation instance
     */
    public static SynapseServerInfo create(String[] args)
    {
        SynapseServerInfoImpl information = new SynapseServerInfoImpl();
        information.setAxis2RepoLocation(args[0]);
        if (args.length == 1) {
            log.warn("Configuring server manager using deprecated " +
                    "system properties; please update your configuration");
            information.setAxis2XmlLocation(System.getProperty(Axis2Constants.AXIS2_CONF));
            information.setSynapseHome(System.getProperty(SynapseConstants.SYNAPSE_HOME));
            information.setSynapseXMLLocation(System.getProperty(SynapseConstants.SYNAPSE_XML));
            information.setResolveRoot(System.getProperty(SynapseConstants.RESOLVE_ROOT));
            information.setServerName(System.getProperty(SynapseConstants.SERVER_NAME));
            information.setDeploymentMode(System.getProperty(SynapseConstants.DEPLOYMENT_MODE));
        } else if (args.length == 4) {
            information.setAxis2XmlLocation(args[1]);
            information.setSynapseHome(args[2]);
            information.setSynapseXMLLocation(args[3]);
            information.setResolveRoot(args[2] + File.separator + "repository");
        } else if (args.length == 5) {
            information.setAxis2XmlLocation(args[1]);
            information.setSynapseHome(args[2]);
            information.setSynapseXMLLocation(args[3]);
            information.setResolveRoot(args[4]);
        } else if (args.length == 6) {
            information.setAxis2XmlLocation(args[1]);
            information.setSynapseHome(args[2]);
            information.setSynapseXMLLocation(args[3]);
            information.setResolveRoot(args[4]);
            information.setDeploymentMode(args[5]);
        } else if (args.length == 7) {
            information.setAxis2XmlLocation(args[1]);
            information.setSynapseHome(args[2]);
            information.setSynapseXMLLocation(args[3]);
            information.setResolveRoot(args[4]);
            information.setDeploymentMode(args[5]);
            information.setServerName(args[6]);
        }

        return information;
    }
}
