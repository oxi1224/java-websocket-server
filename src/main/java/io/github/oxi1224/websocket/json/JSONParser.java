package io.github.oxi1224.websocket.json;

/**
 * A utility class for parsing JSON strings into {@link JSONObject} objects.
 * This class performs JSON validation and throws appropriate errors when standards are not met.
 * Has to be used in conjunction with {@link JSONTokenizer}
 *
 * @see JSONTokenizer
 * @see JSONException
 * @see JSONObject
 */
public class JSONParser {
  /**
   * Uses the provided JSONTokenizer to parse a JSONObject
   * @param tok - The JSONTokenizer
   * @return JSONObject The parsed JSONObject
   * @throws NumberFormatException if JSON contains invalid numbers
   * @throws JSONException if the parser encounters a problem with the JSON during parsing
   * @see JSONObject
   * @see JSONTokenizer
   */
  public static JSONObject parse(JSONTokenizer tok) throws NumberFormatException, JSONException {
    JSONObject out = new JSONObject();
    if (tok.current != '{') throw new JSONException("JSON object does not start with a \"{\"");
    char c;
    while (!tok.end) {
      c = tok.nextClean();
      if (c == ',') {
        if (tok.peekNextClean() == '}') {
          throw new JSONException("Trailing comma", tok.getLine(), tok.getPos());
        } else {
          c = tok.nextClean();
        }
      } else if (c == '}') break;
      String key = tok.nextString();
      c = tok.next();
      if (c != ':') throw new JSONException("Unexpected character (expected \":\")", tok.getLine(), tok.getPos(), escapeChar(c));
      c = tok.nextClean();
      Object value = null;
      switch (tok.current) {
        case '"':
          value = tok.nextString();
          break;
        case '[':
          value = parseArray(tok);
          break;
        case '{':
          value = parse(tok);
          break;
        default:
          // add the first character as it would otherwise get cut off
          value = tok.parseUnquoted(tok.current + tok.nextUntil(" ,\r\n}"));
          break;
      }
      out.set(key, new JSONValue(value));
    }
    return out;
  }
  
  /**
   * Parses the provided string into a JSONObject
   * @param in - The JSON string (gets validated)
   * @return The parsed JSONObject
   * @throws NumberFormatException if JSON contains invalid numbers
   * @throws JSONException if the parser encounters a problem with the JSON during parsing
   * @see JSONObject
   */
  public static JSONObject parse(String in) throws NumberFormatException, JSONException {
    JSONTokenizer tok = new JSONTokenizer(in);
    tok.next();
    return parse(tok);
  }
  
  /**
   * Parses a JSON array from a JSONTokenizer
   * @param tok - The JSONTokenizer to use
   * @return List containg all values in found JSON array
   * @throws NumberFormatException if array contains invalid numbers
   * @throws JSONException if the parser encounters a problem with the array during parsing
   * @see JSONTokenizer
   */
  private static JSONValue.Array parseArray(JSONTokenizer tok) throws NumberFormatException, JSONException {
    JSONValue.Array out = new JSONValue.Array();
    char c;
    while (true) {
      Object value = null;
      c = tok.nextClean();
      if (c == ',') {
        if (tok.peekNextClean() == ']') {
          throw new JSONException("Trailing comma in array", tok.getLine(), tok.getPos());
        } else {
          c = tok.nextClean();
        }
      } else if (c == ']') {
        tok.next();
        break;
      }
      switch (tok.current) {
        case '"':
          value = tok.nextString();
          break;
        case '[':
          value = parseArray(tok);
          break;
        case '{':
          value = parse(tok);
          break;
        default:
          // add the first character as it would otherwise get cut off
          value = tok.parseUnquoted(tok.current + tok.nextUntil(",]"));
          break;
      }
      out.add(new JSONValue(value));
    }
    return out;
  }
  
  /**
   * Escapes JSON escape characters so they can be represented properly when printing
   * @param c - The character to escape
   * @return The escapes character
   */
  public static String escapeChar(char c) {
    switch (c) {
      case '\n':
        return "\\n";
      case '\r':
        return "\\r";
      case '"':
        return "\"";
      case '\\':
        return "\\";
      case '\'':
        return "'";
      case '\f':
        return "\\f";
      case '\t':
        return "\\t";
      case '\b':
        return "\\b";
      default:
        return String.valueOf(c);
    }
  }
}
