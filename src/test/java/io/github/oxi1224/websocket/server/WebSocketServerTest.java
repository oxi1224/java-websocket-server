package io.github.oxi1224.websocket.server;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import io.github.oxi1224.websocket.client.Client;
import io.github.oxi1224.websocket.core.Opcode;
import io.github.oxi1224.websocket.json.JSONException;
import io.github.oxi1224.websocket.json.JSONObject;
import io.github.oxi1224.websocket.shared.exceptions.ConnectionException;

import java.io.IOException;
import java.util.function.Consumer;

/// TODO: Rewrite this test cause wtf is this
class ConnectTask implements Consumer<Integer> {
  private WebSocketServer s;
  private ClientSocket c;
  public boolean disablejson = true;

  public void accept(Integer port) {
    try {
      s = new WebSocketServer(port);
      if (disablejson) s.disableJSON();
      c = new ClientSocket(s.accept());
      if (disablejson) c.disableJSON();
      c.sendHandshake();
    } catch (IOException e) { e.printStackTrace(); }
  }

  public WebSocketServer getServer() { return this.s; }
  public ClientSocket getClient() { return this.c; }
}

class WebSocketServerTest {
  @Test
  public void testPing() throws IOException, InterruptedException, ConnectionException {
    ConnectTask c = new ConnectTask();
    c.disablejson = true;
    Thread t = new Thread(() -> c.accept(8000));
    t.start();
    Thread.sleep(10); // Needed so the server starts first
    Client.disableJSON();
    Client client = Client.connect("127.0.0.1", 8000);
    ClientSocket client_sock = c.getClient();
    runInThread(() -> { 
      try { client.pingServer(); }
      catch (IOException e) { e.printStackTrace(); }
    });
    client_sock.read();
    assertEquals(Opcode.PING, client_sock.getPayloadStartFrame().getOpcode(), "Invalid opcode received");
  }

  @Test
  public void testClose() throws IOException, InterruptedException, ConnectionException {
    ConnectTask c = new ConnectTask();
    c.disablejson = true;
    Thread t = new Thread(() -> c.accept(8001));
    t.start();
    Thread.sleep(10); // Needed so the server starts first
    Client.disableJSON();
    Client client = Client.connect("127.0.0.1", 8001);
    ClientSocket client_sock = c.getClient();
    runInThread(() -> { 
      try { client.close(); }
      catch (IOException e) { e.printStackTrace(); }
    });
    client_sock.read();
    assertEquals(Opcode.CLOSE, client_sock.getPayloadStartFrame().getOpcode(), "Invalid opcode received");
  }

  @Test
  public void testMessage() throws IOException, InterruptedException, ConnectionException {
    ConnectTask c = new ConnectTask();
    c.disablejson = true;
    Thread t = new Thread(() -> c.accept(8002));
    t.start();
    Thread.sleep(10); // Needed so the server starts first
    Client.disableJSON();
    Client client = Client.connect("127.0.0.1", 8002);
    ClientSocket client_sock = c.getClient();
    runInThread(() -> { 
      try { client.write("Hello World"); }
      catch (IOException e) { e.printStackTrace(); }
    });
    client_sock.read();
    assertEquals("Hello World", client_sock.getPayload(), "Invalid opcode received");
  }

  @Test
  public void testJSONMessage() throws IOException, InterruptedException, ConnectionException, JSONException {
    ConnectTask c = new ConnectTask();
    c.disablejson = false;
    Thread t = new Thread(() -> c.accept(8003));
    t.start();
    Thread.sleep(10); // Needed so the server starts first
    Client.enableJSON();
    Client client = Client.connect("127.0.0.1", 8003);
    ClientSocket client_sock = c.getClient();
    JSONObject obj = new JSONObject();
    obj.set("message", "hello world");
    runInThread(() -> { 
      try {
        client.write("", obj);
      }
      catch (IOException e) { e.printStackTrace(); }
    });
    client_sock.read();
    assertEquals(obj.toString(), client_sock.getJSONPayload().toString(), "Invalid opcode received");
  }

  public static void runInThread(Runnable cb) {
    new Thread(() -> cb.run()).start();
  }
}
