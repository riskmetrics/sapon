package org.apache.synapse.core.axis2.policy;

import javax.xml.namespace.QName;

public class OperationPolicyInfo extends AbstractPolicyInfo {

	private final QName operation;

    public OperationPolicyInfo(String policyKey, QName operation) {
    	super(policyKey);
        this.operation = operation;
    }

    public QName getOperation() {
        return operation;
    }
}
