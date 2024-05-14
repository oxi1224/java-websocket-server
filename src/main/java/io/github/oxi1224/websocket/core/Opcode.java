package io.github.oxi1224.websocket.core;

public enum Opcode {
  CONTINUE(0x0),
  TEXT(0x1),
  BINARY(0x2),
  CLOSE(0x8), 
  PING(0x9),
  PONG(0xA),
  UNUSED(0xF);

  public static Opcode findByVal(int value) {
    for (Opcode c : values()) {
      if (c.getValue() == value) return c;
    }
    return Opcode.UNUSED;
  }
  private final byte code;
  Opcode(int code) { this.code = (byte)(code); }
  public byte getValue() { return code; }
}

