/*
 * (c) Copyright IBM Corp 2001, 2005
 */

package javax.wsdl.extensions.soap;

import java.util.List;

import javax.wsdl.extensions.ExtensibilityElement;

/**
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public interface SOAPFault extends ExtensibilityElement, java.io.Serializable
{
  /**
   * Set the name for this SOAP fault.
   *
   * @param name the desired name
   */
  public void setName(String name);

  /**
   * Get the name for this SOAP fault.
   */
  public String getName();

  /**
   * Set the use for this SOAP fault.
   *
   * @param use the desired use
   */
  public void setUse(String use);

  /**
   * Get the use for this SOAP fault.
   */
  public String getUse();

  /**
   * Set the encodingStyles for this SOAP fault.
   *
   * @param encodingStyles the desired encodingStyles
   */
  public void setEncodingStyles(List<String> encodingStyles);

  /**
   * Get the encodingStyles for this SOAP fault.
   */
  public List<String> getEncodingStyles();

  /**
   * Set the namespace URI for this SOAP fault.
   *
   * @param namespaceURI the desired namespace URI
   */
  public void setNamespaceURI(String namespaceURI);

  /**
   * Get the namespace URI for this SOAP fault.
   */
  public String getNamespaceURI();
}