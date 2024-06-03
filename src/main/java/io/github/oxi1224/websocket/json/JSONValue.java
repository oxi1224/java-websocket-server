package io.github.oxi1224.websocket.json;

import java.util.ArrayList;
import java.util.Collection;

public class JSONValue {
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
  
  public static final Class<?> NullValue = null;

  public static class Null {
    public String getValue() { return null; }
    @Override
    public String toString() { return "null"; }
  }

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
      String out = "[";
      for (Object obj : this) {
        if (obj instanceof String) {
          out += "\"" + obj + "\"";
        } else {
          out += obj.toString();
        }
        out += "]";
      }
      return out;
    }
  }
  
  private Object value;

  public JSONValue(Object value) {
    if (!isValidValue(value)) throw new JSONError(value);
    this.value = value;
  }
  
  public <T> T getValue(Class<T> castClass) {
    try {
      return this.value instanceof Null ? null : castClass.cast(this.value);
    } catch (ClassCastException e) {
      throw new JSONError("Failed to cast value to " + castClass.getName());
    }
  }

  public Object getValue() {
    return this.value instanceof Null ? null : this.value;
  }
  
  @Override
  public String toString() {
    return this.value.toString();
  }
}
