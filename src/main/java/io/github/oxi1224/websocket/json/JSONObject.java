package io.github.oxi1224.websocket.json;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


public class JSONObject implements Iterable<JSONPair> {
  public static class JSONIterator implements Iterator<JSONPair> {
    private Iterator<Map.Entry<String, JSONValue>> iter;

    public JSONIterator(JSONObject obj) {
      iter = obj.data.entrySet().iterator();
    }

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public JSONPair next() {
      Map.Entry<String, JSONValue> entry = iter.next();
      return new JSONPair(entry.getKey(), entry.getValue());
    }
  }

  private LinkedHashMap<String, JSONValue> data = new LinkedHashMap<>();
  
  public JSONObject() {}

  public JSONValue get(String key) {
    return data.get(key);
  };

  public <T> T get(String key, Class<T> castType) throws JSONException {
    JSONValue v = data.get(key);
    try {
      return castType.cast(v.getValue());
    } catch (ClassCastException e) {
      e.printStackTrace();
      throw new JSONException("Failed to cast value to " + castType.getName());
    }
  };

  public void set(String key, JSONValue value) {
    data.put(key, value);
  };

  public void set(String key, Object value) {
    if (!JSONValue.isValidValue(value)) throw new JSONError("Invalid value provided, valid ones include: JSONObject, String, Number, Boolean, Null, JSONValue.Array");
    JSONValue jValue = new JSONValue(value);
    data.put(key, jValue);
  }

  public JSONValue getNested(String key) throws JSONException {
    String[] split = key.split("\\.");
    JSONObject cur = this;
    JSONValue ret = null;
    for (int i = 0; i < split.length; i++) {
      if (i == split.length - 1) {
        ret = cur.get(split[i]);
      } else {
        cur = cur.get(split[i], JSONObject.class);
        if (cur == null) throw new JSONException("Could not find specified key (" + key + ")");
      }
    }
    if (ret == null) throw new JSONException("Could not find specified key (" + key + ")");
    return ret; 
  }

  public <T> T getNested(String key, Class<T> castType) throws JSONException {
    JSONValue jValue = getNested(key);
    try {
      return castType.cast(jValue.getValue());
    } catch (ClassCastException e) {
      throw new JSONException("Failed to cast value to " + castType.getName());
    }
  }
    
  public Iterator<JSONPair> iterator() {
    return new JSONIterator(this);
  }
}
