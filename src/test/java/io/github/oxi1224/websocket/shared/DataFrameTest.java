package io.github.oxi1224.websocket.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class DataFrameTest {
  @Test public void testMaskedFrame() throws IOException {
    // fin - 1 rsv1-3 - 0, mask - 1, data = "Hello"
    byte[] frame = hexToByteArray("818537fa213d7f9f4d5158");
    InputStream in = new ByteArrayInputStream(frame);
    DataFrame parsed = new DataFrame().read(in);
    assertEquals(true, parsed.getFin(), "Fin should be true in frame 2");
    assertEquals(true, parsed.getMask(), "Mask should be true in frame 2");
    String parsedData = new String(parsed.getPayload(), StandardCharsets.UTF_8);
    assertEquals("Hello", parsedData, String.format("Expected payload to be \"Hello\", received %s", parsedData));
  }

  @Test public void testUnmaskedFrame() throws IOException {
    // fin - 1, rsv1-3 - 0, mask - 0, data = "Hello"
    byte[] frame = hexToByteArray("810548656c6c6f");
    InputStream in = new ByteArrayInputStream(frame);
    DataFrame parsed = new DataFrame().read(in);
    assertEquals(true, parsed.getFin(), "Fin should be true in frame 1");
    assertEquals(false, parsed.getMask(), "Mask should be false in frame 1");
    String parsedData = new String(parsed.getPayload(), StandardCharsets.UTF_8);
    assertEquals("Hello", parsedData, String.format("Expected payload to be \"Hello\", received %s", parsedData));
  }

  @Test public void testSerialization() {
    // fin - 0, rsv1-3 - 0, mask - 0, data = "Hello"
    byte[] byteFrame = hexToByteArray("810548656c6c6f");
    byte[] bytePayload = "Hello".getBytes(StandardCharsets.UTF_8);
    DataFrame frame = new DataFrame(true, false, false, false, Opcode.TEXT, false, bytePayload.length, null, bytePayload);
    byte[] serialized = frame.getBytes();
    assertArrayEquals(byteFrame, serialized, "Incorrectly serialized frame");
  }

  @Test public void testSerializationMasked() {
    // fin - 1 rsv1-3 - 0, mask - 1, data = "Hello"
    byte[] byteFrame = hexToByteArray("818537fa213d7f9f4d5158");
    byte[] bytePayload = "Hello".getBytes(StandardCharsets.UTF_8);
    DataFrame frame = new DataFrame(true, false, false, false, Opcode.TEXT, true, bytePayload.length, hexToByteArray("37fa213d"), bytePayload);
    byte[] serialized = frame.getBytes();
    assertArrayEquals(byteFrame, serialized, "Incorrectly serialized masked frame");
  }

  public static byte[] hexToByteArray(String hstr) {
    if ((hstr.length() < 0) || ((hstr.length() % 2) != 0)) {
      throw new IllegalArgumentException(String.format("Invalid string length of <%d>",hstr.length()));
    }

    int size = hstr.length() / 2;
    byte buf[] = new byte[size];
    byte hex;
    int len = hstr.length();

    int idx = (int)Math.floor(((size * 2) - (double)len) / 2);
    for (int i = 0; i < len; i++) {
      hex = 0;
      if (i >= 0) {
        hex = (byte)(Character.digit(hstr.charAt(i),16) << 4);
      }
      i++;
      hex += (byte)(Character.digit(hstr.charAt(i),16));

      buf[idx] = (byte)(hex);
      idx++;
    }
    return buf;
  }
}
