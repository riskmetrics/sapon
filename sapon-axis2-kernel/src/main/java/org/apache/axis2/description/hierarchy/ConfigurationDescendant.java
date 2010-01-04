package org.apache.axis2.description.hierarchy;

import org.apache.axis2.engine.AxisConfiguration;

/**
 * Mixin interface that must be implemented by the descendants of an
 * AxisConfiguration within the description hierarchy.
 *
 * @author jfager
 */
public interface ConfigurationDescendant {

	/**
	 * @return The ancestral AxisConfiguration of the object's current
	 *         description hierarchy.
	 */
	AxisConfiguration getConfiguration();
}
