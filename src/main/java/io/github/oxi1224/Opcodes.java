package io.github.oxi1224;

public enum Opcodes {
  CONTINUE(0x0),
  TEXT(0x1),
  BINARY(0x2),
  CLOSE(0x8), 
  PING(0x9),
  PONG(0xA),
  UNUSED(0xF);

  public static Opcodes findByVal(int value) {
    for (Opcodes c : values()) {
      if (c.getValue() == value) return c;
    }
    return Opcodes.UNUSED;
  }
  private final byte code;
  Opcodes(int code) { this.code = (byte)(code); }
  public byte getValue() { return code; }
}

