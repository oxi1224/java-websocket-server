package io.github.oxi1224.websocket.shared.exceptions;

public class ConnectionException extends Exception {
  public ConnectionException(String msg) {
    super(msg);
  }

  public ConnectionException() {
    super();
  }
}
