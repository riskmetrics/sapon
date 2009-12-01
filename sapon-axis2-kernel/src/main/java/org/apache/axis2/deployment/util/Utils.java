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

package org.apache.axis2.deployment.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.classloader.JarFileClassLoader;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.DeploymentClassLoader;
import org.apache.axis2.deployment.DeploymentConstants;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.ArchiveReader;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.axis2.description.AxisBinding;
import org.apache.axis2.description.AxisBindingMessage;
import org.apache.axis2.description.AxisBindingOperation;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisEndpoint;
import org.apache.axis2.description.AxisMessage;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisOperationFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Flow;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.description.java2wsdl.DefaultSchemaGenerator;
import org.apache.axis2.description.java2wsdl.DocLitBareSchemaGenerator;
import org.apache.axis2.description.java2wsdl.Java2WSDLConstants;
import org.apache.axis2.description.java2wsdl.SchemaGenerator;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.Loader;
import org.apache.axis2.util.PolicyUtil;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.wsdl.WSDLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.PolicyComponent;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.utils.NamespaceMap;

public class Utils {

    public static String defaultEncoding = new OutputStreamWriter(System.out).getEncoding();

    private static Log log = LogFactory.getLog(Utils.class);

    public static void addFlowHandlers(Flow flow, ClassLoader clsLoader)
            throws AxisFault {
        int count = flow.getHandlerCount();

        for (int j = 0; j < count; j++) {
            HandlerDescription handlermd = flow.getHandler(j);
            Handler handler;

            final Class<?> handlerClass = getHandlerClass(
                    handlermd.getClassName(), clsLoader);

            try {
                handler = AccessController.doPrivileged(new PrivilegedExceptionAction<Handler>() {
                            public Handler run() throws InstantiationException,
                                    IllegalAccessException {
                                return (Handler)handlerClass.newInstance();
                            }
                        });
                handler.init(handlermd);
                handlermd.setHandler(handler);
            } catch (PrivilegedActionException e) {
                throw AxisFault.makeFault(e);
            }
        }
    }

    public static boolean loadHandler(ClassLoader loader1,
                                      HandlerDescription desc) throws DeploymentException {
        String handlername = desc.getClassName();
        Handler handler;
        try {
            final Class<?> handlerClass = Loader.loadClass(loader1, handlername);
            Package aPackage = org.apache.axis2.java.security.AccessController
                    .doPrivileged(new PrivilegedAction<Package>() {
                        public Package run() {
                            return handlerClass.getPackage();
                        }
                    });
            if (aPackage != null
                && aPackage.getName().equals("org.apache.axis2.engine")) {
                String name = handlerClass.getName();
                log.warn("Dispatcher " + name + " is now deprecated.");
                if (name.indexOf("InstanceDispatcher") != -1) {
                    log.warn("Please remove the entry for "
                             + handlerClass.getName() + "from axis2.xml");
                } else {
                    log.warn(
                            "Please edit axis2.xml and replace with the same class in org.apache.axis2.dispatchers package");
                }
            }
            handler = org.apache.axis2.java.security.AccessController
                    .doPrivileged(new PrivilegedExceptionAction<Handler>() {
                        public Handler run() throws InstantiationException,
                                IllegalAccessException {
                            return (Handler)handlerClass.newInstance();
                        }
                    });
            handler.init(desc);
            desc.setHandler(handler);
        } catch (ClassNotFoundException e) {
            if (handlername.indexOf("jaxws") > 0) {
                log.warn("[JAXWS] - unable to load " + handlername);
                return false;
            }
            throw new DeploymentException(e);
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
        return true;
    }

    public static URL[] getURLsForAllJars(URL url, File tmpDir) {
        FileInputStream fin = null;
        InputStream in = null;
        ZipInputStream zin = null;
        try {
            List<URL> array = new ArrayList<URL>();
            in = url.openStream();
            String fileName = url.getFile();
            int index = fileName.lastIndexOf('/');
            if (index != -1) {
                fileName = fileName.substring(index + 1);
            }
            final File f = createTempFile(fileName, in, tmpDir);

            fin = (FileInputStream)org.apache.axis2.java.security.AccessController
                    .doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                        public InputStream run() throws FileNotFoundException {
                            return new FileInputStream(f);
                        }
                    });
            array.add(f.toURI().toURL());
            zin = new ZipInputStream(fin);

            ZipEntry entry;
            String entryName;
            while ((entry = zin.getNextEntry()) != null) {
                entryName = entry.getName();
                /**
                 * id the entry name start with /lib and end with .jar then
                 * those entry name will be added to the arraylist
                 */
                if ((entryName != null)
                    && entryName.toLowerCase().startsWith("lib/")
                    && entryName.toLowerCase().endsWith(".jar")) {
                    String suffix = entryName.substring(4);
                    File f2 = createTempFile(suffix, zin, tmpDir);
                    array.add(f2.toURI().toURL());
                }
            }
            return array.toArray(new URL[array.size()]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    //
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    //
                }
            }
            if (zin != null) {
                try {
                    zin.close();
                } catch (IOException e) {
                    //
                }
            }
        }
    }

    public static File createTempFile(final String suffix, InputStream in,
                                      final File tmpDir) throws IOException {
        byte data[] = new byte[2048];
        int count;
        File f = TempFileManager.createTempFile("axis2", suffix);

//        if (tmpDir == null) {
//            String directory = (String)org.apache.axis2.java.security.AccessController
//                    .doPrivileged(new PrivilegedAction() {
//                        public Object run() {
//                            return System.getProperty("java.io.tmpdir");
//                        }
//                    });
//            final File tempFile = new File(directory, "_axis2");
//            Boolean exists = (Boolean)org.apache.axis2.java.security.AccessController
//                    .doPrivileged(new PrivilegedAction() {
//                        public Object run() {
//                            return tempFile.exists();
//                        }
//                    });
//            if (!exists) {
//                Boolean mkdirs = (Boolean)org.apache.axis2.java.security.AccessController
//                        .doPrivileged(new PrivilegedAction() {
//                            public Object run() {
//                                return tempFile.mkdirs();
//                            }
//                        });
//                if (!mkdirs) {
//                    throw new IOException("Unable to create the directory");
//                }
//            }
//            try {
//                f = (File)org.apache.axis2.java.security.AccessController
//                        .doPrivileged(new PrivilegedExceptionAction() {
//                            public Object run() throws IOException {
//                                return File.createTempFile("axis2", suffix,
//                                                           tempFile);
//                            }
//                        });
//                f.deleteOnExit();
//            } catch (PrivilegedActionException e) {
//                throw (IOException)e.getException();
//            }
//        } else {
//            try {
//                f = (File)org.apache.axis2.java.security.AccessController
//                        .doPrivileged(new PrivilegedExceptionAction() {
//                            public Object run() throws IOException {
//                                return File.createTempFile("axis2", suffix,
//                                                           tmpDir);
//                            }
//                        });
//                f.deleteOnExit();
//            } catch (PrivilegedActionException e) {
//                throw (IOException)e.getException();
//            }
//        }
//        if (log.isDebugEnabled()) {
//            log.debug("Created temporary file : " + f.getAbsolutePath());// $NON-SEC-4
//        }
//        final File f2 = f;
//        org.apache.axis2.java.security.AccessController
//                .doPrivileged(new PrivilegedAction() {
//                    public Object run() {
//                        f2.deleteOnExit();
//                        return null;
//                    }
//                });
        OutputStream out;
        final File f2 = f;
        try {
            out = org.apache.axis2.java.security.AccessController
                    .doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
                        public OutputStream run() throws FileNotFoundException {
                            return new FileOutputStream(f2);
                        }
                    });
        } catch (PrivilegedActionException e) {
            throw (FileNotFoundException)e.getException();
        }
        while ((count = in.read(data, 0, 2048)) != -1) {
            out.write(data, 0, count);
        }
        out.close();
        return f;
    }

    public static ClassLoader getClassLoader(ClassLoader parent, String path)
            throws DeploymentException {
        return getClassLoader(parent, new File(path));
    }

    /**
     * Get a ClassLoader which contains a classpath of a) the passed directory and b) any jar files
     * inside the "lib/" or "Lib/" subdirectory of the passed directory.
     *
     * @param parent parent ClassLoader which will be the parent of the result of this method
     * @param file   a File which must be a directory for this to be useful
     * @return a new ClassLoader pointing to both the passed dir and jar files under lib/
     * @throws DeploymentException if problems occur
     */
    public static ClassLoader getClassLoader(final ClassLoader parent, File file)
            throws DeploymentException {
        URLClassLoader classLoader;

        if (file == null) {
			return null; // Shouldn't this just return the parent?
		}

        try {
            List<URL> urls = new ArrayList<URL>();
            urls.add(file.toURI().toURL());

            // lower case directory name
            File libfiles = new File(file, "lib");
            if (!addFiles(urls, libfiles)) {
                // upper case directory name
                libfiles = new File(file, "Lib");
                addFiles(urls, libfiles);
            }

            final URL urllist[] = new URL[urls.size()];
            for (int i = 0; i < urls.size(); i++) {
                urllist[i] = urls.get(i);
            }
            classLoader = AccessController
                    .doPrivileged(new PrivilegedAction<URLClassLoader>() {
                        public URLClassLoader run() {
                            if (useJarFileClassLoader()) {
                                return new JarFileClassLoader(urllist, parent);
                            } else {
                                return new URLClassLoader(urllist, parent);
                            }
                        }
                    });
            return classLoader;
        } catch (MalformedURLException e) {
            throw new DeploymentException(e);
        }
    }

    private static boolean useJarFileClassLoader() {
        // The JarFileClassLoader was created to address a locking problem seen only on Windows platforms.
        // It carries with it a slight performance penalty that needs to be addressed.  Rather than make
        // *nix OSes carry this burden we'll engage the JarFileClassLoader for Windows or if the user
        // specifically requests it.
        boolean useJarFileClassLoader;
        if (System.getProperty("org.apache.axis2.classloader.JarFileClassLoader") == null) {
            useJarFileClassLoader = System.getProperty("os.name").startsWith("Windows");
        } else {
            useJarFileClassLoader = Boolean.getBoolean("org.apache.axis2.classloader.JarFileClassLoader");
        }
        return useJarFileClassLoader;
    }

    private static boolean addFiles(List<URL> urls, final File libfiles)
            throws MalformedURLException {
        Boolean exists = org.apache.axis2.java.security.AccessController
                .doPrivileged(new PrivilegedAction<Boolean>() {
                    public Boolean run() {
                        return libfiles.exists();
                    }
                });
        if (exists) {
            urls.add(libfiles.toURI().toURL());
            File jarfiles[] = org.apache.axis2.java.security.AccessController
					        .doPrivileged(new PrivilegedAction<File[]>() {
					            public File[] run() {
					                return libfiles.listFiles();
					            }
					        });
            int i = 0;
            while (i < jarfiles.length) {
                File jarfile = jarfiles[i];
                if (jarfile.getName().endsWith(".jar")) {
                    urls.add(jarfile.toURI().toURL());
                }
                i++;
            }
        }
        return exists;
    }

    private static Class<?> getHandlerClass(String className, ClassLoader loader1)
            throws AxisFault {
        Class<?> handlerClass;

        try {
            handlerClass = Loader.loadClass(loader1, className);
        } catch (ClassNotFoundException e) {
            throw AxisFault.makeFault(e);
        }

        return handlerClass;
    }

    /**
     * This guy will create a AxisService using java reflection
     *
     * @param axisService       the target AxisService
     * @param axisConfig        the in-scope AxisConfiguration
     * @param excludeOperations a List of Strings (or null), each containing a method to exclude
     * @param nonRpcMethods     a List of Strings (or null), each containing a non-rpc method name
     * @throws Exception if a problem occurs
     */
    public static void fillAxisService(final AxisService axisService,
                                       final AxisConfiguration axisConfig,
                                       final List<String> excludeOperations,
                                       final List<String> nonRpcMethods)
    	throws Exception
    {
        String serviceClass;
        Parameter implInfoParam = axisService
                .getParameter(Constants.SERVICE_CLASS);
        ClassLoader serviceClassLoader = axisService.getClassLoader();

        if (implInfoParam != null) {
            serviceClass = (String)implInfoParam.getValue();
        } else {
            // if Service_Class is null, every AbstractMR will look for
            // ServiceObjectSupplier. This is user specific and may contain
            // other looks.
            implInfoParam = axisService
                    .getParameter(Constants.SERVICE_OBJECT_SUPPLIER);
            if (implInfoParam != null) {
                String className = ((String)implInfoParam.getValue()).trim();
                final Class<?> serviceObjectMaker = Loader.loadClass(
                        serviceClassLoader, className);
                if (serviceObjectMaker.getModifiers() != Modifier.PUBLIC) {
                    throw new AxisFault("Service class " + className
                                        + " must have public as access Modifier");
                }

                // Find static getServiceObject() method, call it if there
                final Method method = AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
                            public Method run() throws NoSuchMethodException {
                                return serviceObjectMaker.getMethod(
                                        "getServiceObject",
                                        AxisService.class);
                            }
                        });
                Object obj = null;
                if (method != null) {
                    obj = org.apache.axis2.java.security.AccessController
                            .doPrivileged(new PrivilegedExceptionAction<Object>() {
                                public Object run()
                                        throws InstantiationException,
                                        IllegalAccessException,
                                        InvocationTargetException {
                                    return method.invoke(serviceObjectMaker.newInstance(),
                                                         axisService);
                                }
                            });
                }
                if (obj == null) {
                    log.warn("ServiceObjectSupplier implmentation Object could not be found");
                    throw new DeploymentException(
                            "ServiceClass or ServiceObjectSupplier implmentation Object could not be found");
                }
                serviceClass = obj.getClass().getName();
            } else {
                return;
            }
        }
        // adding name spaces
        NamespaceMap map = new NamespaceMap();
        map.put(Java2WSDLConstants.AXIS2_NAMESPACE_PREFIX,
                Java2WSDLConstants.AXIS2_XSD);
        map.put(Java2WSDLConstants.DEFAULT_SCHEMA_NAMESPACE_PREFIX,
                Java2WSDLConstants.URI_2001_SCHEMA_XSD);
        axisService.setNamespaceMap(map);
        SchemaGenerator schemaGenerator;
        Parameter generateBare = axisService
                .getParameter(Java2WSDLConstants.DOC_LIT_BARE_PARAMETER);
        if (generateBare != null && "true".equals(generateBare.getValue())) {
            schemaGenerator = new DocLitBareSchemaGenerator(serviceClassLoader,
                                                            serviceClass.trim(),
                                                            axisService.getSchemaTargetNamespace(),
                                                            axisService
                                                                    .getSchemaTargetNamespacePrefix(),
                                                            axisService);
        } else {
            schemaGenerator = new DefaultSchemaGenerator(serviceClassLoader,
                                                         serviceClass.trim(),
                                                         axisService.getSchemaTargetNamespace(),
                                                         axisService
                                                                 .getSchemaTargetNamespacePrefix(),
                                                         axisService);
        }
        schemaGenerator.setExcludeMethods(excludeOperations);
        schemaGenerator.setNonRpcMethods(nonRpcMethods);
        if (!axisService.isElementFormDefault()) {
            schemaGenerator
                    .setElementFormDefault(Java2WSDLConstants.FORM_DEFAULT_UNQUALIFIED);
        }
        // package to namespace map
        schemaGenerator.setPkg2nsmap(axisService.getP2nMap());
        Collection<XmlSchema> schemas = schemaGenerator.generateSchema();
        axisService.addSchema(schemas);
        axisService.setSchemaTargetNamespace(schemaGenerator
                .getSchemaTargetNameSpace());
        axisService.setTypeTable(schemaGenerator.getTypeTable());
        if (Java2WSDLConstants.DEFAULT_TARGET_NAMESPACE.equals(axisService
                .getTargetNamespace())) {
            axisService
                    .setTargetNamespace(schemaGenerator.getTargetNamespace());
        }

        Method[] method = schemaGenerator.getMethods();
        PhasesInfo pinfo = axisConfig.getPhasesInfo();

        for (Method jmethod : method) {
            String opName = jmethod.getName();
            AxisOperation operation = axisService
                    .getOperation(new QName(opName));
            // if the operation there in services.xml then try to set it schema
            // element name
            if (operation == null) {
                operation = axisService.getOperation(new QName(
                        jmethod.getName()));
            }
            MessageReceiver mr =
                    axisService.getMessageReceiver(operation.getMessageExchangePattern());
            if (mr == null) {
                mr = axisConfig.getMessageReceiver(operation.getMessageExchangePattern());
            }
            if (operation.getMessageReceiver() == null) {
                operation.setMessageReceiver(mr);
            }
            operation.setPhases(pinfo);
            axisService.addOperation(operation);
            if (operation.getSoapAction() == null) {
                operation.setSoapAction("urn:" + opName);
            }
        }
    }

    public static AxisOperation getAxisOperationForJmethod(Method method)
            throws AxisFault {
        AxisOperation operation;
        if ("void".equals(method.getReturnType().getName())) {
            if (method.getExceptionTypes().length > 0) {
                operation = AxisOperationFactory
                        .getAxisOperation(WSDLConstants.MEP_CONSTANT_ROBUST_IN_ONLY);
            } else {
                operation = AxisOperationFactory
                        .getAxisOperation(WSDLConstants.MEP_CONSTANT_IN_ONLY);
            }
        } else {
            operation = AxisOperationFactory
                    .getAxisOperation(WSDLConstants.MEP_CONSTANT_IN_OUT);
        }
        String opName = method.getName();
        operation.setName(new QName(opName));
        WebMethod methodAnnon = method.getAnnotation(WebMethod.class);
        if (methodAnnon != null) {
            String action = methodAnnon.action();
            if (action != null && !"".equals(action)) {
                operation.setSoapAction(action);
            }
        }
        return operation;
    }

    public static OMElement getParameter(String name, String value,
                                         boolean locked) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement parameter = fac.createOMElement("parameter", null);
        parameter.addAttribute("name", name, null);
        parameter.addAttribute("locked", Boolean.toString(locked), null);
        parameter.setText(value);
        return parameter;
    }

    /**
     * Modules can contain services in some cases.  This method will deploy all the services
     * for a given AxisModule into the current AxisConfiguration.
     * <p>
     * The code looks for an "aars/" directory inside the module (either .mar or exploded),
     * and an "aars.list" file inside that to figure out which services to deploy.  Note that all
     * services deployed this way will have access to the Module's classes.
     * </p>
     *
     * @param module the AxisModule to search for services
     * @param configCtx ConfigurationContext in which to deploy
     */

    public static void deployModuleServices(AxisModule module,
                                            ConfigurationContext configCtx) throws AxisFault {
        try {
            AxisConfiguration axisConfig = configCtx.getAxisConfiguration();
            ArchiveReader archiveReader = new ArchiveReader();
            PhasesInfo phasesInfo = axisConfig.getPhasesInfo();
            final ClassLoader moduleClassLoader = module.getModuleClassLoader();
            List<String> services = new ArrayList<String>();
            final InputStream in = org.apache.axis2.java.security.AccessController
                    .doPrivileged(new PrivilegedAction<InputStream>() {
                        public InputStream run() {
                            return moduleClassLoader
                                    .getResourceAsStream("aars/aars.list");
                        }
                    });
            if (in != null) {
                BufferedReader input;
                try {
                    input = new BufferedReader(
                            org.apache.axis2.java.security.AccessController
                                    .doPrivileged(new PrivilegedAction<InputStreamReader>() {
                                        public InputStreamReader run() {
                                            return new InputStreamReader(in);
                                        }
                                    }));
                    String line;
                    while ((line = input.readLine()) != null) {
                        line = line.trim();
                        if (line.length() > 0 && line.charAt(0) != '#') {
                            services.add(line);
                        }
                    }
                    input.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (services.size() > 0) {
                for (Object service1 : services) {
                    final String servicename = (String)service1;
                    if (servicename == null || "".equals(servicename)) {
                        continue;
                    }
                    InputStream fin = org.apache.axis2.java.security.AccessController
                            .doPrivileged(new PrivilegedAction<InputStream>() {
                                public InputStream run() {
                                    return moduleClassLoader
                                            .getResourceAsStream("aars/"
                                                                 + servicename);
                                }
                            });
                    if (fin == null) {
                        throw new AxisFault("No service archive found : "
                                            + servicename);
                    }
                    File inputFile = Utils
                            .createTempFile(
                                    servicename,
                                    fin,
                                    (File)axisConfig
                                            .getParameterValue(
                                                    Constants.Configuration.ARTIFACTS_TEMP_DIR));
                    DeploymentFileData filedata = new DeploymentFileData(
                            inputFile);

                    filedata
                            .setClassLoader(
                                    false,
                                    moduleClassLoader,
                                    (File)axisConfig
                                            .getParameterValue(
                                                    Constants.Configuration.ARTIFACTS_TEMP_DIR));
                    Map<String, AxisService> wsdlservice = archiveReader.processWSDLs(filedata);
                    if (wsdlservice != null && wsdlservice.size() > 0) {
                        Iterator<AxisService> servicesitr = wsdlservice.values().iterator();
                        while (servicesitr.hasNext()) {
                            AxisService service = servicesitr
                                    .next();
                            for(final AxisOperation axisOperation: service.getOperations()) {
                                axisOperation.setPhases(phasesInfo);
                            }
                        }
                    }
                    AxisServiceGroup serviceGroup = new AxisServiceGroup(
                            axisConfig);
                    serviceGroup.setServiceGroupClassLoader(filedata
                            .getClassLoader());
                    List<AxisService> serviceList = archiveReader.processServiceGroup(
                            filedata.getAbsolutePath(), filedata, serviceGroup,
                            false, wsdlservice, configCtx);
                    for (Object aServiceList : serviceList) {
                        AxisService axisService = (AxisService)aServiceList;
                        Parameter moduleService = new Parameter();
                        moduleService.setValue("true");
                        moduleService.setName(AxisModule.MODULE_SERVICE);
                        axisService.addParameter(moduleService);
                        serviceGroup.addService(axisService);
                    }
                    axisConfig.addServiceGroup(serviceGroup);
                    fin.close();
                }
            }
        } catch (IOException e) {
            throw AxisFault.makeFault(e);
        }
    }

    /**
     * Normalize a uri containing ../ and ./ paths.
     *
     * @param uri The uri path to normalize
     * @return The normalized uri
     */
    public static String normalize(String uri) {
        if ("".equals(uri)) {
            return uri;
        }
        int leadingSlashes;
        for (leadingSlashes = 0; leadingSlashes < uri.length()
                                 && uri.charAt(leadingSlashes) == '/'; ++leadingSlashes) {
            // FIXME: this block is empty!!
        }
        boolean isDir = (uri.charAt(uri.length() - 1) == '/');
        StringTokenizer st = new StringTokenizer(uri, "/");
        LinkedList<String> clean = new LinkedList<String>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if ("..".equals(token)) {
                if (!clean.isEmpty() && !"..".equals(clean.getLast())) {
                    clean.removeLast();
                    if (!st.hasMoreTokens()) {
                        isDir = true;
                    }
                } else {
                    clean.add("..");
                }
            } else if (!".".equals(token) && !"".equals(token)) {
                clean.add(token);
            }
        }
        StringBuffer sb = new StringBuffer();
        while (leadingSlashes-- > 0) {
            sb.append('/');
        }
        for (Iterator<String> it = clean.iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append('/');
            }
        }
        if (isDir && sb.length() > 0 && sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }

    public static String getPath(String parent, String childPath) {
        Stack<String> parentStack = new Stack<String>();
        Stack<String> childStack = new Stack<String>();
        if (parent != null) {
            String[] values = parent.split("/");
            if (values.length > 0) {
                for (String value : values) {
                    parentStack.push(value);
                }
            }
        }
        String[] values = childPath.split("/");
        if (values.length > 0) {
            for (String value : values) {
                childStack.push(value);
            }
        }
        String filepath = "";
        while (!childStack.isEmpty()) {
            String value = childStack.pop();
            if ("..".equals(value)) {
                parentStack.pop();
            } else if (!"".equals(value)) {
                if ("".equals(filepath)) {
                    filepath = value;
                } else {
                    filepath = value + "/" + filepath;
                }
            }
        }
        while (!parentStack.isEmpty()) {
            String value = parentStack.pop();
            if (!"".equals(value)) {
                filepath = value + "/" + filepath;
            }
        }
        return filepath;
    }

    /**
     * Get names of all *.jar files inside the lib/ directory of a given jar URL
     *
     * @param url base URL of a JAR to search
     * @return a List containing file names (Strings) of all files matching "[lL]ib/*.jar"
     */
    public static List<String> findLibJars(URL url) {
        List<String> embedded_jars = new ArrayList<String>();
        try {
            ZipInputStream zin = new ZipInputStream(url.openStream());
            ZipEntry entry;
            String entryName;
            while ((entry = zin.getNextEntry()) != null) {
                entryName = entry.getName();
                /**
                 * if the entry name start with /lib and ends with .jar add it
                 * to the the arraylist
                 */
                if (entryName != null
                    && (entryName.startsWith("lib/") || entryName
                        .startsWith("Lib/"))
                    && entryName.endsWith(".jar")) {
                    embedded_jars.add(entryName);
                }
            }
            zin.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return embedded_jars;
    }

    /**
     * Add the Axis2 lifecycle / session methods to a pre-existing list of names that will be
     * excluded when generating schemas.
     *
     * @param excludeList an ArrayList containing method names - we'll add ours to this.
     */
    public static void addExcludeMethods(List<String> excludeList) {
        excludeList.add("init");
        excludeList.add("setOperationContext");
        excludeList.add("startUp");
        excludeList.add("destroy");
        excludeList.add("shutDown");
    }

    public static DeploymentClassLoader createClassLoader(File serviceFile)
            throws MalformedURLException {
        ClassLoader contextClassLoader =
                org.apache.axis2.java.security.AccessController
		        .doPrivileged(new PrivilegedAction<ClassLoader>() {
		            public ClassLoader run() {
		                return Thread.currentThread().getContextClassLoader();
		            }
		        });
        return createDeploymentClassLoader(new URL[]{serviceFile.toURI().toURL()},
                                           contextClassLoader, new ArrayList<String>());
    }

    public static ClassLoader createClassLoader(List<URL> urls,
                                                ClassLoader serviceClassLoader,
                                                boolean extractJars,
                                                File tmpDir) {
        URL url = urls.get(0);
        if (extractJars) {
            try {
                URL[] urls1 = Utils.getURLsForAllJars(url, tmpDir);
                urls.remove(0);
                urls.addAll(0, Arrays.asList(urls1));
                URL[] urls2 = urls.toArray(new URL[urls.size()]);
                return createDeploymentClassLoader(urls2, serviceClassLoader,
                                                   null);
            } catch (Exception e) {
                log
                        .warn("Exception extracting jars into temporary directory : "
                              + e.getMessage()
                              + " : switching to alternate class loading mechanism");
                log.debug(e.getMessage(), e);
            }
        }
        List<String> embedded_jars = Utils.findLibJars(url);
        URL[] urls2 = urls.toArray(new URL[urls.size()]);
        return createDeploymentClassLoader(urls2, serviceClassLoader,
                                           embedded_jars);
    }

    public static File toFile(URL url) throws UnsupportedEncodingException {
        String path = URLDecoder.decode(url.getPath(), defaultEncoding);
        return new File(path.replace('/', File.separatorChar).replace('|', ':'));
    }

    public static ClassLoader createClassLoader(URL[] urls,
                                                ClassLoader serviceClassLoader,
                                                boolean extractJars,
                                                File tmpDir) {
        if (extractJars) {
            try {
                URL[] urls1 = Utils.getURLsForAllJars(urls[0], tmpDir);
                return createDeploymentClassLoader(urls1, serviceClassLoader,
                                                   null);
            } catch (Exception e) {
                log
                        .warn("Exception extracting jars into temporary directory : "
                              + e.getMessage()
                              + " : switching to alternate class loading mechanism");
                log.debug(e.getMessage(), e);
            }
        }
        List<String> embedded_jars = Utils.findLibJars(urls[0]);
        return createDeploymentClassLoader(urls, serviceClassLoader,
                                           embedded_jars);
    }

    private static DeploymentClassLoader createDeploymentClassLoader(
            final URL[] urls, final ClassLoader serviceClassLoader,
            final List<String> embeddedJars) {
        return AccessController
                .doPrivileged(new PrivilegedAction<DeploymentClassLoader>() {
                    public DeploymentClassLoader run() {
                        return new DeploymentClassLoader(urls, embeddedJars,
                                                         serviceClassLoader);
                    }
                });
    }

    /**
     * This method is to process bean exclude parameter and the XML format of
     * that would be
     * <parameter name="beanPropertyRules">
     *   <bean class="full qualified class name"
     *         excludeProperties="name,age" />
     * </parameter>
     *
     * @param service , AxisService object
     */
    public static void processBeanPropertyExclude(AxisService service) {
        Parameter excludeBeanProperty = service
                .getParameter("beanPropertyRules");
        if (excludeBeanProperty != null) {
        	Object obj = excludeBeanProperty.getValue();
        	if(obj instanceof OMElement) {
        		OMElement parameterElement = (OMElement)obj;
        		ExcludeInfo excludeInfo = new ExcludeInfo();
        		for(OMElement bean: parameterElement.getChildrenWithName(new QName("bean"))) {
        			String clazz = bean.getAttributeValue(new QName(
        				DeploymentConstants.TAG_CLASS_NAME));
        			String excludePropertees = bean.getAttributeValue(new QName(
                        DeploymentConstants.TAG_EXCLUDE_PROPERTIES));
        			String includeProperties = bean.getAttributeValue(new QName(
                        DeploymentConstants.TAG_INCLUDE_PROPERTIES));
        			excludeInfo.putBeanInfo(clazz, new BeanExcludeInfo(
                        excludePropertees, includeProperties));
        		}
        		service.setExcludeInfo(excludeInfo);
            }
        }
    }

    public static String getShortFileName(String filename) {
        File file = new File(filename);
        return file.getName();
    }

    /**
     * The util method to prepare the JSR 181 annotated service name from given annotation or for
     * defaults JSR 181 specifies that the in javax.jws.WebService the parameter serviceName
     * contains the wsdl:service name to mapp. If its not available then the default will be Simple
     * name of the class + "Service"
     *
     * @param serviceClass the service Class
     * @param serviceAnnotation a WebService annotation, or null
     * @return String version of the ServiceName according to the JSR 181 spec
     */
    public static String getAnnotatedServiceName(Class<?> serviceClass, WebService serviceAnnotation) {
        String serviceName = "";
        if (serviceAnnotation != null && serviceAnnotation.serviceName() != null) {
            serviceName = serviceAnnotation.serviceName();
        }
        if (serviceName.equals("")) {
            serviceName = serviceClass.getName();
            int firstChar = serviceName.lastIndexOf('.') + 1;
            if (firstChar > 0) {
                serviceName = serviceName.substring(firstChar);
            }
            serviceName += "Service";
        }
        return serviceName;
    }

    public static void addEndpointsToService(AxisService axisService)
            throws AxisFault {

        String serviceName = axisService.getName();
        Iterator<String> transportInValues = null;

        if (axisService.isEnableAllTransports()) {
            AxisConfiguration axisConfiguration = axisService
                    .getAxisConfiguration();
            if (axisConfiguration != null) {
                List<String> transports = new ArrayList<String>();
                for (Object o : axisConfiguration.getTransportsIn().values()) {
                    TransportInDescription transportInDescription = (TransportInDescription)o;
                    transports.add(transportInDescription.getName());
                }
                transportInValues = transports.iterator();
            }
        } else {
            transportInValues = axisService.getExposedTransports().iterator();
        }

        Map<QName, AxisBinding> bindingCache = new HashMap<QName, AxisBinding>();

        if (transportInValues != null) {
            for (; transportInValues.hasNext();) {
                String transportName = transportInValues.next();
                String protocol = transportName.substring(0, 1).toUpperCase()
                                  + transportName.substring(1, transportName.length())
                        .toLowerCase();

                //TODO do we use this method , we need to disable Http, SOAP11,SOAP12
                // Bindings according to parameters if we are using this
                /*
                     * populates soap11 endpoint
                     */
                String soap11EndpointName = serviceName + protocol
                                            + "Soap11Endpoint";

                AxisEndpoint httpSoap11Endpoint = new AxisEndpoint();
                httpSoap11Endpoint.setName(soap11EndpointName);
                httpSoap11Endpoint.setParent(axisService);
                httpSoap11Endpoint.setTransportInDescription(transportName);
                populateSoap11Endpoint(axisService, httpSoap11Endpoint,
                                       bindingCache);
                axisService.addEndpoint(httpSoap11Endpoint.getName(),
                                        httpSoap11Endpoint);
                // setting soap11 endpoint as the default endpoint
                axisService.setEndpointName(soap11EndpointName);

                /*
                     * generating Soap12 endpoint
                     */
                String soap12EndpointName = serviceName + protocol
                                            + "Soap12Endpoint";
                AxisEndpoint httpSoap12Endpoint = new AxisEndpoint();
                httpSoap12Endpoint.setName(soap12EndpointName);
                httpSoap12Endpoint.setParent(axisService);
                httpSoap12Endpoint.setTransportInDescription(transportName);
                populateSoap12Endpoint(axisService, httpSoap12Endpoint,
                                       bindingCache);
                axisService.addEndpoint(httpSoap12Endpoint.getName(),
                                        httpSoap12Endpoint);

                /*
                     * generating Http endpoint
                     */
                if ("http".equals(transportName)) {
                    String httpEndpointName = serviceName + protocol
                                              + "Endpoint";
                    AxisEndpoint httpEndpoint = new AxisEndpoint();
                    httpEndpoint.setName(httpEndpointName);
                    httpEndpoint.setParent(axisService);
                    httpEndpoint.setTransportInDescription(transportName);
                    populateHttpEndpoint(axisService, httpEndpoint, bindingCache);
                    axisService.addEndpoint(httpEndpoint.getName(),
                                            httpEndpoint);
                }
            }
        }
    }

    public static void addEndpointsToService(AxisService axisService,
                                             AxisConfiguration axisConfiguration) throws AxisFault {

        String serviceName = axisService.getName();
        Iterator<String> transportInValues = null;

        if (axisConfiguration != null) {
            List<String> transports = new ArrayList<String>();
            for (Object o : axisConfiguration.getTransportsIn().values()) {
                TransportInDescription transportInDescription = (TransportInDescription)o;
                transports.add(transportInDescription.getName());
            }
            transportInValues = transports.iterator();
        }

        Map<QName, AxisBinding> bindingCache = new HashMap<QName, AxisBinding>();
        if (transportInValues != null) {
            for (; transportInValues.hasNext();) {
                String transportName = transportInValues.next();
                String protocol = transportName.substring(0, 1).toUpperCase()
                                  + transportName.substring(1, transportName.length())
                        .toLowerCase();

                // axis2.xml indicated no HTTP binding?
                boolean disableREST = false;
                Parameter disableRESTParameter = axisService
                        .getParameter(org.apache.axis2.Constants.Configuration.DISABLE_REST);
                if (disableRESTParameter != null
                    && JavaUtils.isTrueExplicitly(disableRESTParameter.getValue())) {
                    disableREST = true;
                }

                boolean disableSOAP11 = false;
                Parameter disableSOAP11Parameter = axisService
                        .getParameter(org.apache.axis2.Constants.Configuration.DISABLE_SOAP11);
                if (disableSOAP11Parameter != null
                    && JavaUtils.isTrueExplicitly(disableSOAP11Parameter.getValue())) {
                    disableSOAP11 = true;
                }

                boolean disableSOAP12 = false;
                Parameter disableSOAP12Parameter = axisService
                        .getParameter(org.apache.axis2.Constants.Configuration.DISABLE_SOAP12);
                if (disableSOAP12Parameter != null
                    && JavaUtils
                        .isTrueExplicitly(disableSOAP12Parameter.getValue())) {
                    disableSOAP12 = true;
                }

                /*
                     * populates soap11 endpoint
                     */
                if (!disableSOAP11) {
                    String soap11EndpointName = serviceName + protocol
                                                + "Soap11Endpoint";

                    AxisEndpoint httpSoap11Endpoint = new AxisEndpoint();
                    httpSoap11Endpoint.setName(soap11EndpointName);
                    httpSoap11Endpoint.setParent(axisService);
                    httpSoap11Endpoint.setTransportInDescription(transportName);
                    populateSoap11Endpoint(axisService, httpSoap11Endpoint,
                                           bindingCache);
                    axisService.addEndpoint(httpSoap11Endpoint.getName(),
                                            httpSoap11Endpoint);
                    // setting soap11 endpoint as the default endpoint
                    axisService.setEndpointName(soap11EndpointName);
                }

                /*
                     * generating Soap12 endpoint
                     */
                if (!disableSOAP12) {
                    String soap12EndpointName = serviceName + protocol
                                                + "Soap12Endpoint";
                    AxisEndpoint httpSoap12Endpoint = new AxisEndpoint();
                    httpSoap12Endpoint.setName(soap12EndpointName);
                    httpSoap12Endpoint.setParent(axisService);
                    httpSoap12Endpoint.setTransportInDescription(transportName);
                    populateSoap12Endpoint(axisService, httpSoap12Endpoint,
                                           bindingCache);
                    axisService.addEndpoint(httpSoap12Endpoint.getName(),
                                            httpSoap12Endpoint);
                }

                /*
                     * generating Http endpoint
                     */
                if (("http".equals(transportName)
                     || "https".equals(transportName)) && !disableREST) {
                    String httpEndpointName = serviceName + protocol
                                              + "Endpoint";
                    AxisEndpoint httpEndpoint = new AxisEndpoint();
                    httpEndpoint.setName(httpEndpointName);
                    httpEndpoint.setParent(axisService);
                    httpEndpoint.setTransportInDescription(transportName);
                    populateHttpEndpoint(axisService, httpEndpoint, bindingCache);
                    axisService.addEndpoint(httpEndpoint.getName(),
                                            httpEndpoint);
                }
            }
        }
    }

    public static void addSoap11Endpoint(AxisService axisService, URL url)
            throws Exception {
        String protocol = url.getProtocol();
        protocol = protocol.substring(0, 1).toUpperCase()
                   + protocol.substring(1, protocol.length()).toLowerCase();

        String serviceName = axisService.getName();
        String soap11EndpointName = serviceName + protocol + "Soap11Endpoint";

        AxisEndpoint httpSoap11Endpoint = new AxisEndpoint();
        httpSoap11Endpoint.setName(soap11EndpointName);
        httpSoap11Endpoint.setParent(axisService);
        httpSoap11Endpoint.setEndpointURL(url.toString());
        httpSoap11Endpoint.setTransportInDescription(url.getProtocol());

        populateSoap11Endpoint(axisService, httpSoap11Endpoint, null);
        axisService.addEndpoint(httpSoap11Endpoint.getName(),
                                httpSoap11Endpoint);
        // setting soap11 endpoint as the default endpoint
        axisService.setEndpointName(soap11EndpointName);
    }

    public static void addSoap12Endpoint(AxisService axisService, URL url)
            throws Exception {
        String protocol = url.getProtocol();
        protocol = protocol.substring(0, 1).toUpperCase()
                   + protocol.substring(1, protocol.length()).toLowerCase();

        String serviceName = axisService.getName();
        String soap12EndpointName = serviceName + protocol + "Soap12Endpoint";

        AxisEndpoint httpSoap12Endpoint = new AxisEndpoint();
        httpSoap12Endpoint.setName(soap12EndpointName);
        httpSoap12Endpoint.setParent(axisService);
        httpSoap12Endpoint.setEndpointURL(url.toString());
        httpSoap12Endpoint.setTransportInDescription(url.getProtocol());

        populateSoap12Endpoint(axisService, httpSoap12Endpoint, null);
        axisService.addEndpoint(httpSoap12Endpoint.getName(),
                                httpSoap12Endpoint);
    }

    public static void addHttpEndpoint(AxisService axisService, URL url) {
        String serviceName = axisService.getName();
        String protocol = url.getProtocol();
        protocol = protocol.substring(0, 1).toUpperCase()
                   + protocol.substring(1, protocol.length()).toLowerCase();

        String httpEndpointName = serviceName + protocol + "Endpoint";
        AxisEndpoint httpEndpoint = new AxisEndpoint();
        httpEndpoint.setName(httpEndpointName);
        httpEndpoint.setParent(axisService);
        httpEndpoint.setEndpointURL(url.toString());
        httpEndpoint.setTransportInDescription(url.getProtocol());
        populateHttpEndpoint(axisService, httpEndpoint, null);
        axisService.addEndpoint(httpEndpoint.getName(), httpEndpoint);
    }

    public static void processPolicyAttachments(Iterable<OMElement> attachmentElements,
                                                AxisService service) throws XMLStreamException,
            FactoryConfigurationError {

        Map<String, List<PolicyComponent>> attachmentsMap
        	= new HashMap<String, List<PolicyComponent>>();

        for(OMElement attachmentElement: attachmentElements) {
            OMElement appliesToElem = attachmentElement
                    .getFirstChildWithName(new QName(
                            DeploymentConstants.POLICY_NS_URI,
                            DeploymentConstants.TAG_APPLIES_TO));
            List<PolicyComponent> policyComponents = new ArrayList<PolicyComponent>();

            // process <wsp:Policy> elements ..
            for (OMElement elem: attachmentElement.getChildrenWithName(
            		new QName( DeploymentConstants.POLICY_NS_URI,
                               DeploymentConstants.TAG_POLICY))) {
                PolicyComponent policy = PolicyUtil.getPolicyFromOMElement(elem);
                policyComponents.add(policy);
            }

            // process <wsp:PolicyReference> elements ..
            for (OMElement elem: attachmentElement.getChildrenWithName(
            		new QName( DeploymentConstants.POLICY_NS_URI,
            				   DeploymentConstants.TAG_POLICY_REF))) {
                PolicyComponent policyRef = PolicyUtil.getPolicyReferenceFromOMElement(elem);
                policyComponents.add(policyRef);
            }

            for (OMElement policySubject: appliesToElem.getChildrenWithName(new QName("policy-subject"))) {
                String identifier = policySubject.getAttributeValue(new QName("identifier"));

                List<PolicyComponent> values = attachmentsMap.get(identifier);
                if (values == null) {
                    values = new ArrayList<PolicyComponent>();
                    attachmentsMap.put(identifier, values);
                }
                values.addAll(policyComponents);
            }
        }

        for (Object o : attachmentsMap.keySet()) {
            String identifier = (String)o;
            if (identifier.startsWith("binding:soap")) {
                processSoapAttachments(identifier, attachmentsMap
                        .get(identifier), service);
            }
        }
    }

    private static void populateSoap11Endpoint(AxisService axisService,
                                               AxisEndpoint axisEndpoint,
                                               Map<QName, AxisBinding> bindingCache) {
        String serviceName = axisService.getName();

        QName bindingName = new QName(serviceName + "Soap11Binding");

        AxisBinding axisBinding = (bindingCache != null) ? (AxisBinding)bindingCache
                .get(bindingName)
                : null;
        if (axisBinding == null) {
            axisBinding = new AxisBinding();
            axisBinding.setName(bindingName);

            axisBinding.setType(Java2WSDLConstants.TRANSPORT_URI);
            axisBinding.setProperty(WSDLConstants.WSDL_1_1_STYLE,
                                    WSDLConstants.STYLE_DOC);

            axisBinding.setProperty(WSDL2Constants.ATTR_WSOAP_VERSION,
                                    SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);

            for (final AxisDescription desc: axisService.getChildren()) {
                final AxisOperation operation = (AxisOperation)desc;
                final AxisBindingOperation axisBindingOperation
                	= new AxisBindingOperation();

                axisBindingOperation.setName(operation.getName());
                axisBindingOperation.setAxisOperation(operation);

                String soapAction = operation.getSoapAction();
                if (soapAction != null) {
                    axisBindingOperation.setProperty(
                            WSDL2Constants.ATTR_WSOAP_ACTION, soapAction);
                }
                axisBinding.addChild(axisBindingOperation.getName(),
                                     axisBindingOperation);
                populateBindingOperation(axisBinding,
                                         axisBindingOperation);
            }
            if (bindingCache != null) {
                bindingCache.put(bindingName, axisBinding);
            }
        }
        axisBinding.setParent(axisEndpoint);
        axisEndpoint.setBinding(axisBinding);
    }

    private static void populateSoap12Endpoint(AxisService axisService,
                                               AxisEndpoint axisEndpoint,
                                               Map<QName, AxisBinding> bindingCache) {
        String serviceName = axisService.getName();
        QName bindingName = new QName(serviceName + "Soap12Binding");

        AxisBinding axisBinding = (bindingCache != null) ? bindingCache.get(bindingName)
        												 : null;
        if (axisBinding == null) {
            axisBinding = new AxisBinding();
            axisBinding.setName(bindingName);

            axisBinding.setType(Java2WSDLConstants.TRANSPORT_URI);
            axisBinding.setProperty(WSDLConstants.WSDL_1_1_STYLE,
                                    WSDLConstants.STYLE_DOC);

            axisBinding.setProperty(WSDL2Constants.ATTR_WSOAP_VERSION,
                                    SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);

            for (final AxisDescription desc: axisService.getChildren()) {
                final AxisOperation operation = (AxisOperation)desc;
                final AxisBindingOperation axisBindingOperation
                	= new AxisBindingOperation();

                axisBindingOperation.setName(operation.getName());
                axisBindingOperation.setAxisOperation(operation);

                String soapAction = operation.getSoapAction();
                if (soapAction != null) {
                    axisBindingOperation.setProperty(
                            WSDL2Constants.ATTR_WSOAP_ACTION, soapAction);
                }
                axisBinding.addChild(axisBindingOperation.getName(),
                                     axisBindingOperation);

                populateBindingOperation(axisBinding,
                                         axisBindingOperation);
            }
            if (bindingCache != null) {
                bindingCache.put(bindingName, axisBinding);
            }
        }
        axisBinding.setParent(axisEndpoint);
        axisEndpoint.setBinding(axisBinding);
    }

    private static void populateHttpEndpoint(AxisService axisService,
                                             AxisEndpoint axisEndpoint,
                                             Map<QName, AxisBinding> bindingCache) {
        String serviceName = axisService.getName();
        QName bindingName = new QName(serviceName + "HttpBinding");

        AxisBinding axisBinding = (bindingCache != null) ? bindingCache.get(bindingName)
                										 : null;

        if (axisBinding == null) {
            axisBinding = new AxisBinding();
            axisBinding.setName(bindingName);

            axisBinding.setType(WSDL2Constants.URI_WSDL2_HTTP);
            axisBinding.setProperty(WSDL2Constants.ATTR_WHTTP_METHOD, "POST");

            for (final AxisDescription desc: axisService.getChildren()) {
                final AxisOperation operation = (AxisOperation)desc;
                final AxisBindingOperation axisBindingOperation
                	= new AxisBindingOperation();

                QName operationQName = operation.getName();
                axisBindingOperation.setName(operationQName);
                axisBindingOperation.setAxisOperation(operation);
                String httpLocation = operationQName.getLocalPart();
                axisBindingOperation.setProperty(WSDL2Constants.ATTR_WHTTP_LOCATION, httpLocation);
                axisBinding.addChild(axisBindingOperation.getName(),
                                     axisBindingOperation);

                populateBindingOperation(axisBinding,
                                         axisBindingOperation);
            }
            if (bindingCache != null) {
                bindingCache.put(bindingName, axisBinding);
            }
        }
        axisBinding.setParent(axisEndpoint);
        axisEndpoint.setBinding(axisBinding);
    }

    private static void populateBindingOperation(AxisBinding axisBinding,
                                                 AxisBindingOperation axisBindingOperation) {

        AxisOperation axisOperation = axisBindingOperation.getAxisOperation();

        if (WSDLUtil.isInputPresentForMEP(axisOperation
                .getMessageExchangePattern())) {
            AxisMessage axisInMessage = axisOperation
                    .getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            AxisBindingMessage axisBindingInMessage = new AxisBindingMessage();

            axisBindingInMessage.setName(axisInMessage.getName());
            axisBindingInMessage.setDirection(axisInMessage.getDirection());
            axisBindingInMessage.setAxisMessage(axisInMessage);

            axisBindingInMessage.setParent(axisBindingOperation);
            axisBindingOperation.addChild(WSDLConstants.MESSAGE_LABEL_IN_VALUE,
                                          axisBindingInMessage);
        }

        if (WSDLUtil.isOutputPresentForMEP(axisOperation
                .getMessageExchangePattern())) {
            AxisMessage axisOutMessage = axisOperation
                    .getMessage(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
            AxisBindingMessage axisBindingOutMessage = new AxisBindingMessage();

            axisBindingOutMessage.setName(axisOutMessage.getName());
            axisBindingOutMessage.setDirection(axisOutMessage.getDirection());
            axisBindingOutMessage.setAxisMessage(axisOutMessage);

            axisBindingOutMessage.setParent(axisBindingOperation);
            axisBindingOperation.addChild(
                    WSDLConstants.MESSAGE_LABEL_OUT_VALUE,
                    axisBindingOutMessage);
        }

        List<AxisMessage> faultMessagesList = axisOperation.getFaultMessages();
        for (Object aFaultMessagesList : faultMessagesList) {
            AxisMessage axisFaultMessage = (AxisMessage)aFaultMessagesList;
            AxisBindingMessage axisBindingFaultMessage = new AxisBindingMessage();
            axisBindingFaultMessage.setName(axisFaultMessage.getName());
            axisBindingFaultMessage.setFault(true);
            axisBindingFaultMessage.setAxisMessage(axisFaultMessage);
            axisBindingFaultMessage.setParent(axisBindingOperation);
            axisBindingOperation.addFault(axisBindingFaultMessage);
            axisBinding.addFault(axisBindingFaultMessage);
        }

        axisBindingOperation.setAxisOperation(axisOperation);
        axisBindingOperation.setParent(axisBinding);
    }

    private static void processSoapAttachments(String identifier,
                                               List<PolicyComponent> policyComponents,
                                               AxisService service) {
        Map<String, AxisEndpoint> map = service.getEndpoints();
        String soapVersion =
                (identifier.indexOf("soap12") > -1) ? SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI
                        : SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI;

        for (Object o : map.values()) {
            AxisEndpoint axisEndpoint = (AxisEndpoint)o;
            AxisBinding axisBinding = axisEndpoint.getBinding();
            String wsoap = (String)axisBinding
                    .getProperty(WSDL2Constants.ATTR_WSOAP_VERSION);
            if (soapVersion.equals(wsoap)) {
                String[] identifiers = identifier.split("/");
                int key = identifiers.length;
                if (key == 1) {
                    axisBinding.getPolicySubject().attachPolicyComponents(
                            policyComponents);
                } else if (key == 2 || key == 3) {
                    String opName = identifiers[1];
                    opName = opName.substring(opName.indexOf(":") + 1, opName
                            .length());
                    AxisBindingOperation bindingOperation = null;
                    boolean found = false;
                    for(final AxisBindingOperation operation: axisBinding.getChildren()) {
                        if (opName.equals(operation.getName().getLocalPart())) {
                            found = true;
                            bindingOperation = operation;
                            break;
                        }
                    }
                    if (!found) {
                        throw new IllegalArgumentException(
                                "Invalid binding operation " + opName);
                    }

                    if (key == 2) {
                        bindingOperation.getPolicySubject()
                                .attachPolicyComponents(policyComponents);
                    } else {
                        if ("in".equals(identifiers[2])) {
                            AxisBindingMessage bindingInMessage =
                                    (AxisBindingMessage)bindingOperation
                                            .getChild(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
                            bindingInMessage.getPolicySubject()
                                    .attachPolicyComponents(policyComponents);

                        } else if ("out".equals(identifiers[2])) {
                            AxisBindingMessage bindingOutMessage =
                                    (AxisBindingMessage)bindingOperation
                                            .getChild(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
                            bindingOutMessage.getPolicySubject()
                                    .attachPolicyComponents(policyComponents);
                        } else {
                            // FIXME faults
                        }
                    }
                }
                break;
            }
        }
    }

    public static boolean isSoap11Binding(AxisBinding binding) {
        String type = binding.getType();
        if (Java2WSDLConstants.TRANSPORT_URI.equals(type)
            || WSDL2Constants.URI_WSDL2_SOAP.equals(type)) {
            String v = (String)binding
                    .getProperty(WSDL2Constants.ATTR_WSOAP_VERSION);
            if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(v)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSoap12Binding(AxisBinding binding) {
        String type = binding.getType();
        if (Java2WSDLConstants.TRANSPORT_URI.equals(type)
            || WSDL2Constants.URI_WSDL2_SOAP.equals(type)) {
            String v = (String)binding
                    .getProperty(WSDL2Constants.ATTR_WSOAP_VERSION);
            if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(v)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHttpBinding(AxisBinding binding) {
        String type = binding.getType();
        return WSDL2Constants.URI_WSDL2_HTTP.equals(type);
    }

    public static AxisBinding getSoap11Binding(AxisService service) {
        for (Object o : service.getEndpoints().values()) {
            AxisEndpoint endpoint = (AxisEndpoint)o;
            AxisBinding binding = endpoint.getBinding();

            if (isSoap11Binding(binding)) {
                return binding;
            }
        }
        return null;
    }

    public static AxisBinding getSoap12Binding(AxisService service) {
        for (Object o : service.getEndpoints().values()) {
            AxisEndpoint endpoint = (AxisEndpoint)o;
            AxisBinding binding = endpoint.getBinding();

            if (isSoap12Binding(binding)) {
                return binding;
            }
        }
        return null;
    }

    public static AxisBinding getHttpBinding(AxisService service) {
        for (Object o : service.getEndpoints().values()) {
            AxisEndpoint endpoint = (AxisEndpoint)o;
            AxisBinding binding = endpoint.getBinding();

            if (isHttpBinding(binding)) {
                return binding;
            }
        }
        return null;
    }

    public static AxisBindingOperation getBindingOperation(AxisBinding binding,
                                                           AxisOperation operation) {
        QName opName = operation.getName();
        for (final AxisBindingOperation bindingOp: binding.getChildren()) {
            if (opName.equals(bindingOp.getName())) {
                return bindingOp;
            }
        }
        return null;
    }

    public static AxisBindingMessage getBindingMessage(AxisBindingOperation bindingOperation,
                                                       AxisMessage message) {
        String msgName = message.getName();
        for (final AxisDescription desc: bindingOperation.getChildren()) {
            AxisBindingMessage bindingMessage = (AxisBindingMessage)desc;
            if (msgName.equals(bindingMessage.getName())) {
                return bindingMessage;
            }
        }
        return null;
    }
}
