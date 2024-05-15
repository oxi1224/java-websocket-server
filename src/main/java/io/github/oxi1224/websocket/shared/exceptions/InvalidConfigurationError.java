package io.github.oxi1224.websocket.shared.exceptions;

public class InvalidConfigurationError extends Error {
  public InvalidConfigurationError(String msg) {
    super(msg);
  }

  public InvalidConfigurationError() {
    super();
  }
}
