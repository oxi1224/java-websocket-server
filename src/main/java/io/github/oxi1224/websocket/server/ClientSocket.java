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
import java.util.ArrayList;
import java.util.Base64;

import io.github.oxi1224.http.HeaderMap;
import io.github.oxi1224.http.HttpRequest;
import io.github.oxi1224.http.HttpResponse;
import io.github.oxi1224.websocket.shared.*;

public class ClientSocket extends DataWriter {
  private Socket javaSocket;
  private InputStream in;
  private OutputStream out;
  private DataReader reader;
  private Timer timer;
  private Consumer<ClientSocket> onCloseCallback;
 
  public ClientSocket(Socket sock) throws IOException {
    super(sock.getOutputStream());
    javaSocket = sock;
    in = sock.getInputStream();
    out = sock.getOutputStream();
    reader = new DataReader(in);
  }

  public void sendHandshake() throws IOException, NoSuchAlgorithmException, InterruptedException {
    while (in.available() == 0) Thread.sleep(100);
    HttpRequest req = HttpRequest.parse(in);
    String key = req.getFirstValue("Sec-WebSocket-Key");
    byte[] sha1 = MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8));
    String encoded = Base64.getEncoder().encodeToString(sha1);
    HeaderMap headers = new HeaderMap(
      new HeaderMap.HeaderPair("Upgrade", "websocket"),
      new HeaderMap.HeaderPair("Connection", "Upgrade"),
      new HeaderMap.HeaderPair("Sec-WebSocket-accept", encoded)
    );
    HttpResponse res = new HttpResponse("1.1", 101, "Switching Protocols", headers, "");
    byte[] outbuf = res.getBytes();
    out.write(outbuf, 0, outbuf.length);
  }

  public void read() throws IOException {
    try {
      reader.read();
    } catch (UnexpectedFrameException e) {
      e.printStackTrace();
      close();
      return;
    }
    DataFrame refFrame = reader.getStartFrame();
    Opcode opcode = refFrame.getOpcode();
    if (opcode == Opcode.PING) pong(reader.getBytePayload());
    if (opcode == Opcode.CLOSE) closeWithoutWait();
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
    catch (UnexpectedFrameException e) { 
      e.printStackTrace();
      close();
    }
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
      if (onCloseCallback != null) onCloseCallback.accept(this);
    } catch (IOException e) {} // Ignore error, timeoutTimer closed connection while trying to read
    catch (UnexpectedFrameException e) {
      e.printStackTrace();
      timer.cancel();
      closeWithoutWait();
      return;
    }
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
      javaSocket.close();
      if (onCloseCallback != null) onCloseCallback.accept(this);
    } catch (IOException e) {} // Ignore error, timeoutTimer closed connection while trying to read
    catch (UnexpectedFrameException e) {
      e.printStackTrace();
      timer.cancel();
      closeWithoutWait();
      return;
    }
    timer.cancel();
  }

  private void closeWithoutWait() throws IOException {
    write(true, Opcode.CLOSE, new byte[0]);
    javaSocket.close();
    if (onCloseCallback != null) onCloseCallback.accept(this);
  }

  private void startTimeoutTimer(long delay) {
    timer = new Timer();
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
  public DataFrame getPayloadStartFrame() { return this.reader.getStartFrame(); }
  public ArrayList<DataFrame> getPayloadFrames() { return this.reader.getFrameStream(); }
  public Socket getJavaSocket() { return this.javaSocket; }
}
