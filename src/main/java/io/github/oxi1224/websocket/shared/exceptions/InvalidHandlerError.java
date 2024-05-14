package io.github.oxi1224.websocket.shared.exceptions;

public class InvalidHandlerError extends Error {
  public InvalidHandlerError() {
    super();
  }

  public InvalidHandlerError(String msg) {
    super(msg);
  }
}
