package io.github.oxi1224.websocket.json;

import java.util.ArrayList;
import java.util.List;
/// TODO: STRING TO JSON, STRING SERIALIZATION

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
    char c = tok.next();
    if (c != '{') throw new JSONException("JSON object does not start with a \"{\"");
    while (!tok.end) {
      c = tok.nextClean();
      if (c == '}') break;
      String key = tok.nextString();
      c = tok.next();
      if (c != ':') throw new JSONException("Unexpected character (expected \":\")", tok.getLine(), tok.getPos());
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
  private static List<Object> parseArray(JSONTokenizer tok) throws NumberFormatException, JSONException {
    List<Object> out = new ArrayList<>();
    tok.next();
    while (true) {
      Object value = null;
      tok.nextUntilRegex("[^\\s]");
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
        case ']':
          tok.next();
          throw new JSONException("Trailing comma", tok.getLine(), tok.getPos());
        default:
          value = tok.parseUnquoted(tok.nextUntil(",]"));
          break;
      }
      out.add(value);
      char peeked = tok.peekNextClean();
      if (peeked == ',') {
        tok.nextClean();
      } else if (peeked == ']') {
        tok.nextClean();
        tok.next();
        break;
      } else {
        tok.nextClean();
        throw new JSONException(
          "Unexpected token in array (expected \",\" or \"]\")",
          tok.getLine(),
          tok.getPos(),
          escapeChar(peeked)
        );
      }
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
