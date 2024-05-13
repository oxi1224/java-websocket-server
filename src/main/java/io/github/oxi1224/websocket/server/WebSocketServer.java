package io.github.oxi1224.websocket.server;

import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import io.github.oxi1224.websocket.shared.*;

public class WebSocketServer extends java.net.ServerSocket {
  public Map<ClientSocket, Thread> clients = new HashMap<>();
  private ClientCallback onMessageCallback = (c) -> { assert true; }; // Do nothing
  private ClientCallbackWithFrame onPingCallback = (f, c) -> { assert true; }; // Do nothing
  private ClientCallback onCloseCallback = (c) -> { assert true; }; // Do nothing

  public interface ClientCallback {
    void accept(ClientSocket client) throws IOException;
  }

  public interface ClientCallbackWithFrame {
    void accept(DataFrame frame, ClientSocket client) throws IOException;
  }

  public WebSocketServer(int port) throws IOException {
    super(port);
  }

  public WebSocketServer(int port, int backlog) throws IOException {
    super(port, backlog);
  }

  public WebSocketServer(int port, int backlog, InetAddress bindAddr) throws IOException {
    super(port, backlog, bindAddr);
  }
  
  public void start() throws IOException, NoSuchAlgorithmException {
    while (true) {
      ClientSocket client = new ClientSocket(this.accept());
      try {
        client.sendHandshake();
      } catch (InterruptedException e) {
        client.close();
      }
      client.onClose((c) -> cleanupSocket(c));
      Thread clientThread = new Thread(() -> {
        while (!Thread.interrupted()) {
          try {
            client.read();
            DataFrame refFrame = client.getPayloadStartFrame();
            Opcode opcode = refFrame.getOpcode();
            if (opcode == Opcode.PING) {
              onPingCallback.accept(refFrame, client);
            } else if (opcode == Opcode.CLOSE) {
              onCloseCallback.accept(client);
            } else if (opcode == Opcode.PONG) { 
              continue;
            } else if (onMessageCallback != null) onMessageCallback.accept(client); 
          } catch (IOException e) {
            if (e.getMessage() != "Socket closed") e.printStackTrace();
            break;
          }
        }
      });
      clients.put(client, clientThread);
      clientThread.start();
    }
  }

  private void cleanupSocket(ClientSocket socket) {
    synchronized(clients) {
      Thread t = clients.remove(socket);
      if (t != null) t.interrupt();
    }
  }

  public void onMessage(ClientCallback callback) {
    onMessageCallback = callback;
  }

  public void onPing(ClientCallbackWithFrame callback) {
    onPingCallback = callback; 
  }

  public void onClose(ClientCallback callback) {
    onCloseCallback = callback;
  }
}
