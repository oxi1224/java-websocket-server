package io.github.oxi1224;

import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    WebSocketServer serv = new WebSocketServer();
    try { 
      serv.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
