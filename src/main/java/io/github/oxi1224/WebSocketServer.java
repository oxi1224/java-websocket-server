package io.github.oxi1224;

import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

interface ClientCallback {
  void accept(ClientSocket client) throws IOException;
}

interface ClientCallbackWithFrame {
  void accept(DataFrame frame, ClientSocket client) throws IOException;
}

public class WebSocketServer extends java.net.ServerSocket {
  private ArrayList<Pair<ClientSocket, Thread>> clients = new ArrayList<Pair<ClientSocket, Thread>>();
  private ClientCallback onMessageCallback;
  private ClientCallbackWithFrame onPingCallback = (DataFrame f, ClientSocket s) -> s.pong(f.getPayload());
  private ClientCallback onCloseCallback = (ClientSocket s) -> s.close();

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
      Thread clientThread = new Thread(() -> {
        while (true) {
          try {
            if (client.in.available() < 1) {
              Thread.sleep(100);
              continue;
            }
            DataReader r = client.read();
            DataFrame refFrame = r.getStartFrame();
            Opcode opcode = refFrame.getOpcode();
            if (opcode == Opcode.PING) {
              onPingCallback.accept(refFrame, client);
            } else if (opcode == Opcode.CLOSE) {
              onCloseCallback.accept(client);
            } else if (onMessageCallback != null) onMessageCallback.accept(client); 
          } catch (IOException e) {
            e.printStackTrace();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }); 
      clients.add(new Pair<ClientSocket, Thread>(client, clientThread));
      clientThread.start();
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
