package io.github.oxi1224.websocket.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A utility class for tokenizing JSON strings.
 * This class provides methods to read characters one by one or by tokens from a JSON string.
 *
 * <p>
 * This class DOES NOT do any validation on the data other than what is required to determine valid strings,
 * the rest should be handled manually (or by {@link JSONParser})
 * </p>
 * 
 * @see JSONException
 * @see JSONParser
 * @see JSONObject
 */
public class JSONTokenizer {
  private final Reader reader;
  public boolean end = false;
  private int previous;
  private int pos = 1;
  private int line = 1;
  private int len;

  public static final String NUMBER_REGEX = "-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?";

  /**
   * @param in - The JSON string. NOT VALIDATED
   */
  public JSONTokenizer(String in) {
    this.reader = new StringReader(in);
    this.len = in.length();
  }

  /**
   * Gets the next character from provided String
   * @return The read character
   */
  public char next() {
    int c = 0;
    try {
      c = reader.read();
    } catch (IOException e) {
      end = true;
      e.printStackTrace();
      return 0;
    }
    if (c <= 0) {
      end = true;
      return 0;
    }

    if (c == '\n' || (c == '\r' && previous != '\n')) {
      line++;
      pos = 1;
    } else {
      pos++;
    }
    previous = c;
    return (char)previous;
  }
  
  /**
   * Reads n characters and returns them concatenated in a String
   * @param n - The amount of characters to read
   * @return The read characters combined in a string
   */
  public String next(int n) {
    if (n == 0) return "";
    char[] chars = new char[n];
    for (int i = 0; i < n; i++) {
      chars[i] = next();
    }
    return new String(chars);
  }
  
  /**
   * Reads characters until a non-whitespace character is read
   * @return The first non-whitespace character
   */
  public char nextClean() {
    char c = next();
    switch (c) {
      case '\r':
      case '\n':
      case ' ':
        return nextClean();
      default:
        return c;
    }
  }
  
  /**
   * Reads (peeks) at the next character without consuming it
   * @return The next character in line
   */
  public char peek() {
    char c = 0;
    try {
      reader.mark(1);
      c = next();
      reader.reset();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return c;
  }
  
  /**
   * Reads characters without consuming them until a non-whitespace character is read
   * @return The first non-whitespace character in line
   */
  public char peekNextClean() {
    try {
      reader.mark(len);
      char c = (char)reader.read();
      while (true) {
        switch(c) {
          case '\r':
          case '\n':
          case ' ':
            c = (char)reader.read();
            break;
          default:
            reader.reset();
            return c;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }

  /**
   * Reads characters until delim is read
   * @param delim - The delimeter to use
   * @return All characters before reading delim
   */
  public String nextUntil(char delim) {
    StringBuilder sb = new StringBuilder();
    char c = 0;
    while (c != delim) {
      c = next();
      sb.append(c);
    }
    return sb.toString().trim();
  }
  
  /**
   * Reads characters until any character from delims is encountered
   * @param delims - String containing all delimeters
   * @return All characters before reading a delim
   */
  public String nextUntil(String delims) {
    StringBuilder sb = new StringBuilder();
    char c = next();
    sb.append(c);
    while (!(delims.indexOf(peek()) >= 0)) {
      c = next();
      sb.append(c);
    }
    return sb.toString().trim();
  }
  
  /**
   * Reads characters until any is read that matches the regex
   * @param regex - The regex the character has to match
   * @return All characters before reading a matching one
   */
  public String nextUntilRegex(String regex) {
    StringBuilder sb = new StringBuilder();
    char c;
    while (!(String.valueOf(peek()).matches(regex))) {
      c = next();
      sb.append(c);
    }
    return sb.toString().trim();
  }

  /**
   * Reads a JSON string (text within double quotes)
   * <p>The last consumed character has to be the opening quote</p>
   * @return The read string
   * @throws JSONException if line has no closing bracket
   * @throws JSONException if string contains an invalid escape sequence
   */
  public String nextString() throws JSONException {
    StringBuilder sb = new StringBuilder();
    while (true) {
      char c = next();
      switch (c) {
        case 0:
        case '\n':
        case '\r':
          throw new JSONException("Unexpected line end when parsing string", line, pos);
        case '\\':
          c = next();
          switch (c) {
            case '"':
            case '\\':
            case '/':
            case '\'':
              sb.append(c);
              break;
            case 'b':
              sb.append('\b');
              break;
            case 'f':
              sb.append('\f');
              break;
            case 'n':
              sb.append('\n');
              break;
            case 'r':
              sb.append('\r');
              break;
            case 't':
              sb.append('\t');
              break;
            case 'u':
              String next = next(4);
              sb.append((char)Integer.parseInt(next, 16));
              break;
            default:
            throw new JSONException("Invalid escape sequence", line, pos);
          }
          break;
        default:
          if (c == '"') return sb.toString();
          sb.append(c);
      }
    }
  }
  
  /**
   * Reads unquoted JSON text (booleans, null, numbers)
   * <p>The last consumed character has to be the whitespace preceeding the token</p>
   * @param str - The string to search for values
   * @return The parsed object (Boolean, null, Number)
   * @throws JSONException if provied a value that doesn't match any standard ones
   * @throws NumberFormatException if provided a value that matches number format but does not resolve to one
   */
  public Object parseUnquoted(String str) throws NumberFormatException, JSONException {
    if (str.equals("false")) return Boolean.FALSE;
    if (str.equals("true")) return Boolean.TRUE;
    if (str.equals("null")) return null;
    
    char first = str.charAt(0);
    if (!str.matches(NUMBER_REGEX)) throw new JSONException("Unknown unquoted value", line, pos, str);
    if ((first >= '0' && first <= '9') || first == '-') {
      return parseJSONNumber(str);
    } else {
      throw new JSONException("Unknown unquoted value", line, pos, str);
    }
  }

  /**
   * Resolves a string containing a JSON number
   * @return The resolved Number
   * @throws NumberFormatException if provided a value that does not resolve to a number
   */
  public Number parseJSONNumber(String str) throws NumberFormatException {
    if (str.contains(".") || str.contains("e") || str.contains("E")) {
      try {
        BigDecimal bigDec = new BigDecimal(str);

        try {
          float floatVal = bigDec.floatValue();
          if (BigDecimal.valueOf(floatVal).compareTo(bigDec) == 0) return floatVal;
        } catch (NumberFormatException ignored) {}

        try {
          double doubleVal = bigDec.doubleValue();
          if (BigDecimal.valueOf(doubleVal).compareTo(bigDec) == 0) return doubleVal;
        } catch (NumberFormatException ignored) {}
        
        return bigDec;
      } catch (NumberFormatException e) {
        throw new NumberFormatException("String " + str + "is not a valid number");
      }
    } else {
      try {
        BigInteger bigInt = new BigInteger(str);
        if (bigInt.bitLength() <= 31) return bigInt.intValue();
        if (bigInt.bitLength() <= 63) return bigInt.longValue();
        return bigInt;
      } catch (NumberFormatException ex) {
        throw new NumberFormatException("String " + str + "is not a valid number");
      }
    }
  }

  public int getPos() { return this.pos; }
  public int getLine() { return this.line; }
}
