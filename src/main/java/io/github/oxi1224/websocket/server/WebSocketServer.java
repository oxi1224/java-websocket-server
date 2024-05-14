package io.github.oxi1224.websocket.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.oxi1224.websocket.core.DataFrame;
import io.github.oxi1224.websocket.core.Opcode;
import io.github.oxi1224.websocket.shared.Handler;
import io.github.oxi1224.websocket.shared.exceptions.InvalidHandlerError;
import io.github.oxi1224.websocket.shared.util.ClassScanner;
import io.github.oxi1224.websocket.shared.util.Pair;

public class WebSocketServer extends java.net.ServerSocket {
  public Map<ClientSocket, Thread> clients = new HashMap<>();
  public Map<String, Pair<Object, Method>> handlers = new HashMap<>();
  private String handlersPackageName;
  private ClientCallback onMessageCallback = (c) -> { assert true; }; // Do nothing
  private ClientCallbackWithFrame onPingCallback = (f, c) -> { assert true; }; // Do nothing
  private ClientCallback onCloseCallback = (c) -> { assert true; }; // Do nothing

  public interface ClientCallback {
    void accept(ClientSocket client) throws IOException;
  }

  public interface ClientCallbackWithFrame {
    void accept(DataFrame frame, ClientSocket client) throws IOException;
  }

  public WebSocketServer(int port) throws IOException {
    super(port);
  }

  public WebSocketServer(int port, int backlog) throws IOException {
    super(port, backlog);
  }

  public WebSocketServer(int port, int backlog, InetAddress bindAddr) throws IOException {
    super(port, backlog, bindAddr);
  }

  public void setHandlersPacakgeName(String name) {
    handlersPackageName = name;
  }
  
  public void start() throws IOException, NoSuchAlgorithmException {
    // collectHandlers();
    while (true) {
      ClientSocket client = new ClientSocket(this.accept());
      try {
        client.sendHandshake();
      } catch (InterruptedException e) {
        client.close();
      }
      client.onClose((c) -> cleanupSocket(c));
      createClientThread(client);
    }
  }

  private void createClientThread(ClientSocket client) {
    Thread clientThread = new Thread(() -> {
      while (!Thread.interrupted()) {
        try {
          client.read();
          DataFrame refFrame = client.getPayloadStartFrame();
          Opcode opcode = refFrame.getOpcode();
          if (opcode == Opcode.PING) {
            onPingCallback.accept(refFrame, client);
          } else if (opcode == Opcode.CLOSE) {
            onCloseCallback.accept(client);
          } else if (opcode == Opcode.PONG) { 
            continue;
          } else if (onMessageCallback != null) onMessageCallback.accept(client); 
        } catch (IOException e) {
          if (e.getMessage() != "Socket closed") e.printStackTrace();
          break;
        }
      }
    });
    clients.put(client, clientThread);
    clientThread.start();
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
        method = c.getMethod("onMessage", ClientSocket.class);
      } catch (NoSuchMethodException e) {
        throw new InvalidHandlerError("Failed to find onMessage method on class %s" + c.getName());
      }
      try {
        Object classInstance = c.getDeclaredConstructor().newInstance();
        handlers.put(msgID, new Pair<>(classInstance, method));
      } catch (
        InvocationTargetException | IllegalAccessException |
        InstantiationException | NoSuchMethodException e
      ) {
        System.out.println("Exception when instantiating handler " + c.getName());
        e.printStackTrace();
      }
    }
  }

  private void cleanupSocket(ClientSocket socket) {
    synchronized(clients) {
      Thread t = clients.remove(socket);
      if (t != null) t.interrupt();
    }
  }

  public void onMessage(ClientCallback callback) {
    onMessageCallback = callback;
  }

  public void onPing(ClientCallbackWithFrame callback) {
    onPingCallback = callback; 
  }

  public void onClose(ClientCallback callback) {
    onCloseCallback = callback;
  }
}
