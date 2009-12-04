/*
 * (c) Copyright IBM Corp 2001, 2005
 */

package javax.wsdl.extensions.soap;

import java.util.List;

import javax.wsdl.extensions.ExtensibilityElement;

/**
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public interface SOAPBody extends ExtensibilityElement, java.io.Serializable
{
  /**
   * Set the parts for this SOAP body.
   *
   * @param parts the desired parts
   */
  public void setParts(List<String> parts);

  /**
   * Get the parts for this SOAP body.
   */
  public List<String> getParts();

  /**
   * Set the use for this SOAP body.
   *
   * @param use the desired use
   */
  public void setUse(String use);

  /**
   * Get the use for this SOAP body.
   */
  public String getUse();

  /**
   * Set the encodingStyles for this SOAP body.
   *
   * @param encodingStyles the desired encodingStyles
   */
  public void setEncodingStyles(List<String> encodingStyles);

  /**
   * Get the encodingStyles for this SOAP body.
   */
  public List<String> getEncodingStyles();

  /**
   * Set the namespace URI for this SOAP body.
   *
   * @param namespaceURI the desired namespace URI
   */
  public void setNamespaceURI(String namespaceURI);

  /**
   * Get the namespace URI for this SOAP body.
   */
  public String getNamespaceURI();
}