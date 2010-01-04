package org.apache.axis2.description.hierarchy;

import org.apache.axis2.description.AxisEndpoint;

/**
 * Mixin interface that must be implemented by the descendants of an
 * AxisEndpoint within the description hierarchy.
 *
 * @author jfager
 */
public interface EndpointDescendant
	extends ServiceDescendant
{
	/**
	 * @return The ancestral AxisEndpoint of the object's current
	 *         description hierarchy.
	 */
	AxisEndpoint getEndpoint();
}
