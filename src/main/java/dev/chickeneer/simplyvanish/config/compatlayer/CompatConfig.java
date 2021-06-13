package dev.chickeneer.simplyvanish.config.compatlayer;


import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CONVENTIONS:
 * - Return strings if objects can be made strings.
 * - No exceptions, rather leave elements out of lists.
 * - Lists of length 0 and null can not always be distinguished (?->extra safe wrapper ?)
 * - All contents are treated alike, even if given as a string (!): true and 'true', 1 and '1'
 * <hr>
 * Convention for methods without default value is to return null if not present, except for the getKeys and related methods.
 */
public interface CompatConfig {

    // Boolean

    /**
     * Only accepts true and false , 'true' and 'false'.
     *
     * @param path
     * @param defaultValue
     * @return
     */
    Boolean getBoolean(String path, Boolean defaultValue);

    Boolean getBoolean(String path);

    // Long
    Long getLong(String path);

    Long getLong(String path, Long defaultValue);

    // Double
    Double getDouble(String path);

    Double getDouble(String path, Double defaultValue);

    List<Double> getDoubleList(String path);

    List<Double> getDoubleList(String path, List<Double> defaultValue);

    // Integer (abbreviation)
    Integer getInt(String path);

    Integer getInt(String path, Integer defaultValue);

    List<Integer> getIntList(String path);

    List<Integer> getIntList(String path, List<Integer> defaultValue);

    // Integer (full name)
    Integer getInteger(String path);

    Integer getInteger(String path, Integer defaultValue);

    List<Integer> getIntegerList(String path);

    List<Integer> getIntegerList(String path, List<Integer> defaultValue);

    // String
    String getString(String path);

    String getString(String path, String defaultValue);

    List<String> getStringList(String path);

    List<String> getStringList(String path, List<String> defaultValue);

    /**
     * Get a HashSet from a list.
     *
     * @param path
     * @return
     */
    Set<String> getSetFromStringList(String path);

    /**
     * Get a HashSet from a list.
     *
     * @param path
     * @param defaultValue
     * @return
     */
    Set<String> getSetFromStringList(String path, Set<String> defaultValue);

    /**
     * Get a HashSet from a list, with string options which are applied if a list was read.
     *
     * @param path
     * @param defaultValue
     * @param trim         if to trim entries.
     * @param lowerCase    If to add entries as lower-case.
     * @return
     */
    Set<String> getSetFromStringList(String path, Set<String> defaultValue, boolean trim, boolean lowerCase);

    // Generic methods:
    Object get(String path);

    Object get(String path, Object defaultValue);

    Object getProperty(String path);

    Object getProperty(String path, Object defaultValue);

    void set(String path, Object value);

    void setProperty(String path, Object value);

    /**
     * Add entry in form of a list.
     *
     * @param path
     * @param set
     */
    <T> void setAsList(String path, Set<T> set);

    /**
     * Add map entries as section entries at path - meant for simple keys.
     *
     * @param path
     * @param map
     */
    void setAsSection(String path, Map<String, ?> map);

    /**
     * Remove a path (would also remove sub sections, unless for path naming problems).
     *
     * @param path
     */
    void remove(String path);

    /**
     * Works same as remove(path): removes properties and sections alike.
     *
     * @param path
     */
    void removeProperty(String path);

    // Contains/has
    boolean hasEntry(String path);

    boolean contains(String path);

    // Keys (Object): [possibly deprecated]

    /**
     * @param path
     * @return
     * @deprecated Seems not to be supported anymore by new configuration, use getStringKeys(String path);
     */
    List<Object> getKeys(String path);

    /**
     * @return
     * @deprecated Seems not to be supported anymore by new configuration, use getStringKeys();
     */
    List<Object> getKeys();

    // Keys (String):

    /**
     * @return never null
     */
    List<String> getStringKeys();

    List<String> getStringKeys(String path);

    /**
     * Get all keys from the section (deep or shallow).
     *
     * @param path
     * @param deep
     * @return Never null.
     */
    Set<String> getStringKeys(String path, boolean deep);

    /**
     * convenience method.
     *
     * @param path
     * @return never null
     */
    Set<String> getStringKeysDeep(String path);

    // Values:

    /**
     * Equivalent to new config: values(true)
     *
     * @return
     */
    Map<String, Object> getValuesDeep();

    // Technical / IO:

    /**
     * False if not supported.
     *
     * @param sep
     * @return
     */
    boolean setPathSeparatorChar(char sep);

    void load();

    boolean save();

    /**
     * Clear all contents.
     */
    void clear();

    /**
     * Return a YAML-String representation of the contents, null if not supported.
     *
     * @return null if not supported.
     */
    String getYAMLString();

    /**
     * Add all entries from the YAML-String representation to the configuration, forget or clear all previous entries.
     *
     * @return
     */
    boolean fromYamlString(String input);


}
