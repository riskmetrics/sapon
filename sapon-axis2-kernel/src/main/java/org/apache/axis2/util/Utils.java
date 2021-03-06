/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.axis2.util;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axis2.Axis2Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Flow;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.InOnlyAxisOperation;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.PhaseRule;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisError;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.receivers.RawXMLINOutMessageReceiver;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Utils {
    private static final Log log = LogFactory.getLog(Utils.class);

    public static void addHandler(Flow flow, Handler handler, String phaseName) {
        HandlerDescription handlerDesc = new HandlerDescription(handler.getName());
        PhaseRule rule = new PhaseRule(phaseName);

        handlerDesc.setRules(rule);
        handler.init(handlerDesc);
        handlerDesc.setHandler(handler);
        flow.addHandler(handlerDesc);
    }

    /**
     * @see org.apache.axis2.util.MessageContextBuilder:createOutMessageContext()
     * @deprecated (post1.1branch)
     */
    @Deprecated
	public static MessageContext createOutMessageContext(MessageContext inMessageContext)
            throws AxisFault {
        return MessageContextBuilder.createOutMessageContext(inMessageContext);
    }

    public static AxisService createSimpleService(QName serviceName, String className, QName opName)
            throws AxisFault {
        return createSimpleService(serviceName, new RawXMLINOutMessageReceiver(), className,
                                   opName);
    }

    public static AxisService createSimpleServiceforClient(QName serviceName, String className,
                                                           QName opName)
            throws AxisFault {
        return createSimpleServiceforClient(serviceName, new RawXMLINOutMessageReceiver(),
                                            className,
                                            opName);
    }


    public static AxisService createSimpleInOnlyService(QName serviceName,
                                                        MessageReceiver messageReceiver,
                                                        QName opName)
            throws AxisFault {
        AxisService service = new AxisService(serviceName.getLocalPart());
        service.setClassLoader(getContextClassLoader_DoPriv());

        AxisOperation axisOp = new InOnlyAxisOperation(opName);

        axisOp.setMessageReceiver(messageReceiver);
        axisOp.setStyle(WSDLConstants.STYLE_RPC);
        service.addOperation(axisOp);
        service.mapActionToOperation(Axis2Constants.AXIS2_NAMESPACE_URI + "/" + opName.getLocalPart(),
                                     axisOp);

        return service;
    }

    private static ClassLoader getContextClassLoader_DoPriv() {
        return AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                }
        );
    }


    public static AxisService createSimpleService(QName serviceName,
                                                  MessageReceiver messageReceiver, String className,
                                                  QName opName)
            throws AxisFault {
        AxisService service = new AxisService(serviceName.getLocalPart());

        service.setClassLoader(getContextClassLoader_DoPriv());
        service.addParameter(new Parameter(Axis2Constants.SERVICE_CLASS, className));

        AxisOperation axisOp = new InOutAxisOperation(opName);

        axisOp.setMessageReceiver(messageReceiver);
        axisOp.setStyle(WSDLConstants.STYLE_RPC);
        service.addOperation(axisOp);
        service.mapActionToOperation(Axis2Constants.AXIS2_NAMESPACE_URI + "/" + opName.getLocalPart(),
                                     axisOp);

        return service;
    }

    public static AxisService createSimpleServiceforClient(QName serviceName,
                                                           MessageReceiver messageReceiver,
                                                           String className,
                                                           QName opName)
            throws AxisFault {
        AxisService service = new AxisService(serviceName.getLocalPart());

        service.setClassLoader(getContextClassLoader_DoPriv());
        service.addParameter(new Parameter(Axis2Constants.SERVICE_CLASS, className));

        AxisOperation axisOp = new OutInAxisOperation(opName);

        axisOp.setMessageReceiver(messageReceiver);
        axisOp.setStyle(WSDLConstants.STYLE_RPC);
        service.addOperation(axisOp);

        return service;
    }

    public static ServiceContext fillContextInformation(AxisService axisService,
                                                        ConfigurationContext configurationContext)
            throws AxisFault {

        // 2. if null, create new opCtxt
        // fill the service group context and service context info
        return fillServiceContextAndServiceGroupContext(axisService, configurationContext);
    }

    private static ServiceContext fillServiceContextAndServiceGroupContext(AxisService axisService,
                                                                           ConfigurationContext configurationContext)
            throws AxisFault {
        String serviceGroupContextId = UUIDGenerator.getUUID();
        ServiceGroupContext serviceGroupContext =
                configurationContext.createServiceGroupContext(axisService.getServiceGroup());

        serviceGroupContext.setId(serviceGroupContextId);
        configurationContext.addServiceGroupContextIntoSoapSessionTable(serviceGroupContext);
        return serviceGroupContext.getServiceContext(axisService);
    }

    /**
     * Break a full path into pieces
     *
     * @return an array where element [0] always contains the service, and element 1, if not null, contains
     *         the path after the first element. all ? parameters are discarded.
     */
    public static String[] parseRequestURLForServiceAndOperation( final String requestURL,
    															  final String baseName)
    {
        if (log.isDebugEnabled()) {
            log.debug("parseRequestURLForServiceAndOperation : ["
            			+ requestURL + "][" + baseName + "]");
        }
        if (requestURL == null || baseName == null) {
            return null;
        }

        //This was 'lastIndexOf' before, but I can't tell why from the contexts
        //I've seen this called from (in fact, it seems like it would be
        //potentially dangerous, which is what the previous TODO comment was
        //referring to).  The real problem is that there's no documented
        //requirement for what path and servicePath are supposed to be.
        final int baseNameStart = requestURL.indexOf(baseName);
        if(baseNameStart < 0) {
        	if (log.isDebugEnabled()) {
                log.debug("Unable to parse request URL ["
                		+ requestURL + "][" + baseName + "]");
            }
        	//TODO: This returned an array of nulls before.
        	//Client code should already be checking for null from this method,
        	//since another code path can throw it, but there's probably some
        	//that isn't (be prepared).
        	return null;
        }

        final int baseNameEnd = baseNameStart + baseName.length();
        if (requestURL.length() <= baseNameEnd + 1) {
        	return null;
        }

        int queryIndex = requestURL.indexOf('?');

        String service;
        if (queryIndex > 0) {
        	service = requestURL.substring(baseNameEnd + 1, queryIndex);
        } else {
        	service = requestURL.substring(baseNameEnd + 1);
        }

        final String[] values = new String[2];
        int operationIndex = service.indexOf('/');
        if (operationIndex > 0) {
        	values[0] = service.substring(0, operationIndex);
        	values[1] = service.substring(operationIndex + 1);
        	operationIndex = values[1].lastIndexOf('/');
        	if (operationIndex > 0) {
        		values[1] = values[1].substring(operationIndex + 1);
        	}
        } else {
        	values[0] = service;
        }

        return values;
    }

    public static ConfigurationContext getNewConfigurationContext(String repositry)
            throws Exception {
        final File file = new File(repositry);
        boolean exists = exists(file);
        if (!exists) {
            throw new Exception("repository directory " + file.getAbsolutePath()
                                + " does not exists");
        }
        File axis2xml = new File(file, "axis.xml");
        String axis2xmlString = null;
        if (exists(axis2xml)) {
            axis2xmlString = axis2xml.getName();
        }
        String path = AccessController.doPrivileged(
                new PrivilegedAction<String>() {
                    public String run() {
                        return file.getAbsolutePath();
                    }
                }
        );
        return ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(path, axis2xmlString);
    }

    private static boolean exists(final File file) {
        Boolean exists = AccessController.doPrivileged(
                new PrivilegedAction<Boolean>() {
                    public Boolean run() {
                        return Boolean.valueOf(file.exists());
                    }
                }
        );
        return exists.booleanValue();
    }

    public static String getParameterValue(Parameter param) {
        if (param == null) {
            return null;
        } else {
            return (String) param.getValue();
        }
    }

    /**
     * Get the name of the module , where archive name is combination of module name + its version
     * The format of the name is as follows:
     * moduleName-00.0000
     * Example: "addressing-01.0001.mar" would return "addressing"
     *
     * @param moduleName the name of the module archive
     * @return the module name parsed out of the file name
     */
    public static String getModuleName(String moduleName) {
        if (moduleName.endsWith("-SNAPSHOT")) {
            return moduleName.substring(0, moduleName.indexOf("-SNAPSHOT"));
        }
        char delimiter = '-';
        int version_index = moduleName.lastIndexOf(delimiter);
        if (version_index > 0) {
            String versionString = getModuleVersion(moduleName);
            if (versionString == null) {
                return moduleName;
            } else {
                return moduleName.substring(0, version_index);
            }
        } else {
            return moduleName;
        }
    }

    public static String getModuleVersion(String moduleName) {
        if (moduleName.endsWith("-SNAPSHOT")) {
            return "SNAPSHOT";
        }
        char version_seperator = '-';
        int version_index = moduleName.lastIndexOf(version_seperator);
        if (version_index > 0) {
            String versionString = moduleName.substring(version_index + 1, moduleName.length());
            try {
                Float.parseFloat(versionString);
                return versionString;
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static String getModuleName(String moduleName, String moduleVersion) {
        if (moduleVersion != null && moduleVersion.length() != 0) {
            moduleName = moduleName + "-" + moduleVersion;
        }
        return moduleName;
    }

    /**
     * - if he trying to engage the same module then method will returen false
     * - else it will return true
     *
     */
    public static boolean checkVersion(String module1version,
                                       String module2version) throws AxisFault {
        if ((module1version !=null && !module1version.equals(module2version)) ||
                module2version !=null && !module2version.equals(module1version)) {
            throw new AxisFault("trying to engage two different module versions " +
                    module1version + " : " + module2version);
        }
        return true;
    }

    public static void calculateDefaultModuleVersion(Map<?, AxisModule> modules,
                                                     AxisConfiguration axisConfig) {
        Map<String, String> defaultModules = new HashMap<String, String>();
        for(AxisModule axisModule: modules.values()) {
            String moduleName = axisModule.getName();
            String moduleNameString;
            String moduleVersionString;
            if (AxisModule.VERSION_SNAPSHOT.equals(axisModule.getVersion())) {
                moduleNameString = axisModule.getName();
                moduleVersionString = axisModule.getVersion();
            } else {
                if (axisModule.getVersion() == null) {
                    moduleNameString = getModuleName(moduleName);
                    moduleVersionString = getModuleVersion(moduleName);
                    if (moduleVersionString != null) {
                        try {
                            Float.valueOf(moduleVersionString);
                            axisModule.setVersion(moduleVersionString);
                            axisModule.setName(moduleName);
                        } catch (NumberFormatException e) {
                            moduleVersionString = null;
                        }
                    }
                } else {
                    moduleNameString = axisModule.getName();
                    moduleVersionString = axisModule.getVersion();
                }
            }
            String currentDefaultVerison = defaultModules.get(moduleNameString);
            if (currentDefaultVerison != null) {
                // if the module version is null then , that will be ignore in this case
                if (!AxisModule.VERSION_SNAPSHOT.equals(currentDefaultVerison)) {
                    if (moduleVersionString != null &&
                        isLatest(moduleVersionString, currentDefaultVerison)) {
                        defaultModules.put(moduleNameString, moduleVersionString);
                    }
                }
            } else {
                defaultModules.put(moduleNameString, moduleVersionString);
            }

        }
        for(Map.Entry<String, String> e: defaultModules.entrySet()) {
            axisConfig.addDefaultModuleVersion(e.getKey(), e.getValue());
        }
    }

    public static boolean isLatest(String moduleVersion, String currentDefaultVersion) {
        if (AxisModule.VERSION_SNAPSHOT.equals(moduleVersion)) {
            return true;
        } else {
            float m_version = Float.parseFloat(moduleVersion);
            float m_c_vresion = Float.parseFloat(currentDefaultVersion);
            return m_version > m_c_vresion;
        }
    }

    /**
     * Check if a MessageContext property is true.
     *
     * @param messageContext the MessageContext
     * @param propertyName   the property name
     * @return true if the property is Boolean.TRUE, "true", 1, etc. or false otherwise
     * @deprecated please use MessageContext.isTrue(propertyName) instead
     */
    @Deprecated
	public static boolean isExplicitlyTrue(MessageContext messageContext, String propertyName) {
        Object flag = messageContext.getProperty(propertyName);
        return JavaUtils.isTrueExplicitly(flag);
    }

    /**
     * Maps the String URI of the Message exchange pattern to a integer.
     * Further, in the first lookup, it will cache the looked
     * up value so that the subsequent method calls are extremely efficient.
     */
    @SuppressWarnings("deprecation")
	public static int getAxisSpecifMEPConstant(String messageExchangePattern) {


        int mepConstant = WSDLConstants.MEP_CONSTANT_INVALID;

        if (WSDL2Constants.MEP_URI_IN_OUT.equals(messageExchangePattern) ||
            WSDLConstants.WSDL20_2006Constants.MEP_URI_IN_OUT.equals(messageExchangePattern) ||
            WSDLConstants.WSDL20_2004_Constants.MEP_URI_IN_OUT.equals(messageExchangePattern)) {
            mepConstant = WSDLConstants.MEP_CONSTANT_IN_OUT;
        } else if (
                WSDL2Constants.MEP_URI_IN_ONLY.equals(messageExchangePattern) ||
                WSDLConstants.WSDL20_2006Constants.MEP_URI_IN_ONLY.equals(messageExchangePattern) ||
                WSDLConstants.WSDL20_2004_Constants.MEP_URI_IN_ONLY
                        .equals(messageExchangePattern)) {
            mepConstant = WSDLConstants.MEP_CONSTANT_IN_ONLY;
        } else if (WSDL2Constants.MEP_URI_IN_OPTIONAL_OUT
                .equals(messageExchangePattern) ||
                                                WSDLConstants.WSDL20_2006Constants.MEP_URI_IN_OPTIONAL_OUT
                                                        .equals(messageExchangePattern) ||
                                                                                        WSDLConstants.WSDL20_2004_Constants.MEP_URI_IN_OPTIONAL_OUT
                                                                                                .equals(messageExchangePattern)) {
            mepConstant = WSDLConstants.MEP_CONSTANT_IN_OPTIONAL_OUT;
        } else if (WSDL2Constants.MEP_URI_OUT_IN.equals(messageExchangePattern) ||
                   WSDLConstants.WSDL20_2006Constants.MEP_URI_OUT_IN.equals(messageExchangePattern) ||
                   WSDLConstants.WSDL20_2004_Constants.MEP_URI_OUT_IN
                           .equals(messageExchangePattern)) {
            mepConstant = WSDLConstants.MEP_CONSTANT_OUT_IN;
        } else if (WSDL2Constants.MEP_URI_OUT_ONLY.equals(messageExchangePattern) ||
                   WSDLConstants.WSDL20_2006Constants.MEP_URI_OUT_ONLY
                           .equals(messageExchangePattern) ||
                                                           WSDLConstants.WSDL20_2004_Constants
                                                                   .MEP_URI_OUT_ONLY.equals(messageExchangePattern)) {
            mepConstant = WSDLConstants.MEP_CONSTANT_OUT_ONLY;
        } else if (WSDL2Constants.MEP_URI_OUT_OPTIONAL_IN.equals(messageExchangePattern) ||
                   WSDLConstants.WSDL20_2006Constants.MEP_URI_OUT_OPTIONAL_IN
                           .equals(messageExchangePattern) ||
                                                           WSDLConstants.WSDL20_2004_Constants.MEP_URI_OUT_OPTIONAL_IN
                                                                   .equals(messageExchangePattern)) {
            mepConstant = WSDLConstants.MEP_CONSTANT_OUT_OPTIONAL_IN;
        } else if (WSDL2Constants.MEP_URI_ROBUST_IN_ONLY.equals(messageExchangePattern) ||
                   WSDLConstants.WSDL20_2006Constants.MEP_URI_ROBUST_IN_ONLY
                           .equals(messageExchangePattern) ||
                                                           WSDLConstants.WSDL20_2004_Constants.MEP_URI_ROBUST_IN_ONLY
                                                                   .equals(messageExchangePattern)) {
            mepConstant = WSDLConstants.MEP_CONSTANT_ROBUST_IN_ONLY;
        } else if (WSDL2Constants.MEP_URI_ROBUST_OUT_ONLY.equals(messageExchangePattern) ||
                   WSDLConstants.WSDL20_2006Constants.MEP_URI_ROBUST_OUT_ONLY
                           .equals(messageExchangePattern) ||
                                                           WSDLConstants.WSDL20_2004_Constants.MEP_URI_ROBUST_OUT_ONLY
                                                                   .equals(messageExchangePattern)) {
            mepConstant = WSDLConstants.MEP_CONSTANT_ROBUST_OUT_ONLY;
        }

        if (mepConstant == WSDLConstants.MEP_CONSTANT_INVALID) {
            throw new AxisError(Messages.getMessage("mepmappingerror"));
        }


        return mepConstant;
    }

    /**
     * Get an AxisFault object to represent the SOAPFault in the SOAPEnvelope attached
     * to the provided MessageContext. This first check for an already extracted AxisFault
     * and otherwise does a simple extract.
     * <p/>
     * MUST NOT be passed a MessageContext which does not contain a SOAPFault
     *
     * @param messageContext
     * @return
     */
    public static AxisFault getInboundFaultFromMessageContext(MessageContext messageContext) {
        // Get the fault if it's already been extracted by a handler
        AxisFault result = (AxisFault) messageContext.getProperty(Axis2Constants.INBOUND_FAULT_OVERRIDE);
        // Else, extract it from the SOAPBody
        if (result == null) {
            SOAPEnvelope envelope = messageContext.getEnvelope();
            SOAPFault soapFault;
            SOAPBody soapBody;
            if (envelope != null && (soapBody = envelope.getBody()) != null) {
                if ((soapFault = soapBody.getFault()) != null) {
                    return new AxisFault(soapFault, messageContext);
                }
                // If its a REST response the content is not a SOAP envelop and hence we will
                // Have use the soap body as the exception
                if (messageContext.isDoingREST() && soapBody.getFirstElement() != null) {
                    return new AxisFault(soapBody.getFirstElement().toString());
                }
            }
            // Not going to be able to
            throw new IllegalArgumentException(
                    "The MessageContext does not have an associated SOAPFault.");
        }
        return result;
    }

    /**
     * This method will provide the logic needed to retrieve an Object's classloader
     * in a Java 2 Security compliant manner.
     */
    public static ClassLoader getObjectClassLoader(final Object object) {
        if(object == null) {
            return null;
        }
        else {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return object.getClass().getClassLoader();
                }
            });
        }
    }

    public static int getMtomThreshold(MessageContext msgCtxt){
    	Integer value = null;
        if(!msgCtxt.isServerSide()){
	        value = (Integer)msgCtxt.getProperty(Axis2Constants.Configuration.MTOM_THRESHOLD);
        }else{
        	Parameter param = msgCtxt.getParameter(Axis2Constants.Configuration.MTOM_THRESHOLD);
        	if(param!=null){
        		value = (Integer)param.getValue();
        	}
        }

        int threshold = (value == null) ? 0 : value.intValue();
        if(log.isDebugEnabled()){
        	log.debug("MTOM optimized Threshold value ="+threshold);
        }
        return threshold;
    }
     /**
     * Returns the ip address to be used for the replyto epr
     * CAUTION:
     * This will go through all the available network interfaces and will try to return an ip address.
     * First this will try to get the first IP which is not loopback address (127.0.0.1). If none is found
     * then this will return this will return 127.0.0.1.
     * This will <b>not<b> consider IPv6 addresses.
     * <p/>
     * TODO:
     * - Improve this logic to genaralize it a bit more
     * - Obtain the ip to be used here from the Call API
     *
     * @return Returns String.
     * @throws java.net.SocketException
      */
    public static String getIpAddress() throws SocketException {
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        String address = "127.0.0.1";

        while (e.hasMoreElements()) {
            NetworkInterface netface = e.nextElement();
            Enumeration<InetAddress> addresses = netface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress ip = addresses.nextElement();
                if (!ip.isLoopbackAddress() && isIP(ip.getHostAddress())) {
                    return ip.getHostAddress();
                }
            }
        }

        return address;
    }

    /**
     * First check whether the hostname parameter is there in AxisConfiguration (axis2.xml) ,
     * if it is there then this will retun that as the host name , o.w will return the IP address.
     */
    public static String getIpAddress(AxisConfiguration axisConfiguration) throws SocketException {
        if(axisConfiguration!=null){
            Parameter param = axisConfiguration.getParameter(TransportListener.HOST_ADDRESS);
            if (param != null) {
                String  hostAddress = ((String) param.getValue()).trim();
                if(hostAddress!=null){
                    return hostAddress;
                }
            }
        }
        return getIpAddress();
    }

    /**
     * First check whether the hostname parameter is there in AxisConfiguration (axis2.xml) ,
     * if it is there then this will return that as the host name , o.w will return the IP address.
     * @param axisConfiguration
     * @return hostname
     */
    public static String getHostname(AxisConfiguration axisConfiguration) {
        if(axisConfiguration!=null){
            Parameter param = axisConfiguration.getParameter(TransportListener.HOST_ADDRESS);
            if (param != null) {
                String  hostAddress = ((String) param.getValue()).trim();
                if(hostAddress!=null){
                    return hostAddress;
                }
            }
        }
        return null;
    }

    private static boolean isIP(String hostAddress) {
        return hostAddress.split("[.]").length == 4;
    }
}
