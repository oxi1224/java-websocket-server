package io.github.oxi1224.websocket.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JSONTokenizer {
  private final Reader reader;
  public int character;
  public boolean end = false;
  public char prevCharacter;

  public static final String NUMBER_REGEX = "-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?";

  public JSONTokenizer(String in) {
    this.reader = new StringReader(in);
  }

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
    character = c;
    return (char)character;
  }

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

  public String next(int n) {
    if (n == 0) return "";
    char[] chars = new char[n];
    for (int i = 0; i < n; i++) {
      chars[i] = next();
    }
    return new String(chars);
  }

  public String nextString() {
    StringBuilder sb = new StringBuilder();
    while (true) {
      char c = next();
      switch (c) {
        case 0:
        case '\n':
        case '\r':
        /// TODO: Proper errors
          return "";
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
          }
          break;
        default:
          if (c == '"') return sb.toString();
          sb.append(c);
      }
    }
  }

  public String nextUntil(char delim) {
    StringBuilder sb = new StringBuilder();
    char c = 0;
    while (c != delim) {
      c = next();
      sb.append(c);
    }
    return sb.toString().trim();
  }

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

  public String nextUntilRegex(String regex) {
    StringBuilder sb = new StringBuilder();
    char c;
    while (!(String.valueOf(peek()).matches(regex))) {
      c = next();
      sb.append(c);
    }
    return sb.toString().trim();
  }

  public Object parseUnquoted(String str) throws NumberFormatException {
    if (str == "false") return Boolean.FALSE;
    if (str == "true") return Boolean.TRUE;
    if (str == "null") return null;
    
    char first = str.charAt(0);
    if (!str.matches(NUMBER_REGEX)) return str;
    if ((first >= '0' && first <= '9') || first == '-') {
      return parseJSONNumber(str);
    } else {
      return str;
    }
  }

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
}
