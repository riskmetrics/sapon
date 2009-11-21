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

package org.apache.axis2.builder;

import java.io.InputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.fileupload.FileItem;

public class MultipartFormDataBuilder implements Builder {

    /**
     * @return Returns the document element.
     */
//    public OMElement getDocumentElement(InputStream inputStream, String contentType,
//                                     MessageContext messageContext)
//            throws AxisFault {
//        MultipleEntryHashMap parameterMap;
//        HttpServletRequest request = (HttpServletRequest) messageContext
//                .getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
//        if (request == null) {
//            throw new AxisFault("Cannot create DocumentElement without HttpServletRequest");
//        }
//
//        // TODO: Do check ContentLength for the max size,
//        //       but it can't be configured anywhere.
//        //       I think that it cant be configured at web.xml or axis2.xml.
//
//        String charSetEncoding = (String)messageContext.getProperty(Constants.Configuration.CHARACTER_SET_ENCODING);
//        if (charSetEncoding == null) {
//            charSetEncoding = request.getCharacterEncoding();
//        }
//
//        try {
//            parameterMap = getParameterMap(request, charSetEncoding);
//            return BuilderUtil.buildsoapMessage(messageContext, parameterMap,
//                                                OMAbstractFactory.getSOAP12Factory());
//
//        } catch (FileUploadException e) {
//            throw AxisFault.makeFault(e);
//        }
//
//    }
//
//    private MultipleEntryHashMap getParameterMap(HttpServletRequest request,
//                                                 String charSetEncoding)
//            throws FileUploadException {
//
//        MultipleEntryHashMap parameterMap = new MultipleEntryHashMap();
//
//        List<FileItem> items = parseRequest(new ServletRequestContext(request));
//        for(FileItem fileItem: items) {
//            boolean isFormField = fileItem.isFormField();
//
//            Object value;
//            try {
//                if (isFormField) {
//                    value = getTextParameter(fileItem, charSetEncoding);
//                } else {
//                    value = getFileParameter(fileItem);
//                }
//            } catch (Exception ex) {
//                throw new FileUploadException(ex.getMessage());
//            }
//            parameterMap.put(fileItem.getFieldName(), value);
//        }
//
//        return parameterMap;
//    }

//    @SuppressWarnings("unchecked")
//	private static List<FileItem> parseRequest(ServletRequestContext requestContext)
//            throws FileUploadException {
//        // Create a factory for disk-based file items
//        FileItemFactory factory = new DiskFileItemFactory();
//        // Create a new file upload handler
//        ServletFileUpload upload = new ServletFileUpload(factory);
//        // Parse the request
//        return upload.parseRequest(requestContext);
//    }

    private String getTextParameter(FileItem fileItem,
                                    String characterEncoding) throws Exception {
        return fileItem.getString(characterEncoding);
    }

    private DataHandler getFileParameter(FileItem fileItem)
            throws Exception {

        DataSource dataSource = new FileItemDataSource(fileItem);
        DataHandler dataHandler = new DataHandler(dataSource);

        return dataHandler;
    }


	@Override
	public OMElement getDocumentElement(InputStream inputStream,
			String contentType, MessageContext messageContext) throws AxisFault {
		// TODO Auto-generated method stub
		return null;
	}

}
