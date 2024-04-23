package io.github.oxi1224;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class WebSocketServer {
  private ServerSocket server;
  private Socket client;
  private OutputStream out;
  private InputStream in;

  public void start() throws IOException {
    server = new ServerSocket(4000);
    client = server.accept();
    out = client.getOutputStream();
    in = client.getInputStream();
    HttpParser parsed = new HttpParser(new BufferedReader(new InputStreamReader(in)));
    try {
      byte[] handshake = getHandshakeResponse(parsed.headers.get("Sec-WebSocket-Key"));
      out.write(handshake, 0, handshake.length);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    while (true) {
      DataFrame frame = new DataFrame(in);
      System.out.println(new String(frame.getPayload(), StandardCharsets.UTF_8));
    }
  }

  private byte[] getHandshakeResponse(String key) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    String res = ("HTTP/1.1 101 Switching Protocols\r\n"
    + "Upgrade: websocket\r\n"
    + "Connection: Upgrade\r\n"
  );
    byte[] sha1 = MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8"));
    String encoded = Base64.getEncoder().encodeToString(sha1);
    res += "Sec-WebSocket-Accept: " + encoded + "\r\n\r\n\r\n";
    return res.getBytes("UTF-8");
  }
}
