package io.github.oxi1224;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;

interface ClientCallback {
  void accept(ClientSocket client) throws IOException;
}

public class WebSocketServer extends java.net.ServerSocket {
  private ArrayList<Pair<ClientSocket, Thread>> clients = new ArrayList<Pair<ClientSocket, Thread>>();
  private ClientCallback onMessageCallback;
  private ClientCallback onPingCallback;
  private ClientCallback onCloseCallback;

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
    ClientSocket client = new ClientSocket(this.accept());
    client.sendHandshake();
    Thread clientThread = new Thread() {
      @Override
      public void run() {
        while (true) {
          try {
            DataReader r = client.read();
            DataFrame refFrame = r.getStartFrame();
            Opcode opcode = refFrame.getOpcode();
            if (opcode == Opcode.PING) {
              if (onPingCallback != null) onPingCallback.accept(client);
              else client.pong(refFrame.getPayload());
            } else if (opcode == Opcode.CLOSE) {
              if (onCloseCallback != null) onCloseCallback.accept(client);
              else client.close();
            } else {
              if (onMessageCallback != null) onMessageCallback.accept(client);
            }
          } catch (IOException e) {
            System.out.println("Exception caught in client thread");
            e.printStackTrace();
          }
        }
      }
    };
    clients.add(new Pair<ClientSocket, Thread>(client, clientThread));
    clientThread.start();
    // clients.add(client);
    // out = client.getOutputStream();
    // in = client.getInputStream();
    // HttpParser parsed = new HttpParser(new BufferedReader(new InputStreamReader(in)));
    // try {
      // byte[] handshake = getHandshakeResponse(parsed.headers.get("Sec-WebSocket-Key"));
      // out.write(handshake, 0, handshake.length);
    // } catch (NoSuchAlgorithmException e) {
      // e.printStackTrace();
    // } catch (UnsupportedEncodingException e) {
      // e.printStackTrace();
    // }
    // while (true) {
      // DataFrame frame = new DataFrame(in);
      // System.out.println(new String(frame.getPayload(), StandardCharsets.UTF_8));
    // }
  }

  public void onMessage(ClientCallback callback) {
    onMessageCallback = callback;
  }

  public void onPing(ClientCallback callback) {
    onPingCallback = callback; 
  }

  public void onClose(ClientCallback callback) {
    onCloseCallback = callback;
  }

  // private byte[] getHandshakeResponse(String key) throws NoSuchAlgorithmException {
    // String res = ("HTTP/1.1 101 Switching Protocols\r\n"
    // + "Upgrade: websocket\r\n"
    // + "Connection: Upgrade\r\n"
  // );
    // byte[] sha1 = MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8));
    // String encoded = Base64.getEncoder().encodeToString(sha1);
    // res += "Sec-WebSocket-Accept: " + encoded + "\r\n\r\n\r\n";
    // return res.getBytes(StandardCharsets.UTF_8);
  // }
}
