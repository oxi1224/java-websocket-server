package io.github.oxi1224.websocket.messages;

/**
 * An interface that every valid handler must extends
 * @param T - The type that onMessage callback accepts
 */
public interface MessageHandler<T> {
  public void onMessage(T data);
}

