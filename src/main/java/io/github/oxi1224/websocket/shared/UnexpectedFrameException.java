package io.github.oxi1224.websocket.shared;

public class UnexpectedFrameException extends Exception {
  public UnexpectedFrameException(String message) {
    super(message);
  }
}
