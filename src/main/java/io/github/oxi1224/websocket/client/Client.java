package io.github.oxi1224.websocket.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.github.oxi1224.websocket.core.DataFrame;
import io.github.oxi1224.websocket.core.DataReader;
import io.github.oxi1224.websocket.core.DataWriter;
import io.github.oxi1224.websocket.core.Opcode;
import io.github.oxi1224.websocket.core.StatusCode;
import io.github.oxi1224.websocket.messages.Handler;
import io.github.oxi1224.websocket.messages.HandlerPair;
import io.github.oxi1224.websocket.messages.MessageHandler;
import io.github.oxi1224.websocket.messages.DefaultHandlerID;
import io.github.oxi1224.websocket.shared.Constants;
import io.github.oxi1224.websocket.shared.exceptions.ConnectionException;
import io.github.oxi1224.websocket.shared.exceptions.InvalidConfigurationError;
import io.github.oxi1224.websocket.shared.exceptions.InvalidHandlerError;
import io.github.oxi1224.websocket.shared.exceptions.UnexpectedFrameException;
import io.github.oxi1224.websocket.shared.http.HeaderMap;
import io.github.oxi1224.websocket.shared.http.HttpRequest;
import io.github.oxi1224.websocket.shared.http.HttpResponse;
import io.github.oxi1224.websocket.shared.util.ClassScanner;

public class Client extends DataWriter {
  /** Whether or not to use regular websockets (no message identification) */
  public static boolean normalWebsocket = false;
  private final Socket socket;
  private final InputStream in;
  private final DataReader reader;
  private Timer timer;
  private String handlersPackageName;
  private HashMap<String, HandlerPair> handlers = new HashMap<String, HandlerPair>();
  
  /**
   * Automatically performs the websocket handshake
   * @param socket - java socket, required for IO streams
   * @param origin - origin to use in the websocket handshake
   * @exception ConnectionException if receives a non 101 response or invalid Sec-WebSocket-Accept
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/WebSocket">WebSocket - MDN web docs</a>
   */
  private Client(Socket socket, String origin) throws IOException, ConnectionException {
    super(socket.getOutputStream());
    setMasking(true);
    String websocketKey = generateKey();
    HeaderMap headers = new HeaderMap(
      new HeaderMap.HeaderPair("Host", socket.getInetAddress().getHostAddress() + ":" + socket.getPort()),
      new HeaderMap.HeaderPair("Origin", origin),
      new HeaderMap.HeaderPair("Upgrade", "websocket"),
      new HeaderMap.HeaderPair("Connection", "Upgrade"),
      new HeaderMap.HeaderPair("Sec-WebSocket-Key", websocketKey),
      new HeaderMap.HeaderPair("Sec-WebSocket-Version", Integer.toString(Constants.WS_VERSION))
    );
    if (!normalWebsocket) headers.put(new HeaderMap.HeaderPair("Sec-WebSocket-Protocol", Constants.SUBPROTOCOL_NAME));
    HttpRequest req = new HttpRequest("GET", "/", "1.1", headers, "");
    byte[] bytes = req.getBytes(); 
    socket.getOutputStream().write(bytes, 0, bytes.length);
    this.socket = socket;
    in = (socket.getInputStream());
    while (in.available() == 0) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        System.out.println("Client thread had been interrupted");
        e.printStackTrace();
        socket.close();
        System.exit(1);
      }
    }
    HttpResponse res = HttpResponse.parse(in);
    if (res.getStatusCode() != 101) {
      System.out.println("Failed to connect to the specified host");
      System.out.println(String.format("Received code %s with message %s", res.getStatusCode(), res.getStatusMessage()));
      if (!res.getBody().isBlank()) {
        System.out.println("Response body:");
        System.out.println(res.getBody());
      }
      throw new ConnectionException("Failed to connect to the specified host");
    }

    String acceptKey = res.getFirstHeaderValue("Sec-WebSocket-Accept");
    byte[] sha1 = new byte[0];
    try {
      sha1 = MessageDigest.getInstance("SHA-1")
        .digest((websocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
        .getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {}
    String encodedKey = Base64.getEncoder().encodeToString(sha1);

    if (!encodedKey.equals(acceptKey)) {
      socket.close();
      throw new ConnectionException("The server has provided an invalid Sec-WebSocket-Accept key");
    }
    reader = new DataReader(new BufferedInputStream(in));
  }
  
  /**
   * @param url - any valid string that can be converted to a {@link java.net.URI}
   * @return a connected Client class
   */
  public static Client connect(String url) throws IOException, URISyntaxException, ConnectionException {
    URI uri = new URI(url);
    String host = uri.getHost();
    int port = uri.getPort() == -1 ? 80 : uri.getPort();
    return new Client(new Socket(host, port), "http://" + url);
  }
  
  /**
   * @param host - IP address poiting to the host
   * @param port - port of the server
   * @return a connected Client class
   */
  public static Client connect(String host, int port) throws IOException, ConnectionException {
    return new Client(new Socket(host, port), "http://" + host); 
  }
  
  public static Client connect(
    String host,
    int port,
    InetAddress localAddr,
    int localPort
  ) throws IOException, ConnectionException {
    return new Client(new Socket(host, port, localAddr, localPort), "http://" + host);
  }

  /**
   * Forces the client into using standard WebSocket (no message identification)
   */
  public static void useNormalWebsocket() {
    normalWebsocket = true;
  }
  
  /**
   * Sets the package name where all message handlers are located
   * The handlers must have an @Handler annotation and must extend MessageHandler
   * @see io.github.oxi1224.websocket.messages.MessageHandler
   * @see io.github.oxi1224.websocket.messages.handler
   */
  public void setHandlersPackageName(String packageName) {
    handlersPackageName = packageName;
  }
  
  /**
   * Collects all handlers extending MessageHandler with @Handler annotation
   */
  private void collectHandlers() {
    List<Class<?>> found = ClassScanner.findAllWithAnnotation(Handler.class, handlersPackageName);
    for (Class<?> c : found) {
      if (!MessageHandler.class.isAssignableFrom(c)) throw new InvalidHandlerError(
        String.format("Class %s uses @Handler annotation but does not implement MessageHandler interface", c.getName())
      );
      String msgID = c.getAnnotation(Handler.class).id();
      if (handlers.containsKey(msgID)) throw new InvalidHandlerError("Duplicate handler ID " + msgID);
      Method method;
      try {
        method = c.getMethod("onMessage", this.getClass());
      } catch (NoSuchMethodException e) {
        throw new InvalidHandlerError("Failed to find onMessage method on class %s" + c.getName());
      }
      try {
        Object classInstance = c.getDeclaredConstructor().newInstance();
        handlers.put(msgID, new HandlerPair(classInstance, method));
      } catch (
        InvocationTargetException | IllegalAccessException |
        InstantiationException | NoSuchMethodException e
      ) {
        System.out.println("Exception when instantiating handler " + c.getName());
        e.printStackTrace();
      }
    }
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
    if (opcode == Opcode.CLOSE) onReceiveClose(refFrame);
  }
  
  /**
   * Sends a ping frame to the server, waits 10s before timing out
   * and closing the connection
   */
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
    if (handlers.containsKey(DefaultHandlerID.PONG)) {
      handlers.get(DefaultHandlerID.PONG).invoke(this);
    }
  }
  
  /**
   * Pongs are handled automatically
   *
   * Sends a pong frame to the server
   */
  public void pong(byte[] payload) throws IOException {
    write(true, Opcode.PONG, payload);
    if (handlers.containsKey(DefaultHandlerID.PING)) {
      handlers.get(DefaultHandlerID.PING).invoke(this);
    }
  }

  /**
   * Starts the closing procedure, awaits for a response otherwise closes after 10s
   */
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
  }
  
  /**
   * Starts the closing procedure, awaits for a response otherwise closes after 10s
   * @param statusCode - A status code from {@link io.github.oxi1224.websocket.core.StatusCode}
   * @param reason - The reason for closure
   */
  public void close(StatusCode statusCode, String reason) throws IOException {
    // Included in the RFC, 123 because statusCode always takes 2 bytes
    if (reason.length() > 123) throw new IOException("Payload length may not be over 125 in a control frame");
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
  }

  private void onReceiveClose(DataFrame frame) throws IOException {
    write(frame);
    socket.close();
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
          write(true, Opcode.CLOSE, new byte[0]);
          socket.close();
        } catch (IOException err) {
          System.out.println("Exception while closing connection");
          err.printStackTrace();
        }
      }
    }, delay); 
  }
  
  /**
   * Calls collectHandlers() and starts the main loop
   */
  public void listen() {
    if (handlersPackageName == null || handlersPackageName.isBlank()) {
      throw new InvalidConfigurationError("handlersPackageName is blank, set it via setHandlersPackageName");
    }
    collectHandlers();

    while (true) {
      try {
        read();
        if (normalWebsocket || !getPayloadStartFrame().getRsv1()) {
          HandlerPair p = handlers.get(DefaultHandlerID.DEFAULT);
          if (p != null) p.invoke(this);
        } else {
          String payload = getPayload();
          String messageID = payload.substring(0, payload.indexOf(" "));
          HandlerPair p = handlers.get(handlers.containsKey(messageID) ? messageID : DefaultHandlerID.DEFAULT);
          if (p != null) p.invoke(this);
        }
      } catch (IOException e) {
        if (e.getMessage() != "Socket closed") e.printStackTrace();
        break;
      }
    }
  }
  
  /**
   * Generates a random Sec-WebSocket-Key 
   * @return the generated key
   */
  public static String generateKey() {
    byte[] key = new byte[16];
    SecureRandom random = new SecureRandom();
    random.nextBytes(key);
    return Base64.getEncoder().encodeToString(key);
  }

  public byte[] getBytePayload() { return this.reader.getBytePayload(); }
  public String getPayload() { return this.reader.getPayload(); }
  public String getPayload(Charset chrset) { return this.reader.getPayload(chrset); }
  /**
   * @return the first frame of entire payload
   */
  public DataFrame getPayloadStartFrame() { return this.reader.getStartFrame(); }
  public ArrayList<DataFrame> getPayloadFrames() { return this.reader.getFrameStream(); }
  public boolean isConnected() { return this.socket.isConnected(); }
  public Socket getSocket() { return this.socket; }
}
