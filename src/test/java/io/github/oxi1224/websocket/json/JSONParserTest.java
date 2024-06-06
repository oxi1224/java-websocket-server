package io.github.oxi1224.websocket.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;

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

    JSONObject todoObj = JSONParser.parse(
"""
{
  "todos": [
    {
      "id": 1,
      "todo": "Do something nice for someone you care about",
      "completed": false,
      "userId": 152
    },
    {
      "id": 2,
      "todo": "Memorize a poem",
      "completed": true,
      "userId": 13
    },
    {
      "id": 3,
      "todo": "Watch a classic movie",
      "completed": true,
      "userId": 68
    }
  ],
  "total": 254,
  "skip": 0,
  "limit": 3
}
""");
    JSONValue.Array array = todoObj.get("todos", JSONValue.Array.class);
    int i = 1;
    for (JSONValue v : array) {
      if (v.castableTo(JSONObject.class)) {
        JSONObject jObj = v.getValue(JSONObject.class);
        assertEquals(i, jObj.get("id", Integer.class));
        i++;
      } else {
        throw new JSONException("v should be castable to JSONObject");
      }
    }
    assertEquals(254, todoObj.get("total", Integer.class));
    assertEquals(0, todoObj.get("skip", Integer.class));
  }

  @Test public void testIterator() throws JSONException {
    String[] expectedKeys = new String[]{ "key-one", "key-two", "key-three", "key-four" };
    String[] expectedValues = new String[]{ "value-one", "value-two", "value-three", "value-four" };
    JSONObject obj = JSONParser.parse(
"""
{
  "key-one": "value-one",
  "key-two": "value-two",
  "key-three": "value-three",
  "key-four": "value-four"
}
"""
    );

    int i = 0;
    for (JSONPair p : obj) {
      JSONValue v = p.getValue();
      if (v.castableTo(String.class)) {
        String str = v.getValue(String.class);
        assertEquals(expectedKeys[i], p.getKey());
        assertEquals(expectedValues[i], str);
      } else {
        throw new JSONException("v should be castable to String");
      }
      i++;
    }

    i = 0;
    Iterator<JSONPair> iter = obj.iterator();
    while (iter.hasNext()) {
      JSONPair p = iter.next();
      JSONValue v = p.getValue();
      if (v.castableTo(String.class)) {
        String str = v.getValue(String.class);
        assertEquals(expectedKeys[i], p.getKey());
        assertEquals(expectedValues[i], str);
      } else {
        throw new JSONException("v should be castable to String");
      }
      i++;
    }
  }

  @Test void testToString() throws JSONException {
    String stringJSON =
"""
{"todos":[{"id":1,"todo":"Do something nice for someone you care about","completed":false,"userId":152},{"id":2,"todo":"Memorize a poem","completed":true,"userId":13}],"total":254,"skip":0,"limit":3}
""".trim();
    JSONObject obj = JSONParser.parse(stringJSON);
    assertEquals(stringJSON, obj.toString());
  }
}
