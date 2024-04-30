package io.github.oxi1224;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Main {
  public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
    WebSocketServer serv = new WebSocketServer(4000);
    serv.onMessage(client -> {
      client.write("Hello");
    });
    serv.start();
    serv.close();
  }
}
