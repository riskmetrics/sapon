/*
 * (c) Copyright IBM Corp 2001, 2006
 */

package com.ibm.wsdl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Output;

/**
 * This class represents a WSDL operation.
 * It includes information on input, output and fault
 * messages associated with usage of the operation.
 *
 * @author Paul Fremantle (pzf@us.ibm.com)
 * @author Nirmal Mukhi (nmukhi@us.ibm.com)
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class OperationImpl extends AbstractWSDLElement implements Operation
{
  protected String name = null;
  protected Input input = null;
  protected Output output = null;
  protected Map<String, Fault> faults = new HashMap<String, Fault>();
  protected OperationType style = null;
  protected List<String> parameterOrder = null;
  protected List<String> nativeAttributeNames =
    Arrays.asList(Constants.OPERATION_ATTR_NAMES);
  protected boolean isUndefined = true;

  public static final long serialVersionUID = 1;

  /**
   * Set the name of this operation.
   *
   * @param name the desired name
   */
  public void setName(String name)
  {
    this.name = name;
  }

  /**
   * Get the name of this operation.
   *
   * @return the operation name
   */
  public String getName()
  {
    return name;
  }

  /**
   * Set the input message specification for this operation.
   *
   * @param input the new input message
   */
  public void setInput(Input input)
  {
    this.input = input;
  }

  /**
   * Get the input message specification for this operation.
   *
   * @return the input message
   */
  public Input getInput()
  {
    return input;
  }

  /**
   * Set the output message specification for this operation.
   *
   * @param output the new output message
   */
  public void setOutput(Output output)
  {
    this.output = output;
  }

  /**
   * Get the output message specification for this operation.
   *
   * @return the output message specification for the operation
   */
  public Output getOutput()
  {
    return output;
  }

  /**
   * Add a fault message that must be associated with this
   * operation.
   *
   * @param fault the new fault message
   */
  public void addFault(Fault fault)
  {
    faults.put(fault.getName(), fault);
  }

  /**
   * Get the specified fault message.
   *
   * @param name the name of the desired fault message.
   * @return the corresponding fault message, or null if there wasn't
   * any matching message
   */
  public Fault getFault(String name)
  {
    return faults.get(name);
  }

  /**
   * Remove the specified fault message.
   *
   * @param name the name of the fault message to be removed
   * @return the fault message which was removed.
   */
  public Fault removeFault(String name)
  {
    return faults.remove(name);
  }

  /**
   * Get all the fault messages associated with this operation.
   *
   * @return names of fault messages
   */
  public Map<String, Fault> getFaults()
  {
    return faults;
  }

  /**
   * Set the style for this operation (request-response,
   * one way, solicit-response or notification).
   *
   * @param style the new operation style
   */
  public void setStyle(OperationType style)
  {
    this.style = style;
  }

  /**
   * Get the operation type.
   *
   * @return the operation type
   */
  public OperationType getStyle()
  {
    return style;
  }

  /**
   * Set the parameter ordering for a request-response,
   * or solicit-response operation.
   *
   * @param parameterOrder, a list of named parameters
   * containing the part names to reflect the desired
   * order of parameters for RPC-style operations
   */
  public void setParameterOrdering(List<String> parameterOrder)
  {
    this.parameterOrder = parameterOrder;
  }

  /**
   * Get the parameter ordering for this operation.
   *
   * @return the parameter ordering, a list consisting
   * of message part names
   */
  public List<String> getParameterOrdering()
  {
    return parameterOrder;
  }

  public void setUndefined(boolean isUndefined)
  {
    this.isUndefined = isUndefined;
  }

  public boolean isUndefined()
  {
    return isUndefined;
  }

  @Override
public String toString()
  {
    StringBuffer strBuf = new StringBuffer();

    strBuf.append("Operation: name=" + name);

    if (parameterOrder != null)
    {
      strBuf.append("\nparameterOrder=" + parameterOrder);
    }

    if (style != null)
    {
      strBuf.append("\nstyle=" + style);
    }

    if (input != null)
    {
      strBuf.append("\n" + input);
    }

    if (output != null)
    {
      strBuf.append("\n" + output);
    }

    if (faults != null)
    {
      for(Fault fault: faults.values()) {
        strBuf.append("\n" + fault);
      }
    }

    String superString = super.toString();
    if(!superString.equals(""))
    {
      strBuf.append("\n");
      strBuf.append(superString);
    }

    return strBuf.toString();
  }

  /**
   * Get the list of local attribute names defined for this element in
   * the WSDL specification.
   *
   * @return a List of Strings, one for each local attribute name
   */
  public List<String> getNativeAttributeNames()
  {
    return nativeAttributeNames;
  }
}
