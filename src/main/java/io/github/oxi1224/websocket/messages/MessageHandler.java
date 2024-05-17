package io.github.oxi1224.websocket.messages;

public interface MessageHandler<T> {
  public void onMessage(T data);
}
