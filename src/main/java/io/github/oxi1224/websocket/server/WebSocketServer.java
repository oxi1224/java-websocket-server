package io.github.oxi1224.websocket.server;

import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import io.github.oxi1224.websocket.shared.*;

public class WebSocketServer extends java.net.ServerSocket {
  private ArrayList<Pair<ClientSocket, Thread>> clients = new ArrayList<Pair<ClientSocket, Thread>>();
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
      client.sendHandshake();
      client.onClose((c) -> cleanupSocket(c));
      Thread clientThread = new Thread(() -> {
        while (!Thread.interrupted()) {
          try {
            DataReader r = client.read();
            DataFrame refFrame = r.getStartFrame();
            Opcode opcode = refFrame.getOpcode();
            if (opcode == Opcode.PING) {
              onPingCallback.accept(refFrame, client);
            } else if (opcode == Opcode.CLOSE) {
              onCloseCallback.accept(client);
            } else if (opcode == Opcode.PONG) { 
              continue;
            } else if (onMessageCallback != null) onMessageCallback.accept(client); 
          } catch (IOException e) {
            e.printStackTrace();
            break;
          }
        }
      }); 
      clients.add(new Pair<ClientSocket, Thread>(client, clientThread));
      clientThread.start();
    }
  }

  private void cleanupSocket(ClientSocket socket) {
    for (Pair<ClientSocket, Thread> p : clients) {
      if (p.getKey().equals(socket)) {
        p.getValue().interrupt();
        clients.remove(p);
        break;
      }
    }
    return;
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
