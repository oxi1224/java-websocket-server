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
import io.github.oxi1224.websocket.json.JSONException;
import io.github.oxi1224.websocket.json.JSONObject;
import io.github.oxi1224.websocket.shared.Constants;
import io.github.oxi1224.websocket.shared.exceptions.UnexpectedFrameException;
import io.github.oxi1224.websocket.shared.exceptions.UsageError;
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
  /** Whether or not to use regular websockets (no message identification) */
  private boolean normalWebsocket = false;
  private boolean jsonProtocol = true;
  private Timer timer;
  private Consumer<ClientSocket> onCloseCallback;
 
  public ClientSocket(Socket sock) throws IOException {
    super(sock.getOutputStream());
    socket = sock;
    in = sock.getInputStream();
    out = sock.getOutputStream();
    reader = new DataReader(in);
  }
  
  /**
   * Forces the client into using regular WebSocket
   * <p>Disables JSON communication and message identification</p>
   */
  public void useNormalWebsocket() {
    normalWebsocket = true;
    jsonProtocol = false;
  }
  
  /**
   * Disables JSON communication
   * <p>Does not disable message identification</p>
   */
  public void disableJSON() {
    jsonProtocol = false;
  }
  
  /**
   * Handles the incoming handshake from a client
   */
  public void sendHandshake() throws IOException {
    // Read entire HTTP request before parsing
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
    
    // Verify that the incoming request is the standard websocket one
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

    // Verify that the server can handle incoming connection
    boolean supportsClient = true;
    List<String> protocols = req.getHeader("Sec-WebSocket-Protocol");
    String selectedProtocol = Constants.JSON_SUBPROTOCOL;
    if (!jsonProtocol) selectedProtocol = Constants.SUBPROTOCOL_NAME;
    if (normalWebsocket) selectedProtocol = "";
    if ((protocols != null && selectedProtocol == null) || !protocols.contains(selectedProtocol)) supportsClient = false;
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
      String body;
      if (!supportsClient) body = "No supported protocol found (" + selectedProtocol + ")";
      else body = "Version mismatch: expected WebSocket version 13";
      HttpResponse res = new HttpResponse("1.1", 426, "Upgrade Required", headers, body);
      httpClose(res);
      return;
    }

    // Finish the handshake
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
    if (selectedProtocol != null) {
      headers.put(new HeaderMap.HeaderPair("Sec-WebSocket-Protocol", selectedProtocol));
    }
    HttpResponse res = new HttpResponse("1.1", 101, "Switching Protocols", headers, "");
    byte[] outbuf = res.getBytes();
    out.write(outbuf, 0, outbuf.length);
  }
  
  /**
   * Utility method to properly close all underlying connections
   * @param res - The respond to send to the client
   */
  private void httpClose(HttpResponse res) throws IOException {
    byte[] outbuf = res.getBytes();
    out.write(outbuf, 0, outbuf.length);
    this.socket.close();
    if (onCloseCallback != null) onCloseCallback.accept(this);
  }

  /**
   * Reads data until it receives a frame with FIN = 1
   * Handles incoming PING and CLOSE frames
   */ 
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
  
  /**
   * Sends a ping frame to the server, waits 10s before timing out
   * and closing the connection
   */
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
  
  /**
   * Sends a pong frame to the server
   * <p>Pongs are handled automatically</p>
   */
  public void pong(byte[] pingPayload) throws IOException {
    write(true, Opcode.PONG, pingPayload);
  }
  
  /**
   * Starts the closing procedure, awaits for a response otherwise closes after 10s
   */
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
  
  /**
   * Starts the closing procedure, awaits for a response otherwise closes after 10s
   * @param statusCode - A status code from {@link StatusCode}
   * @param reason - The reason for closure
   */
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
  
  /**
   * Closes the connection without waiting for acknowledgment
   */
  private void closeWithoutWait() throws IOException {
    write(true, Opcode.CLOSE, new byte[0]);
    socket.close();
    if (onCloseCallback != null) onCloseCallback.accept(this);
  }
  
  /**
   * Starts the timeout timer which will close the connection after delay
   */
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

  public byte[] getBytePayload() {
    if (!normalWebsocket) throw new UsageError("Current server configuration does not support reading as bytes");
    return this.reader.getBytePayload();
  }

  public String getPayload() {
    if (jsonProtocol) throw new UsageError("Current server configuration does not support reading as a string");
    return this.reader.getPayload();
  }

  public String getPayload(Charset chrset) {
    if (jsonProtocol) throw new UsageError("Current server configuration does not support reading as a string");
    return this.reader.getPayload(chrset);
  }
  
  public JSONObject getFullJSONPayload() throws JSONException {
    if (!jsonProtocol) throw new UsageError("Current server configuration does not support reading as JSON");
    return this.reader.getJSONPayload();
  }

  public JSONObject getJSONPayload() throws JSONException {
    if (!jsonProtocol) throw new UsageError("Current server configuration does not support reading as JSON");
    return this.reader.getJSONPayload().get("__data", JSONObject.class);
  }
  
  public void onClose(Consumer<ClientSocket> callback) { this.onCloseCallback = callback; }
  public DataFrame getPayloadStartFrame() { return this.reader.getStartFrame(); }
  public ArrayList<DataFrame> getPayloadFrames() { return this.reader.getFrameStream(); }
  public Socket getSocket() { return this.socket; }
}
