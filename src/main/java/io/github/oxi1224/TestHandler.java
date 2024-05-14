package io.github.oxi1224;

import java.io.IOException;

import io.github.oxi1224.websocket.server.ClientSocket;
import io.github.oxi1224.websocket.server.MessageHandler;
import io.github.oxi1224.websocket.shared.Handler;

@Handler
public class TestHandler implements MessageHandler {
  public void onMessage(ClientSocket c) {
    try {
      c.write(c.getPayload());
    } catch (IOException e) { e.printStackTrace(); }
  }
}
