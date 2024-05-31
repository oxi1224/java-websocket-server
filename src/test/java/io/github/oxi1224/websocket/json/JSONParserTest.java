package io.github.oxi1224.websocket.json;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

class ParserTest {
  @Test public void testParsing() throws NumberFormatException {
    JSONObject out = JSONParser.parse(
"""
{
  "string": "hello",
  "boolean": true,
  "null": null,
  "number": 25189.125,
  "array": [
    "array-string",
    true,
    null
  ],
  "object": {
    "sub-string": "string",
    "sub-boolean": true,
    "sub-array": ["sub-array-string", true]
  },
  "object-array": [
    { "key": 0 },
    { "key": 1 },
    { "key": 2 },
  ]
}
"""
    );
    for (Map.Entry<String, Object> p : out.data.entrySet()) {
      System.out.printf("%s : %s\n", p.getKey(), p.getValue());
    }
  }
}

