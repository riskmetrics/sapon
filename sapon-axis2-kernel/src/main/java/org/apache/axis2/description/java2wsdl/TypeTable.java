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

package org.apache.axis2.description.java2wsdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;

public class TypeTable {

    private static final Map<String, QName> simpleTypeToXSD
    	= new HashMap<String, QName>();
    public static final QName ANY_TYPE = new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "anyType", "xs");

    private Map<String, QName> complexTypeMap;

    public TypeTable() {
        //complex type table is reset every time this is instantiated
        complexTypeMap = new HashMap<String, QName>();
    }

    static{
          populateSimpleTypes();
    }

    private static void populateSimpleTypes() {
        //TODO: use the types from org.apache.ws.commons.schema.constants.Constants
        simpleTypeToXSD.put("int",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "int", "xs"));
        simpleTypeToXSD.put("java.lang.String",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "string", "xs"));
        simpleTypeToXSD.put("boolean",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "boolean", "xs"));
        simpleTypeToXSD.put("float",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "float", "xs"));
        simpleTypeToXSD.put("double",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "double", "xs"));
        simpleTypeToXSD.put("short",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "short", "xs"));
        simpleTypeToXSD.put("long",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "long", "xs"));
        simpleTypeToXSD.put("byte",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "byte", "xs"));
        simpleTypeToXSD.put("char",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "string", "xs"));
        simpleTypeToXSD.put("java.lang.Integer",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "int", "xs"));
        simpleTypeToXSD.put("java.lang.Double",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "double", "xs"));
        simpleTypeToXSD.put("java.lang.Float",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "float", "xs"));
        simpleTypeToXSD.put("java.lang.Long",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "long", "xs"));
        simpleTypeToXSD.put("java.lang.Character",
                ANY_TYPE);
        simpleTypeToXSD.put("java.lang.Boolean",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "boolean", "xs"));
        simpleTypeToXSD.put("java.lang.Byte",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "byte", "xs"));
        simpleTypeToXSD.put("java.lang.Short",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "short", "xs"));
        simpleTypeToXSD.put("java.util.Date",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "date", "xs"));
        simpleTypeToXSD.put("java.util.Calendar",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "dateTime", "xs"));

        // SQL date time
         simpleTypeToXSD.put("java.sql.Date",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "date", "xs"));
         simpleTypeToXSD.put("java.sql.Time",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "time", "xs"));
        simpleTypeToXSD.put("java.sql.Timestamp",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "dateTime", "xs"));

        //consider BigDecimal, BigInteger, Day, Duration, Month, MonthDay,
        //Time, Year, YearMonth as SimpleType as well
        simpleTypeToXSD.put("java.math.BigDecimal",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "decimal", "xs"));
        simpleTypeToXSD.put("java.math.BigInteger",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "integer", "xs"));
        simpleTypeToXSD.put("org.apache.axis2.databinding.types.Day",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "gDay", "xs"));
        simpleTypeToXSD.put("org.apache.axis2.databinding.types.Duration",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "duration", "xs"));
        simpleTypeToXSD.put("org.apache.axis2.databinding.types.Month",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "gMonth", "xs"));
        simpleTypeToXSD.put("org.apache.axis2.databinding.types.MonthDay",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "gMonthDay", "xs"));
        simpleTypeToXSD.put("org.apache.axis2.databinding.types.Time",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "time", "xs"));
        simpleTypeToXSD.put("org.apache.axis2.databinding.types.Year",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "gYear", "xs"));
        simpleTypeToXSD.put("org.apache.axis2.databinding.types.YearMonth",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "gYearMonth", "xs"));

        simpleTypeToXSD.put("java.lang.Object",
                ANY_TYPE);

        // Any types
        simpleTypeToXSD.put(OMElement.class.getName(),
                ANY_TYPE);
        simpleTypeToXSD.put(ArrayList.class.getName(),
                ANY_TYPE);
        simpleTypeToXSD.put(Vector.class.getName(),
                ANY_TYPE);
        simpleTypeToXSD.put(List.class.getName(),
                ANY_TYPE);
        simpleTypeToXSD.put(HashMap.class.getName(),
                 ANY_TYPE);
        simpleTypeToXSD.put(Hashtable.class.getName(),
                 ANY_TYPE);

        //byteArray
        simpleTypeToXSD.put("base64Binary",
                new QName(Java2WSDLConstants.URI_2001_SCHEMA_XSD, "base64Binary", "xs"));
    }

    /**
     * Return the schema type QName given the type class name
     * @param typeName  the name of the type
     * @return   the name of the simple type or null if it is not a simple type
     */
    public QName getSimpleSchemaTypeName(String typeName) {
        QName qName = simpleTypeToXSD.get(typeName);
        if(qName == null){
            if((typeName.startsWith("java.lang")||typeName.startsWith("javax.")) &&
                    !Exception.class.getName().equals(typeName)){
                return ANY_TYPE;
            }
        }
        return qName;
    }

    /**
     * Return whether the given type is a simple type or not
     * @param typeName the name of the type
     * @return  true if the type is a simple type
     */
    public boolean isSimpleType(String typeName) {

        if (simpleTypeToXSD.keySet().contains(typeName)){
            return true;
        }else if(typeName.startsWith("java.lang")||typeName.startsWith("javax.")){
            return true;
        }
        return false;
    }

    /**
     * Return the complex type map
     * @return  the map with complex types
     */
    public Map<String, QName> getComplexSchemaMap() {
        return complexTypeMap;
    }

    public void addComplexSchema(String name, QName schemaType) {
        complexTypeMap.put(name, schemaType);
    }

    public QName getComplexSchemaType(String name) {
        return complexTypeMap.get(name);
    }

    /**
     * Get the QName for a type
     * first try the simple types if not try the complex types
     * @param typeName  name of the type
     * @return  the Qname for this type
     */
    public QName getQNamefortheType(String typeName) {
        QName type = getSimpleSchemaTypeName(typeName);
        if (type == null) {
            type = getComplexSchemaType(typeName);
        }
        return type;
    }
}


