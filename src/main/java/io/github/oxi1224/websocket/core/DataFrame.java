package io.github.oxi1224.websocket.core;

import java.io.IOException;
import java.io.InputStream;

public class DataFrame {
  private boolean fin;
  private boolean rsv1;
  private boolean rsv2;
  private boolean rsv3;
  private Opcode opcode;
  private boolean mask;
  private int payloadLength;
  private byte[] maskingKey = null;
  private byte[] payload;
 
  public DataFrame() {}
  public DataFrame(
    boolean fin,
    boolean rsv1,
    boolean rsv2,
    boolean rsv3,
    Opcode opcode,
    boolean mask,
    int payloadLength,
    byte[] maskingKey,
    byte[] payload
  ) {
    this.fin = fin;
    this.rsv1 = rsv1;
    this.rsv2 = rsv2;
    this.rsv3 = rsv3;
    this.opcode = opcode;
    this.mask = mask;
    this.payloadLength = payloadLength;
    this.maskingKey = maskingKey;
    this.payload = payload;
  }
  
  public byte[] getBytes() {
    int calcLength = 2 + payloadLength; // 2 = min len;
    if (payloadLength <= 65535 && payloadLength > 125) calcLength += 3; // 1 byte + next 2
    else if (payloadLength > 65535) calcLength += 9; // 1 byte + next 8
    if (mask) {
      if (maskingKey.length != 4) throw new IllegalArgumentException("Mask is set to true but maskingkey length is not 4");
      else calcLength += 4;
    }
    byte[] out = new byte[calcLength];
    int curIdx = 0;
    byte b = 0x0;
    if (fin) b |= 0b10000000;
    if (rsv1) b |= 0b01000000;
    if (rsv2) b |= 0b00100000;
    if (rsv3) b |= 0b00010000;
    b |= opcode.getValue();
    out[curIdx] = (byte)(b & 0xFF);
    curIdx += 1;

    if (payloadLength <= 125) {
      b = 0x0;
      if (mask) b |= 0b10000000;
      b |= payloadLength;
      out[curIdx] = b;
      curIdx += 1;
    } else {
      if (payloadLength <= 65535) {
        if (mask) out[curIdx] = (byte)(0b10000000);
        out[curIdx] |= 126;
        out[curIdx + 1] = (byte)((payloadLength >> 8) & 0xFF); 
        out[curIdx + 2] = (byte)(payloadLength & 0xFF);
        curIdx += 3;
      } else {
        if (mask) out[curIdx] = (byte)(0b10000000);
        out[curIdx] |= 127;
        curIdx += 1;
        for (int i = 7; i >= 0; i--) {
          out[curIdx + 8 - i] = (byte)((payloadLength >> (i * 8)) & 0xFF);
          curIdx += 1;
        }
      }
    }
    if (mask) {
      for (int i = 0; i < 4; i++) {
        out[curIdx + i] = maskingKey[i];
      }
      curIdx += 4;
      for (int i = 0; i < payloadLength; i++) {
        out[curIdx + i] = (byte)(payload[i] ^ maskingKey[i % 4]);
      }
      curIdx += payloadLength - 1;
    } else {
      System.arraycopy(payload, 0, out, curIdx, payloadLength);
    }
    return out;
  }

  public static DataFrame read(InputStream in) throws IOException {
    byte b = (byte)(in.read()); 
    boolean fin = (b & 0x80) != 0;
    boolean rsv1 = (b & 0x70) != 0;
    boolean rsv2 = (b & 0x60) != 0;
    boolean rsv3 = (b & 0x50) != 0;
    Opcode opcode = Opcode.findByVal(b & 0x0F);

    b = (byte)(in.read());
    boolean mask = (0x80 & b) != 0;
    int payloadLength = (byte)(0x7F & b);
    int bytesToRead = 0;
    if (payloadLength == 126) bytesToRead = 2;
    if (payloadLength == 127) bytesToRead = 8;
    if (bytesToRead != 0) {
      byte[] bytes = in.readNBytes(bytesToRead);
      payloadLength = 0; 
      for (byte _b : bytes) {
        payloadLength = (payloadLength << 8) + (_b & 0xFF);
      }
    };
    byte[] maskingKey = null;
    if (mask) {
      maskingKey = new byte[4];
      in.read(maskingKey, 0, 4);
    }
    byte[] payload = new byte[payloadLength];
    in.read(payload, 0, payloadLength);
    if (mask) {
      for (int i = 0; i < payloadLength; i++) {
        payload[i] = (byte)(payload[i] ^ maskingKey[i % 4]);
      }
    }
    return new DataFrame(fin, rsv1, rsv2, rsv3, opcode, mask, payloadLength, maskingKey, payload);
  };

  public static byte[] genMaskingKey() {
    byte[] mask = new byte[4];
    for (int i = 0; i < 4; i++) {
      mask[i] = (byte)(Math.random() * 255);
    }
    return mask;
  }

  public boolean getFin() { return fin; }
  public boolean getRsv1() { return rsv1; }
  public boolean getRsv2() { return rsv2; }
  public boolean getRsv3() { return rsv3; }
  public Opcode getOpcode() { return opcode; }
  public boolean getMask() { return mask; }
  public int getPayloadLength() { return payloadLength; }
  public byte[] getMaskingKey() { return maskingKey; }
  public byte[] getPayload() { return payload; }
}
