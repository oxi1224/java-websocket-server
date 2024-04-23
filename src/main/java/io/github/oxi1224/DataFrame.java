package io.github.oxi1224;

import java.io.IOException;
import java.io.InputStream;

public class DataFrame {
  private boolean fin;
  private boolean rsv1;
  private boolean rsv2;
  private boolean rsv3;
  private int opcode;
  private boolean mask;
  private int payloadLength;
  private byte[] maskingKey = null;
  private byte[] payload;

  public DataFrame(InputStream in) throws IOException {
    byte b = (byte)(in.read()); 
    fin = (b & 0x80) != 0;
    rsv1 = (b & 0x70) != 0;
    rsv2 = (b & 0x60) != 0;
    rsv3 = (b & 0x50) != 0;
    opcode = (byte)(b & 0x0F);

    b = (byte)(in.read());
    mask = (0x80 & b) != 0;
    payloadLength = (byte)(0x7F & b);
    int bytesToRead = 0;
    if (payloadLength == 126) bytesToRead = 2;
    if (payloadLength == 127) bytesToRead = 8;
    if (bytesToRead != 0) {
      byte[] bytes = in.readNBytes(bytesToRead);
      payloadLength = 0; 
      for (byte _b : bytes) {
        payloadLength = (payloadLength << 8) + (_b & 0xFF);
      }
    }
    if (mask) {
      maskingKey = new byte[4];
      in.read(maskingKey, 0, 4);
    }
    payload = new byte[payloadLength];
    in.read(payload, 0, payloadLength);
    if (mask) {
      for (int i = 0; i < payloadLength; i++) {
        payload[i] = (byte)(payload[i] ^ maskingKey[i % 4]);
      }
    }
  }

  private void parseFrame(InputStream in) throws IOException {};

  private void handleOpCode() {};

  public boolean getFin() { return fin; }
  public boolean getRsv1() { return rsv1; }
  public boolean getRsv2() { return rsv2; }
  public boolean getRsv3() { return rsv3; }
  public int getOpcode() { return opcode; }
  public boolean getMask() { return mask; }
  public int getPayloadLength() { return payloadLength; }
  public byte[] getMaskingKey() { return maskingKey; }
  public byte[] getPayload() { return payload; }
}
