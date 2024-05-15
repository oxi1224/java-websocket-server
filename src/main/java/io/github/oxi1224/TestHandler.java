package io.github.oxi1224;

import java.io.IOException;

import io.github.oxi1224.websocket.server.ClientSocket;
import io.github.oxi1224.websocket.shared.DefaultHandlerID;
import io.github.oxi1224.websocket.shared.Handler;
import io.github.oxi1224.websocket.shared.MessageHandler;

@Handler(id = DefaultHandlerID.DEFAULT)
public class TestHandler implements MessageHandler<ClientSocket> {
  public void onMessage(ClientSocket c) {
    try {
      c.write(c.getPayload());
    } catch (IOException e) { e.printStackTrace(); }
  }
}