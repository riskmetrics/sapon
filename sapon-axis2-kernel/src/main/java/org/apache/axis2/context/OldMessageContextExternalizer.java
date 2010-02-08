package org.apache.axis2.context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Axis2Constants.Configuration;
import org.apache.axis2.alt.Flows;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.externalize.ActivateUtils;
import org.apache.axis2.context.externalize.ExternalizeConstants;
import org.apache.axis2.context.externalize.MessageExternalizeUtils;
import org.apache.axis2.context.externalize.SafeObjectInputStream;
import org.apache.axis2.context.externalize.SafeObjectOutputStream;
import org.apache.axis2.description.AxisMessage;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.LoggingControl;
import org.apache.axis2.util.MetaDataEntry;
import org.apache.axis2.util.SelfManagedDataHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OldMessageContextExternalizer {

	private static final Log log
		= LogFactory.getLog(OldMessageContextExternalizer.class);

	private final OldMessageContext context;

    /**
     * The ordered list of metadata for handlers/phases
     * used during re-constitution of the message context
     */
    private transient ArrayList<MetaDataEntry> metaExecutionChain = null;

    /**
     * The ordered list of metadata for executed phases
     * used during re-constitution of the message context
     */
    private transient LinkedList<MetaDataEntry> metaExecuted = null;

    /**
     * Index into the executuion chain of the currently executing handler
     */
    private transient int metaHandlerIndex = 0;

    /**
     * Index into the current Phase of the currently executing handler (if any)
     */
    private transient int metaPhaseIndex = 0;

    /**
     * The AxisOperation metadata will be used during
     * activate to match up with an existing object
     */
    private transient MetaDataEntry metaAxisOperation = null;

    /**
     * The AxisService metadata will be used during
     * activate to match up with an existing object
     */
    private transient MetaDataEntry metaAxisService = null;

    /**
     * The AxisServiceGroup metadata will be used during
     * activate to match up with an existing object
     */
    private transient MetaDataEntry metaAxisServiceGroup = null;

    /**
     * The TransportOutDescription metadata will be used during
     * activate to match up with an existing object
     */
    private transient MetaDataEntry metaTransportOut = null;

    /**
     * The TransportInDescription metadata will be used during
     * activate to match up with an existing object
     */
    private transient MetaDataEntry metaTransportIn = null;

    /**
     * The AxisMessage metadata will be used during
     * activate to match up with an existing object
     */
    private transient MetaDataEntry metaAxisMessage = null;
	public OldMessageContextExternalizer(OldMessageContext context) {
		this.context = context;
	}

    /**
     * SelfManagedData cannot be restored until the configurationContext
     * is available, so we have to hold the data from readExternal until
     * activate is called.
     */
    private transient ArrayList<SelfManagedDataHolder> selfManagedDataListHolder = null;

    /**
     * selfManagedDataHandlerCount is a count of the number of handlers
     * that actually saved data during serialization
     */
    private transient int selfManagedDataHandlerCount = 0;

    /**
     * Restore the contents of the MessageContext that was
     * previously saved.
     * <p/>
     * NOTE: The field data must read back in the same order and type
     * as it was written.  Some data will need to be validated when
     * resurrected.
     *
     * @param in The stream to read the object contents from
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void readExternal(ObjectInput inObject)
    	throws IOException, ClassNotFoundException
    {
        SafeObjectInputStream in = SafeObjectInputStream.install(inObject);
        // set the flag to indicate that the message context is being
        // reconstituted and will need to have certain object references
        // to be reconciled with the current engine setup
        context.setNeedsToBeActivated(true);

        // trace point
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("Bytes available in stream:" + in.available());
        }

        //---------------------------------------------------------
        // object level identifiers
        //---------------------------------------------------------

        // serialization version ID
        long suid = in.readLong();

        // revision ID
        int revID = in.readInt();

        // make sure the object data is in a version we can handle
        if (suid != OldMessageContext.serialVersionUID) {
            throw new ClassNotFoundException(ExternalizeConstants.UNSUPPORTED_SUID);
        }

        // make sure the object data is in a revision level we can handle
        if (revID != OldMessageContext.REVISION_2) {
            throw new ClassNotFoundException(ExternalizeConstants.UNSUPPORTED_REVID);
        }

        //---------------------------------------------------------
        // various simple fields
        //---------------------------------------------------------

        // the type of execution flow for the message context
        context.setFlow((Flows)in.readObject());

        // various flags
        context.setProcessingFault(in.readBoolean());
        context.setPaused(in.readBoolean());
        context.setOutputWritten(in.readBoolean());
        context.setNewThreadRequired(in.readBoolean());
        context.setSOAP11(in.readBoolean());
        context.setDoingREST(in.readBoolean());
        context.setDoingMTOM(in.readBoolean());
        context.setDoingSwA(in.readBoolean());
        context.setResponseWritten(in.readBoolean());
        context.setServerSide(in.readBoolean());

        long time = in.readLong();
        context.setLastTouchedTime(time);
        context.setLogCorrelationID((String) in.readObject());

        // trace point
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("readExternal():  reading the input stream for  " +
                      context.getLogCorrelationID());
        }

        //---------------------------------------------------------
        // Message
        // Read the message and attachments
        //---------------------------------------------------------
        try {
        	context.setEnvelope(
        		MessageExternalizeUtils.readExternal(	in, context,
        												context.getLogCorrelationID()));
        } catch(AxisFault af) {
        	throw new IOException("Could not set envelope while reading "
        			+ "message context from external", af);
        }

        //---------------------------------------------------------
        // ArrayList executionChain
        //     handler and phase related data
        //---------------------------------------------------------
        // Restore the metadata about each member of the list
        // and the order of the list.
        // This metadata will be used to match up with phases
        // and handlers on the engine.
        //
        // Non-null list:
        //    UTF          - description string
        //    boolean      - active flag
        //    int          - current handler index
        //    int          - current phase index
        //    int          - expected number of entries in the list
        //                        not including the last entry marker
        //    objects      - MetaDataEntry object per list entry
        //                        last entry will be empty MetaDataEntry
        //                        with MetaDataEntry.LAST_ENTRY marker
        //    int          - adjusted number of entries in the list
        //                        includes the last empty entry
        //
        // Empty list:
        //    UTF          - description string
        //    boolean      - empty flag
        //---------------------------------------------------------

        // the local chain is not enabled until the
        // list has been reconstituted
        context.setCurrentHandlerIndex(-1);
        context.setCurrentPhaseIndex(0);
        metaExecutionChain = null;

        String marker = in.readUTF();
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("readExternal(): About to read executionChain, marker is: " + marker);
        }
        boolean gotChain = in.readBoolean();

        if (gotChain == ExternalizeConstants.ACTIVE_OBJECT) {
            metaHandlerIndex = in.readInt();
            metaPhaseIndex = in.readInt();

            int expectedNumberEntries = in.readInt();

            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace("readExternal(): execution chain:  expected number of entries [" +
                        expectedNumberEntries + "]");
            }

            // setup the list
            metaExecutionChain = new ArrayList<MetaDataEntry>();

            // process the objects
            boolean keepGoing = true;
            int count = 0;

            while (keepGoing) {
                // stop when we get to the end-of-list marker

                // get the object
                Object tmpObj = in.readObject();

                count++;

                MetaDataEntry mdObj = (MetaDataEntry) tmpObj;

                // get the class name, then add it to the list
                String tmpClassNameStr;
                String tmpQNameAsStr;

                if (mdObj != null) {
                    tmpClassNameStr = mdObj.getClassName();

                    if (tmpClassNameStr.equalsIgnoreCase(MetaDataEntry.END_OF_LIST)) {
                        // this is the last entry
                        keepGoing = false;
                    } else {
                        // add the entry to the meta data list
                        metaExecutionChain.add(mdObj);

                        tmpQNameAsStr = mdObj.getQNameAsString();

                        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                            String tmpHasList = mdObj.isListEmpty() ? "no children" : "has children";

                            if (log.isTraceEnabled()) {
                                log.trace("meta data class [" + tmpClassNameStr +
                                    "] qname [" + tmpQNameAsStr + "]  index [" + count + "]   [" +
                                    tmpHasList + "]");
                            }
                        }
                    }
                } else {
                    // some error occurred
                    keepGoing = false;
                }

            } // end while keep going

            int adjustedNumberEntries = in.readInt();

            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace("readExternal(): adjusted number of entries ExecutionChain [" +
                        adjustedNumberEntries + "]    ");
            }
        }

        if ((metaExecutionChain == null) || (metaExecutionChain.isEmpty())) {
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace("meta data for Execution Chain is NULL");
            }
        }

        //---------------------------------------------------------
        // LinkedList executedPhases
        //
        // Note that in previous versions of Axis2, this was
        // represented by two lists: "inboundExecutedPhases", "outboundExecutedPhases",
        // however since the message context itself represents a flow
        // direction, one of these lists was always null.  This was changed
        // around 2007-06-08 revision r545615.  For backward compatability
        // with streams saved in previous versions of Axis2, we need
        // to be able to process both the old style and new style.
        //---------------------------------------------------------
        // Restore the metadata about each member of the list
        // and the order of the list.
        // This metadata will be used to match up with phases
        // and handlers on the engine.
        //
        // Non-null list:
        //    UTF          - description string
        //    boolean      - active flag
        //    int          - expected number of entries in the list
        //                        not including the last entry marker
        //    objects      - MetaDataEntry object per list entry
        //                        last entry will be empty MetaDataEntry
        //                        with MetaDataEntry.LAST_ENTRY marker
        //    int          - adjusted number of entries in the list
        //                        includes the last empty entry
        //
        // Empty list:
        //    UTF          - description string
        //    boolean      - empty flag
        //---------------------------------------------------------

        // the local chain is not enabled until the
        // list has been reconstituted
        metaExecuted = null;

        marker = in.readUTF();
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("About to read executedPhases, marker is: " + marker);
        }

        // Previous versions of Axis2 saved two phases in the stream, although one should
        // always have been null.  The two phases and their associated markers are, in this order:
        // "inboundExecutedPhases", "outboundExecutedPhases".
        boolean gotInExecList = in.readBoolean();
        boolean oldStyleExecutedPhases = false;
        if (marker.equals("inboundExecutedPhases")) {
            oldStyleExecutedPhases = true;
        }

        if (oldStyleExecutedPhases && (gotInExecList == ExternalizeConstants.EMPTY_OBJECT)) {
            // There are an inboundExecutedPhases and an outboundExecutedPhases and this one
            // is empty, so skip over it and read the next one
            marker = in.readUTF();
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace("Skipping over oldStyle empty inboundExecutedPhases");
                log.trace("About to read executedPhases, marker is: " + marker);
            }
            gotInExecList = in.readBoolean();
        }

        /*
         * At this point, the stream should point to either "executedPhases" if this is the
         * new style of serialization.  If it is the oldStyle, it should point to whichever
         * of "inbound" or "outbound" executed phases contains an active object, since only one
         * should
         */
        if (gotInExecList == ExternalizeConstants.ACTIVE_OBJECT) {
            int expectedNumberInExecList = in.readInt();

            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace(	"executed phases:  expected number of entries [" +
                        	expectedNumberInExecList + "]");
            }

            // setup the list
            metaExecuted = new LinkedList<MetaDataEntry>();

            // process the objects
            boolean keepGoing = true;
            int count = 0;

            while (keepGoing) {
                // stop when we get to the end-of-list marker

                // get the object
                Object tmpObj = in.readObject();

                count++;

                MetaDataEntry mdObj = (MetaDataEntry) tmpObj;

                // get the class name, then add it to the list
                String tmpClassNameStr;
                String tmpQNameAsStr;
                String tmpHasList = "no list";

                if (mdObj != null) {
                    tmpClassNameStr = mdObj.getClassName();

                    if (tmpClassNameStr.equalsIgnoreCase(MetaDataEntry.END_OF_LIST)) {
                        // this is the last entry
                        keepGoing = false;
                    } else {
                        // add the entry to the meta data list
                        metaExecuted.add(mdObj);

                        tmpQNameAsStr = mdObj.getQNameAsString();

                        if (!mdObj.isListEmpty()) {
                            tmpHasList = "has list";
                        }

                        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                            log.trace("meta data class [" + tmpClassNameStr +
                                    "] qname [" + tmpQNameAsStr + "]  index [" + count + "]   [" +
                                    tmpHasList + "]");
                        }
                    }
                } else {
                    // some error occurred
                    keepGoing = false;
                }

            } // end while keep going

            int adjustedNumberInExecList = in.readInt();

            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace("adjusted number of entries executedPhases [" +
                        adjustedNumberInExecList + "]    ");
            }
        }

        if ((metaExecuted == null) || (metaExecuted.isEmpty())) {
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace("meta data for executedPhases list is NULL");
            }
        }

        marker = in.readUTF(); // Read marker
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("After reading executedPhases, marker is: " + marker);
        }

        // If this is an oldStyle that contained both an inbound and outbound executed phases,
        // and the outbound phases wasn't read above, then we need to skip over it
        if (marker.equals("outboundExecutedPhases")) {
            Boolean gotOutExecList = in.readBoolean();
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace("Skipping over outboundExecutedPhases, marker is: " + marker +
                          ", is list an active object: " + gotOutExecList);
            }
            if (gotOutExecList != ExternalizeConstants.EMPTY_OBJECT) {
                throw new IOException("Both inboundExecutedPhases and outboundExecutedPhases had active objects");
            }

            marker = in.readUTF();
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace("After skipping ooutboundExecutePhases, marker is: " + marker);
            }
        }

        //---------------------------------------------------------
        // options
        //---------------------------------------------------------

        Options options = (Options) in.readObject();
        context.setOptions(options);

        if (options != null) {
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace("restored Options [" + options.getLogCorrelationIDString() + "]");
            }
        }

        //---------------------------------------------------------
        // operation
        //---------------------------------------------------------

        // axisOperation is not usable until the meta data has been reconciled
        //axisOperation = null;
        marker = in.readUTF();  // Read Marker
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("About to read axisOperation, marker is: " + marker);
        }
        metaAxisOperation = (MetaDataEntry) in.readObject();

        // operation context is not usable until it has been activated
        // NOTE: expect this to be the parent
        marker = in.readUTF();  // Read marker
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("About to read operationContext, marker is: " + marker);
        }
        OperationContext operationContext = (OperationContext) in.readObject();
        context.setOperationContext(operationContext);

        if (operationContext != null) {
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace("restored OperationContext [" +
                        operationContext.getLogCorrelationIDString() + "]");
            }
        }

        //---------------------------------------------------------
        // service
        //---------------------------------------------------------

        // axisService is not usable until the meta data has been reconciled
        //axisService = null;
        marker = in.readUTF(); // Read marker
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("About to read axisService, marker is: " + marker);
        }
        metaAxisService = (MetaDataEntry) in.readObject();

        //-------------------------
        // serviceContextID string
        //-------------------------
        context.setServiceContextID((String) in.readObject());

        //-------------------------
        // serviceContext
        //-------------------------
        marker = in.readUTF(); // Read marker
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("About to read serviceContext, marker is: " + marker);
        }

        boolean servCtxActive = in.readBoolean();
        if (servCtxActive != ExternalizeConstants.EMPTY_OBJECT) {
            boolean isParent = in.readBoolean();
            // there's an object to read in if it is not the parent of the
            // operation context
            if (!isParent) {
                context.setServiceContext((ServiceContext) in.readObject());
            }
        }

        //---------------------------------------------------------
        // serviceGroup
        //---------------------------------------------------------

        // axisServiceGroup is not usable until the meta data has been reconciled
        //axisServiceGroup = null;
        marker = in.readUTF(); // Read marker
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("About to read AxisServiceGroup, marker is: " + marker);
        }
        metaAxisServiceGroup = (MetaDataEntry) in.readObject();

        //-----------------------------
        // serviceGroupContextId string
        //-----------------------------
        context.setServiceGroupContextId((String) in.readObject());

        //-----------------------------
        // serviceGroupContext
        //-----------------------------
        marker = in.readUTF();
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("About to read ServiceGroupContext, marker is: " + marker);
        }

        boolean servGrpCtxActive = in.readBoolean();
        if (servGrpCtxActive != ExternalizeConstants.EMPTY_OBJECT) {
            boolean isParentSGC = in.readBoolean();
            // there's an object to read in if it is not the parent of the service group context
            if (!isParentSGC) {
                context.setServiceGroupContext((ServiceGroupContext) in.readObject());
            }
        }

        //---------------------------------------------------------
        // axis message
        //---------------------------------------------------------

        // axisMessage is not usable until the meta data has been reconciled
        //axisMessage = null;
        marker = in.readUTF();  // Read marker
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("About to read AxisMessage, marker is: " + marker);
        }
        metaAxisMessage = (MetaDataEntry) in.readObject();
        context.setReconcileAxisMessage(metaAxisMessage != null);


        //---------------------------------------------------------
        // configuration context
        //---------------------------------------------------------

        // TODO: check to see if there is any runtime data important to this
        //       message context in the configuration context
        //       if so, then need to restore the saved runtime data and reconcile
        //       it with the configuration context on the system when
        //       this message context object is restored

        //---------------------------------------------------------
        // session context
        //---------------------------------------------------------
        context.setSessionContext((SessionContext<?>) in.readObject());

        //---------------------------------------------------------
        // transport
        //---------------------------------------------------------

        //------------------------------
        // incomingTransportName string
        //------------------------------
        context.setIncomingTransportName((String) in.readObject());

        // TransportInDescription transportIn
        // is not usable until the meta data has been reconciled
        //transportIn = null;
        metaTransportIn = (MetaDataEntry) in.readObject();

        // TransportOutDescription transportOut
        // is not usable until the meta data has been reconciled
        //transportOut = null;
        metaTransportOut = (MetaDataEntry) in.readObject();

        //---------------------------------------------------------
        // properties
        //---------------------------------------------------------
        // read local properties
        marker = in.readUTF(); // Read marker
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("About to read properties, marker is: " + marker);
        }
        context.setProperties(in.readMap(new HashMapUpdateLockable<String, Object>()));


        //---------------------------------------------------------
        // special data
        //---------------------------------------------------------
        marker = in.readUTF(); // Read marker
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("About to read SpecialData, marker is: " + marker);
        }

        boolean gotSelfManagedData = in.readBoolean();

        if (gotSelfManagedData == ExternalizeConstants.ACTIVE_OBJECT) {
            selfManagedDataHandlerCount = in.readInt();

            if (selfManagedDataListHolder == null) {
                selfManagedDataListHolder = new ArrayList<SelfManagedDataHolder>();
            } else {
                selfManagedDataListHolder.clear();
            }

            for (int i = 0; i < selfManagedDataHandlerCount; i++) {
                selfManagedDataListHolder.add((SelfManagedDataHolder) in.readObject());
            }
        }

        //---------------------------------------------------------
        // done
        //---------------------------------------------------------

        // trace point
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace("MessageContext object created");
        }
    }

    /**
     * Save the contents of this MessageContext instance.
     * <p/>
     * NOTE: Transient fields and static fields are not saved.
     * Also, objects that represent "static" data are
     * not saved, except for enough information to be
     * able to find matching objects when the message
     * context is re-constituted.
     *
     * @param out The stream to write the object contents to
     * @throws IOException
     */
    public void writeExternal(ObjectOutput o)
    	throws IOException
    {
        SafeObjectOutputStream out = SafeObjectOutputStream.install(o);
        String logCorrelationIDString = context.getLogCorrelationID();

        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(logCorrelationIDString + ":writeExternal(): writing to output stream");
        }

        //---------------------------------------------------------
        // in order to handle future changes to the message
        // context definition, be sure to maintain the
        // object level identifiers
        //---------------------------------------------------------
        // serialization version ID
        out.writeLong(OldMessageContext.serialVersionUID);

        // revision ID
        out.writeInt(OldMessageContext.revisionID);

        //---------------------------------------------------------
        // various simple fields
        //---------------------------------------------------------

        // the type of execution flow for the message context
        out.writeObject(context.getFlow());

        // various flags
        out.writeBoolean(context.isProcessingFault());
        out.writeBoolean(context.isPaused());
        out.writeBoolean(context.isOutputWritten());
        out.writeBoolean(context.isNewThreadRequired());
        out.writeBoolean(context.isSOAP11());
        out.writeBoolean(context.isDoingREST());
        out.writeBoolean(context.isDoingMTOM());
        out.writeBoolean(context.isDoingSwA());
        out.writeBoolean(context.isResponseWritten());
        out.writeBoolean(context.isServerSide());

        out.writeLong(context.getLastTouchedTime());
        out.writeObject(context.getLogCorrelationID());

        //-----------------------------------------------------------------------
        // Create and initialize the OMOutputFormat for Message Externalization
        //-----------------------------------------------------------------------

        OMOutputFormat outputFormat= new OMOutputFormat();
        outputFormat.setSOAP11(context.isSOAP11());
        boolean persistOptimized = getPersistOptimized(context);
        if (persistOptimized) {
            outputFormat.setDoOptimize(true);
        }
        String charSetEnc = (String) context.getProperty(MessageContext.CHARACTER_SET_ENCODING);
        if (charSetEnc == null) {
            OperationContext opContext = context.getOperationContext();
            if (opContext != null) {
                charSetEnc =
                        (String) opContext.getProperty(MessageContext.CHARACTER_SET_ENCODING);
            }
        }
        if (charSetEnc == null) {
            charSetEnc = MessageContext.DEFAULT_CHAR_SET_ENCODING;
        }
        outputFormat.setCharSetEncoding(charSetEnc);

        // ----------------------------------------------------------
        // Externalize the Message
        // ----------------------------------------------------------
        MessageExternalizeUtils.writeExternal(out, context, logCorrelationIDString, outputFormat);

        // ---------------------------------------------------------
        // ArrayList executionChain
        //     handler and phase related data
        //---------------------------------------------------------
        // The strategy is to save some metadata about each
        // member of the list and the order of the list.
        // Then when the message context is re-constituted,
        // try to match up with phases and handlers on the
        // engine.
        //
        // Non-null list:
        //    UTF          - description string
        //    boolean      - active flag
        //    int          - current handler index
        //    int          - current phase index
        //    int          - expected number of entries in the list
        //    objects      - MetaDataEntry object per list entry
        //                        last entry will be empty MetaDataEntry
        //                        with MetaDataEntry.LAST_ENTRY marker
        //    int          - adjusted number of entries in the list
        //                        includes the last empty entry
        //
        // Empty list:
        //    UTF          - description string
        //    boolean      - empty flag
        //---------------------------------------------------------
        out.writeUTF("executionChain");
        List<? extends Handler> executionChain = context.getExecutionChain();
        if (executionChain != null && executionChain.size() > 0) {
            // start writing data to the output stream
            out.writeBoolean(ExternalizeConstants.ACTIVE_OBJECT);
            out.writeInt(context.getCurrentHandlerIndex());
            out.writeInt(context.getCurrentPhaseIndex());
            out.writeInt(executionChain.size());

            // put the metadata on each member of the list into a buffer

            // match the current index with the actual saved list
            int nextIndex = 0;

            Iterator<? extends Handler> i = executionChain.iterator();

            while (i.hasNext()) {
                Object obj = i.next();
                String objClass = obj.getClass().getName();
                // start the meta data entry for this object
                MetaDataEntry mdEntry = new MetaDataEntry();
                mdEntry.setClassName(objClass);

                // get the correct object-specific name
                String qnameAsString;

                if (obj instanceof Phase) {
                    Phase phaseObj = (Phase) obj;
                    qnameAsString = phaseObj.getName();

                    // add the list of handlers to the meta data
                    setupPhaseList(phaseObj, mdEntry);
                } else if (obj instanceof Handler) {
                    Handler handlerObj = (Handler) obj;
                    qnameAsString = handlerObj.getName();
                } else {
                    qnameAsString = "NULL";
                }

                mdEntry.setQName(qnameAsString);

                // update the index for the entry in the chain

                if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                    log.trace(logCorrelationIDString +
                            ":writeExternal(): ***BEFORE OBJ WRITE*** executionChain entry class [" +
                            objClass + "] qname [" + qnameAsString + "]");
                }

                out.writeObject(mdEntry);

                // update the index so that the index
                // now indicates the next entry that
                // will be attempted
                nextIndex++;

                if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                    log.trace(logCorrelationIDString +
                            ":writeExternal(): ***AFTER OBJ WRITE*** executionChain entry class [" +
                            objClass + "] qname [" + qnameAsString + "]");
                }

            } // end while entries in execution chain

            // done with the entries in the execution chain
            // add the end-of-list marker
            MetaDataEntry lastEntry = new MetaDataEntry();
            lastEntry.setClassName(MetaDataEntry.END_OF_LIST);

            out.writeObject(lastEntry);
            nextIndex++;

            // nextIndex also gives us the number of entries
            // that were actually saved as opposed to the
            // number of entries in the executionChain
            out.writeInt(nextIndex);

        } else {
            // general case: handle "null" or "empty"
            out.writeBoolean(ExternalizeConstants.EMPTY_OBJECT);

            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace(logCorrelationIDString + ":writeExternal(): executionChain is NULL");
            }
        }

        //---------------------------------------------------------
        // LinkedList executedPhases
        //---------------------------------------------------------
        // The strategy is to save some metadata about each
        // member of the list and the order of the list.
        // Then when the message context is re-constituted,
        // try to match up with phases and handlers on the
        // engine.
        //
        // Non-null list:
        //    UTF          - description string
        //    boolean      - active flag
        //    int          - expected number of entries in the list
        //    objects      - MetaDataEntry object per list entry
        //                        last entry will be empty MetaDataEntry
        //                        with MetaDataEntry.LAST_ENTRY marker
        //    int          - adjusted number of entries in the list
        //                        includes the last empty entry
        //
        // Empty list:
        //    UTF          - description string
        //    boolean      - empty flag
        //---------------------------------------------------------
        out.writeUTF("executedPhases");
        List<? extends Handler> executedPhases = context.getExecutedPhases();
        if (executedPhases != null && executedPhases.size() > 0) {

            // start writing data to the output stream
            out.writeBoolean(ExternalizeConstants.ACTIVE_OBJECT);
            out.writeInt(executedPhases.size());

            // put the metadata on each member of the list into a buffer

            int execNextIndex = 0;

            Iterator<? extends Handler> iterator = executedPhases.iterator();

            while (iterator.hasNext()) {
                Object obj = iterator.next();
                String objClass = obj.getClass().getName();
                // start the meta data entry for this object
                MetaDataEntry mdEntry = new MetaDataEntry();
                mdEntry.setClassName(objClass);

                // get the correct object-specific name
                String qnameAsString;

                if (obj instanceof Phase) {
                    Phase inPhaseObj = (Phase) obj;
                    qnameAsString = inPhaseObj.getName();

                    // add the list of handlers to the meta data
                    setupPhaseList(inPhaseObj, mdEntry);
                } else if (obj instanceof Handler) {
                    Handler inHandlerObj = (Handler) obj;
                    qnameAsString = inHandlerObj.getName();
                } else {
                    qnameAsString = "NULL";
                }

                mdEntry.setQName(qnameAsString);

                if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                    log.trace(logCorrelationIDString +
                            ":writeExternal(): ***BEFORE Executed List OBJ WRITE*** executedPhases entry class [" +
                            objClass + "] qname [" + qnameAsString + "]");
                }

                out.writeObject(mdEntry);

                // update the index so that the index
                // now indicates the next entry that
                // will be attempted
                execNextIndex++;

                if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                    log.trace(logCorrelationIDString + ":writeExternal(): " +
                            "***AFTER Executed List OBJ WRITE*** " +
                            "executedPhases entry class [" + objClass + "] " +
                            "qname [" + qnameAsString + "]");
                }
            } // end while entries in execution chain

            // done with the entries in the execution chain
            // add the end-of-list marker
            MetaDataEntry lastEntry = new MetaDataEntry();
            lastEntry.setClassName(MetaDataEntry.END_OF_LIST);

            out.writeObject(lastEntry);
            execNextIndex++;

            // execNextIndex also gives us the number of entries
            // that were actually saved as opposed to the
            // number of entries in the executedPhases
            out.writeInt(execNextIndex);

        } else {
            // general case: handle "null" or "empty"
            out.writeBoolean(ExternalizeConstants.EMPTY_OBJECT);

            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace(
                        logCorrelationIDString + ":writeExternal(): executedPhases is NULL");
            }
        }

        //---------------------------------------------------------
        // options
        //---------------------------------------------------------
        // before saving the Options, make sure there is a message ID
        String tmpID = context.getMessageID();
        if (tmpID == null) {
            // get an id to use when restoring this object
            tmpID = UUIDGenerator.getUUID();
            context.setMessageID(tmpID);
        }

        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(logCorrelationIDString + ":writeExternal():   message ID [" + tmpID + "]");
        }

        out.writeUTF("options");
        out.writeObject(context.getOptions());

        //---------------------------------------------------------
        // operation
        //---------------------------------------------------------
        // axis operation
        //---------------------------------------------------------
        out.writeUTF("axisOperation");
        metaAxisOperation = null;
        AxisOperation axisOperation = context.getAxisOperation();
        if (axisOperation != null) {
            // TODO: may need to include the meta data for the axis service that is
            //       the parent of the axis operation
            // make sure the axis operation has a name associated with it
            QName aoTmpQName = axisOperation.getName();

            if (aoTmpQName == null) {
                aoTmpQName = new QName(ExternalizeConstants.EMPTY_MARKER);
                axisOperation.setName(aoTmpQName);
            }

            metaAxisOperation = new MetaDataEntry(axisOperation.getClass().getName(),
                                                  axisOperation.getName().toString());
        }
        out.writeObject(metaAxisOperation);

        //---------------------------------------------------------
        // operation context
        //---------------------------------------------------------
        // The OperationContext has pointers to MessageContext objects.
        // In order to avoid having multiple copies of the object graph
        // being saved at different points in the serialization,
        // it is important to isolate this message context object.
        out.writeUTF("operationContext");
        OperationContext operationContext = context.getOperationContext();
        if (operationContext != null) {
            operationContext.isolateMessageContext(context);
        }

        out.writeObject(operationContext);


        //---------------------------------------------------------
        // service
        //---------------------------------------------------------
        // axis service
        //-------------------------
        // this is expected to be the parent of the axis operation object
        out.writeUTF("axisService");
        metaAxisService = null;
        AxisService axisService = context.getAxisService();
        if (axisService != null) {
            metaAxisService = new MetaDataEntry(axisService.getClass().getName(),
                                                axisService.getName());
        }
        out.writeObject(metaAxisService);

        //-------------------------
        // serviceContextID string
        //-------------------------
        out.writeObject(context.getServiceContextID());

        //-------------------------
        // serviceContext
        //-------------------------
        // is this the same as the parent of the OperationContext?
        boolean isParent = false;
        out.writeUTF("serviceContext");
        ServiceContext serviceContext = context.getServiceContext();
        if (operationContext != null) {
            ServiceContext opctxParent = operationContext.getServiceContext();

            if (serviceContext != null) {
                if (serviceContext.equals(opctxParent)) {
                    // the ServiceContext is the parent of the OperationContext
                    isParent = true;
                }
            }
        }

        if (serviceContext == null) {
            out.writeBoolean(ExternalizeConstants.EMPTY_OBJECT);
        } else {
            out.writeBoolean(ExternalizeConstants.ACTIVE_OBJECT);
            out.writeBoolean(isParent);

            // only write out the object if it is not the parent
            if (!isParent) {
                out.writeObject(serviceContext);
            }
        }

        //---------------------------------------------------------
        // axisServiceGroup
        //---------------------------------------------------------
        out.writeUTF("axisServiceGroup");
        metaAxisServiceGroup = null;
        AxisServiceGroup axisServiceGroup = context.getAxisServiceGroup();
        if (axisServiceGroup != null) {
            metaAxisServiceGroup = new MetaDataEntry(axisServiceGroup.getClass().getName(),
                                                     axisServiceGroup.getName());
        }
        out.writeObject(metaAxisServiceGroup);

        //-----------------------------
        // serviceGroupContextId string
        //-----------------------------
        out.writeObject(context.getServiceGroupContextId());

        //-------------------------
        // serviceGroupContext
        //-------------------------
        // is this the same as the parent of the ServiceContext?
        isParent = false;
        out.writeUTF("serviceGroupContext");
        ServiceGroupContext serviceGroupContext = context.getServiceGroupContext();
        if (serviceContext != null) {
            ServiceGroupContext srvgrpctxParent = serviceContext.getParent();

            if (serviceGroupContext != null) {
                if (serviceGroupContext.equals(srvgrpctxParent)) {
                    // the ServiceGroupContext is the parent of the ServiceContext
                    isParent = true;
                }
            }
        }

        if (serviceGroupContext == null) {
            out.writeBoolean(ExternalizeConstants.EMPTY_OBJECT);
        } else {
            out.writeBoolean(ExternalizeConstants.ACTIVE_OBJECT);
            out.writeBoolean(isParent);

            // only write out the object if it is not the parent
            if (!isParent) {
                out.writeObject(serviceGroupContext);
            }
        }

        //---------------------------------------------------------
        // axis message
        //---------------------------------------------------------
        out.writeUTF("axisMessage");
        metaAxisMessage = null;
        AxisMessage axisMessage = context.getAxisMessage();
        if (axisMessage != null) {
            // This AxisMessage is expected to belong to the AxisOperation
            // that has already been recorded for this MessageContext.
            // If an AxisMessage associated with this Messagecontext is
            // associated with a different AxisOperation, then more
            // meta information would need to be saved

            // make sure the axis message has a name associated with it
            String amTmpName = axisMessage.getName();

            if (amTmpName == null) {
                amTmpName = ExternalizeConstants.EMPTY_MARKER;
                axisMessage.setName(amTmpName);
            }

            // get the element name if there is one
            QName amTmpElementQName = axisMessage.getElementQName();
            String amTmpElemQNameString = null;

            if (amTmpElementQName != null) {
                amTmpElemQNameString = amTmpElementQName.toString();
            }

            metaAxisMessage = new MetaDataEntry(axisMessage.getClass().getName(),
                                                axisMessage.getName(), amTmpElemQNameString);

        }
        out.writeObject(metaAxisMessage);

        //---------------------------------------------------------
        // configuration context
        //---------------------------------------------------------

        // NOTE: Currently, there does not seem to be any
        //       runtime data important to this message context
        //       in the configuration context.
        //       if so, then need to save that runtime data and reconcile
        //       it with the configuration context on the system when
        //       this message context object is restored

        //---------------------------------------------------------
        // session context
        //---------------------------------------------------------
        out.writeObject(context.getSessionContext());

        //---------------------------------------------------------
        // transport
        //---------------------------------------------------------

        //------------------------------
        // incomingTransportName string
        //------------------------------
        out.writeObject(context.getIncomingTransportName());

        metaTransportIn = null;
        TransportInDescription transportIn = context.getTransportIn();
        if (transportIn != null) {
            metaTransportIn = new MetaDataEntry(null, transportIn.getName());
        }
        out.writeObject(metaTransportIn);

        metaTransportOut = null;
        TransportOutDescription transportOut = context.getTransportOut();
        if (transportOut != null) {
            metaTransportOut = new MetaDataEntry(null, transportOut.getName());
        }
        out.writeObject(metaTransportOut);


        //---------------------------------------------------------
        // properties
        //---------------------------------------------------------
        // Write out the local properties on the MessageContext
        // Don't write out the properties from other hierarchical layers.
        // (i.e. don't use getProperties())
        out.writeUTF("properties"); // write marker
        out.writeMap(context.getProperties());

        //---------------------------------------------------------
        // special data
        //---------------------------------------------------------
        out.writeUTF("selfManagedData");
        serializeSelfManagedData(out);

        //---------------------------------------------------------
        // done
        //---------------------------------------------------------

        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(logCorrelationIDString +
                    ":writeExternal(): completed writing to output stream for " +
                    logCorrelationIDString);
        }
    }

    /**
     * This method checks to see if additional work needs to be done in order
     * to complete the object reconstitution.  Some parts of the object
     * restored from the readExternal() cannot be completed until we have a
     * configurationContext from the active engine. The configurationContext is
     * used to help this object to plug back into the engine's configuration
     * and deployment objects.
     *
     * @param cc The ConfigurationContext representing the active configuration
     */
    public void activate(ConfigurationContext cc) {
        if (!context.needsToBeActivated()) {
            return;
        }

        context.setConfigurationContext(cc);

        AxisConfiguration axisConfig = cc.getAxisConfiguration();

        if (metaAxisService != null) {
            context.setAxisService(ActivateUtils.findService(axisConfig,
                                                metaAxisService.getClassName(),
                                                metaAxisService.getQNameAsString()));
        }

        if (metaAxisServiceGroup != null) {
            context.setAxisServiceGroup(ActivateUtils.findServiceGroup(axisConfig,
                                                metaAxisServiceGroup.getClassName(),
                                                metaAxisServiceGroup.getQNameAsString()));
        }

        if (metaAxisOperation != null) {
            AxisService serv = context.getAxisService();
            if (serv != null) {
                // TODO: check for the empty name
                context.setAxisOperation(ActivateUtils.findOperation(serv,
                                                                  metaAxisOperation.getClassName(),
                                                                  metaAxisOperation.getQName()));
            } else {
                context.setAxisOperation(ActivateUtils.findOperation(axisConfig,
                                                                  metaAxisOperation.getClassName(),
                                                                  metaAxisOperation.getQName()));
            }
        }

        if (metaAxisMessage != null) {
            AxisOperation op = context.getAxisOperation();
            if (op != null) {
                // TODO: check for the empty name
                context.setAxisMessage(ActivateUtils.findMessage(op,
                                                              metaAxisMessage.getQNameAsString(),
                                                              metaAxisMessage.getExtraName()));
            }
        }

        //---------------------------------------------------------------------
        // operation context
        //---------------------------------------------------------------------
        // this will do a full hierarchy, so do it first
        // then we can re-use its objects
        OperationContext operationContext = context.getOperationContext();
        if (operationContext != null) {
            operationContext.activate(cc);

            // this will be set as the parent of the message context
            // after the other context objects have been activated
        }

        //---------------------------------------------------------------------
        // service context
        //---------------------------------------------------------------------
        ServiceContext serviceContext = context.getServiceContext();
        if (serviceContext == null) {
            if (operationContext != null) {
            	serviceContext = operationContext.getServiceContext();
                context.setServiceContext(serviceContext);
            }
        }

        if (serviceContext != null) {
            // for some reason, the service context might be set differently
        	// from the operation context parent
            serviceContext.activate(cc);
        }

        //---------------------------------------------------------------------
        // service group context
        //---------------------------------------------------------------------

        ServiceGroupContext serviceGroupContext = context.getServiceGroupContext();
        if (serviceGroupContext == null) {
            // get the parent serviceGroupContext of the serviceContext
            if (serviceContext != null) {
            	serviceGroupContext = serviceContext.getParent();
                context.setServiceGroupContext(serviceGroupContext);
            }
        }

        // if we have a service group context, make sure it is usable
        if (serviceGroupContext != null) {
            // for some reason, the service group context might be set differently from
            // the service context parent
            serviceGroupContext.activate(cc);
        }

        //---------------------------------------------------------------------
        // other context-related reconciliation
        //---------------------------------------------------------------------

        context.setParent(operationContext);

        //---------------------------------------------------------------------
        // options
        //---------------------------------------------------------------------
        Options options = context.getOptions();
        if (options != null) {
            options.activate(cc);
        }

        String tmpID = context.getMessageID();
        String logCorrelationIDString = context.getLogCorrelationID();

        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(logCorrelationIDString + ":activate():   message ID [" + tmpID + "] for " +
                    logCorrelationIDString);
        }

        //---------------------------------------------------------------------
        // transports
        //---------------------------------------------------------------------

        if (metaTransportIn != null) {
            QName qin = metaTransportIn.getQName();
            TransportInDescription tmpIn = null;
            try {
                tmpIn = axisConfig.getTransportIn(qin.getLocalPart());
            }
            catch (Exception exin) {
                log.trace(logCorrelationIDString +
                        "activate():  exception caught when getting the TransportInDescription [" +
                        qin.toString() + "]  from the AxisConfiguration [" +
                        exin.getClass().getName() + " : " + exin.getMessage() + "]");
            }

            if (tmpIn != null) {
                context.setTransportIn(tmpIn);
            } else {
                context.setTransportIn(null);
            }
        } else {
            context.setTransportIn(null);
        }

        if (metaTransportOut != null) {
            // TODO : Check if this should really be a QName?
            QName qout = metaTransportOut.getQName();
            TransportOutDescription tmpOut = null;
            try {
                tmpOut = axisConfig.getTransportOut(qout.getLocalPart());
            }
            catch (Exception exout) {
                // if a fault is thrown, log it and continue
                if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                    log.trace(logCorrelationIDString +
                        "activate():  exception caught when getting the TransportOutDescription [" +
                        qout.toString() + "]  from the AxisConfiguration [" +
                        exout.getClass().getName() + " : " + exout.getMessage() + "]");
                }
            }

            if (tmpOut != null) {
                context.setTransportOut(tmpOut);
            } else {
                context.setTransportOut(null);
            }
        } else {
            context.setTransportOut(null);
        }

        //-------------------------------------------------------
        // reconcile the execution chain
        //-------------------------------------------------------
        if (metaExecutionChain != null) {
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace(
                        logCorrelationIDString + ":activate(): reconciling the execution chain...");
            }

            context.setCurrentHandlerIndex(metaHandlerIndex);
            context.setCurrentPhaseIndex(metaPhaseIndex);
            context.setExecutionChain(restoreHandlerList(metaExecutionChain));

            try {
                deserializeSelfManagedData();
            }
            catch (Exception ex) {
                // log the exception
                if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                    log.trace(logCorrelationIDString +
                        ":activate(): *** WARNING *** deserializing the self managed data encountered Exception [" +
                        ex.getClass().getName() + " : " + ex.getMessage() + "]", ex);
                }
            }
        }

        //-------------------------------------------------------
        // reconcile the lists for the executed phases
        //-------------------------------------------------------
        if (metaExecuted != null) {
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace(logCorrelationIDString +
                        ":activate(): reconciling the executed chain...");
            }

            if (!(context.isExecutedPhasesReset())) {
                context.setExecutedPhasesExplicit(
                		restoreExecutedList(context.getExecutedPhases(),
                							metaExecuted));
            }
        }

        if (context.getExecutedPhases() == null) {
            context.setExecutedPhasesExplicit(new LinkedList<Handler>());
        }


        //-------------------------------------------------------
        // finish up remaining links
        //-------------------------------------------------------
        if (operationContext != null) {
            operationContext.restoreMessageContext(context);
        }

        //-------------------------------------------------------
        // done, reset the flag
        //-------------------------------------------------------
        context.setNeedsToBeActivated(false);
    }


    /**
     * This method checks to see if additional work needs to be
     * done in order to complete the object reconstitution.
     * Some parts of the object restored from the readExternal()
     * cannot be completed until we have an object that gives us
     * a view of the active object graph from the active engine.
     * <p/>
     * NOTE: when activating an object, you only need to call
     * one of the activate methods (activate() or activateWithOperationContext())
     * but not both.
     *
     * @param operationCtx The operation context object that is a member of the active object graph
     */
    public void activateWithOperationContext(OperationContext operationCtx) {
        // see if there's any work to do
        if (!(context.needsToBeActivated())) {
            // return quick
            return;
        }

        String logCorrelationIDString = context.getLogCorrelationID();
        // trace point
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(logCorrelationIDString + ":activateWithOperationContext():  BEGIN");
        }

        if (operationCtx == null) {
            // won't be able to finish
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace(logCorrelationIDString +
                    ":activateWithOperationContext():  *** WARNING ***  No active OperationContext object is available.");
            }
            return;
        }

        //---------------------------------------------------------------------
        // locate the objects in the object graph
        //---------------------------------------------------------------------
        ConfigurationContext configCtx = operationCtx.getConfigurationContext();

        if (configCtx == null) {
            // won't be able to finish
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace(logCorrelationIDString +
                    ":activateWithOperationContext():  *** WARNING ***  No active ConfigurationContext object is available.");
            }
            return;
        }

        AxisConfiguration axisCfg = configCtx.getAxisConfiguration();

        AxisOperation axisOp = operationCtx.getAxisOperation();
        ServiceContext serviceCtx = operationCtx.getServiceContext();

        ServiceGroupContext serviceGroupCtx = null;
        AxisService axisSrv = null;
        AxisServiceGroup axisSG = null;

        if (serviceCtx != null) {
            serviceGroupCtx = serviceCtx.getServiceGroupContext();
            axisSrv = serviceCtx.getAxisService();
        }

        if (serviceGroupCtx != null) {
            axisSG = serviceGroupCtx.getDescription();
        }

        //---------------------------------------------------------------------
        // link to the objects in the object graph
        //---------------------------------------------------------------------

        context.setConfigurationContext(configCtx);

        context.setAxisOperation(axisOp);
        context.setAxisService(axisSrv);
        context.setAxisServiceGroup(axisSG);

        context.setServiceGroupContext(serviceGroupCtx);
        context.setServiceContext(serviceCtx);
        context.setOperationContext(operationCtx);

        //---------------------------------------------------------------------
        // reconcile the remaining objects
        //---------------------------------------------------------------------

        // We previously saved metaAxisMessage; restore it
        if (metaAxisMessage != null) {
            if (axisOp != null) {
                // TODO: check for the empty name
            	context.setAxisMessage(ActivateUtils.findMessage(axisOp,
                                                 metaAxisMessage.getQNameAsString(),
                                                 metaAxisMessage.getExtraName()));
            }
        }

        //---------------------------------------------------------------------
        // options
        //---------------------------------------------------------------------
        Options options = context.getOptions();
        if (options != null) {
            options.activate(configCtx);
        }

        String tmpID = context.getMessageID();

        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(logCorrelationIDString + ":activateWithOperationContext():   message ID [" +
                    tmpID + "]");
        }

        //---------------------------------------------------------------------
        // transports
        //---------------------------------------------------------------------

        // We previously saved metaTransportIn; restore it
        if (metaTransportIn != null) {
            QName qin = metaTransportIn.getQName();
            TransportInDescription tmpIn = null;
            try {
                tmpIn = axisCfg.getTransportIn(qin.getLocalPart());
            }
            catch (Exception exin) {
                // if a fault is thrown, log it and continue
                if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                    log.trace(logCorrelationIDString +
                        "activateWithOperationContext():  exception caught when getting the TransportInDescription [" +
                        qin.toString() + "]  from the AxisConfiguration [" +
                        exin.getClass().getName() + " : " + exin.getMessage() + "]");
                }

            }

            if (tmpIn != null) {
            	context.setTransportIn(tmpIn);
            } else {
            	context.setTransportIn(null);
            }
        } else {
        	context.setTransportIn(null);
        }

        // We previously saved metaTransportOut; restore it
        if (metaTransportOut != null) {
            QName qout = metaTransportOut.getQName();
            TransportOutDescription tmpOut = null;
            try {
                tmpOut = axisCfg.getTransportOut(qout.getLocalPart());
            }
            catch (Exception exout) {
                // if a fault is thrown, log it and continue
                if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                    log.trace(logCorrelationIDString +
                        "activateWithOperationContext():  exception caught when getting the TransportOutDescription [" +
                        qout.toString() + "]  from the AxisConfiguration [" +
                        exout.getClass().getName() + " : " + exout.getMessage() + "]");
                }
            }

            if (tmpOut != null) {
            	context.setTransportOut(tmpOut);
            } else {
            	context.setTransportOut(null);
            }
        } else {
        	context.setTransportOut(null);
        }

        //-------------------------------------------------------
        // reconcile the execution chain
        //-------------------------------------------------------
        if (metaExecutionChain != null) {
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace(logCorrelationIDString +
                        ":activateWithOperationContext(): reconciling the execution chain...");
            }

            context.setCurrentHandlerIndex(metaHandlerIndex);
            context.setCurrentPhaseIndex(metaPhaseIndex);

            context.setExecutionChain(restoreHandlerList(metaExecutionChain));

            try {
                deserializeSelfManagedData();
            }
            catch (Exception ex) {
                // log the exception
                if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                    log.trace(logCorrelationIDString +
                        ":activateWithOperationContext(): *** WARNING *** deserializing the self managed data encountered Exception [" +
                        ex.getClass().getName() + " : " + ex.getMessage() + "]", ex);
                }
            }
        }

        //-------------------------------------------------------
        // reconcile the lists for the executed phases
        //-------------------------------------------------------
        if (metaExecuted != null) {
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace(logCorrelationIDString +
                        ":activateWithOperationContext(): reconciling the executed chain...");
            }

            if (!(context.isExecutedPhasesReset())) {
            	context.setExecutedPhasesExplicit(
                        restoreExecutedList(context.getExecutedPhases(), metaExecuted));
            }
        }

        if (context.getExecutedPhases() == null) {
        	context.setExecutedPhasesExplicit(new LinkedList<Handler>());
        }

        //-------------------------------------------------------
        // done, reset the flag
        //-------------------------------------------------------
        context.setNeedsToBeActivated(false);

        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(logCorrelationIDString + ":activateWithOperationContext():  END");
        }
    }

    /**
     * Calls the serializeSelfManagedData() method of each handler that
     * implements the <bold>SelfManagedDataManager</bold> interface.
     * Handlers for this message context are identified via the
     * executionChain list.
     *
     * @param out The output stream
     */
    private void serializeSelfManagedData(ObjectOutput out) {
        selfManagedDataHandlerCount = 0;

        try {
        	List<Handler> executionChain = context.getExecutionChain();
        	Map<String, Object> selfManagedDataMap = context.getSelfManagedDataMap();
            if ((selfManagedDataMap == null)
                    || (executionChain == null)
                    || (selfManagedDataMap.size() == 0)
                    || (executionChain.size() == 0)) {
                out.writeBoolean(ExternalizeConstants.EMPTY_OBJECT);

                if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                    log.trace(context.getLogCorrelationID() + ":serializeSelfManagedData(): No data : END");
                }

                return;
            }

            // let's create a temporary list with the handlers
            List<Handler> flatExecChain = context.flattenPhaseListToHandlers(executionChain, null);

            //ArrayList selfManagedDataHolderList = serializeSelfManagedDataHelper(flatExecChain.iterator(), new ArrayList());
            List<SelfManagedDataHolder> selfManagedDataHolderList = serializeSelfManagedDataHelper(flatExecChain);

            if (selfManagedDataHolderList.size() == 0) {
                out.writeBoolean(ExternalizeConstants.EMPTY_OBJECT);

                if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                    log.trace(context.getLogCorrelationID() + ":serializeSelfManagedData(): No data : END");
                }

                return;
            }

            out.writeBoolean(ExternalizeConstants.ACTIVE_OBJECT);

            // SelfManagedData can be binary so won't be able to treat it as a
            // string - need to treat it as a byte []

            // how many handlers actually
            // returned serialized SelfManagedData
            out.writeInt(selfManagedDataHolderList.size());

            for (int i = 0; i < selfManagedDataHolderList.size(); i++) {
                out.writeObject(selfManagedDataHolderList.get(i));
            }

        }
        catch (IOException e) {
            if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                log.trace("MessageContext:serializeSelfManagedData(): Exception [" +
                    e.getClass().getName() + "]  description [" + e.getMessage() + "]", e);
            }
        }

    }

    /*
     * We don't need to create new instances of the handlers
     * since the executionChain is rebuilt after readExternal().
     * We just have to find them in the executionChain and
     * call each handler's deserializeSelfManagedData method.
     */
     private void deserializeSelfManagedData() throws IOException {
         try {
             for (int i = 0;
                  (selfManagedDataListHolder != null) && (i < selfManagedDataListHolder.size()); i++)
             {
                 SelfManagedDataHolder selfManagedDataHolder =
                         selfManagedDataListHolder.get(i);

                 String classname = selfManagedDataHolder.getClassname();
                 String qNameAsString = selfManagedDataHolder.getId();

                 SelfManagedDataManager handler = deserialize_getHandlerFromExecutionChain(
                		 context.getExecutionChain().iterator(), classname, qNameAsString);

                 if (handler == null) {
                     if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                         log.trace(context.getLogCorrelationID() + ":deserializeSelfManagedData():  [" +
                                 classname +
                                 "]  was not found in the executionChain associated with the message context.");
                     }

                     throw new IOException("The class [" + classname +
                             "] was not found in the executionChain associated with the message context.");
                 }

                 ByteArrayInputStream handlerData =
                         new ByteArrayInputStream(selfManagedDataHolder.getData());

                 // the handler implementing SelfManagedDataManager is responsible for repopulating
                 // the SelfManagedData in the MessageContext (this)

                 if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                     log.trace(context.getLogCorrelationID() +
                             ":deserializeSelfManagedData(): calling handler [" + classname + "] [" +
                             qNameAsString + "]  deserializeSelfManagedData method");
                 }

                 handler.deserializeSelfManagedData(handlerData, context);
                 handler.restoreTransientData(context);
             }
         }
         catch (IOException ioe) {
             if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                 log.trace(context.getLogCorrelationID() + ":deserializeSelfManagedData(): IOException thrown: " +
                         ioe.getMessage(), ioe);
             }
             throw ioe;
         }

     }

     /**
      * @return true if the data should be persisted as optimized attachments
      */
     private boolean getPersistOptimized(OldMessageContext context) {
         boolean persistOptimized = false;
         final Attachments attachments = context.getAttachments();
         if (attachments != null && attachments.getContentIDList().size() > 1) {
             persistOptimized = true;
             if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
 				log.trace("attachments present; persist optimized");
 			}
         }
         if (!persistOptimized) {
             Object property = context.getProperty(Configuration.ENABLE_MTOM);
             if (property != null && JavaUtils.isTrueExplicitly(property)) {
                 persistOptimized = true;
                 if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
 					log.trace("ENABLE_MTOM is set; persist optimized");
 				}
             }
         }
         if (!persistOptimized) {
             Object property = context.getProperty(Configuration.ENABLE_SWA);
             if (property != null && JavaUtils.isTrueExplicitly(property)) {
                 persistOptimized = true;
                 if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
 					log.trace("ENAABLE_SWA is set; persist optimized");
 				}
             }
         }
         if (!persistOptimized && LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
 			log.trace("No attachments or attachment settings; persist non-optimized");
 		}
         return persistOptimized;
     }

     /**
      * This is the helper method to do the recursion for serializeSelfManagedData()
      *
      * @param handlers
      * @return ArrayList
      */
     private List<SelfManagedDataHolder> serializeSelfManagedDataHelper(List<? extends Handler> handlers) {
         ArrayList<SelfManagedDataHolder> selfManagedDataHolderList = new ArrayList<SelfManagedDataHolder>();
         try {
             for(final Handler handler: handlers) {
                 //if (handler instanceof Phase)
                 //{
                 //    selfManagedDataHolderList = serializeSelfManagedDataHelper(((Phase)handler).getHandlers().iterator(), selfManagedDataHolderList);
                 //}
                 //else if (SelfManagedDataManager.class.isAssignableFrom(handler.getClass()))
                 if (SelfManagedDataManager.class.isAssignableFrom(handler.getClass())) {
                     // only call the handler's serializeSelfManagedData if it implements SelfManagedDataManager

                     if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                         log.trace(
                                 "MessageContext:serializeSelfManagedDataHelper(): calling handler  [" +
                                         handler.getClass().getName() + "]  name [" +
                                         handler.getName() + "]   serializeSelfManagedData method");
                     }

                     ByteArrayOutputStream baos_fromHandler =
                             ((SelfManagedDataManager) handler).serializeSelfManagedData(context);

                     if (baos_fromHandler != null) {
                         baos_fromHandler.close();

                         try {
                             SelfManagedDataHolder selfManagedDataHolder = new SelfManagedDataHolder(
                                     handler.getClass().getName(), handler.getName(),
                                     baos_fromHandler.toByteArray());
                             selfManagedDataHolderList.add(selfManagedDataHolder);
                             selfManagedDataHandlerCount++;
                         }
                         catch (Exception exc) {
                             if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                                 log.trace("MessageContext:serializeSelfManagedData(): exception [" +
                                     exc.getClass().getName() + "][" + exc.getMessage() +
                                     "]  in setting up SelfManagedDataHolder object for [" +
                                     handler.getClass().getName() + " / " + handler.getName() + "] ",
                                       exc);
                             }
                         }
                     }
                 }
             }

             return selfManagedDataHolderList;
         }
         catch (Exception ex) {
             if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                 log.trace("MessageContext:serializeSelfManagedData(): exception [" +
                     ex.getClass().getName() + "][" + ex.getMessage() + "]", ex);
             }
             return null;
         }

     }

     /**
      * During deserialization, the executionChain will be
      * re-constituted before the SelfManagedData is restored.
      * This means the handler instances are already available.
      * This method lets us find the handler instance from the
      * executionChain so we can call each one's
      * deserializeSelfManagedData method.
      *
      * @param it            The iterator from the executionChain object
      * @param classname     The class name
      * @param qNameAsString The QName in string form
      * @return SelfManagedDataManager handler
      */
     private SelfManagedDataManager deserialize_getHandlerFromExecutionChain(Iterator<? extends Handler> it,
                                                                             String classname,
                                                                             String qNameAsString) {
         SelfManagedDataManager handler_toreturn = null;

         try {
             while ((it.hasNext()) && (handler_toreturn == null)) {
                 Handler handler = it.next();

                 if (handler instanceof Phase) {
                     handler_toreturn = deserialize_getHandlerFromExecutionChain(
                             ((Phase) handler).getHandlers().iterator(), classname, qNameAsString);
                 } else if ((handler.getClass().getName().equals(classname))
                         && (handler.getName().equals(qNameAsString))) {
                     handler_toreturn = (SelfManagedDataManager) handler;
                 }
             }
             return handler_toreturn;
         }
         catch (ClassCastException e) {
             // Doesn't seem likely to happen, but just in case...
             // A handler classname in the executionChain matched up with our parameter
             // classname, but the existing class in the executionChain is a different
             // implementation than the one we saved during serializeSelfManagedData.
             // NOTE: the exception gets absorbed!

             if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                 log.trace(
                     "MessageContext:deserialize_getHandlerFromExecutionChain(): ClassCastException thrown: " +
                             e.getMessage(), e);
             }
             return null;
         }
     }

     /**
      * Process the list of handlers from the Phase object
      * into the appropriate meta data.
      *
      * @param phase   The Phase object containing a list of handlers
      * @param mdPhase The meta data object associated with the specified Phase object
      */
     private void setupPhaseList(Phase phase, MetaDataEntry mdPhase) {
         // get the list from the phase object
         List<Handler> handlers = phase.getHandlers();

         if (handlers.isEmpty()) {
             // done, make sure there is no list in the given meta data
             mdPhase.removeList();
             return;
         }

         // get the metadata on each member of the list

         int listSize = handlers.size();

         if (listSize > 0) {

             Iterator<Handler> i = handlers.iterator();

             while (i.hasNext()) {
                 Object obj = i.next();
                 String objClass = obj.getClass().getName();

                 // start the meta data entry for this object
                 MetaDataEntry mdEntry = new MetaDataEntry();
                 mdEntry.setClassName(objClass);

                 // get the correct object-specific name
                 String qnameAsString;

                 if (obj instanceof Phase) {
                     // nested condition, the phase object contains another phase!
                     Phase phaseObj = (Phase) obj;
                     qnameAsString = phaseObj.getName();

                     // add the list of handlers to the meta data
                     setupPhaseList(phaseObj, mdEntry);
                 } else if (obj instanceof Handler) {
                     Handler handlerObj = (Handler) obj;
                     qnameAsString = handlerObj.getName();
                 } else {
                     // TODO: will there be any other kinds of objects
                     // in the list?
                     qnameAsString = "NULL";
                 }

                 mdEntry.setQName(qnameAsString);

                 // done with setting up the meta data for the list entry
                 // so add it to the parent
                 mdPhase.addToList(mdEntry);

                 if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
                     log.trace(context.getLogCorrelationID() + ":setupPhaseList(): list entry class [" +
                             objClass + "] qname [" + qnameAsString + "]");
                 }

             } // end while entries in list
         } else {
             // a list with no entries
             // done, make sure there is no list in the given meta data
             mdPhase.removeList();
         }
     }

     /**
      * Using meta data for phases/handlers, create a linked list of actual
      * phase/handler objects.  The created list is composed of the objects
      * from the base list at the top of the created list followed by the
      * restored objects.
      *
      * @param base            Linked list of phase/handler objects
      * @param metaDataEntries Linked list of MetaDataEntry objects
      * @return LinkedList of objects or NULL if none available
      */
     private LinkedList<Handler> restoreExecutedList(LinkedList<Handler> base, LinkedList<MetaDataEntry> metaDataEntries) {
         if (metaDataEntries == null) {
             return base;
         }

         // get a list of existing handler/phase objects for the restored objects

         ArrayList<MetaDataEntry> tmpMetaDataList = new ArrayList<MetaDataEntry>(metaDataEntries);

         ArrayList<Handler> existingList = restoreHandlerList(tmpMetaDataList);

         if ((existingList == null) || (existingList.isEmpty())) {
             return base;
         }

         // set up a list to return

         LinkedList<Handler> returnedList = new LinkedList<Handler>();

         if (base != null) {
             returnedList.addAll(base);
         }

         returnedList.addAll(existingList);

         return returnedList;
     }

     /**
      * @param metaDataEntries ArrayList of MetaDataEntry objects
      * @return ArrayList of Handlers based on our list of handlers from the reconstituted deserialized list, and the existing handlers in the AxisConfiguration object.  May return null.
      */
     private ArrayList<Handler> restoreHandlerList(ArrayList<MetaDataEntry> metaDataEntries) {
         AxisConfiguration axisConfig = context.getConfigurationContext().getAxisConfiguration();

         List<Handler> existingHandlers = new ArrayList<Handler>();

         // TODO: I'm using clone for the ArrayList returned from axisConfig object.
         //     Does it do a deep clone of the Handlers held there?  Does it matter?
         switch (context.getFlow()) {
             case IN:
                 existingHandlers.addAll(axisConfig.getInFlowPhases());
                 break;

             case OUT:
             	existingHandlers.addAll(axisConfig.getOutFlowPhases());
                 break;

             case IN_FAULT:
             	existingHandlers.addAll(axisConfig.getInFaultFlowPhases());
                 break;

             case OUT_FAULT:
             	existingHandlers.addAll(axisConfig.getOutFaultFlowPhases());
                 break;
         }

         existingHandlers = context.flattenHandlerList(existingHandlers, null);

         ArrayList<Handler> handlerListToReturn = new ArrayList<Handler>();

         for (int i = 0; i < metaDataEntries.size(); i++) {
             Handler handler = (Handler) ActivateUtils
                     .findHandler(existingHandlers, metaDataEntries.get(i));

             if (handler != null) {
                 handlerListToReturn.add(handler);
             }
         }

         return handlerListToReturn;
     }
}
