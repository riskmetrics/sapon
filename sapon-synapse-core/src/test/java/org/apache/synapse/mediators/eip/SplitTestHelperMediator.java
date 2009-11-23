package org.apache.synapse.mediators.eip;

import java.util.ArrayList;
import java.util.List;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * Created by IntelliJ IDEA. User: ruwan Date: Oct 3, 2007 Time: 7:26:09 AM To change this template
 * use File | Settings | File Templates.
 */
public class SplitTestHelperMediator extends AbstractMediator implements ManagedLifecycle {

    private List<SynapseMessageContext> mediatedContext = new ArrayList<SynapseMessageContext>();
    int msgcount;
    String checkString;

    public boolean mediate(SynapseMessageContext synCtx) {
        synchronized(this) {
            if (msgcount == 0) {
                SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
                try {
                    synCtx.setEnvelope(envelope);
                } catch (AxisFault ignore) {
                }
            } else {
                checkString = synCtx.getEnvelope().getBody().getFirstElement().getText();
                if ("".equals(checkString)) {
                    checkString = synCtx.getEnvelope().getBody().getFirstElement().getFirstElement().getText();
                }
            }
            mediatedContext.add(synCtx);
            msgcount++;
            return false;
        }
    }

    public SynapseMessageContext getMediatedContext(int position) {
        if (mediatedContext.size() > position) {
            return mediatedContext.get(position);
        } else {
            return null;
        }
    }

    public void clearMediatedContexts() {
        mediatedContext.clear();
        msgcount = 0;
    }

    public String getCheckString() {
        return checkString;
    }

    public void init(SynapseEnvironment se) {
        msgcount = 0;
    }

    public void destroy() {
        clearMediatedContexts();
        msgcount = 0;
    }
}
