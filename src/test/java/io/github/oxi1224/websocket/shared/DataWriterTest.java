package io.github.oxi1224.websocket.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class DataWriterTest {
  @Test public void testWriting() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataWriter writer = new DataWriter(out);
    DataFrame testFrame = new DataFrame(true, false, false, false, Opcode.TEXT, false, 5, null, "hello".getBytes());
    writer.write(testFrame);
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    DataFrame readFrame = new DataFrame().read(in);
    
    assertEquals(testFrame.getFin(), readFrame.getFin(), "Frame FIN fields are different");
    assertEquals(testFrame.getPayloadLength(), readFrame.getPayloadLength(), "Frame payload lenghts are different");
    assertArrayEquals(testFrame.getPayload(), readFrame.getPayload(), "Frame payloads are different");
    assertEquals(testFrame.getOpcode().getValue(), readFrame.getOpcode().getValue(), "Frame opcodes are different");
  }
}
