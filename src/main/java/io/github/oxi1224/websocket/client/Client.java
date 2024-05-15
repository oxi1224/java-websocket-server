package io.github.oxi1224.websocket.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
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
import io.github.oxi1224.websocket.shared.Handler;
import io.github.oxi1224.websocket.shared.HandlerPair;
import io.github.oxi1224.websocket.shared.MessageHandler;
import io.github.oxi1224.websocket.shared.DefaultHandlerID;
import io.github.oxi1224.websocket.shared.exceptions.InvalidConfigurationError;
import io.github.oxi1224.websocket.shared.exceptions.InvalidHandlerError;
import io.github.oxi1224.websocket.shared.exceptions.UnexpectedFrameException;
import io.github.oxi1224.websocket.shared.http.HeaderMap;
import io.github.oxi1224.websocket.shared.http.HttpRequest;
import io.github.oxi1224.websocket.shared.http.HttpResponse;
import io.github.oxi1224.websocket.shared.util.ClassScanner;

public class Client extends DataWriter {
  private final Socket socket;
  private InputStream in;
  private DataReader reader;
  private Timer timer;
  private String handlersPackageName;
  private boolean normalWebsocket = false;
  private HashMap<String, HandlerPair> handlers = new HashMap<String, HandlerPair>();

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
 
  public void useNormalWebsocket() {
    normalWebsocket = true;
  }

  public void setHandlersPacakgeName(String packageName) {
    handlersPackageName = packageName;
  }

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
    if (handlers.containsKey(DefaultHandlerID.PONG)) {
      handlers.get(DefaultHandlerID.PONG).invoke(this);
    }
  }

  public void pong(byte[] payload) throws IOException {
    write(true, Opcode.PONG, payload);
    if (handlers.containsKey(DefaultHandlerID.PING)) {
      handlers.get(DefaultHandlerID.PING).invoke(this);
    }
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
  }

  private void onReceiveClose(DataFrame frame) throws IOException {
    write(frame);
    socket.close();
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

  public static String generateKey() {
    byte[] key = new byte[16];
    SecureRandom random = new SecureRandom();
    random.nextBytes(key);
    return Base64.getEncoder().encodeToString(key);
  }

  public byte[] getBytePayload() { return this.reader.getBytePayload(); }
  public String getPayload() { return this.reader.getPayload(); }
  public String getPayload(Charset chrset) { return this.reader.getPayload(chrset); }
  public DataFrame getPayloadStartFrame() { return this.reader.getStartFrame(); }
  public ArrayList<DataFrame> getPayloadFrames() { return this.reader.getFrameStream(); }
  public boolean isConnected() { return this.socket.isConnected(); }
  public Socket getSocket() { return this.socket; }
}
