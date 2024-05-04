package io.github.oxi1224.websocket.server;

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
import java.util.function.Consumer;
import java.util.Base64;
import java.util.HashSet;
import java.util.Scanner;
import io.github.oxi1224.websocket.shared.*;

public class ClientSocket extends DataWriter {
  private Socket javaSocket;
  private InputStream in;
  private OutputStream out;
  private DataReader reader;
  private Timer timer = new Timer();
  private Consumer<ClientSocket> onCloseCallback;
 
  public ClientSocket(Socket sock) throws IOException {
    super(sock.getOutputStream());
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
    res += "Sec-WebSocket-Accept: " + encoded + "\r\n\r\n";
    byte[] outbuf = res.getBytes(StandardCharsets.UTF_8);
    out.write(outbuf, 0, outbuf.length);
  }

  public DataReader read() throws IOException {
    reader.read();
    DataFrame refFrame = reader.getStartFrame();
    Opcode opcode = refFrame.getOpcode();
    if (opcode == Opcode.PING) pong(reader.getBytePayload());
    if (opcode == Opcode.CLOSE) closeWithoutWait();
    return reader;
  }

  public void ping() throws IOException {
    write(true, Opcode.PING, new byte[0]);
    startTimeoutTimer(10000);
    try {
      reader.read();
      Opcode resOpcode = reader.getStartFrame().getOpcode();
      if (resOpcode != Opcode.PONG) javaSocket.close();
      else timer.cancel();
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
      if (onCloseCallback != null) onCloseCallback.accept(this);
      javaSocket.close();
    } catch (IOException e) {} // Ignore error, timeoutTimer closed connection while trying to read
    timer.cancel();
  }

  public void close(StatusCode statusCode, String reason) throws IOException{
    byte[] stringBytes = reason.getBytes();
    byte[] codeBytes = statusCode.getBytes();
    byte[] payload = new byte[2 + stringBytes.length];
    System.arraycopy(codeBytes, 0, payload, 0, 2);
    System.arraycopy(stringBytes, 0, payload, 2, stringBytes.length);
    write(true, Opcode.CLOSE, payload);
    startTimeoutTimer(10000);
    try {
      reader.read();
      if (onCloseCallback != null) onCloseCallback.accept(this);
      javaSocket.close();
    } catch (IOException e) {} // Ignore error, timeoutTimer closed connection while trying to read
    timer.cancel();
  }

  private void closeWithoutWait() throws IOException {
    if (onCloseCallback != null) onCloseCallback.accept(ClientSocket.this);
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
  
  public void onClose(Consumer<ClientSocket> callback) { this.onCloseCallback = callback; }
  public byte[] getBytePayload() { return this.reader.getBytePayload(); }
  public String getPayload() { return this.reader.getPayload(); }
  public String getPayload(Charset chrset) { return this.reader.getPayload(chrset); }
  public DataFrame getPaylodStartFrame() { return this.reader.getStartFrame(); }
  public HashSet<DataFrame> getPayloadFrames() { return this.reader.getFrameStream(); }
  public Socket getJavaSocket() { return this.javaSocket; }
}
