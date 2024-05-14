package io.github.oxi1224.websocket.client;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import io.github.oxi1224.websocket.core.DataFrame;
import io.github.oxi1224.websocket.core.Opcode;
import io.github.oxi1224.websocket.server.WebSocketServer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

class ClientTest {
  @Test
  public void testPing() throws IOException, InterruptedException{
    WebSocketServer server = new WebSocketServer(9000);
    startServerThread(server);
    Client client = Client.connect("127.0.0.1", 9000);

    client.pingServer();
    DataFrame frame = client.getPayloadStartFrame();
    assertEquals(Opcode.PONG, frame.getOpcode(), "Expected client to receive back a PONG frame");
  }

  @Test
  public void testClose() throws IOException, InterruptedException {
    WebSocketServer server = new WebSocketServer(9001);
    startServerThread(server);
    Client client = Client.connect("127.0.0.1", 9001);
    
    client.close();
    DataFrame frame = client.getPayloadStartFrame();
    assertEquals(Opcode.CLOSE, frame.getOpcode(), "Expected client to receive back a CLOSE frame");
  }

  @Test void testCommunication() throws IOException, InterruptedException {
    WebSocketServer server = new WebSocketServer(9002);
    startServerThread(server);
    Client client = Client.connect("127.0.0.1", 9002);

    client.write("Hello World");
    client.read();
    assertEquals("Hello World", client.getPayload(), "Sent/Received data differ");
  }

  public static void startServerThread(WebSocketServer srv) {
    Thread t = new Thread(() -> {
      try {
        srv.onMessage((c) -> {
          c.write(c.getPayload());
        });
        srv.start();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }
    });
    t.start();
  }
}
