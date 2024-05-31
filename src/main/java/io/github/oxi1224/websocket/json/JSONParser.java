package io.github.oxi1224.websocket.json;

import java.util.ArrayList;
import java.util.List;
/// TODO: HANDLE EDGE CASES, MAKE PROPER ERROR SYSTEM
/// TODO: STRING TO JSON, STRING SERIALIZATION
public class JSONParser {
  public static JSONObject parse(JSONTokenizer tok) throws NumberFormatException {
    JSONObject out = new JSONObject();
    char c = tok.next();
    if (c != '{') return null;
    while (!tok.end) {
      c = tok.nextClean();
      if (c == '}') break;
      String key = tok.nextString();
      c = tok.next();
      if (c != ':') return null;
      if (tok.peek() == ' ') c = tok.next();
      Object value = null;
      switch (tok.peek()) {
        case '"':
          c = tok.next();
          value = tok.nextString();
          break;
        case '[':
          value = parseArray(tok);
          break;
        case '{':
          value = parse(tok);
          break;
        default:
          value = tok.parseUnquoted(tok.nextUntil(" ,\r\n}"));
          break;
      }
      if (tok.peek() == ',') c = tok.next();
      out.set(key, value);
    }
    return out;
  }

  public static JSONObject parse(String in) throws NumberFormatException {
    JSONTokenizer tok = new JSONTokenizer(in);
    return parse(tok);
  }

  private static List<Object> parseArray(JSONTokenizer tok) {
    List<Object> out = new ArrayList<>();
    tok.next();
    while (true) {
      Object value = null;
      tok.nextUntilRegex("[^\\s]"); // Go until next character isnt a whitespace
      if (tok.peek() == ']') {
        tok.next();
        break;
      }
      switch (tok.peek()) {
        case '"':
          tok.next();
          value = tok.nextString();
          break;
        case '[':
          value = parseArray(tok);
          break;
        case '{':
          value = parse(tok);
          break;
        default:
          value = tok.parseUnquoted(tok.nextUntil(" ,\r\n]"));
          break;
      }
      if (tok.peek() == ',') tok.next();
      out.add(value);
    }
    return out;
  }
}
