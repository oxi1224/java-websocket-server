package io.github.oxi1224.websocket.client;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import io.github.oxi1224.websocket.core.DataFrame;
import io.github.oxi1224.websocket.core.Opcode;
import io.github.oxi1224.websocket.json.JSONException;
import io.github.oxi1224.websocket.json.JSONObject;
import io.github.oxi1224.websocket.server.WebSocketServer;
import io.github.oxi1224.websocket.shared.exceptions.ConnectionException;

import java.io.IOException;

class ClientTest {
  @Test
  public void testPing() throws IOException, ConnectionException {
    WebSocketServer server = new WebSocketServer(9000);
    server.disableJSON();
    startServerThread(server);
    Client.disableJSON();
    Client client = Client.connect("127.0.0.1", 9000);

    client.pingServer();
    DataFrame frame = client.getPayloadStartFrame();
    assertEquals(Opcode.PONG, frame.getOpcode(), "Expected client to receive back a PONG frame");
  }

  @Test
  public void testClose() throws IOException, ConnectionException {
    WebSocketServer server = new WebSocketServer(9001);
    server.disableJSON();
    startServerThread(server);
    Client.disableJSON();
    Client client = Client.connect("127.0.0.1", 9001);
    
    client.close();
    DataFrame frame = client.getPayloadStartFrame();
    assertEquals(Opcode.CLOSE, frame.getOpcode(), "Expected client to receive back a CLOSE frame");
  }

  @Test void testCommunication() throws IOException, ConnectionException {
    WebSocketServer server = new WebSocketServer(9002);
    server.disableJSON();
    startServerThread(server);
    Client.disableJSON();
    Client client = Client.connect("127.0.0.1", 9002);

    client.write("Hello World");
    client.read();
    assertEquals("Hello World", client.getPayload(), "Sent/Received data differ");
  }

  @Test void testJSONCommunication() throws IOException, ConnectionException, JSONException {
    WebSocketServer server = new WebSocketServer(9003);
    startServerThread(server);
    Client.enableJSON();
    Client client = Client.connect("127.0.0.1", 9003);
    JSONObject obj = new JSONObject();
    obj.set("message", "hello world");
    client.write("json", obj);
    client.read();
    assertEquals(obj.toString(), client.getJSONPayload().toString());
  }

  public static void startServerThread(WebSocketServer srv) {
    Thread t = new Thread(() -> {
      try {
        srv.setHandlersPackageName("io.github.oxi1224.websocket.client");
        srv.start();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    t.start();
  }
}
