package io.github.oxi1224.websocket.json;

public class JSONException extends Exception {
  public JSONException(String msg) {
    super(msg);
  }
    
  public JSONException(String msg, int line, int pos)  {
    super(String.format("%s at line %s:%s", msg, line, pos));
  }

  public JSONException(String msg, int line, int pos, String refValue) {
    super(String.format("%s at line %s:%s (%s)", msg, line, pos, refValue));
  }
}
