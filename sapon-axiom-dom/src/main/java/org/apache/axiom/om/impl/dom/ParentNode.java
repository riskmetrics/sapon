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

package org.apache.axiom.om.impl.dom;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMComment;
import org.apache.axiom.om.OMDocType;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMProcessingInstruction;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.OMContainerEx;
import org.apache.axiom.om.impl.OMNodeEx;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.factory.OMDOMFactory;
import org.apache.axiom.om.impl.traverse.OMChildrenIterator;
import org.apache.axiom.om.impl.traverse.OMChildrenLocalNameIterator;
import org.apache.axiom.om.impl.traverse.OMChildrenNamespaceIterator;
import org.apache.axiom.om.impl.traverse.OMChildrenQNameIterator;
import org.apache.axiom.om.impl.traverse.OMFilterIterator;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class ParentNode extends ChildNode implements OMContainerEx {

    protected ChildNode firstChild;

    protected ChildNode lastChild;

    /** @param ownerDocument  */
    protected ParentNode(DocumentImpl ownerDocument, OMFactory factory) {
        super(ownerDocument, factory);
    }

    protected ParentNode(OMFactory factory) {
        super(factory);
    }

    // /
    // /OMContainer methods
    // /

    public void addChild(OMNode omNode) {
        if (omNode.getOMFactory() instanceof OMDOMFactory) {
            Node domNode = (Node) omNode;
            if (this.ownerNode != null && !domNode.getOwnerDocument().equals(this.ownerNode)) {
                this.appendChild(this.ownerNode.importNode(domNode, true));
            } else {
                this.appendChild(domNode);
            }
        } else {
            addChild(importNode(omNode));
        }

    }

    public void buildNext() {
        if (!this.done) {
			builder.next();
		}
    }

    public Iterable<OMNode> getChildren() {
        return new OMChildrenIterator(getFirstOMChild());
    }

    /**
     * Returns an iterator of child nodes having a given qname.
     *
     * @see org.apache.axiom.om.OMContainer#getChildrenWithName (javax.xml.namespace.QName)
     */
    public Iterable<OMElement> getChildrenWithName(QName elementQName) throws OMException {
        return new OMChildrenQNameIterator(getFirstOMChild(), elementQName);
    }

    public Iterable<OMElement> getChildrenWithLocalName(String localName) {
        return new OMChildrenLocalNameIterator(getFirstOMChild(),
                                               localName);
    }


    public Iterable<OMElement> getChildrenWithNamespaceURI(String uri) {
        return new OMChildrenNamespaceIterator(getFirstOMChild(),
                                               uri);
    }

    /**
     * Returns the first OMElement child node.
     *
     * @see org.apache.axiom.om.OMContainer#getFirstChildWithName (javax.xml.namespace.QName)
     */
    public OMElement getFirstChildWithName(QName elementQName)
            throws OMException {
        Iterator<OMElement> children = new OMChildrenQNameIterator(getFirstOMChild(),
                                                        elementQName);
        if(children.hasNext()) {
            return children.next();
        }
        return null;
    }

    public OMNode getFirstOMChild() {
        while ((firstChild == null) && !done) {
            buildNext();
        }
        return firstChild;
    }

    public void setFirstChild(OMNode omNode) {
        if (firstChild != null) {
            ((OMNodeEx) omNode).setParent(this);
        }
        this.firstChild = (ChildNode) omNode;
    }

    /**
     * Forcefully set the last child
     * @param omNode
     */
    public void setLastChild(OMNode omNode) {
        this.lastChild = (ChildNode) omNode;
    }

    // /
    // /DOM Node methods
    // /

    @Override
	public NodeList getChildNodes() {
        if (!this.done) {
            this.build();
        }
        return new NodeListImpl() {
            @Override
			protected Iterator<Node> getIterator() {
                return new OMFilterIterator<Node>(getChildren().iterator()) {
					@Override
					protected Node matches(OMNode node) {
						return (Node)node;
					}
                };
            }
        };
    }

    @Override
	public Node getFirstChild() {
        return (Node) this.getFirstOMChild();
    }

    @Override
	public Node getLastChild() {
        if (!this.done) {
            this.build();
        }
        return this.lastChild;
    }

    @Override
	public boolean hasChildNodes() {
        while ((firstChild == null) && !done) {
            buildNext();
        }
        return this.firstChild != null;
    }

    /**
     * Inserts newChild before the refChild. If the refChild is null then the newChild is made the
     * last child.
     */
    @Override
	public Node insertBefore(Node newChild, Node refChild) throws DOMException {

        ChildNode newDomChild = (ChildNode) newChild;
        ChildNode refDomChild = (ChildNode) refChild;

        if (this == newChild || !isAncestor(newChild)) {
            throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN,
                                           "HIERARCHY_REQUEST_ERR", null));
        }

        if (newDomChild.parentNode != null && newDomChild.ownerNode == this.ownerNode) {
            //If the newChild is already in the tree remove it
            newDomChild.parentNode.removeChild(newDomChild);
        }

        if (!(this instanceof Document)
                && !(this.ownerNode == newDomChild.getOwnerDocument())) {
            throw new DOMException(DOMException.WRONG_DOCUMENT_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN,
                                           "WRONG_DOCUMENT_ERR", null));
        }

        if (this.isReadonly()) {
            throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN,
                                           "NO_MODIFICATION_ALLOWED_ERR", null));
        }

        if (this instanceof Document) {
            if (newDomChild instanceof ElementImpl) {
                if (((DocumentImpl) this).documentElement != null) {
                    // Throw exception since there cannot be two document elements
                    throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
                                           DOMMessageFormatter.formatMessage(
                                                   DOMMessageFormatter.DOM_DOMAIN,
                                                   "HIERARCHY_REQUEST_ERR", null));
                }
                if (newDomChild.parentNode == null) {
                    newDomChild.parentNode = this;
                }
                // set the document element
                ((DocumentImpl) this).documentElement = (ElementImpl) newDomChild;
            } else if (!(newDomChild instanceof CommentImpl
                    || newDomChild instanceof ProcessingInstructionImpl
                    || newDomChild instanceof DocumentFragmentImpl)) {
                // TODO: we should also check for DocumentType,
                //       but since we don't have an implementation yet, we can leave it
                //       like this for now
                throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
                        DOMMessageFormatter.formatMessage(
                                DOMMessageFormatter.DOM_DOMAIN,
                                "HIERARCHY_REQUEST_ERR", null));
            }
        }
        boolean compositeChild = newDomChild.nextSibling != null;
        ChildNode endChild = null;

        if(compositeChild) {
            ChildNode tempNextChild = newDomChild.nextSibling;
            while(tempNextChild != null) {
                tempNextChild.parentNode = this;
                endChild = tempNextChild;
                tempNextChild = tempNextChild.nextSibling;
            }
        }

        if (refChild == null) { // Append the child to the end of the list
            // if there are no children
            if (this.lastChild == null && firstChild == null) {
                if(compositeChild) {
                    this.lastChild = endChild;
                } else {
                    this.lastChild = newDomChild;
                }
                this.firstChild = newDomChild;
                this.firstChild.isFirstChild(true);
                newDomChild.setParent(this);
            } else {
                this.lastChild.nextSibling = newDomChild;
                newDomChild.previousSibling = this.lastChild;

                if(compositeChild) {
                    this.lastChild = endChild;
                } else {
                    this.lastChild = newDomChild;
                }
                this.lastChild.nextSibling = null;
            }
            if (newDomChild.parentNode == null) {
                newDomChild.parentNode = this;
            }
            return newChild;
        } else {
            boolean found = false;
            for(OMNode node: getChildren()) {
                ChildNode tempNode = (ChildNode)node;

                if (tempNode.equals(refChild)) {
                    // RefChild found
                    if (this.firstChild == tempNode) { // If the refChild is the
                        // first child

                        if (newChild instanceof DocumentFragmentImpl) {
                            // The new child is a DocumentFragment
                            DocumentFragmentImpl docFrag =
                                    (DocumentFragmentImpl) newChild;
                            this.firstChild = docFrag.firstChild;
                            docFrag.lastChild.nextSibling = refDomChild;
                            refDomChild.previousSibling =
                                    docFrag.lastChild.nextSibling;

                        } else {

                            // Make the newNode the first Child
                            this.firstChild = newDomChild;

                            newDomChild.nextSibling = refDomChild;
                            refDomChild.previousSibling = newDomChild;

                            this.firstChild.isFirstChild(true);
                            refDomChild.isFirstChild(false);
                            newDomChild.previousSibling = null; // Just to be
                            // sure :-)

                        }
                    } else { // If the refChild is not the fist child
                        ChildNode previousNode = refDomChild.previousSibling;

                        if (newChild instanceof DocumentFragmentImpl) {
                            // the newChild is a document fragment
                            DocumentFragmentImpl docFrag =
                                    (DocumentFragmentImpl) newChild;

                            previousNode.nextSibling = docFrag.firstChild;
                            docFrag.firstChild.previousSibling = previousNode;

                            docFrag.lastChild.nextSibling = refDomChild;
                            refDomChild.previousSibling = docFrag.lastChild;
                        } else {

                            previousNode.nextSibling = newDomChild;
                            newDomChild.previousSibling = previousNode;

                            newDomChild.nextSibling = refDomChild;
                            refDomChild.previousSibling = newDomChild;
                        }

                    }
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new DOMException(DOMException.NOT_FOUND_ERR,
                                       DOMMessageFormatter.formatMessage(
                                               DOMMessageFormatter.DOM_DOMAIN,
                                               "NOT_FOUND_ERR", null));
            }

            if (newDomChild.parentNode == null) {
                newDomChild.parentNode = this;
            }

            return newChild;
        }
    }

    /** Replaces the oldChild with the newChild. */
    @Override
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        ChildNode newDomChild = (ChildNode) newChild;
        ChildNode oldDomChild = (ChildNode) oldChild;

        if (newChild == null) {
            return this.removeChild(oldChild);
        }

        if (this == newChild || !isAncestor(newChild)) {
            throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN,
                                           "HIERARCHY_REQUEST_ERR", null));
        }

        if (newDomChild != null &&
                //This is the case where this is an Element in the document
                (this.ownerNode != null && !this.ownerNode.equals(newDomChild.ownerNode)) ||
                //This is the case where this is the Document itself
                (this.ownerNode == null && !this.equals(newDomChild.ownerNode))) {
            throw new DOMException(DOMException.WRONG_DOCUMENT_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN,
                                           "WRONG_DOCUMENT_ERR", null));
        }

        if (this.isReadonly()) {
            throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN,
                                           "NO_MODIFICATION_ALLOWED_ERR", null));
        }

        boolean found = false;
        for(OMNode node: getChildren()) {
            ChildNode tempNode = (ChildNode) node;
            if (tempNode.equals(oldChild)) {
                if (newChild instanceof DocumentFragmentImpl) {
                    DocumentFragmentImpl docFrag =
                            (DocumentFragmentImpl) newDomChild;
                    ChildNode child = (ChildNode) docFrag.getFirstChild();
                    this.replaceChild(child, oldChild);

                    //set the parent of all kids to me
                    while(child != null) {
                        child.parentNode = this;
                        child = child.nextSibling;
                    }

                    this.lastChild = (ChildNode)docFrag.getLastChild();

                } else {
                    if (this.firstChild == oldDomChild) {

                        if (this.firstChild.nextSibling != null) {
                            this.firstChild.nextSibling.previousSibling = newDomChild;
                            newDomChild.nextSibling = this.firstChild.nextSibling;
                        }

                        //Cleanup the current first child
                        this.firstChild.parentNode = null;
                        this.firstChild.nextSibling = null;

                        //Set the new first child
                        this.firstChild = newDomChild;


                    } else {
                        newDomChild.nextSibling = oldDomChild.nextSibling;
                        newDomChild.previousSibling = oldDomChild.previousSibling;

                        oldDomChild.previousSibling.nextSibling = newDomChild;

                        // If the old child is not the last
                        if (oldDomChild.nextSibling != null) {
                            oldDomChild.nextSibling.previousSibling = newDomChild;
                        } else {
                            this.lastChild = newDomChild;
                        }

                    }

                    newDomChild.parentNode = this;
                }
                found = true;

                // remove the old child's references to this tree
                oldDomChild.nextSibling = null;
                oldDomChild.previousSibling = null;
                oldDomChild.parentNode = null;
            }
        }

        if (!found) {
			throw new DOMException(DOMException.NOT_FOUND_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR",
                                           null));
		}

        return oldChild;
    }

    /** Removes the given child from the DOM Tree. */
    @Override
	public Node removeChild(Node oldChild) throws DOMException {
        // Check if this node is readonly
        if (this.isReadonly()) {
            throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN,
                                           "NO_MODIFICATION_ALLOWED_ERR", null));
        }

        // Check if the Child is there
        boolean childFound = false;
        for(OMNode node: getChildren()) {
            ChildNode tempNode = (ChildNode) node;
            if (tempNode.equals(oldChild)) {

                if (this.firstChild == tempNode) {
                    // If this is the first child
                    this.firstChild = null;
                    this.lastChild = null;
                    tempNode.parentNode = null;
                } else if (this.lastChild == tempNode) {
                    // not the first child, but the last child
                    ChildNode prevSib = tempNode.previousSibling;
                    this.lastChild = prevSib;
                    prevSib.nextSibling = null;
                    tempNode.parentNode = null;
                    tempNode.previousSibling = null;
                } else {

                    ChildNode oldDomChild = (ChildNode) oldChild;
                    ChildNode privChild = oldDomChild.previousSibling;

                    privChild.nextSibling = oldDomChild.nextSibling;
                    oldDomChild.nextSibling.previousSibling = privChild;

                    // Remove old child's references to this tree
                    oldDomChild.nextSibling = null;
                    oldDomChild.previousSibling = null;
                }
                // Child found
                childFound = true;
            }
        }

        if (!childFound) {
			throw new DOMException(DOMException.NOT_FOUND_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR",
                                           null));
		}

        return oldChild;
    }

    private boolean isAncestor(Node newNode) {

        // TODO isAncestor
        return true;
    }

    @Override
	public Node cloneNode(boolean deep) {

        ParentNode newnode = (ParentNode) super.cloneNode(deep);

        // set owner document
        newnode.ownerNode = ownerNode;

        // Need to break the association w/ original kids
        newnode.firstChild = null;
        newnode.lastChild = null;

        // Then, if deep, clone the kids too.
        if (deep) {
            for (ChildNode child = firstChild; child != null;
                 child = child.nextSibling) {
                newnode.appendChild(child.cloneNode(true));
            }
        }
        return newnode;
    }

    /**
     * This method is intended only to be used by Axiom intenals when merging Objects from different
     * Axiom implementations to the DOOM implementation.
     *
     * @param child
     */
    protected OMNode importNode(OMNode child) {
        int type = child.getType();
        switch (type) {
            case (OMNode.ELEMENT_NODE): {
                OMElement childElement = (OMElement) child;
                OMElement newElement = (new StAXOMBuilder(this.factory,
                                                          childElement.getXMLStreamReader()))
                        .getDocumentElement();
                newElement.build();
                return (OMNode) this.ownerNode.importNode((Element) newElement,
                                                          true);
            }
            case (OMNode.TEXT_NODE): {
                OMText importedText = (OMText) child;
                OMText newText;
                if (importedText.isBinary()) {
                    boolean isOptimize = importedText.isOptimized();
                    newText = this.factory.createOMText(importedText
                            .getDataHandler(), isOptimize);
                } else if (importedText.isCharacters()) {
                    newText = new TextImpl((DocumentImpl) this.getOwnerDocument(),
                                           importedText.getTextCharacters(), this.factory);
                } else {
                    newText = new TextImpl((DocumentImpl) this.getOwnerDocument(),
                                           importedText.getText(), this.factory);
                }
                return newText;
            }

            case (OMNode.PI_NODE): {
                OMProcessingInstruction importedPI = (OMProcessingInstruction) child;
                OMProcessingInstruction newPI = this.factory
                        .createOMProcessingInstruction(this,
                                                       importedPI.getTarget(),
                                                       importedPI.getValue());
                return newPI;
            }
            case (OMNode.COMMENT_NODE): {
                OMComment importedComment = (OMComment) child;
                OMComment newComment = this.factory.createOMComment(this,
                                                                    importedComment.getValue());
                DocumentImpl doc;
                if (this instanceof DocumentImpl) {
                    doc = (DocumentImpl) this;
                } else {
                    doc = (DocumentImpl) (this).getOwnerDocument();
                }
                newComment = new CommentImpl(doc, importedComment.getValue(),
                                             this.factory);
                return newComment;
            }
            case (OMNode.DTD_NODE): {
                OMDocType importedDocType = (OMDocType) child;
                OMDocType newDocType = this.factory.createOMDocType(this,
                                                                    importedDocType.getValue());
                return newDocType;
            }
            default: {
                throw new UnsupportedOperationException(
                        "Not Implemented Yet for the given node type");
            }
        }
    }

    @Override
	public String getTextContent() throws DOMException {
        Node child = getFirstChild();
        if (child != null) {
            Node next = child.getNextSibling();
            if (next == null) {
                return hasTextContent(child) ? ((NodeImpl)child).getTextContent() : "";
            }
            StringBuffer buf = new StringBuffer();
            getTextContent(buf);
            return buf.toString();
        } else {
            return "";
        }
    }

    @Override
	void getTextContent(StringBuffer buf) throws DOMException {
        Node child = getFirstChild();
        while (child != null) {
            if (hasTextContent(child)) {
                ((NodeImpl)child).getTextContent(buf);
            }
            child = child.getNextSibling();
        }
    }

    // internal method returning whether to take the given node's text content
    private static boolean hasTextContent(Node child) {
        return child.getNodeType() != Node.COMMENT_NODE &&
            child.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE /* &&
            (child.getNodeType() != Node.TEXT_NODE ||
             ((TextImpl) child).isIgnorableWhitespace() == false)*/;
    }

    @Override
	public void setTextContent(String textContent) throws DOMException {
        // get rid of any existing children
        // TODO: there is probably a better way to remove all children
        Node child;
        while ((child = getFirstChild()) != null) {
            removeChild(child);
        }
        // create a Text node to hold the given content
        if (textContent != null && textContent.length() != 0) {
            addChild(factory.createOMText(textContent));
        }
    }
}
