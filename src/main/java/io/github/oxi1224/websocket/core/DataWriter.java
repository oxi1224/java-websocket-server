package io.github.oxi1224.websocket.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DataWriter {
  private OutputStream out;

  public DataWriter(OutputStream out) {
    this.out = out;
  }

  public void write(DataFrame frame) throws IOException {
    writeInternal(frame);
  }

  public void write(
    boolean fin,
    boolean rsv1,
    boolean rsv2,
    boolean rsv3,
    Opcode opcode,
    boolean mask,
    int payloadLength,
    byte[] maskingKey,
    byte[] payload
  ) throws IOException {
    DataFrame frame = new DataFrame(fin, rsv1, rsv2, rsv3, opcode, mask, payloadLength, maskingKey, payload);
    writeInternal(frame);
  }

  public void write(boolean fin, Opcode opcode, byte[] payload) throws IOException {
    write(fin, opcode, payload.length, null, payload);
  }

  public void write(boolean fin, Opcode opcode, String payload) throws IOException {
    byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
    write(fin, opcode, payloadBytes.length, null, payloadBytes);
  }

  public void write(byte[] payload) throws IOException {
    write(true, Opcode.BINARY, payload);
  }

  public void write(String payload) throws IOException {
    write(true, Opcode.TEXT, payload);
  }

  public void write(boolean fin, Opcode opcode, int payloadLength, byte[] maskingKey, byte[] payload) throws IOException {
    DataFrame frame = new DataFrame(fin, false, false, false, opcode, false, payloadLength, maskingKey, payload);
    writeInternal(frame);
  }

  public void flush() throws IOException { out.flush(); }

  private void writeInternal(DataFrame frame) throws IOException {
    byte[] serialized = frame.getBytes();
    out.write(serialized, 0, serialized.length);
  }
}
