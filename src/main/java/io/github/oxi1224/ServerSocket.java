package io.github.oxi1224;

import java.io.IOException;
import java.net.InetAddress;

public class ServerSocket extends java.net.ServerSocket {
  
  public ServerSocket(int port) throws IOException {
    super(port);
  }

  public ServerSocket(int port, int backlog) throws IOException {
    super(port, backlog);
  }

  public ServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
    super(port, backlog, bindAddr);
  }
}
