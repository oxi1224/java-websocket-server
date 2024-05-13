package io.github.oxi1224.websocket.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

class DataReaderTest {
  @Test public void testContinuation() throws IOException, UnexpectedFrameException {
    byte[] frames = hexToByteArray("010748656c6c6f2c208005776f726c64");
    InputStream in = new ByteArrayInputStream(frames);
    DataReader reader = new DataReader(in);
    reader.read();
    assertEquals("Hello, world", reader.getPayload(), String.format("Expected payload to be Hello, World got %s", reader.getPayload()));
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

      buf[idx] = hex;
      idx++;
    }
    return buf;
  }
}
