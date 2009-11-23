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

package org.apache.axis2.mex.om;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.EndpointReferenceHelper;
import org.apache.axis2.dataretrieval.OutputForm;
import org.apache.axis2.mex.MexConstants;
import org.apache.axis2.mex.MexException;
import org.apache.axis2.mex.util.MexUtil;

/**
 *
 * Class implementing mex:Metadata element
 *
 */

public class Metadata extends MexOM implements IMexOM {
	private String namespaceValue = null;
	private final OMFactory factory;
	private List<MetadataSection> metadataSections = new ArrayList<MetadataSection>();
	private OMAttribute attribute = null;

	/**
	 * Constructor
	 *
	 * @throws MexException
	 */

	public Metadata() throws MexException {

		this.factory = MexUtil
				.getSOAPFactory(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
		;
		this.namespaceValue = MexConstants.Spec_2004_09.NS_URI;
	}

	/**
	 *
	 * @param defaultFactory
	 * @param namespaceValue
	 * @throws MexOMException
	 */

	public Metadata(OMFactory defaultFactory, String namespaceValue)
			throws MexOMException {
		this.factory = defaultFactory;
		this.namespaceValue = namespaceValue;
	}

	/**
	 *
	 * @return Array of MetadataSection of metadata units
	 */
	public MetadataSection[] getMetadatSections() {
		return metadataSections.toArray(new MetadataSection[0]);
	}

	/**
	 *
	 * @param dialect
	 * @param identifier
	 * @return Array of MetadataSection for the specified dialect metadata type
	 *         and identifier
	 */
	public MetadataSection[] getMetadataSection(String dialect,
			String identifier) {
		MetadataSection[] sections = getMetadataSection(dialect, identifier,
				null);
		return sections;
	}

	/**
	 *
	 * Answers the Metadata Sections that met the criteria specified in the
	 * dialect, identifier, and form. Note: Null value parameter will be treated
	 * as wild card.
	 *
	 * @param dialect
	 * @param identifier
	 * @param form
	 *            specify the form of metadata: inline or by reference See
	 *            <code>OutputForm</code> for valid output forms.
	 * @return Array of MetadataSection for the specified dialect metadata type
	 *         and identifier of the form specified.
	 *
	 */
	public MetadataSection[] getMetadataSection(String dialect,
			String identifier, OutputForm form) {

		List<MetadataSection> foundSections = new ArrayList<MetadataSection>();
		for (final MetadataSection aSection : metadataSections) {
			if ((dialect == null || dialect.equals(aSection.getDialect()))
					&& (identifier == null || identifier.equals(aSection
							.getIdentifier()))
					&& matchOutputForm(aSection, form)) {
				foundSections.add(aSection);
			}

		}
		return foundSections.toArray(new MetadataSection[0]);
	}

	/**
	 * Populates an Metadata object based on the <code>OMElement</code> passed.
	 *
	 * @param inElement
	 *            mex:Metadata element or element contains mex:Metadata element
	 * @return Metadata
	 * @throws MexOMException
	 */
	public Metadata fromOM(OMElement inElement) throws MexOMException {

		OMElement mexElement = null;
		if (inElement == null) {
			throw new MexOMException("Null element passed.");
		}

		if (inElement.getLocalName().equals(MexConstants.SPEC.METADATA)) {
			mexElement = inElement;
		}
		if (inElement.getLocalName().equals("EndpointReference")) {
			try {
				EndpointReference epr = EndpointReferenceHelper
						.fromOM(inElement);

				List<OMElement> metadata = epr.getMetaData();
				if (metadata != null) {
					mexElement = metadata.get(0);
				} else {
					List<OMElement> refParm = epr.getExtensibleElements();
					for (int i = 0; i < refParm.size(); i++) {
						OMElement elem = refParm.get(i);
						if (elem.getLocalName().equals(
								MexConstants.SPEC.METADATA)) {
							mexElement = elem;
							break;
						}
					}
				}
			} catch (AxisFault e) {
				throw new MexOMException(e);
			}

			if (mexElement == null) {
				throw new MexOMException(
						"Missing expected Metadata element in element passed.");
			}
		} else {
			mexElement = inElement;
		}

		OMFactory aFactory = mexElement.getOMFactory();
		if (aFactory == null) {
			aFactory = factory;
		}
		Iterable<OMElement> mexSections = mexElement
				.getChildrenWithName(new QName(namespaceValue,
						MexConstants.SPEC.METADATA_SECTION));

		if (mexSections == null) {
			throw new MexOMException(
					"Metadata element does not contain MetadataSection element.");
		}

		for (OMElement aSection : mexSections) {
			MetadataSection metadataSection = new MetadataSection(aFactory,
					namespaceValue);
			addMetadataSection(metadataSection.fromOM(aSection));
		}

		return this;
	}

	/**
	 *
	 * @return Array of MetadataSection of metadata units
	 */
	@Override
	public OMElement toOM() throws MexOMException {
		OMNamespace mexNamespace = factory.createOMNamespace(namespaceValue,
				MexConstants.SPEC.NS_PREFIX);
		OMElement metadata = factory.createOMElement(
				MexConstants.SPEC.METADATA, mexNamespace);

		for (MetadataSection aSection : metadataSections) {
			metadata.addChild(aSection.toOM());
		}
		if (attribute != null) {
			metadata.addAttribute(attribute); // ???
		}
		return metadata;
	}

	public void setMetadataSections(List<MetadataSection> in_metadataSections) {
		metadataSections = in_metadataSections;
	}

	public void addMetadataSections(List<MetadataSection> in_metadataSections) {
		metadataSections.addAll(in_metadataSections);
	}

	public void addMetadataSection(MetadataSection section) {
		metadataSections.add(section);
	}

	public void setAttribute(OMAttribute in_attribute) {
		attribute = in_attribute;
	}

	// check if section contains data matching the output form requested
	private boolean matchOutputForm(MetadataSection section,
			OutputForm outputForm) {
		boolean match = (outputForm == null); // no matching needed in null
												// outputForm is passed

		if (!match) {
			if (outputForm == OutputForm.LOCATION_FORM) {
				match = (section.getLocation() != null);
			} else if (outputForm == OutputForm.REFERENCE_FORM) {
				match = (section.getMetadataReference() != null);
			} else if (outputForm == OutputForm.INLINE_FORM) {
				match = (section.getInlineData() != null);
			}
		}

		return match;
	}

}
