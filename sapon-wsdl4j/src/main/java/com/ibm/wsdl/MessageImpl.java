/*
 * (c) Copyright IBM Corp 2001, 2006
 */

package com.ibm.wsdl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.xml.namespace.QName;

/**
 * This class describes a message used for communication with an operation.
 *
 * @author Paul Fremantle
 * @author Nirmal Mukhi
 * @author Matthew J. Duftler
 */
public class MessageImpl extends AbstractWSDLElement implements Message
{
  protected Map<String, Part> parts = new HashMap<String, Part>();
  protected List<String> additionOrderOfParts = new Vector<String>();
  protected QName name = null;
  protected List<String> nativeAttributeNames =
    Arrays.asList(Constants.MESSAGE_ATTR_NAMES);
  protected boolean isUndefined = true;

  public static final long serialVersionUID = 1;

  /**
   * Set the name of this message.
   *
   * @param name the desired name
   */
  public void setQName(QName name)
  {
    this.name = name;
  }

  /**
   * Get the name of this message.
   *
   * @return the message name
   */
  public QName getQName()
  {
    return name;
  }

  /**
   * Add a part to this message.
   *
   * @param part the part to be added
   */
  public void addPart(Part part)
  {
    String partName = part.getName();

    parts.put(partName, part);
    additionOrderOfParts.add(partName);
  }

  /**
   * Get the specified part.
   *
   * @param name the name of the desired part.
   * @return the corresponding part, or null if there wasn't
   * any matching part
   */
  public Part getPart(String name)
  {
    return parts.get(name);
  }

  /**
   * Remove the specified part.
   *
   * @param name the name of the part to be removed.
   * @return the part which was removed
   */
  public Part removePart(String name)
  {
    return parts.remove(name);
  }

  /**
   * Get all the parts defined here.
   */
  public Map<String, Part> getParts()
  {
    return parts;
  }

  /**
   * Get an ordered list of parts as specified by the partOrder
   * argument.
   *
   * @param partOrder a list of strings, with each string referring
   * to a part by its name. If this argument is null, the parts are
   * returned in the order in which they were added to the message.
   * @return the list of parts
   */
  public List<Part> getOrderedParts(List<String> partOrder)
  {
    List<Part> orderedParts = new Vector<Part>();

    if (partOrder == null)
    {
      partOrder = additionOrderOfParts;
    }

    for(String partName: partOrder) {
      Part part = getPart(partName);

      if (part != null) {
        orderedParts.add(part);
      }
    }

    return orderedParts;
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

    strBuf.append("Message: name=" + name);

    if (parts != null) {
      for(Part part: parts.values()) {
        strBuf.append("\n" + part);
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
