package org.apache.axis2.description.hierarchy;

import org.apache.axis2.description.AxisBindingOperation;

/**
 * Mixin interface that must be implemented by the descendants of an
 * AxisBindingOperation within the description hierarchy.
 *
 * @author jfager
 */
public interface BindingOperationDescendant
	extends BindingDescendant
{
	/**
	 * @return The ancestral AxisBindingOperation of the object's current
	 *         description hierarchy.
	 */
	AxisBindingOperation getBindingOperation();
}
