package io.github.oxi1224;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientSocket extends DataWriter {
  private Socket javaSocket;
  private InputStream in;
  private OutputStream out;
  private DataReader reader;

  public ClientSocket(Socket sock) throws IOException {
    super(sock.getOutputStream());
    javaSocket = sock;
    in = sock.getInputStream();
    out = sock.getOutputStream();
    reader = new DataReader();
  }

  public void read() throws IOException {
    Opcode opcode = reader.read(in);
    if (opcode == Opcode.PING) pong();
  }

  public void ping() throws IOException {
    write(true, Opcode.PING, new byte[0]);
    Opcode resOpcode = reader.read(in);
    if (resOpcode != Opcode.PONG) javaSocket.close();
  }

  public void pong() throws IOException {
    write(true, Opcode.PONG, new byte[0]);
  }

  public Socket getJavaSocket() { return this.javaSocket; }
}
