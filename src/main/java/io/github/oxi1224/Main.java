package io.github.oxi1224;

import java.io.IOException;

import io.github.oxi1224.websocket.server.*;

public class Main {
  public static void main(String[] args) throws IOException {
    WebSocketServer serv = new WebSocketServer(9001);
    serv.setHandlersPacakgeName("io.github.oxi1224");
      // serv.onMessage(client -> { 
        // try {
          // msgcallback.invoke(c.getDeclaredConstructor().newInstance(), client);
        // } catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchMethodException e) { e.printStackTrace(); }
      // });
    // }
    serv.start();
    // serv.close();

    // WebSocketServer serv = new WebSocketServer(9001);
    // serv.onMessage(client -> {
      // client.write(client.getPayload());
    // });
    // new Thread(() -> {
      // try {
        // serv.start();
      // } catch (IOException e) {}
      // catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
    // }).start();
    // Thread.sleep(2000);
    // int i = 0;
    // while (i < 1000) {
      // Thread.sleep(3);
      // new Thread(() -> {
        // try {
          // Client c = Client.connect("127.0.0.1", 9001);
          // c.pingServer();
          // Thread.sleep(randInt(200, 2500));
          // c.write(UUID.randomUUID().toString());
          // c.read();
          // Thread.sleep(randInt(200, 2500));
          // c.write(UUID.randomUUID().toString());
          // c.read();
          // Thread.sleep(randInt(200, 2500));
          // c.pingServer();
          // Thread.sleep(randInt(200, 2500));
          // c.write(UUID.randomUUID().toString());
          // c.read();
          // Thread.sleep(randInt(200, 2500));
          // c.write(UUID.randomUUID().toString());
          // c.read();
          // Thread.sleep(randInt(200, 5000));
          // c.close();
        // } catch (InterruptedException e) { e.printStackTrace(); }
        // catch (IOException e) { e.printStackTrace(); }
      // }).start();
      // System.out.print('\r');
      // System.out.printf("Starting thread %s", i + 1);
      // i++;
    // }
    // System.out.print("\n");
    // System.out.printf("All sockets connected (%s).%n", i);

    // Runtime rt = Runtime.getRuntime();
    // while (true) { 
      // Thread.sleep(500);
      // long usedMem = rt.totalMemory() - rt.freeMemory();
      // System.out.print("\033[H\033[2J");  
      // System.out.flush();
      // System.out.printf("Memory usage: %s%n", usedMem / 1000);
      // System.out.printf("Connected clients: %s%n", serv.clients.size());
      // if (serv.clients.size() == 0) break;
    // }
    // serv.close();
  }

  // private static int randInt(int min, int max) {
    // return (int)((Math.random() * (max - min)) + min);
  // }
}
