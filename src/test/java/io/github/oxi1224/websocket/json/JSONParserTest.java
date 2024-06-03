package io.github.oxi1224.websocket.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
  @Test public void testParsing() throws NumberFormatException, JSONException {
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
    "sub-array": ["sub-array-string", true],
  },
  "object-array": [
    { "key": 0 },
    { "key": 1 },
    { "key": 2 }
  ],
}
"""
    );
    String stringVal = out.get("string", String.class);
    assertEquals("hello", stringVal);
    Boolean booleanVal = out.get("boolean", Boolean.class);
    assertEquals(true, booleanVal);
    JSONValue.Null nullVal = out.get("null", JSONValue.Null.class);
    assertEquals(null, nullVal);

    JSONValue.Array arrayVal = out.get("array", JSONValue.Array.class);
    JSONValue.Array expectedArrayVal = new JSONValue.Array("array-string", true, new JSONValue.Null());
    assertEquals(expectedArrayVal.size(), arrayVal.size(), "arrayVal.size() differs from expectedArrayVal.size()");
    for (int i = 0; i < arrayVal.size(); i++) {
      assertEquals(expectedArrayVal.get(i).getValue(), arrayVal.get(i).getValue(), String.format("arrayVal.get(%s) differs from expectedArrayVal.get(%s)", i, i));
    }
  
    String nestedString = out.getNested("object.sub-string", String.class);
    assertEquals("string", nestedString);

    JSONValue.Array objectArray = out.get("object-array", JSONValue.Array.class);
    assertEquals(3, objectArray.size(), "objectArray.size() is not 3");
    for (int i = 0; i < 3; i++) {
      JSONObject obj = objectArray.get(i, JSONObject.class);
      int key = obj.get("key", Integer.class);
      assertEquals(i, key);
    }
  }
}

