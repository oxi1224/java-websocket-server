package io.github.oxi1224;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Base64;
import java.util.Scanner;

public class ClientSocket extends DataWriter {
  private UUID id;
  private Socket javaSocket;
  private InputStream in;
  private OutputStream out;
  private DataReader reader;
  private Timer timer = new Timer();
 
  public ClientSocket(Socket sock) throws IOException {
    super(sock.getOutputStream());
    id = UUID.randomUUID();
    javaSocket = sock;
    in = sock.getInputStream();
    out = sock.getOutputStream();
    reader = new DataReader(in);
  }

  public void sendHandshake() throws IOException, NoSuchAlgorithmException {
    HttpParser parsed = new HttpParser(new Scanner(in));
    String res = ("HTTP/1.1 101 Switching Protocols\r\n"
      + "Upgrade: websocket\r\n"
      + "Connection: Upgrade\r\n"
    );
    String key = parsed.headers.get("Sec-WebSocket-Key");
    byte[] sha1 = MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8));
    String encoded = Base64.getEncoder().encodeToString(sha1);
    res += "Sec-WebSocket-Accept: " + encoded + "\r\n\r\n\r\n";
    byte[] outbuf = res.getBytes(StandardCharsets.UTF_8);
    out.write(outbuf, 0, outbuf.length); 
  }

  public DataReader read() throws IOException {
    reader.read();
    DataFrame refFrame = reader.getStartFrame();
    Opcode opcode = refFrame.getOpcode();
    if (opcode == Opcode.PING) pong(reader.getPayload());
    if (opcode == Opcode.CLOSE) onReceiveClose();
    return reader;
  }

  public void ping() throws IOException {
    write(true, Opcode.PING, new byte[0]);
    startTimeoutTimer(10000);
    try {
      reader.read();
      Opcode resOpcode = reader.getStartFrame().getOpcode();
      if (resOpcode != Opcode.PONG) javaSocket.close();
    } catch (IOException e) {} // Ignore error, timeoutTimer closed connection while trying to read
  }

  public void pong(byte[] pingPayload) throws IOException {
    write(true, Opcode.PONG, pingPayload);
  }

  public void close() throws IOException {
    write(true, Opcode.CLOSE, new byte[0]);
    startTimeoutTimer(10000);
    try {
      reader.read();
      javaSocket.close();
    } catch (IOException e) {} // Ignore error, timeoutTimer closed connection while trying to read
    timer.cancel();
  }

  private void closeWithoutWait() throws IOException {
    write(true, Opcode.CLOSE, new byte[0]);
    javaSocket.close();
  }

  private void onReceiveClose() throws IOException {
    write(true, Opcode.CLOSE, new byte[0]);
    javaSocket.close();
  }

  private void startTimeoutTimer(long delay) {
    timer.schedule(new TimerTask() {
      @Override 
      public void run() {
        try {
          closeWithoutWait();
        } catch (IOException err) {
          System.out.println("Exception while calling closeWithoutWait()");
          err.printStackTrace();
        }
      }
    }, delay); 
  }
  public UUID getID() { return this.id; }
  public byte[] getPayload() { return this.reader.getPayload(); }
  public String getStringPayload() { return this.reader.getStringPayload(); }
  public String getStringPayload(Charset chrset) { return this.reader.getStringPayload(chrset); }
  public Socket getJavaSocket() { return this.javaSocket; }
}
