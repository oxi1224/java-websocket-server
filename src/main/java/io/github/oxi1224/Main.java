package io.github.oxi1224;

import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    TCPServer serv = new TCPServer();
    try { 
      serv.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
