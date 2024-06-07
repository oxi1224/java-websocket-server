package io.github.oxi1224.websocket.server;

import java.io.IOException;

import io.github.oxi1224.websocket.client.Client;
import io.github.oxi1224.websocket.messages.DefaultHandlerID;
import io.github.oxi1224.websocket.messages.Handler;
import io.github.oxi1224.websocket.messages.MessageHandler;

@Handler(id = DefaultHandlerID.DEFAULT)
public class EchoHandler implements MessageHandler<Client> {
  public void onMessage(Client c) {
    try {
      c.write(c.getPayload());
    } catch (IOException e) {}
  }
}

