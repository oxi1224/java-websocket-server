package io.github.oxi1224.websocket.json;

public class JSONError extends Error {
  public JSONError(String msg) {
    super(msg);
  }

  public JSONError(Object obj) {
    super(String.format(
      "Invalid object (%s) provided to JSONValue. Valid ones include: JSONObject, String, Number, Boolean, Null, JSONValue.Array",
      obj.getClass().getName()
    ));
  }
}
