package io.github.oxi1224.websocket.shared.util;

import io.github.oxi1224.websocket.messages.Handler;
import io.github.oxi1224.websocket.messages.MessageHandler;

@Handler
public class TestHandler implements MessageHandler<String> {
  public void onMessage(String a) {}
}
