package io.github.oxi1224.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import io.github.oxi1224.common.Pair;

public class HeaderMap extends LinkedHashMap<String, ArrayList<String>> {
  public static class HeaderPair extends Pair<String, String> {
    public HeaderPair(String key, String val) {
      super(key, val);
    }
  }

  public HeaderMap() {
    super();
  }

  public HeaderMap(HeaderPair... pairs) {
    super();
    put(pairs);
  }

  public void put(String key, String val) {
    if (!containsKey(key)) put(key, new ArrayList<String>());
    get(key).add(val);
  }
  
  public void put(HeaderPair... pairs) {
    for (Pair<String, String> kvp : pairs) {
      if (!containsKey(kvp.getKey())) put(kvp.getKey(), new ArrayList<String>());
      get(kvp.getKey()).add(kvp.getValue());
    }
  }

  public String getFirstValue(String key) {
    if (!containsKey(key)) return null;
    return get(key).iterator().next();
  }
}
