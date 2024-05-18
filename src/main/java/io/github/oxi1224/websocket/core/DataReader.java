package io.github.oxi1224.websocket.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import io.github.oxi1224.websocket.shared.exceptions.UnexpectedFrameException;

/**
 * A utility class which reads a stream until a DataFrame with fin=true is encountered
 */
public class DataReader {
  /**
   * The collected data, gets reset evey read() call
   */
  private byte[] payload;

  /**
   * All frames making up the payload, gets reset every read() call
   */
  private ArrayList<DataFrame> frameStream;
  private InputStream in;

  public DataReader(InputStream in) {
    this.in = in;
  }
  
  /**
   * Reads a stream of data until fin=1 is encountered
   * @exception UnexpectedFrameException when receiving an invalid frame order (no fin=1 frame before start of the next one)
   */
  public void read() throws IOException, UnexpectedFrameException {
    frameStream = new ArrayList<DataFrame>();
    DataFrame frame = DataFrame.read(in);
    frameStream.add(frame);
    payload = frame.getPayload();
    if (!frame.getFin()) readNext();
  }
  
  /**
   * Recursively reads frames one by one until finds one with fin=1
   * @exception UnexpectedFrameException when receiving an invalid frame order (no fin=1 frame before start of the next one)
   */
  private void readNext() throws IOException, UnexpectedFrameException {
    DataFrame frame = DataFrame.read(in);
    if (frame.getOpcode() != Opcode.CONTINUE) throw new UnexpectedFrameException("Expected to receive CONTINUE frame");
    byte[] framePayload = frame.getPayload();
    byte[] newPayload = new byte[payload.length + framePayload.length];
    System.arraycopy(payload, 0, newPayload, 0, payload.length);
    System.arraycopy(framePayload, 0, newPayload, payload.length, framePayload.length);
    payload = newPayload;
    frameStream.add(frame);
    if (!frame.getFin()) readNext();
  }
  
  /**
   * @return the colleted payload in bytes
   */
  public byte[] getBytePayload() {
    return payload;
  }
  
  /**
   * @return the collected payload as a UTF-8 string
   */
  public String getPayload() {
    return new String(payload, StandardCharsets.UTF_8);
  }
  
  /**
   * @param chrset - The charset to use when parsing into a string
   * @return the collected payload as a string in the specified charset
   * @see java.nio.charset.StandardCharsets
   */
  public String getPayload(Charset chrset) {
    return new String(payload, chrset);
  }

  public DataFrame getStartFrame() {
    return frameStream.isEmpty() ? null : frameStream.iterator().next();
  }
  
  public ArrayList<DataFrame> getFrameStream() {
    return frameStream;
  }
}
