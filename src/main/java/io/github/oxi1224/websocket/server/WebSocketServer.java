package io.github.oxi1224.websocket.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.oxi1224.websocket.core.DataFrame;
import io.github.oxi1224.websocket.core.Opcode;
import io.github.oxi1224.websocket.messages.Handler;
import io.github.oxi1224.websocket.messages.HandlerPair;
import io.github.oxi1224.websocket.messages.MessageHandler;
import io.github.oxi1224.websocket.messages.DefaultHandlerID;
import io.github.oxi1224.websocket.shared.exceptions.InvalidConfigurationError;
import io.github.oxi1224.websocket.shared.exceptions.InvalidHandlerError;
import io.github.oxi1224.websocket.shared.util.ClassScanner;

public class WebSocketServer extends java.net.ServerSocket {
  public Map<ClientSocket, Thread> clients = new HashMap<>();
  public Map<String, HandlerPair> handlers = new HashMap<String, HandlerPair>();
  private String handlersPackageName;
  private boolean normalWebsocket = false;

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

  public void useNormalWebsocket() {
    normalWebsocket = true;
  }
 
  public void start() throws IOException {
    if (handlersPackageName == null || handlersPackageName.isBlank()) {
      throw new InvalidConfigurationError("handlersPackageName is blank, set it via setHandlersPackageName");
    }
    collectHandlers(); 

    while (true) {
      ClientSocket client = new ClientSocket(this.accept());
      HandlerPair closeHandler = handlers.get(DefaultHandlerID.CLOSE);
      if (closeHandler != null) client.onClose((c) -> closeHandler.invoke(c));
      client.sendHandshake();
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
          switch (opcode) {
            case PING: {
              HandlerPair p = handlers.get(DefaultHandlerID.PING);
              if (p != null) p.invoke(client);
              break;
            }
            case CLOSE: {
              HandlerPair p = handlers.get(DefaultHandlerID.CLOSE);
              if (p != null) p.invoke(client);
              break;
            }
            case PONG: {
              break;
            }
            default: {
              if (normalWebsocket || refFrame.getRsv1()) {
                HandlerPair p = handlers.get(DefaultHandlerID.DEFAULT);
                if (p != null) p.invoke(client);
              } else {
                String payload = client.getPayload();
                String messageID = payload.substring(0, payload.indexOf(" "));
                HandlerPair p = handlers.get(handlers.containsKey(messageID) ? messageID : DefaultHandlerID.DEFAULT);
                if (p != null) p.invoke(client);
              }
              break;
            }
          }
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

  private void cleanupSocket(ClientSocket socket) {
    synchronized(clients) {
      Thread t = clients.remove(socket);
      if (t != null) t.interrupt();
    }
  }
}
