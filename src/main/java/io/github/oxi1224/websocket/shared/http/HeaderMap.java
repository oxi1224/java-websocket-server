package io.github.oxi1224.websocket.shared.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import io.github.oxi1224.websocket.shared.util.Pair;

/**
 * A custom map used for defining HTTP Headers
 */
public class HeaderMap extends LinkedHashMap<String, ArrayList<String>> {
  /**
   * A pair with predifned types to avoid warnings and make it easier to create new headers
   */
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
  
  /**
   * Adds a single key-value-pair
   * @param key - The header
   * @param val - The value
   */
  public void put(String key, String val) {
    if (!containsKey(key)) put(key, new ArrayList<String>());
    get(key).add(val);
  }
  
  /**
   * Adds one or multiple HeaerPair classes
   * @param pairs - the pairs to add
   */
  public void put(HeaderPair... pairs) {
    for (Pair<String, String> kvp : pairs) {
      String key = kvp.getKey();
      if (!containsKey(key)) put(key, new ArrayList<String>());
      get(key).add(kvp.getValue());
    }
  }
  
  /**
   * @param key - the header to return the value of
   * @return the first value of the specified header
   */
  public String getFirstValue(String key) {
    if (!containsKey(key)) return null;
    return get(key).iterator().next();
  }
}
