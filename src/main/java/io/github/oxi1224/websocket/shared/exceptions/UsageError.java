package io.github.oxi1224.websocket.shared.exceptions;

public class UsageError extends Error {
  public UsageError(String msg) {
    super(msg);
  }

  public UsageError() {
    super();
  }
}
