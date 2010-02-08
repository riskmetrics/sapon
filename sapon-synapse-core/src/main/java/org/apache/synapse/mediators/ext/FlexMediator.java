package org.apache.synapse.mediators.ext;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;

/**
 * Implement this interface in a Mediator used as the target of a ClassMediator,
 * and you'll get complete control over how you're initialized based on the
 * configuration element given in the synapse.xml.
 *
 * @author jfager
 */
public interface FlexMediator extends Mediator {
	/**
	 * You're an adult.  Here's the config element that the user created you
	 * with, go do what you'd like with it.
	 *
	 * @param elem
	 */
	void initWith(OMElement elem);
}
