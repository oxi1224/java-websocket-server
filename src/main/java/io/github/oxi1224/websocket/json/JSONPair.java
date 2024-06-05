package io.github.oxi1224.websocket.json;

import io.github.oxi1224.websocket.shared.util.Pair;

public class JSONPair extends Pair<String, JSONValue> {
  public JSONPair(String key, JSONValue val) {
    super(key, val);
  }
}
