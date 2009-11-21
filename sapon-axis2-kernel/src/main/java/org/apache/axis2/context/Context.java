package org.apache.axis2.context;

import java.util.Map;

import org.apache.axis2.AxisFault;

public interface Context<P extends Context<?>> {

	P getParent();

    /**
     * @param context
     * @return true if the context is an ancestor
     */
    boolean isAncestor(Context<?> context);

    /**
     * @return An unmodifiable view of the context's properties.
     */
	Map<String, Object> getProperties();

    /**
     * An iterator over a collection of <code>String</code> objects, which are
     * the keys in the properties object.
     *
     * @return Iterator over a collection of keys
     */
    Iterable<String> getPropertyNames();

    /**
     * Retrieves an object given a key.
     *
     * @param key - if not found, will return null
     * @return Returns the property.
     */
    Object getProperty(String key);

    /**
     * Retrieves an object given a key. Only searches at this level
     * i.e. getLocalProperty on MessageContext does not look in
     * the OperationContext properties map if a local result is not
     * found.
     *
     * @param key - if not found, will return null
     * @return Returns the property.
     */
    Object getLocalProperty(String key);

    /**
     * Retrieves an object given a key. The retrieved property will not be replicated to
     * other nodes in the clustered scenario.
     *
     * @param key - if not found, will return null
     * @return Returns the property.
     */
    Object getPropertyNonReplicable(String key);

    /**
     * Store a property in this context
     *
     * @param key
     * @param value
     */
    void setProperty(String key, Object value);

    /**
     * Store a property in this context.
     * But these properties should not be replicated when Axis2 is clustered.
     *
     * @param key
     * @param value
     */
    void setNonReplicableProperty(String key, Object value);

    /**
     * Remove a property. Only properties at this level will be removed.
     * Properties of the parents cannot be removed using this method.
     *
     * @param key
     */
    void removeProperty(String key);

    /**
     * Remove a property. Only properties at this level will be removed.
     * Properties of the parents cannot be removed using this method.
     * The removal of the property will not be replicated when Axis2 is clustered.
     *
     * @param key
     */
    void removePropertyNonReplicable(String key);

    /**
     * Get the property differences since the last transmission by the clustering
     * mechanism
     *
     * @return The property differences
     */
    Map<String, Object> getPropertyDifferences();

    /**
     * Once the clustering mechanism transmits the property differences,
     * it should call this method to avoid retransmitting stuff that has already
     * been sent.
     */
    void clearPropertyDifferences();

    /**
     * @param context
     */
    void setParent(P context);

    /**
     * This will set the properties to the context. But in setting that one may need to "copy" all
     * the properties from the source properties to the target properties. To enable this we introduced
     * a property ({@link #COPY_PROPERTIES}) so that if set to true, this code
     * will copy the whole thing, without just referencing to the source.
     *
     * @param properties
     */
    void setProperties(Map<String, Object> properties);

    /**
     * This will do a copy of the given properties to the current properties
     * table.
     *
     * @param props The table of properties to copy
     */
    void mergeProperties(Map<String, Object> props);

    long getLastTouchedTime();

    void setLastTouchedTime(long t);

    void touch();

    void flush() throws AxisFault;

    ConfigurationContext getRootContext();
}
