package io.github.oxi1224.websocket.core;

public enum StatusCode {
  S_1000(1000, new byte[]{(byte)0x03, (byte)0xE8}), 
  S_1001(1001, new byte[]{(byte)0x03, (byte)0xE9}), 
  S_1002(1002, new byte[]{(byte)0x03, (byte)0xEA}), 
  S_1003(1003, new byte[]{(byte)0x03, (byte)0xEB}), 
  S_1005(1005, new byte[]{(byte)0x03, (byte)0xED}), 
  S_1006(1006, new byte[]{(byte)0x03, (byte)0xEE}), 
  S_1007(1007, new byte[]{(byte)0x03, (byte)0xEF}), 
  S_1008(1008, new byte[]{(byte)0x03, (byte)0xF0}), 
  S_1009(1009, new byte[]{(byte)0x03, (byte)0xF1}), 
  S_1010(1010, new byte[]{(byte)0x03, (byte)0xF2}),
  S_1011(1011, new byte[]{(byte)0x03, (byte)0xF3});

  private final int code;
  private final byte[] bytes;

  StatusCode(int code, byte[] bytes) {
    this.code = code;
    this.bytes = bytes;
  }

  public int getCode() { return code; }
  public byte[] getBytes() { return bytes; }
}
