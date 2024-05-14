package io.github.oxi1224.websocket.shared.exceptions;

public class UnexpectedFrameException extends Exception {
  public UnexpectedFrameException() {
    super();
  }

  public UnexpectedFrameException(String message) {
    super(message);
  }
}
