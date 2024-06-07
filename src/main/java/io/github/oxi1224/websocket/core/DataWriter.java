package io.github.oxi1224.websocket.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import io.github.oxi1224.websocket.json.JSONObject;

/**
 * Utility class containing multiple, valid overloads of write()
 */
public class DataWriter {
  private OutputStream out;
  private boolean maskFrames = false;

  public DataWriter(OutputStream out) {
    this.out = out;
  }
  
  /**
   * Sets wheter or not the writer should by default mask frames
   * @param mask - What to set masking to
   */
  public void setMasking(boolean mask) {
    maskFrames = mask;
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
    byte[] maskingKey = null;
    if (maskFrames) maskingKey = DataFrame.genMaskingKey();
    write(fin, false, false, false, opcode, maskFrames, payload.length, maskingKey, payload);
  }

  public void write(boolean fin, Opcode opcode, String payload) throws IOException {
    byte[] maskingKey = null;
    if (maskFrames) maskingKey = DataFrame.genMaskingKey();
    byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
    write(fin, false, false, false, opcode, maskFrames, payloadBytes.length, maskingKey, payloadBytes);
  }
  
  /**
   * Includes an additional messageID parameter when using message identification
   */
  public void write(boolean fin, Opcode opcode, String messageID, String payload) throws IOException {
    byte[] maskingKey = null;
    if (maskFrames) maskingKey = DataFrame.genMaskingKey();
    String fullPayload = messageID + " " + payload;
    byte[] payloadBytes = fullPayload.getBytes(StandardCharsets.UTF_8);
    write(fin, true, false, false, opcode, maskFrames, payloadBytes.length, maskingKey, payloadBytes);
  }

  public void write(byte[] payload) throws IOException {
    write(true, Opcode.BINARY, payload);
  }

  public void write(String payload) throws IOException {
    write(true, Opcode.TEXT, payload);
  }

  public void write(String messageID, JSONObject payload) throws IOException {
    JSONObject obj = new JSONObject();
    obj.set("messageID", messageID);
    obj.set("__data", payload);
    write(true, Opcode.JSON, obj.toString());
  }

  /**
   * Includes an additional messageID parameter when using message identification
   */
  public void write(String messageID, String payload) throws IOException {
    write(true, Opcode.TEXT, messageID, payload);
  }

  public void flush() throws IOException { out.flush(); }

  private void writeInternal(DataFrame frame) throws IOException {
    byte[] serialized = frame.getBytes();
    out.write(serialized, 0, serialized.length);
  }
}
