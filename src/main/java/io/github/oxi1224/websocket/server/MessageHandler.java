package io.github.oxi1224.websocket.server;

public interface MessageHandler {
  public void onMessage(ClientSocket client);
}
