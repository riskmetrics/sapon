package org.apache.axis2.description.hierarchy;

import org.apache.axis2.description.AxisBinding;

/**
 * Mixin interface that must be implemented by the descendants of an
 * AxisBinding within the description hierarchy.
 *
 * @author jfager
 */
public interface BindingDescendant
	extends EndpointDescendant
{
	/**
	 * @return The ancestral AxisBinding of the object's current
	 *         description hierarchy.
	 */
	AxisBinding getBinding();
}
