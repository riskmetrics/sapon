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

package org.apache.synapse.config;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.llom.util.AXIOMUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ServerContextInformation;
import org.apache.synapse.ServerManager;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseServerInfo;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.statistics.StatisticsCollector;
import org.apache.synapse.commons.security.definition.IdentityKeyStoreInformation;
import org.apache.synapse.commons.security.definition.KeyStoreInformation;
import org.apache.synapse.commons.security.definition.TrustKeyStoreInformation;
import org.apache.synapse.commons.security.definition.factory.KeyStoreInformationFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.DropMediator;
import org.apache.synapse.mediators.builtin.LogMediator;
import org.apache.synapse.util.SynapseBinaryDataSource;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;
import org.xml.sax.InputSource;

public class SynapseConfigUtils {

    private static final Log log = LogFactory.getLog(SynapseConfigUtils.class);

    /**
     * Return a StreamSource for the given Object
     *
     * @param o the object
     * @return the StreamSource
     */
    public static StreamSource getStreamSource(Object o) {

        if (o == null) {
            handleException("Cannot convert null to a StreamSource");

        } else if (o instanceof OMElement) {
            OMElement omElement = (OMElement) o;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                omElement.serialize(baos);
                return new StreamSource(new ByteArrayInputStream(baos.toByteArray()));
            } catch (XMLStreamException e) {
                handleException("Error converting to a StreamSource", e);
            }

        } else if (o instanceof OMText) {
            DataHandler dataHandler = (DataHandler) ((OMText) o).getDataHandler();
            if (dataHandler != null) {
                try {
                    return new StreamSource(dataHandler.getInputStream());
                } catch (IOException e) {
                    handleException("Error in reading content as a stream ");
                }
            }
        } else {

            handleException("Cannot convert object to a StreamSource");
        }
        return null;
    }

    public static InputStream getInputStream(Object o) {

        if (o == null) {
            handleException("Cannot convert null to a StreamSource");

        } else if (o instanceof OMElement) {
            OMElement omElement = (OMElement) o;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                omElement.serialize(baos);
                return new ByteArrayInputStream(baos.toByteArray());
            } catch (XMLStreamException e) {
                handleException("Error converting to a StreamSource", e);
            }

        } else if (o instanceof OMText) {
            DataHandler dataHandler = (DataHandler) ((OMText) o).getDataHandler();
            if (dataHandler != null) {
                try {
                    return dataHandler.getInputStream();
                } catch (IOException e) {
                    handleException("Error in reading content as a stream ");
                }
            }
        } else if (o instanceof URI) {
            try {
                return ((URI) (o)).toURL().openStream();
            } catch (IOException e) {
                handleException("Error opening stream form URI", e);
            }
        } else {
            handleException("Cannot convert object to a StreamSource");
        }
        return null;
    }

    /**
     * Get an object from a given URL. Will first fetch the content from the
     * URL and depending on the content-type, a suitable XMLToObjectMapper
     * (if available) would be used to transform this content into an Object.
     * If a suitable XMLToObjectMapper cannot be found, the content would be
     * treated as XML and an OMNode would be returned
     *
     * @param url the URL to the resource
     * @return an Object created from the given URL
     */
    public static Object getObject(URL url) {
        try {
            if (url != null && "file".equals(url.getProtocol())) {
                try {
                    url.openStream();
                } catch (IOException ignored) {
                    String path = url.getPath();
                    if (log.isDebugEnabled()) {
                        log.debug("Can not open a connection to the URL with a path :" +
                                path);
                    }
                    String synapseHome = getSynapseHome();
                    if (synapseHome != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Trying  to resolve an absolute path of the " +
                                    " URL using the synapse.home : " + synapseHome);
                        }
                        if (synapseHome.endsWith("/")) {
                            synapseHome = synapseHome.substring(0, synapseHome.lastIndexOf("/"));
                        }
                        url = new URL(url.getProtocol() + ":" + synapseHome + "/" + path);
                        try {
                            url.openStream();
                        } catch (IOException e) {
                            if (log.isDebugEnabled()) {
                                log.debug("Faild to resolve an absolute path of the " +
                                        " URL using the synapse.home : " + synapseHome);
                            }
                            log.warn("IO Error reading from URL " + url.getPath() + e);
                        }
                    }
                }
            }
            if (url == null) {
                return null;
            }
            URLConnection connection = getURLConnection(url);
            if (connection == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot create a URLConnection for given URL : " + url);
                }
                return null;
            }
            XMLToObjectMapper xmlToObject =
                    getXmlToObjectMapper(connection.getContentType());
            InputStream inputStream = connection.getInputStream();
            try {
                XMLStreamReader parser = XMLInputFactory.newInstance().
                        createXMLStreamReader(inputStream);
                StAXOMBuilder builder = new StAXOMBuilder(parser);
                OMElement omElem = builder.getDocumentElement();

                // detach from URL connection and keep in memory
                // TODO remove this
                omElem.build();

                if (xmlToObject != null) {
                    return xmlToObject.getObjectFromOMNode(omElem);
                } else {
                    return omElem;
                }

            } catch (XMLStreamException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Content at URL : " + url + " is non XML..");
                }
                return readNonXML(url);
            } catch (OMException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Content at URL : " + url + " is non XML..");
                }
                return readNonXML(url);
            } finally {
                inputStream.close();
            }

        } catch (IOException e) {
            handleException("Error connecting to URL : " + url, e);
        }
        return null;
    }

    /**
     * Helper method to handle non-XMl resources
     *
     * @param url The resource url
     * @return The content as an OMNode
     */
    public static OMNode readNonXML(URL url) {

        try {
            // Open a new connection
            URLConnection newConnection = getURLConnection(url);
            if (newConnection == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot create a URLConnection for given URL : " + url);
                }
                return null;
            }

            BufferedInputStream newInputStream = new BufferedInputStream(
                    newConnection.getInputStream());

            OMFactory omFactory = OMAbstractFactory.getOMFactory();
            return omFactory.createOMText(
                    new DataHandler(new SynapseBinaryDataSource(newInputStream,
                            newConnection.getContentType())), true);

        } catch (IOException e) {
            handleException("Error when getting a stream from resource's content", e);
        }
        return null;
    }

    /**
     * Return an OMElement from a URL source
     *
     * @param urlStr a URL string
     * @return an OMElement of the resource
     * @throws IOException for invalid URL's or IO errors
     */
    public static OMNode getOMElementFromURL(String urlStr) throws IOException {

        URL url = getURLFromPath(urlStr);
        if (url == null) {
            return null;
        }
        URLConnection connection = getURLConnection(url);
        if (connection == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot create a URLConnection for given URL : " + urlStr);
            }
            return null;
        }
        InputStream inStream = connection.getInputStream();
        try {
            StAXOMBuilder builder = new StAXOMBuilder(inStream);
            OMElement doc = builder.getDocumentElement();
            doc.build();
            return doc;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.info("Content at URL : " + url + " is non XML..");
            }
            return readNonXML(url);
        } finally {
            try {
                inStream.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static InputSource getInputSourceFormURI(URI uri) {

        if (uri == null) {
            if (log.isDebugEnabled()) {
                log.debug("Can not create a URL from 'null' ");
            }
            return null;
        }
        try {
            URL url = uri.toURL();
            String protocol = url.getProtocol();
            String path = url.getPath();
            if (protocol == null || "".equals(protocol)) {
                url = new URL("file:" + path);
            }
            URLConnection connection = getURLConnection(url);
            if (connection == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot create a URLConnection for given URL : " + uri);
                }
                return null;
            }
            BufferedInputStream urlInStream = new BufferedInputStream(connection.getInputStream());
            return new InputSource(urlInStream);
        } catch (MalformedURLException e) {
            handleException("Invalid URL ' " + uri + " '", e);
        } catch (IOException e) {
            handleException("IOError when getting a stream from given url : " + uri, e);
        }
        return null;
    }

    private static void handleException(String msg, Exception e) {
        log.warn(msg, e);
        throw new SynapseException(msg, e);
    }

    /**
     * Helper method to create a HttpSURLCOnnection with provided KeyStores
     *
     * @param url Https URL
     * @return
     */
    private static HttpsURLConnection getHttpsURLConnection(URL url) {

        if (log.isDebugEnabled()) {
            log.debug("Creating a HttpsURL Connection from given URL : " + url);
        }

        KeyManager[] keyManagers = null;
        TrustManager[] trustManagers = null;

        Properties synapseProperties = SynapsePropertiesLoader.loadSynapseProperties();

        IdentityKeyStoreInformation identityInformation =
                KeyStoreInformationFactory.createIdentityKeyStoreInformation(synapseProperties);

        if (identityInformation != null) {
            KeyManagerFactory keyManagerFactory =
                    identityInformation.getIdentityKeyManagerFactoryInstance();
            if (keyManagerFactory != null) {
                keyManagers = keyManagerFactory.getKeyManagers();
            }

        } else {
            if (log.isDebugEnabled()) {
                log.debug("There is no private key entry store configuration." +
                        " Will use JDK's default one");
            }
        }

        TrustKeyStoreInformation trustInformation =
                KeyStoreInformationFactory.createTrustKeyStoreInformation(synapseProperties);

        if (trustInformation != null) {
            TrustManagerFactory trustManagerFactory =
                    trustInformation.getTrustManagerFactoryInstance();
            if (trustManagerFactory != null) {
                trustManagers = trustManagerFactory.getTrustManagers();
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("There is no trusted certificate store configuration." +
                        " Will use JDK's default one");
            }
        }

        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            //Create a SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers,
                    trustManagers, null);
            connection.setSSLSocketFactory(sslContext.getSocketFactory());

            if (trustInformation != null) {
                // Determine is it need to overwrite default Host Name verifier
                boolean enableHostnameVerifier = true;
                String value =
                        trustInformation.getParameter(
                                KeyStoreInformation.ENABLE_HOST_NAME_VERIFIER);
                if (value != null) {
                    enableHostnameVerifier = Boolean.parseBoolean(value);
                }

                if (!enableHostnameVerifier) {

                    if (log.isDebugEnabled()) {
                        log.debug("Overriding default HostName Verifier." +
                                "HostName verification disabled");
                    }

                    connection.setHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                        public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
                            if (log.isTraceEnabled()) {
                                log.trace("HostName verification disabled");
                                log.trace("Host:   " + hostname);
                                log.trace("Peer Host:  " + session.getPeerHost());
                            }
                            return true;
                        }

                        @SuppressWarnings("unused")
						public boolean verify(String hostname, String certHostname) {
                            if (log.isTraceEnabled()) {
                                log.trace("HostName verification disabled");
                                log.trace("Host:   " + hostname);
                                log.trace("Cert HostName:  " + certHostname);
                            }
                            return true;
                        }
                    });
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Using default HostName verifier...");
                    }
                }
            }
            return connection;

        } catch (NoSuchAlgorithmException e) {
            handleException("Error loading SSLContext ", e);
        } catch (KeyManagementException e) {
            handleException("Error initiation SSLContext with KeyManagers", e);
        } catch (IOException e) {
            handleException("Error opening a https connection from URL : " + url, e);
        }
        return null;
    }

    /**
     * Returns a URLCOnnection for given URL. If the URL is https one , then URLConnectin is a
     * HttpsURLCOnnection and it is configured with KeyStores given in the synapse.properties file
     *
     * @param url URL
     * @return URLConnection for given URL
     */
    public static URLConnection getURLConnection(URL url) {

        try {
            if (url == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Provided URL is null");
                }
                return null;
            }
            URLConnection connection;
            if (url.getProtocol().equalsIgnoreCase("https")) {
                connection = getHttpsURLConnection(url);
            } else {
                connection = url.openConnection();
            }
            connection.setReadTimeout(getReadTimeout());
            connection.setConnectTimeout(getConnectTimeout());
            connection.setRequestProperty("Connection", "close"); // if http is being used
            return connection;
        } catch (IOException e) {
            handleException("Error reading at URI ' " + url + " ' ", e);
        }
        return null;
    }

    private static void handleException(String msg) {
        log.warn(msg);
        throw new SynapseException(msg);
    }

    /**
     * Return a suitable XMLToObjectMapper for the given content type if one
     * is available, else return null;
     *
     * @param contentType the content type for which a mapper is required
     * @return a suitable XMLToObjectMapper or null if none can be found
     */
    public static XMLToObjectMapper getXmlToObjectMapper(String contentType) {
        return null;
    }

    /**
     * Utility method to resolve url(only If need) path using synapse home system property
     *
     * @param path Path to the URL
     * @return Valid URL instance or null(if it is inavalid or can not open a connection to it )
     */
    public static URL getURLFromPath(String path) {
        if (path == null || "null".equals(path)) {
            if (log.isDebugEnabled()) {
                log.debug("Can not create a URL from 'null' ");
            }
            return null;
        }
        URL url = null;
        try {
            url = new URL(path);
            if ("file".equals(url.getProtocol())) {
                try {
                    url.openStream();
                } catch (MalformedURLException e) {
                    handleException("Invalid URL reference : " + path, e);
                } catch (IOException ignored) {
                    if (log.isDebugEnabled()) {
                        log.debug("Can not open a connection to the URL with a path :" +
                                path);
                    }
                    String synapseHome = getSynapseHome();
                    if (synapseHome != null) {
                        if (synapseHome.endsWith("/")) {
                            synapseHome = synapseHome.substring(0, synapseHome.lastIndexOf("/"));
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Trying  to resolve an absolute path of the " +
                                    " URL using the synapse.home : " + synapseHome);
                        }
                        try {
                            url = new URL(url.getProtocol() + ":" + synapseHome + "/" +
                                    url.getPath());
                            url.openStream();
                        } catch (MalformedURLException e) {
                            handleException("Invalid URL reference " + url.getPath() + e);
                        } catch (IOException e) {
                            if (log.isDebugEnabled()) {
                                log.debug("Failed to resolve an absolute path of the " +
                                        " URL using the synapse.home : " + synapseHome);
                            }
                            log.warn("IO Error reading from URL : " + url.getPath() + e);
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            handleException("Invalid URL reference :  " + path, e);
        }
        return url;
    }

    public static String resolveRelativeURI(String parentLocation, String relativeLocation) {

        if (relativeLocation == null) {
            throw new IllegalArgumentException("Import URI cannot be null");
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolving import URI ' " + parentLocation + " '  " +
                    "against base URI ' " + relativeLocation + " '  ");
        }

        URI importUri = null;
        try {
            importUri = new URI(relativeLocation);
            if (importUri.isAbsolute()) {
                return importUri.toString();
            }
        } catch (URISyntaxException e) {
            handleException("Invalid URI : " + relativeLocation, e);
        }

        if (parentLocation == null) {
            return importUri.toString();
        } else {
            // if the import-uri is absolute
            if (relativeLocation.startsWith("/") || relativeLocation.startsWith("\\")) {
                if (importUri != null && !importUri.isAbsolute()) {
                    try {
                        importUri = new URI("file:" + relativeLocation);
                        return importUri.toString();
                    } catch (URISyntaxException e) {
                        handleException("Invalid URI ' " + importUri.getPath() + " '", e);
                    }
                }
            } else {
                int index = parentLocation.lastIndexOf("/");
                if (index == -1) {
                    index = parentLocation.lastIndexOf("\\");
                }
                if (index != -1) {
                    String basepath = parentLocation.substring(0, index + 1);
                    String resolvedPath = basepath + relativeLocation;
                    try {
                        URI resolvedUri = new URI(resolvedPath);
                        if (!resolvedUri.isAbsolute()) {
                            resolvedUri = new URI("file:" + resolvedPath);
                        }
                        return resolvedUri.toString();
                    } catch (URISyntaxException e) {
                        handleException("Invalid URI ' " + resolvedPath + " '", e);
                    }
                } else {
                    return importUri.toString();
                }
            }
        }
        return null;
    }

    public static int getConnectTimeout() {
        return Integer.parseInt(SynapsePropertiesLoader.getPropertyValue(
                SynapseConstants.CONNECTTIMEOUT,
                String.valueOf(SynapseConstants.DEFAULT_CONNECTTIMEOUT)));

    }

    public static int getReadTimeout() {
        return Integer.parseInt(SynapsePropertiesLoader.getPropertyValue(
                SynapseConstants.READTIMEOUT,
                String.valueOf(SynapseConstants.DEFAULT_READTIMEOUT)));

    }

    public static long getTimeoutHandlerInterval() {
        return Long.parseLong(SynapsePropertiesLoader.getPropertyValue(
                SynapseConstants.TIMEOUT_HANDLER_INTERVAL,
                String.valueOf(SynapseConstants.DEFAULT_TIMEOUT_HANDLER_INTERVAL)));

    }

    public static long getGlobalTimeoutInterval() {
        return Long.parseLong(SynapsePropertiesLoader.getPropertyValue(
                SynapseConstants.GLOBAL_TIMEOUT_INTERVAL,
                String.valueOf(SynapseConstants.DEFAULT_GLOBAL_TIMEOUT)));

    }

    public static String getSynapseHome() {
        ServerManager serverManager = ServerManager.getInstance();
        if (serverManager.isInitialized()) {
            SynapseServerInfo information =
                    serverManager.getServerConfigurationInformation();
            if (information != null) {
                return information.getSynapseHome();
            }
        }
        return "";
    }

    public static String getServerName() {
        ServerManager serverManager = ServerManager.getInstance();
        if (serverManager.isInitialized()) {
            SynapseServerInfo information =
                    serverManager.getServerConfigurationInformation();
            if (information != null) {
                return information.getServerName();
            }
        }
        return "";
    }

    public static String getResolveRoot() {
        ServerManager serverManager = ServerManager.getInstance();
        if (serverManager.isInitialized()) {
            SynapseServerInfo information =
                    serverManager.getServerConfigurationInformation();
            if (information != null) {
                return information.getResolveRoot();
            }
        }
        return "";
    }

    /**
     * Get the StatisticsCollector from synapse env.
     *
     * @return StatisticsCollector instance if there is any
     */
    public static StatisticsCollector getStatisticsCollector() {

        ServerManager serverManager = ServerManager.getInstance();
        if (serverManager.isInitialized()) {
            ServerContextInformation information =
                    serverManager.getServerContextInformation();
            if (information != null) {
                Object o = information.getServerContext();
                if (o instanceof ConfigurationContext) {
                    ConfigurationContext context = (ConfigurationContext) o;
                    SynapseEnvironment environment =
                            (SynapseEnvironment) context.getAxisConfiguration().getParameterValue(
                                    SynapseConstants.SYNAPSE_ENV);
                    if (environment != null) {
                        return environment.getStatisticsCollector();
                    }
                }
            }
        }
        return null;
    }

    public static OMElement stringToOM(String xml) {
        try {
            return AXIOMUtil.stringToOM(xml);  // Just wrapp to add loging for any errors
        } catch (XMLStreamException e) {
            handleException("Unable to convert a string to OM Node as the string " +
                    "is malformed , String : " + xml, e);
        }
        return null;
    }

    /**
     * Construct a fresh SynapseConfiguration instance and registers the observers
     * with it as specified in the synapse.properties file. Use the initial.observers
     * property in the synapse.properties file to specify observers as a comma separated
     * list.
     *
     * @return a SynapseConfiguration instance
     */
    public static SynapseConfiguration newConfiguration() {
        SynapseConfiguration synConfig = new SynapseConfiguration();
        SynapsePropertiesLoader.loadSynapseProperties();
        return synConfig;
    }

    /**
     * Return the main sequence if one is not defined. This implementation defaults to
     * a simple sequence with a <send/>
     *
     * @param config the configuration to be updated
     */
    public static void setDefaultMainSequence(SynapseConfiguration config) {
        SequenceMediator main = new SequenceMediator();
        main.setName(SynapseConstants.MAIN_SEQUENCE_KEY);
        main.addChild(new LogMediator());
        main.addChild(new DropMediator());
        config.addSequence(SynapseConstants.MAIN_SEQUENCE_KEY, main);
        // set the aspect configuration
        AspectConfiguration configuration = new AspectConfiguration(main.getName());
        main.configure(configuration);
    }

    /**
     * Return the fault sequence if one is not defined. This implementation defaults to
     * a simple sequence :
     * <log level="full">
     *   <property name="MESSAGE" value="Executing default "fault" sequence"/>
     *   <property name="ERROR_CODE" expression="get-property('ERROR_CODE')"/>
     *   <property name="ERROR_MESSAGE" expression="get-property('ERROR_MESSAGE')"/>
     * </log>
     * <drop/>
     *
     * @param config the configuration to be updated
     */
    public static void setDefaultFaultSequence(SynapseConfiguration config) {
        SequenceMediator fault = new SequenceMediator();
        fault.setName(org.apache.synapse.SynapseConstants.FAULT_SEQUENCE_KEY);
        LogMediator log = new LogMediator();
        log.setLogLevel(LogMediator.FULL);

        MediatorProperty mp = new MediatorProperty();
        mp.setName("MESSAGE");
        mp.setValue("Executing default \"fault\" sequence");
        log.addProperty(mp);

        mp = new MediatorProperty();
        mp.setName("ERROR_CODE");
        try {
            mp.setExpression(new SynapseXPath("get-property('ERROR_CODE')"));
        } catch (JaxenException ignore) {}
        log.addProperty(mp);

        mp = new MediatorProperty();
        mp.setName("ERROR_MESSAGE");
        try {
            mp.setExpression(new SynapseXPath("get-property('ERROR_MESSAGE')"));
        } catch (JaxenException ignore) {}
        log.addProperty(mp);

        fault.addChild(log);
        fault.addChild(new DropMediator());

        // set aspect configuration
        AspectConfiguration configuration = new AspectConfiguration(fault.getName());
        fault.configure(configuration);

        config.addSequence(org.apache.synapse.SynapseConstants.FAULT_SEQUENCE_KEY, fault);
    }
}

