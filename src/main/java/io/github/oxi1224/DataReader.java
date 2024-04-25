package io.github.oxi1224;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class DataReader {
  private InputStream in; 
  private byte[] payload;
  private DataFrame startFrame;
  private int dataType;

  public DataReader(InputStream _in) {
    in = _in;
  }

  public void read() throws IOException {
    DataFrame frame = new DataFrame().read(in);
    startFrame = frame;
    payload = frame.getPayload();
    handleOpCode(frame);
  }

  private void readNext() throws IOException {
    DataFrame frame = new DataFrame().read(in);
    byte[] framePayload = frame.getPayload();
    byte[] newPayload = new byte[payload.length + framePayload.length];
    System.arraycopy(payload, 0, newPayload, 0, payload.length);
    System.arraycopy(framePayload, 0, newPayload, payload.length, framePayload.length);
    payload = newPayload;
    handleOpCode(frame);
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
  private void handleOpCode(DataFrame f) throws IOException {
    byte opcode = f.getOpcode();
    if ((opcode >= 0x3 && opcode <= 0x7) || (opcode >= 0xB && opcode <= 0xF)) return;
    if (opcode == 0x1 || opcode == 0x2) dataType = opcode - 1; // 0 - text ;; 1 - binary
    if (opcode == 0x0 || !f.getFin()) {
      if (f.dataType != dataType) return;
      readNext();
    }
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
