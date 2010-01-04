package org.apache.axis2.description.hierarchy;

import org.apache.axis2.description.AxisService;

/**
 * Mixin interface that must be implemented by the descendants of an
 * AxisService within the description hierarchy.
 *
 * @author jfager
 */
public interface ServiceDescendant
	extends ServiceGroupDescendant
{

	/**
	 * @return The ancestral AxisService of the object's current
	 *         description hierarchy.
	 */
	AxisService getService();
}
