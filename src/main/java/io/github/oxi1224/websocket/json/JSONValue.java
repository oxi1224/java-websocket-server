package io.github.oxi1224.websocket.json;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Utility class which handles converting java classes into JSON-acceptable values
 * also includes utility methods and classes for Null and Array JSON values,
 * handles casting back to java classes
 *
 * @see JSONObject
 */
public class JSONValue {
  /**
   * Validates if a given Object is a valid JSON-acceptable value
   * @param obj - The object to check
   */
  public static boolean isValidValue(Object obj) {
    if (
      obj instanceof JSONObject ||
      obj instanceof String ||
      obj instanceof Number ||
      obj instanceof Boolean ||
      obj instanceof Null ||
      obj instanceof Array ||
      obj instanceof JSONValue
    ) return true;
    return false;
  }
  
  /** The value of JSON Null (required as Java has no built-in Null class) */
  public static final Class<?> NullValue = null;
  
  /** Required as Java has no built-in Null class */
  public static class Null {
    public String getValue() { return null; }
    @Override
    public String toString() { return "null"; }
  }
  
  /**
   * Class which represents a JSON array
   *
   * @see JSONValue
   */
  public static class Array extends ArrayList<JSONValue> {
    public Array() {
      super();
    }

    public Array(JSONValue ...objects) {
      super();
      for (JSONValue obj : objects) {
        add(obj);
      }
    }

    public Array(Object ...objects) {
      super();
      for (Object obj : objects) {
        add(new JSONValue(obj));
      }
    }
    
    @Override
    public void add(int i, JSONValue obj) {
      if (!isValidValue(obj)) throw new JSONError(obj);
      super.add(i, obj);
    }
    
    @Override
    public boolean addAll(Collection<? extends JSONValue> c) {
      for (JSONValue obj : c) {
        add(obj);
      }
      return true;
    }
    
    @Override
    public boolean addAll(int i, Collection<? extends JSONValue> c) {
      for (JSONValue obj : c) {
        add(i, obj);
        i++;
      }
      return true;
    }
  
    /**
     * Gets a value at index and casts it to specified class
     * @apiNote Causes JSONError if the user provides an invalid class
     * @param i - The index of the item
     * @param castType - The class the value will get casted to
     * @return The value at i casted to the given class
     */
    public <T> T get(int i, Class<T> castType) {
      JSONValue jValue = get(i);
      try {
        return castType.cast(jValue.getValue());
      } catch (ClassCastException e) {
        throw new JSONError("Failed to cast value to " + castType.getName());
      }
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      for (Object obj : this) {
        if (obj instanceof String) {
          sb.append("\"" + obj.toString() + "\"");
        } else {
          sb.append(obj.toString());
        }
        sb.append(",");
      }
      sb.delete(sb.length() - 1, sb.length());
      sb.append("]");
      return sb.toString();
    }
  }
  
  /** The actual value of the class */
  private Object value;
  
  /**
   * Creates a new JSONValue class, checking if given input is valid
   * @apiNote Causes JSONError if given object isn't valid for JSONValue
   * @param value - The object to convert
   */
  public JSONValue(Object value) {
    if (!isValidValue(value)) throw new JSONError(value);
    this.value = value;
  }
  
  /**
   * Gets the value and casts it to the specified class
   * @apiNote Causes JSONError if value cannot be casted to class
   * @param castClass - The class to cast to
   * @return The value casted to given class
   */
  public <T> T getValue(Class<T> castClass) {
    try {
      return this.value instanceof Null ? null : castClass.cast(this.value);
    } catch (ClassCastException e) {
      throw new JSONError("Failed to cast value to " + castClass.getName());
    }
  }
  
  /**
   * Returns the value as {@link Object}
   */
  public Object getValue() {
    return this.value instanceof Null ? null : this.value;
  }
  
  /**
   * Checks if JSONValue can be casted into given class
   */
  public boolean castableTo(Class<?> type) {
    return type.isInstance(this.value);
  }

  @Override
  public String toString() {
    return this.value.toString();
  }
}
