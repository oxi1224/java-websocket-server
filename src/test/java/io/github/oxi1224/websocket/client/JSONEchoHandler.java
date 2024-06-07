package io.github.oxi1224.websocket.client;

import java.io.IOException;

import io.github.oxi1224.websocket.server.ClientSocket;
import io.github.oxi1224.websocket.json.JSONException;
import io.github.oxi1224.websocket.messages.Handler;
import io.github.oxi1224.websocket.messages.MessageHandler;

@Handler(id = "json")
public class JSONEchoHandler implements MessageHandler<ClientSocket> {
  public void onMessage(ClientSocket c) {
    try {
      c.write("", c.getJSONPayload());
    } catch (IOException e) {}
    catch (JSONException e) {}
  }
}

