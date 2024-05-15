package io.github.oxi1224.websocket.shared;

public interface MessageHandler<T> {
  public void onMessage(T data);
}
