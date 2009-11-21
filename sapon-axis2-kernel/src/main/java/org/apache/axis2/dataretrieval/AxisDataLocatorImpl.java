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

package org.apache.axis2.dataretrieval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Default Axis2 Data Locator implementation
 */

public class AxisDataLocatorImpl implements AxisDataLocator {
    private static final Log log = LogFactory.getLog(AxisDataLocatorImpl.class);

    // HashMap to cache Data elements defined in ServiceData.
    private Map<String, ServiceData> dataMap
    	= new HashMap<String, ServiceData>();

    private AxisService axisService;

    /**
     * Constructor
     *
     * @throws DataRetrievalException
     */
    public AxisDataLocatorImpl(AxisService in_axisService) throws DataRetrievalException {
        super();
        axisService = in_axisService;
    }

    /**
     * Retrieves and returns data based on the specified request.
     */
    public Data[] getData(DataRetrievalRequest request,
                          MessageContext msgContext)
    	throws DataRetrievalException
    {
        final ServiceData[] serviceData;
        final String dialect = request.getDialect();
        final String identifier = request.getIdentifier();
        if (identifier != null) {
            ServiceData s = dataMap.get(dialect + identifier);
            serviceData = (s == null) ? new ServiceData[0]
                                      : new ServiceData[] { s };
        } else {
            serviceData = getServiceData(dialect);
        }

        AxisDataLocator dataLocator
        	= DataLocatorFactory.createDataLocator(dialect, serviceData);

        if (dataLocator == null) {
        	String message = "Failed to instantiate Data Locator for dialect, " + dialect;
        	log.info(message);
        	throw new DataRetrievalException(message);
        }

        try {
        	return dataLocator.getData(request, msgContext);
        }
        catch (Throwable e) {
        	log.info("getData request failed for dialect, " + dialect, e);
        	throw new DataRetrievalException(e);
        }
    }

    /*
    * For AxisService use only!
    */
    public void loadServiceData() {
        DataRetrievalUtil util = DataRetrievalUtil.getInstance();

        OMElement serviceData = null;
        String file = "META-INF/" + DRConstants.SERVICE_DATA.FILE_NAME;
        try {
            serviceData = util.buildOM(axisService.getClassLoader(),
                                       "META-INF/" + DRConstants.SERVICE_DATA.FILE_NAME);
        } catch (DataRetrievalException e) {
            // It is not required to define ServiceData for a Service, just log a warning message
            String message = "Check loading failure for file, " + file;
            log.debug(message + ".Message = " + e.getMessage());
            log.debug(message, e);
        }
        if (serviceData != null) {
            cachingServiceData(serviceData);
        }
    }

    /*
    * caching ServiceData for Axis2 Data Locators
    */
    private void cachingServiceData(OMElement e) {
        String saveKey = "";
        for(OMElement elem: e.getChildrenWithName(new QName(DRConstants.SERVICE_DATA.DATA))) {
        	ServiceData data = new ServiceData(elem);
            saveKey = data.getDialect();

            String identifier = data.getIdentifier();
            if (identifier != null) {
                saveKey = saveKey + identifier;
            }
            dataMap.put(saveKey, data);
        }
    }

    /*
    * Return ServiceData for specified dialect
    */
    private ServiceData[] getServiceData(String dialect) {
        List<ServiceData> dataList = new ArrayList<ServiceData>();
        for(Map.Entry<String, ServiceData> e: dataMap.entrySet()) {
            if (e.getKey().equalsIgnoreCase(dialect)) {
                dataList.add(e.getValue());
            }
        }
        return dataList.toArray(new ServiceData[dataList.size()]);
    }
}