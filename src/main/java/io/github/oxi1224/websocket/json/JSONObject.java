package io.github.oxi1224.websocket.json;

import java.util.LinkedHashMap;

/// TODO: Implement this, maybe implement a better way at distinguishing values e.g JSONArray, JSONValue
public class JSONObject {
  public LinkedHashMap<String, Object> data = new LinkedHashMap<>();
  
  public JSONObject() {}
  public Object get(String key) { return data.get(key); };
  // public <T> T get(String key, Class<T> castType) { };
  public void set(String key, Object value) {
    data.put(key, value);
  };
}
