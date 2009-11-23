package org.apache.synapse.core.axis2.policy;

public abstract class AbstractPolicyInfo implements PolicyInfo {
    private final String policyKey;

    public AbstractPolicyInfo(String policyKey) {
        this.policyKey = policyKey;
    }

    public String getPolicyKey() {
        return policyKey;
    }
}
