package io.github.oxi1224;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public class DataReader {
  private byte[] payload;
  private int dataType;
  private InputStream in;
  private HashSet<DataFrame> frameStream = new HashSet<DataFrame>();

  public DataReader(InputStream in) {
    this.in = in;
  }

  public void read() throws IOException {
    DataFrame frame = new DataFrame().read(in);
    frameStream.add(frame);
    payload = frame.getPayload();
    if (!frame.getFin()) readNext();
  }

  private void readNext() throws IOException {
    DataFrame frame = new DataFrame().read(in);
    byte[] framePayload = frame.getPayload();
    byte[] newPayload = new byte[payload.length + framePayload.length];
    System.arraycopy(payload, 0, newPayload, 0, payload.length);
    System.arraycopy(framePayload, 0, newPayload, payload.length, framePayload.length);
    payload = newPayload;
    frameStream.add(frame);
    if (!frame.getFin()) readNext();
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

  public byte[] getPayload() {
    return dataType == 1 ? payload : null;
  }

  public String getStringPayload() {
    return dataType == 0 ? new String(payload, StandardCharsets.UTF_8) : null;
  }

  public String getStringPayload(Charset chrset) {
    return dataType == 0 ? new String(payload, chrset) : null;
  }

  public DataFrame getStartFrame() {
    return frameStream.isEmpty() ? null : frameStream.iterator().next();
  }
  
  public HashSet<DataFrame> getFrameStream() {
    return frameStream;
  }
}
