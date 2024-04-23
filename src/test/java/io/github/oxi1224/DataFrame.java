package io.github.oxi1224;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class DataFrameTest {
  @Test void testDataFrame() throws IOException {
    // fin - 1, rsv1-3 - 0, mask - 0, data = "Hello"
    byte[] frame_one = hexToByteArray("810548656c6c6f");
    // fin - 1 rsv1-3 - 0, mask - 1, data = "Hello"
    byte[] frame_two = hexToByteArray("818537fa213d7f9f4d5158");
    InputStream in_one = new ByteArrayInputStream(frame_one);
    InputStream in_two = new ByteArrayInputStream(frame_two);
    DataFrame parsed_one = new DataFrame(in_one);
    DataFrame parsed_two = new DataFrame(in_two);

    assertEquals(true, parsed_one.getFin(), "Fin should be true in frame 1");
    assertEquals(false, parsed_one.getMask(), "Mask should be false in frame 1");
    String parsedData_one = new String(parsed_one.getPayload(), StandardCharsets.UTF_8);
    assertEquals("Hello", parsedData_one, String.format("Expected payload to be \"Hello\", received %s", parsedData_one));

    assertEquals(true, parsed_two.getFin(), "Fin should be true in frame 2");
    assertEquals(true, parsed_two.getMask(), "Mask should be true in frame 2");
    String parsedData_two = new String(parsed_two.getPayload(), StandardCharsets.UTF_8);
    assertEquals("Hello", parsedData_two, String.format("Expected payload to be \"Hello\", received %s", parsedData_two));

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
