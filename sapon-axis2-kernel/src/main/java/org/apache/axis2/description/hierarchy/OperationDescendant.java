package org.apache.axis2.description.hierarchy;

import org.apache.axis2.description.AxisOperation;

/**
 * Mixin interface that must be implemented by the descendants of an
 * AxisOperation within the description hierarchy.
 *
 * @author jfager
 */
public interface OperationDescendant
	extends ServiceDescendant
{

	/**
	 * @return The ancestral AxisOperation of the object's current
	 *         description hierarchy.
	 */
	AxisOperation getOperation();
}
