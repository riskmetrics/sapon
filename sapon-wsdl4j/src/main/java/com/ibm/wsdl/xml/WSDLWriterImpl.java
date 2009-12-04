/*
 * (c) Copyright IBM Corp 2001, 2006
 */

package com.ibm.wsdl.xml;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Import;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.AttributeExtensible;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.ibm.wsdl.Constants;
import com.ibm.wsdl.util.StringUtils;
import com.ibm.wsdl.util.xml.DOM2Writer;
import com.ibm.wsdl.util.xml.DOMUtils;

/**
 * This class describes a collection of methods
 * that allow a WSDL model to be written to a writer
 * in an XML format that follows the WSDL schema.
 *
 * @author Matthew J. Duftler
 * @author Nirmal Mukhi
 */
public class WSDLWriterImpl implements WSDLWriter
{
  /**
   * Sets the specified feature to the specified value.
   * <p>
   * There are no minimum features that must be supported.
   * <p>
   * All feature names must be fully-qualified, Java package style. All
   * names starting with javax.wsdl. are reserved for features defined
   * by the JWSDL specification. It is recommended that implementation-
   * specific features be fully-qualified to match the package name
   * of that implementation. For example: com.abc.featureName
   *
   * @param name the name of the feature to be set.
   * @param value the value to set the feature to.
   * @throws IllegalArgumentException if the feature name is not recognized.
   * @see #getFeature(String)
   */
  public void setFeature(String name, boolean value)
    throws IllegalArgumentException
  {
    if (name == null)
    {
      throw new IllegalArgumentException("Feature name must not be null.");
    }
    else
    {
      throw new IllegalArgumentException("Feature name '" + name +
                                         "' not recognized.");
    }
  }

  /**
   * Gets the value of the specified feature.
   *
   * @param name the name of the feature to get the value of.
   * @return the value of the feature.
   * @throws IllegalArgumentException if the feature name is not recognized.
   * @see #setFeature(String, boolean)
   */
  public boolean getFeature(String name) throws IllegalArgumentException
  {
    if (name == null)
    {
      throw new IllegalArgumentException("Feature name must not be null.");
    }
    else
    {
      throw new IllegalArgumentException("Feature name '" + name +
                                         "' not recognized.");
    }
  }

  protected void printDefinition(Definition def, PrintWriter pw)
    throws WSDLException
  {
    if (def == null)
    {
      return;
    }

    if (def.getPrefix(Constants.NS_URI_WSDL) == null)
    {
      String prefix = "wsdl";
      int subscript = 0;

      while (def.getNamespace(prefix) != null)
      {
        prefix = "wsdl" + subscript++;
      }

      def.addNamespace(prefix, Constants.NS_URI_WSDL);
    }

    String tagName =
      DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                 Constants.ELEM_DEFINITIONS,
                                 def);

    pw.print('<' + tagName);

    QName name = def.getQName();
    String targetNamespace = def.getTargetNamespace();
    Map<String, String> namespaces = def.getNamespaces();

    if (name != null)
    {
      DOMUtils.printAttribute(Constants.ATTR_NAME, name.getLocalPart(), pw);
    }

    DOMUtils.printAttribute(Constants.ATTR_TARGET_NAMESPACE,
                            targetNamespace,
                            pw);

    printExtensibilityAttributes(Definition.class, def, def, pw);

    printNamespaceDeclarations(namespaces, pw);

    pw.println('>');

    printDocumentation(def.getDocumentationElement(), def, pw);
    printImports(def.getImports(), def, pw);
    printTypes(def.getTypes(), def, pw);
    printMessages(def.getMessages(), def, pw);
    printPortTypes(def.getPortTypes(), def, pw);
    printBindings(def.getBindings(), def, pw);
    printServices(def.getServices(), def, pw);

    List<ExtensibilityElement> extElements = def.getExtensibilityElements();

    printExtensibilityElements(Definition.class, extElements, def, pw);

    pw.println("</" + tagName + '>');

    pw.flush();
  }

  protected void printServices(Map<?, Service> services,
                               Definition def,
                               PrintWriter pw)
                                 throws WSDLException
  {
    if (services != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_SERVICE,
                                   def);

      for(Service service: services.values()) {

        pw.print("  <" + tagName);

        QName name = service.getQName();

        if (name != null)
        {
          DOMUtils.printAttribute(Constants.ATTR_NAME,
                                  name.getLocalPart(),
                                  pw);
        }

        printExtensibilityAttributes(Service.class, service, def, pw);

        pw.println('>');

        printDocumentation(service.getDocumentationElement(), def, pw);
        printPorts(service.getPorts(), def, pw);

        List<ExtensibilityElement> extElements = service.getExtensibilityElements();

        printExtensibilityElements(Service.class, extElements, def, pw);

        pw.println("  </" + tagName + '>');
      }
    }
  }

  protected void printPorts(Map<?, Port> ports, Definition def, PrintWriter pw)
    throws WSDLException
  {
    if (ports != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_PORT,
                                   def);

      for(Port port: ports.values()) {

        pw.print("    <" + tagName);

        DOMUtils.printAttribute(Constants.ATTR_NAME, port.getName(), pw);

        Binding binding = port.getBinding();

        if (binding != null)
        {
          DOMUtils.printQualifiedAttribute(Constants.ATTR_BINDING,
                                           binding.getQName(),
                                           def,
                                           pw);
        }

        printExtensibilityAttributes(Port.class, port, def, pw);

        pw.println('>');

        printDocumentation(port.getDocumentationElement(), def, pw);

        List<ExtensibilityElement> extElements = port.getExtensibilityElements();

        printExtensibilityElements(Port.class, extElements, def, pw);

        pw.println("    </" + tagName + '>');
      }
    }
  }

  protected void printBindings(Map<?, Binding> bindings,
                               Definition def,
                               PrintWriter pw)
                                 throws WSDLException
  {
    if (bindings != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_BINDING,
                                   def);
      for(Binding binding: bindings.values()) {

        if (!binding.isUndefined())
        {
          pw.print("  <" + tagName);

          QName name = binding.getQName();

          if (name != null)
          {
            DOMUtils.printAttribute(Constants.ATTR_NAME,
                                    name.getLocalPart(),
                                    pw);
          }

          PortType portType = binding.getPortType();

          if (portType != null)
          {
            DOMUtils.printQualifiedAttribute(Constants.ATTR_TYPE,
                                             portType.getQName(),
                                             def,
                                             pw);
          }

          pw.println('>');

          printDocumentation(binding.getDocumentationElement(), def, pw);

          List<ExtensibilityElement> extElements = binding.getExtensibilityElements();

          printExtensibilityElements(Binding.class, extElements, def, pw);

          printBindingOperations(binding.getBindingOperations(), def, pw);

          pw.println("  </" + tagName + '>');
        }
      }
    }
  }

  protected void printBindingOperations(List<BindingOperation> bindingOperations,
                                        Definition def,
                                        PrintWriter pw)
                                          throws WSDLException
  {
    if (bindingOperations != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_OPERATION,
                                   def);
      for(BindingOperation bindingOperation: bindingOperations) {

        pw.print("    <" + tagName);

        DOMUtils.printAttribute(Constants.ATTR_NAME,
                                bindingOperation.getName(),
                                pw);

        printExtensibilityAttributes(BindingOperation.class, bindingOperation, def, pw);

        pw.println('>');

        printDocumentation(bindingOperation.getDocumentationElement(), def, pw);

        List<ExtensibilityElement> extElements = bindingOperation.getExtensibilityElements();

        printExtensibilityElements(BindingOperation.class,
                                   extElements,
                                   def,
                                   pw);

        printBindingInput(bindingOperation.getBindingInput(), def, pw);
        printBindingOutput(bindingOperation.getBindingOutput(), def, pw);
        printBindingFaults(bindingOperation.getBindingFaults(), def, pw);

        pw.println("    </" + tagName + '>');
      }
    }
  }

  protected void printBindingInput(BindingInput bindingInput,
                                   Definition def,
                                   PrintWriter pw)
                                     throws WSDLException
  {
    if (bindingInput != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_INPUT,
                                   def);

      pw.print("      <" + tagName);

      DOMUtils.printAttribute(Constants.ATTR_NAME,
                              bindingInput.getName(),
                              pw);

      printExtensibilityAttributes(BindingInput.class, bindingInput, def, pw);

      pw.println('>');

      printDocumentation(bindingInput.getDocumentationElement(), def, pw);

      List<ExtensibilityElement> extElements = bindingInput.getExtensibilityElements();

      printExtensibilityElements(BindingInput.class, extElements, def, pw);

      pw.println("      </" + tagName + '>');
    }
  }

  protected void printBindingOutput(BindingOutput bindingOutput,
                                    Definition def,
                                    PrintWriter pw)
                                      throws WSDLException
  {
    if (bindingOutput != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_OUTPUT,
                                   def);

      pw.print("      <" + tagName);

      DOMUtils.printAttribute(Constants.ATTR_NAME,
                              bindingOutput.getName(),
                              pw);

      pw.println('>');

      printDocumentation(bindingOutput.getDocumentationElement(), def, pw);

      List<ExtensibilityElement> extElements = bindingOutput.getExtensibilityElements();

      printExtensibilityElements(BindingOutput.class, extElements, def, pw);

      pw.println("      </" + tagName + '>');
    }
  }

  protected void printBindingFaults(Map<?, BindingFault> bindingFaults,
                                    Definition def,
                                    PrintWriter pw)
                                      throws WSDLException
  {
    if (bindingFaults != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_FAULT,
                                   def);
      for(BindingFault bindingFault: bindingFaults.values()) {

        pw.print("      <" + tagName);

        DOMUtils.printAttribute(Constants.ATTR_NAME,
                                bindingFault.getName(),
                                pw);

        printExtensibilityAttributes(BindingFault.class, bindingFault, def, pw);

        pw.println('>');

        printDocumentation(bindingFault.getDocumentationElement(), def, pw);

        List<ExtensibilityElement> extElements = bindingFault.getExtensibilityElements();

        printExtensibilityElements(BindingFault.class, extElements, def, pw);

        pw.println("      </" + tagName + '>');
      }
    }
  }

  protected void printPortTypes(Map<?, PortType> portTypes,
                                Definition def,
                                PrintWriter pw)
                                  throws WSDLException
  {
    if (portTypes != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_PORT_TYPE,
                                   def);

      for(PortType portType: portTypes.values()) {

        if (!portType.isUndefined())
        {
          pw.print("  <" + tagName);

          QName name = portType.getQName();

          if (name != null)
          {
            DOMUtils.printAttribute(Constants.ATTR_NAME,
                                    name.getLocalPart(),
                                    pw);
          }

          printExtensibilityAttributes(PortType.class, portType, def, pw);

          pw.println('>');

          printDocumentation(portType.getDocumentationElement(), def, pw);
          printOperations(portType.getOperations(), def, pw);

          List<ExtensibilityElement> extElements = portType.getExtensibilityElements();
          printExtensibilityElements(PortType.class, extElements, def, pw);

          pw.println("  </" + tagName + '>');
        }
      }
    }
  }

  protected void printOperations(List<Operation> operations,
                                 Definition def,
                                 PrintWriter pw)
                                   throws WSDLException
  {
    if (operations != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_OPERATION,
                                   def);
      for(Operation operation: operations) {

        if (!operation.isUndefined())
        {
          pw.print("    <" + tagName);

          DOMUtils.printAttribute(Constants.ATTR_NAME,
                                  operation.getName(),
                                  pw);
          DOMUtils.printAttribute(Constants.ATTR_PARAMETER_ORDER,
                   StringUtils.getNMTokens(operation.getParameterOrdering()),
                   pw);

          printExtensibilityAttributes(Operation.class, operation, def, pw);

          pw.println('>');

          printDocumentation(operation.getDocumentationElement(), def, pw);

          OperationType operationType = operation.getStyle();

          if (operationType == OperationType.ONE_WAY)
          {
            printInput(operation.getInput(), def, pw);
          }
          else if (operationType == OperationType.SOLICIT_RESPONSE)
          {
            printOutput(operation.getOutput(), def, pw);
            printInput(operation.getInput(), def, pw);
          }
          else if (operationType == OperationType.NOTIFICATION)
          {
            printOutput(operation.getOutput(), def, pw);
          }
          else
          {
            // Must be OperationType.REQUEST_RESPONSE.
            printInput(operation.getInput(), def, pw);
            printOutput(operation.getOutput(), def, pw);
          }

          printFaults(operation.getFaults(), def, pw);

          List<ExtensibilityElement> extElements = operation.getExtensibilityElements();

          printExtensibilityElements(Operation.class, extElements, def, pw);

          pw.println("    </" + tagName + '>');
        }
      }
    }
  }

  protected void printInput(Input input,
                            Definition def,
                            PrintWriter pw)
                              throws WSDLException
  {
    if (input != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_INPUT,
                                   def);

      pw.print("      <" + tagName);

      DOMUtils.printAttribute(Constants.ATTR_NAME, input.getName(), pw);

      Message message = input.getMessage();

      if (message != null)
      {
        DOMUtils.printQualifiedAttribute(Constants.ATTR_MESSAGE,
                                         message.getQName(),
                                         def,
                                         pw);
      }

      printExtensibilityAttributes(Input.class, input, def, pw);

      pw.println('>');

      printDocumentation(input.getDocumentationElement(), def, pw);

      List<ExtensibilityElement> extElements = input.getExtensibilityElements();

      printExtensibilityElements(Input.class, extElements, def, pw);

      pw.println("    </" + tagName + '>');
    }
  }

  protected void printOutput(Output output,
                             Definition def,
                             PrintWriter pw)
                               throws WSDLException
  {
    if (output != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_OUTPUT,
                                   def);

      pw.print("      <" + tagName);

      DOMUtils.printAttribute(Constants.ATTR_NAME, output.getName(), pw);

      Message message = output.getMessage();

      if (message != null)
      {
        DOMUtils.printQualifiedAttribute(Constants.ATTR_MESSAGE,
                                         message.getQName(),
                                         def,
                                         pw);
      }

      printExtensibilityAttributes(Output.class, output, def, pw);

      pw.println('>');

      printDocumentation(output.getDocumentationElement(), def, pw);

      List<ExtensibilityElement> extElements = output.getExtensibilityElements();

      printExtensibilityElements(Output.class, extElements, def, pw);

      pw.println("    </" + tagName + '>');
    }
  }

  protected void printFaults(Map<?, Fault> faults,
                             Definition def,
                             PrintWriter pw)
                               throws WSDLException
  {
    if (faults != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_FAULT,
                                   def);
      for(Fault fault: faults.values()) {

        pw.print("      <" + tagName);

        DOMUtils.printAttribute(Constants.ATTR_NAME, fault.getName(), pw);

        Message message = fault.getMessage();

        if (message != null)
        {
          DOMUtils.printQualifiedAttribute(Constants.ATTR_MESSAGE,
                                           message.getQName(),
                                           def,
                                           pw);
        }

        printExtensibilityAttributes(Fault.class, fault, def, pw);

        pw.println('>');

        printDocumentation(fault.getDocumentationElement(), def, pw);

        List<ExtensibilityElement> extElements = fault.getExtensibilityElements();

        printExtensibilityElements(Fault.class, extElements, def, pw);

        pw.println("    </" + tagName + '>');
      }
    }
  }

  protected void printMessages(Map<?, Message> messages,
                               Definition def,
                               PrintWriter pw)
                                 throws WSDLException
  {
    if (messages != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_MESSAGE,
                                   def);
      for(Message message: messages.values()) {

        if (!message.isUndefined())
        {
          pw.print("  <" + tagName);

          QName name = message.getQName();

          if (name != null)
          {
            DOMUtils.printAttribute(Constants.ATTR_NAME,
                                    name.getLocalPart(),
                                    pw);
          }

          printExtensibilityAttributes(Message.class, message, def, pw);

          pw.println('>');

          printDocumentation(message.getDocumentationElement(), def, pw);
          printParts(message.getOrderedParts(null), def, pw);

          List<ExtensibilityElement> extElements = message.getExtensibilityElements();

          printExtensibilityElements(Message.class, extElements, def, pw);

          pw.println("  </" + tagName + '>');
        }
      }
    }
  }

  protected void printParts(List<Part> parts, Definition def, PrintWriter pw)
    throws WSDLException
  {
    if (parts != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_PART,
                                   def);
      for(Part part: parts) {

        pw.print("    <" + tagName);

        DOMUtils.printAttribute(Constants.ATTR_NAME, part.getName(), pw);
        DOMUtils.printQualifiedAttribute(Constants.ATTR_ELEMENT,
                                         part.getElementName(),
                                         def,
                                         pw);
        DOMUtils.printQualifiedAttribute(Constants.ATTR_TYPE,
                                         part.getTypeName(),
                                         def,
                                         pw);

        printExtensibilityAttributes(Part.class, part, def, pw);

        pw.println('>');

        printDocumentation(part.getDocumentationElement(), def, pw);

        List<ExtensibilityElement> extElements = part.getExtensibilityElements();

        printExtensibilityElements(Part.class, extElements, def, pw);

        pw.println("    </" + tagName + '>');
      }
    }
  }

  protected void printExtensibilityAttributes(Class<?> parentType,
                                              AttributeExtensible attrExt,
                                              Definition def,
                                              PrintWriter pw)
                                                throws WSDLException
  {
    Map<QName, List<QName>> extensionAttributes
    	= attrExt.getExtensionAttributes();

    for(Map.Entry<QName, List<QName>> e: extensionAttributes.entrySet()) {
      QName attrName = e.getKey();
      List<QName> attrValueList = e.getValue();
      String attrStrValue = null;
      QName attrQNameValue = null;

      StringBuffer strBuf = new StringBuffer();
      boolean seenFirst = false;
      for(QName tempQName: attrValueList) {
    	  strBuf.append((seenFirst ? " " : "") +
                        DOMUtils.getQualifiedValue( tempQName.getNamespaceURI(),
                        							tempQName.getLocalPart(),
                                                    def));
    	  seenFirst = true;
      }
      attrStrValue = strBuf.toString();

      if (attrQNameValue != null) {
          DOMUtils.printQualifiedAttribute(attrName, attrQNameValue, def, pw);
      }
      else {
          DOMUtils.printQualifiedAttribute(attrName, attrStrValue, def, pw);
      }
    }
  }

  protected void printDocumentation(Element docElement,
                                    Definition def,
                                    PrintWriter pw)
                                      throws WSDLException
  {
    if (docElement != null)
    {
      DOM2Writer.serializeAsXML(docElement, def.getNamespaces(), pw);

      pw.println();
    }
  }

  protected void printTypes(Types types, Definition def, PrintWriter pw)
    throws WSDLException
  {
    if (types != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_TYPES,
                                   def);
      pw.print("  <" + tagName);

      printExtensibilityAttributes(Types.class, types, def, pw);

      pw.println('>');

      printDocumentation(types.getDocumentationElement(), def, pw);

      List<ExtensibilityElement> extElements = types.getExtensibilityElements();

      printExtensibilityElements(Types.class, extElements, def, pw);

      pw.println("  </" + tagName + '>');
    }
  }

  protected void printImports(Map<?, List<Import>> imports, Definition def, PrintWriter pw)
    throws WSDLException
  {
    if (imports != null)
    {
      String tagName =
        DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL,
                                   Constants.ELEM_IMPORT,
                                   def);
      for(List<Import> importList: imports.values()) {
    	for(Import importDef: importList) {

          pw.print("  <" + tagName);

          DOMUtils.printAttribute(Constants.ATTR_NAMESPACE,
                                  importDef.getNamespaceURI(),
                                  pw);
          DOMUtils.printAttribute(Constants.ATTR_LOCATION,
                                  importDef.getLocationURI(),
                                  pw);

          printExtensibilityAttributes(Import.class, importDef, def, pw);

          pw.println('>');

          printDocumentation(importDef.getDocumentationElement(), def, pw);

          List<ExtensibilityElement> extElements = importDef.getExtensibilityElements();

          printExtensibilityElements(Import.class, extElements, def, pw);

          pw.println("    </" + tagName + '>');
        }
      }
    }
  }

  protected void printNamespaceDeclarations(Map<String, String> namespaces,
                                            PrintWriter pw)
                                              throws WSDLException
  {
    if (namespaces != null) {
      for(Map.Entry<String, String> e: namespaces.entrySet()) {
    	String prefix = e.getKey();
    	String namespace = e.getValue();
        if (prefix == null)
        {
          prefix = "";
          namespace = namespaces.get(prefix);
        }


        DOMUtils.printAttribute(Constants.ATTR_XMLNS +
                                (!prefix.equals("") ? ":" + prefix : ""),
                                namespace,
                                pw);
      }
    }
  }

  protected void printExtensibilityElements(Class<?> parentType,
                                            List<ExtensibilityElement> extensibilityElements,
                                            Definition def,
                                            PrintWriter pw)
                                              throws WSDLException
  {
    if (extensibilityElements != null)
    {
      for(ExtensibilityElement ext: extensibilityElements) {
        QName elementType = ext.getElementType();
        ExtensionRegistry extReg = def.getExtensionRegistry();

        if (extReg == null)
        {
          throw new WSDLException(WSDLException.CONFIGURATION_ERROR,
                                  "No ExtensionRegistry set for this " +
                                  "Definition, so unable to serialize a '" +
                                  elementType +
                                  "' element in the context of a '" +
                                  parentType.getName() + "'.");
        }

        ExtensionSerializer extSer = extReg.querySerializer(parentType,
                                                            elementType);

        extSer.marshall(parentType, elementType, ext, pw, def, extReg);
      }
    }
  }

  private static Document getDocument(InputSource inputSource,
                                      String desc) throws WSDLException
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    factory.setNamespaceAware(true);
    factory.setValidating(false);

    try
    {
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(inputSource);

      return doc;
    }
    catch (RuntimeException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw new WSDLException(WSDLException.PARSER_ERROR,
                              "Problem parsing '" + desc + "'.",
                              e);
    }
  }

  /**
   * Return a document generated from the specified WSDL model.
   */
  public Document getDocument(Definition wsdlDef) throws WSDLException
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    writeWSDL(wsdlDef, pw);

    StringReader sr = new StringReader(sw.toString());
    InputSource is = new InputSource(sr);

    return getDocument(is, "- WSDL Document -");
  }

  /**
   * Write the specified WSDL definition to the specified Writer.
   *
   * @param wsdlDef the WSDL definition to be written.
   * @param sink the Writer to write the xml to.
   */
  public void writeWSDL(Definition wsdlDef, Writer sink)
    throws WSDLException
  {
    PrintWriter pw = new PrintWriter(sink);
    String javaEncoding = (sink instanceof OutputStreamWriter)
                          ? ((OutputStreamWriter)sink).getEncoding()
                          : null;

    String xmlEncoding = DOM2Writer.java2XMLEncoding(javaEncoding);

    if (xmlEncoding == null)
    {
      throw new WSDLException(WSDLException.CONFIGURATION_ERROR,
                              "Unsupported Java encoding for writing " +
                              "wsdl file: '" + javaEncoding + "'.");
    }

    pw.println(Constants.XML_DECL_START +
               xmlEncoding +
               Constants.XML_DECL_END);

    printDefinition(wsdlDef, pw);
  }

  /**
   * Write the specified WSDL definition to the specified OutputStream.
   *
   * @param wsdlDef the WSDL definition to be written.
   * @param sink the OutputStream to write the xml to.
   */
  public void writeWSDL(Definition wsdlDef, OutputStream sink)
    throws WSDLException
  {
    Writer writer = null;

    try
    {
      writer = new OutputStreamWriter(sink, "UTF8");
    }
    catch (UnsupportedEncodingException e)
    {
      e.printStackTrace();

      writer = new OutputStreamWriter(sink);
    }

    writeWSDL(wsdlDef, writer);
  }

  /**
   * A test driver.
   *<code>
   *<pre>Usage:</pre>
   *<p>
   *<pre>  java com.ibm.wsdl.xml.WSDLWriterImpl filename|URL</pre>
   *<p>
   *<pre>    This test driver simply reads a WSDL document into a model
   *    (using a WSDLReader), and then serializes it back to
   *    standard out. In effect, it performs a round-trip test on
   *    the specified WSDL document.</pre>
   */
  public static void main(String[] argv) throws WSDLException
  {
    if (argv.length == 1)
    {
      WSDLFactory wsdlFactory = WSDLFactory.newInstance();
      WSDLReader  wsdlReader  = wsdlFactory.newWSDLReader();
      WSDLWriter  wsdlWriter  = wsdlFactory.newWSDLWriter();

      wsdlWriter.writeWSDL(wsdlReader.readWSDL(null, argv[0]), System.out);
    }
    else
    {
      System.err.println("Usage:");
      System.err.println();
      System.err.println("  java " + WSDLWriterImpl.class.getName() +
                         " filename|URL");
      System.err.println();
      System.err.println("This test driver simply reads a WSDL document " +
                         "into a model (using a WSDLReader), and then " +
                         "serializes it back to standard out. In effect, " +
                         "it performs a round-trip test on the specified " +
                         "WSDL document.");
    }
  }
}
