package io.github.oxi1224;

import java.io.IOException;

import io.github.oxi1224.websocket.server.*;

public class Main {
  public static void main(String[] args) throws IOException {
    WebSocketServer serv = new WebSocketServer(9001);
    serv.setHandlersPackageName("io.github.oxi1224");
    serv.useNormalWebsocket();
    serv.start();
    serv.close();
  }
}
