package io.github.oxi1224.websocket.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.function.Consumer;

import io.github.oxi1224.websocket.shared.*;

public class Client extends DataWriter {
  private final Socket socket;
  private InputStream in;
  private DataReader reader;
  private boolean stopListening = false;
  private Consumer<Client> onCloseCallback;
  private Consumer<Client> onPingCallback;
  private Consumer<Client> onMessageCallback;

  private Client(Socket socket) throws IOException {
    super(socket.getOutputStream());
    this.socket = socket;
    in = socket.getInputStream();
    reader = new DataReader(in);
  }

  public static Client connect(String host, int port) throws IOException {
    return new Client(new Socket(host, port)); 
  }

  public static Client connect(String host, int port, InetAddress localAddr, int localPort) throws IOException {
    return new Client(new Socket(host, port, localAddr, localPort));
  }

  public DataReader read() throws IOException {
    reader.read();
    DataFrame refFrame = reader.getStartFrame();
    Opcode opcode = refFrame.getOpcode();
    if (opcode == Opcode.PING) sendPong(reader.getBytePayload());
    if (opcode == Opcode.CLOSE) onReceiveClose(refFrame);
    return reader;
  }

  public void pingServer() throws IOException {
    write(true, Opcode.PING, new byte[0]);
    reader.read();
    Opcode resOpcode = reader.getStartFrame().getOpcode();
    if (resOpcode != Opcode.PONG) socket.close();
  }

  public void sendPong(byte[] payload) throws IOException {
    if (onPingCallback != null) onPingCallback.accept(this);
    write(true, Opcode.PONG, payload);
  }

  public void close() throws IOException {
    write(true, Opcode.CLOSE, new byte[0]);
    // TODO: timeout after x seconds
    reader.read();
    socket.close();
    if (onCloseCallback != null) onCloseCallback.accept(this);
  }

  public void close(StatusCode statusCode, String reason) throws IOException {
    byte[] stringBytes = reason.getBytes();
    byte[] codeBytes = statusCode.getBytes();
    byte[] payload = new byte[2 + stringBytes.length];
    System.arraycopy(codeBytes, 0, payload, 0, 2);
    System.arraycopy(stringBytes, 0, payload, 2, stringBytes.length);
    write(true, Opcode.CLOSE, payload);
    // TODO: timeout after x seconds
    reader.read();
    socket.close();
    if (onCloseCallback != null) onCloseCallback.accept(this);
  }

  private void onReceiveClose(DataFrame frame) throws IOException {
    write(frame);
    socket.close();
    if (onCloseCallback != null) onCloseCallback.accept(this);
  }


  public void listen() throws IOException {
    while (!stopListening) {
      reader.read();
      if (onMessageCallback !=  null) onMessageCallback.accept(this);
    }
    stopListening = false;
  }

  public void stopListening() {
    stopListening = true;
  }

  public void onClose(Consumer<Client> callback) { this.onCloseCallback = callback; }
  public void onPing(Consumer<Client> callback) { this.onPingCallback = callback; }
  public void onMessage(Consumer<Client> callback) { this.onMessageCallback = callback; }
  public byte[] getBytePayload() { return this.reader.getBytePayload(); }
  public String getPayload() { return this.reader.getPayload(); }
  public String getPayload(Charset chrset) { return this.reader.getPayload(chrset); }
  public DataFrame getPaylodStartFrame() { return this.reader.getStartFrame(); }
  public HashSet<DataFrame> getPayloadFrames() { return this.reader.getFrameStream(); }
  public Socket getSocket() { return this.socket; }
}
