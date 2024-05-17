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

import io.github.oxi1224.websocket.core.DataFrame;
import io.github.oxi1224.websocket.core.DataReader;
import io.github.oxi1224.websocket.core.DataWriter;
import io.github.oxi1224.websocket.core.Opcode;
import io.github.oxi1224.websocket.core.StatusCode;
import io.github.oxi1224.websocket.shared.Constants;
import io.github.oxi1224.websocket.shared.exceptions.UnexpectedFrameException;
import io.github.oxi1224.websocket.shared.http.HeaderMap;
import io.github.oxi1224.websocket.shared.http.HttpRequest;
import io.github.oxi1224.websocket.shared.http.HttpResponse;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ClientSocket extends DataWriter {
  private final Socket socket;
  private final InputStream in;
  private final OutputStream out;
  private final DataReader reader;
  private boolean normalWebsocket = true;
  private Timer timer;
  private Consumer<ClientSocket> onCloseCallback;
 
  public ClientSocket(Socket sock) throws IOException {
    super(sock.getOutputStream());
    socket = sock;
    in = sock.getInputStream();
    out = sock.getOutputStream();
    reader = new DataReader(in);
  }

  public void useNormalWebsocket() {
    normalWebsocket = true;
  }

  public void sendHandshake() throws IOException {
     while (in.available() == 0) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        System.out.println("ClientSocket thread had been interrupted during handshake");
        e.printStackTrace();
        socket.close();
        System.exit(1);
      }
    }
    HttpRequest req = HttpRequest.parse(in);
    HeaderMap headers = new HeaderMap();
    if (
      !req.getMethod().equals("GET") ||
      !req.getFirstHeaderValue("Upgrade").equals("websocket") ||
      req.getFirstHeaderValue("Sec-WebSocket-Version") == null
    ) {
      headers.put(new HeaderMap.HeaderPair("Content-Type", "text/plain"));
      HttpResponse res = new HttpResponse("1.1", 400, "Bad Request", headers, "Expected WebSocket upgrade headers");
      httpClose(res);
      return;
    }
    boolean supportsClient = true;
    List<String> protocols = req.getHeader("Sec-WebSocket-Protocol");
    if (!normalWebsocket && (protocols == null || !protocols.contains(Constants.SUBPROTOCOL_NAME))) supportsClient = false;
    if (
      !req.getFirstHeaderValue("Sec-WebSocket-Version").equals(Constants.WS_VERSION_STR) ||
      !supportsClient
    ) {
      headers.put(
        new HeaderMap.HeaderPair("Upgrade", "websocket"),
        new HeaderMap.HeaderPair("Sec-WebSocket-Protocol", Constants.SUBPROTOCOL_NAME),
        new HeaderMap.HeaderPair("Sec-WebSocket-Version", Constants.WS_VERSION_STR),
        new HeaderMap.HeaderPair("Content-Type", "text/plain")
      );
      HttpResponse res = new HttpResponse("1.1", 426, "Upgrade Required", headers, "Expected WebSocket version 13");
      httpClose(res);
      return;
    }
    String key = req.getFirstHeaderValue("Sec-WebSocket-Key");
    byte[] sha1 = new byte[0];
    try {
      sha1 = MessageDigest.getInstance("SHA-1")
        .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
        .getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {}
    String encoded = Base64.getEncoder().encodeToString(sha1);
    headers.put(
      new HeaderMap.HeaderPair("Upgrade", "websocket"),
      new HeaderMap.HeaderPair("Connection", "Upgrade"),
      new HeaderMap.HeaderPair("Sec-WebSocket-Accept", encoded)
    );
    if (!normalWebsocket) headers.put(new HeaderMap.HeaderPair("Sec-WebSocket-Protocol", Constants.SUBPROTOCOL_NAME));
    HttpResponse res = new HttpResponse("1.1", 101, "Switching Protocols", headers, "");
    byte[] outbuf = res.getBytes();
    out.write(outbuf, 0, outbuf.length);
  }

  private void httpClose(HttpResponse res) throws IOException {
    byte[] outbuf = res.getBytes();
    out.write(outbuf, 0, outbuf.length);
    this.socket.close();
    if (onCloseCallback != null) onCloseCallback.accept(this);
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
      if (resOpcode != Opcode.PONG) socket.close();
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
      socket.close();
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

  public void close(StatusCode statusCode, String reason) throws IOException {
    byte[] stringBytes = reason.getBytes();
    byte[] codeBytes = statusCode.getBytes();
    byte[] payload = new byte[2 + stringBytes.length];
    System.arraycopy(codeBytes, 0, payload, 0, 2);
    System.arraycopy(stringBytes, 0, payload, 2, stringBytes.length);
    write(true, Opcode.CLOSE, payload);
    startTimeoutTimer(10000);
    try {
      reader.read();
      socket.close();
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
    socket.close();
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
  public Socket getSocket() { return this.socket; }
}
