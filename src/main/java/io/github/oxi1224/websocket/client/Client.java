package io.github.oxi1224.websocket.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import io.github.oxi1224.websocket.core.DataFrame;
import io.github.oxi1224.websocket.core.DataReader;
import io.github.oxi1224.websocket.core.DataWriter;
import io.github.oxi1224.websocket.core.Opcode;
import io.github.oxi1224.websocket.core.StatusCode;
import io.github.oxi1224.websocket.shared.exceptions.UnexpectedFrameException;
import io.github.oxi1224.websocket.shared.http.HeaderMap;
import io.github.oxi1224.websocket.shared.http.HttpRequest;
import io.github.oxi1224.websocket.shared.http.HttpResponse;

public class Client extends DataWriter {
  private final Socket socket;
  private InputStream in;
  private DataReader reader;
  private Consumer<Client> onCloseCallback;
  private Consumer<Client> onPingCallback;
  private Consumer<Client> onMessageCallback;
  private Timer timer; 

  private Client(Socket socket) throws IOException, InterruptedException {
    super(socket.getOutputStream());
    HeaderMap headers = new HeaderMap(
      new HeaderMap.HeaderPair("Host", socket.getInetAddress().getHostAddress() + ":" + socket.getPort()),
      new HeaderMap.HeaderPair("Upgrade", "websocket"),
      new HeaderMap.HeaderPair("Connection", "Upgrade"),
      new HeaderMap.HeaderPair("Sec-WebSocket-Key", generateKey()),
      new HeaderMap.HeaderPair("Sec-WebSocket-Version", "13")
    );
    HttpRequest req = new HttpRequest("GET", "/", "1.1", headers, "");
    byte[] bytes = req.getBytes(); 
    socket.getOutputStream().write(bytes, 0, bytes.length);
    this.socket = socket;
    in = (socket.getInputStream());
    while (in.available() == 0) {
      Thread.sleep(100);
    }
    HttpResponse res = HttpResponse.parse(in);
    reader = new DataReader(new BufferedInputStream(in));
  }

  public static Client connect(String host, int port) throws IOException, InterruptedException {
    return new Client(new Socket(host, port)); 
  }

  public static Client connect(String host, int port, InetAddress localAddr, int localPort) throws IOException, InterruptedException {
    return new Client(new Socket(host, port, localAddr, localPort));
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
    if (opcode == Opcode.CLOSE) onReceiveClose(refFrame);
  }

  public void pingServer() throws IOException {
    write(true, Opcode.PING, new byte[0]);
    startTimeoutTimer(10000);
    try {
      reader.read();
    } catch (UnexpectedFrameException e) {
      e.printStackTrace();
      timer.cancel();
      close();
      return;
    }
    timer.cancel();
    Opcode resOpcode = reader.getStartFrame().getOpcode();
    if (resOpcode != Opcode.PONG) socket.close();
  }

  public void pong(byte[] payload) throws IOException {
    if (onPingCallback != null) onPingCallback.accept(this);
    write(true, Opcode.PONG, payload);
  }

  public void close() throws IOException {
    write(true, Opcode.CLOSE, new byte[0]);
    startTimeoutTimer(10000);
    try {
      reader.read();
    } catch (UnexpectedFrameException e) {
      e.printStackTrace();
    }
    timer.cancel();
    try {
      socket.close();
    } catch (IOException e) {}
    if (onCloseCallback != null) onCloseCallback.accept(this);
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
    } catch (UnexpectedFrameException e) {
      e.printStackTrace();
    }
    timer.cancel();
    socket.close();
    if (onCloseCallback != null) onCloseCallback.accept(this);
  }

  private void onReceiveClose(DataFrame frame) throws IOException {
    write(frame);
    socket.close();
    if (onCloseCallback != null) onCloseCallback.accept(this);
  }

  private void startTimeoutTimer(long delay) {
    timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          write(true, Opcode.CLOSE, new byte[0]);
          socket.close();
        } catch (IOException err) {
          System.out.println("Exception while closing connection");
          err.printStackTrace();
        }
      }
    }, delay); 
  }

  public void listen() {
    while (true) {
      try {
        read();
        if (onMessageCallback !=  null) onMessageCallback.accept(this);
      } catch (IOException e) {
        if (e.getMessage() != "Socket closed") e.printStackTrace();
        break;
      }
    }
  }

  public static String generateKey() {
    byte[] key = new byte[16];
    SecureRandom random = new SecureRandom();
    random.nextBytes(key);
    return Base64.getEncoder().encodeToString(key);
  }

  public void onClose(Consumer<Client> callback) { this.onCloseCallback = callback; }
  public void onPing(Consumer<Client> callback) { this.onPingCallback = callback; }
  public void onMessage(Consumer<Client> callback) { this.onMessageCallback = callback; }
  public byte[] getBytePayload() { return this.reader.getBytePayload(); }
  public String getPayload() { return this.reader.getPayload(); }
  public String getPayload(Charset chrset) { return this.reader.getPayload(chrset); }
  public DataFrame getPayloadStartFrame() { return this.reader.getStartFrame(); }
  public ArrayList<DataFrame> getPayloadFrames() { return this.reader.getFrameStream(); }
  public boolean isConnected() { return this.socket.isConnected(); }
  public Socket getSocket() { return this.socket; }
}
