package io.github.oxi1224.websocket.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import io.github.oxi1224.websocket.shared.exceptions.UnexpectedFrameException;

public class DataReader {
  private byte[] payload;
  private InputStream in;
  private ArrayList<DataFrame> frameStream;

  public DataReader(InputStream in) {
    this.in = in;
  }

  public void read() throws IOException, UnexpectedFrameException {
    frameStream = new ArrayList<DataFrame>();
    DataFrame frame = DataFrame.read(in);
    frameStream.add(frame);
    payload = frame.getPayload();
    if (!frame.getFin()) readNext();
  }

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

  public byte[] getBytePayload() {
    return payload;
  }

  public String getPayload() {
    return new String(payload, StandardCharsets.UTF_8);
  }

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
