package org.apache.axis2.description.hierarchy;

import org.apache.axis2.description.AxisServiceGroup;

/**
 * Mixin interface that must be implemented by the descendants of an
 * AxisServiceGroup within the description hierarchy.
 *
 * @author jfager
 */
public interface ServiceGroupDescendant
	extends ConfigurationDescendant
{
	/**
	 * @return The ancestral AxisServiceGroup of the object's current
	 *         description hierarchy.
	 */
	AxisServiceGroup getServiceGroup();
}
