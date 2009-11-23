package org.apache.synapse.core.axis2.policy;

import javax.xml.namespace.QName;

import org.apache.axis2.wsdl.WSDLConstants;

public class MessagePolicyInfo extends OperationPolicyInfo {
    public static final int MESSAGE_TYPE_IN = 1;
    public static final int MESSAGE_TYPE_OUT = 2;

    private final int type;

    public MessagePolicyInfo(final String policyKey, final QName op, final int type) {
    	super(policyKey, op);
    	this.type = type;
    }

    public String getMessageLabel() {
        if (type == MESSAGE_TYPE_IN) {
            return WSDLConstants.MESSAGE_LABEL_IN_VALUE;
        } else if (type == MESSAGE_TYPE_OUT) {
            return WSDLConstants.MESSAGE_LABEL_OUT_VALUE;
        } else {
            return null;
        }
    }

    public int getType() {
    	return type;
    }
}
