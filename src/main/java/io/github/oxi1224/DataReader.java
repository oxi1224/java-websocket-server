package io.github.oxi1224;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class DataReader {
  private byte[] payload;
  private DataFrame startFrame;
  private int dataType;
  private InputStream in;

  public Opcode read(InputStream in) throws IOException {
    this.in = in;
    DataFrame frame = new DataFrame().read(in);
    startFrame = frame;
    payload = frame.getPayload();
    return handleOpCode(frame);
  }

  private Opcode readNext() throws IOException {
    DataFrame frame = new DataFrame().read(in);
    byte[] framePayload = frame.getPayload();
    byte[] newPayload = new byte[payload.length + framePayload.length];
    System.arraycopy(payload, 0, newPayload, 0, payload.length);
    System.arraycopy(framePayload, 0, newPayload, payload.length, framePayload.length);
    payload = newPayload;
    return handleOpCode(frame);
  }

  /**
   * 0x0 - continuation (concatenate payload)
   * 0x1 - text
   * 0x2 - binary
   * 0x9 - ping
   * 0xA - pong (same payload, max len = 125)
   * 0x8 - closing
   * 0x3-0x7 & 0xB-0xF - nothing
  */
  private Opcode handleOpCode(DataFrame f) throws IOException {
    Opcode opcode = f.getOpcode();
    if (opcode == Opcode.UNUSED) return opcode;
    if (opcode == Opcode.TEXT || opcode == Opcode.BINARY) dataType = opcode.getValue() - 1; // 0 - text ;; 1 - binary
    if (opcode == Opcode.CONTINUE|| !f.getFin()) {
      if (f.getDataType() != dataType) return opcode;
      readNext();
    }
    return opcode;
  }

  public byte[] getBinaryPayload() {
    return dataType == 1 ? payload : null;
  }

  public String getStringPayload() {
    return dataType == 0 ? new String(payload, StandardCharsets.UTF_8) : null;
  }

  public String getStringPayload(Charset chrset) {
    return dataType == 0 ? new String(payload, chrset) : null;
  }

  public DataFrame getStartFrame() {
    return startFrame;
  }
}
