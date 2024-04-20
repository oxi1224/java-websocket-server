package io.github.oxi1224;

import java.net.*;
import java.io.*;

public class TCPServer {
  private ServerSocket server;
  private Socket client;
  private PrintWriter out;
  private BufferedReader in;

  public void start() {
    while (true) { 
      try {
        server = new ServerSocket(4000);
        client = server.accept();
        out = new PrintWriter(client.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
      } catch (IOException err) {
        System.out.println("Error happened");
      }
    
      try {
        String inputLine;
        
        while ((inputLine = in.readLine()) != null) {
          System.out.println(inputLine);
        }
        client.close();
      } catch (IOException err) { 
        System.out.println("Error happened");
      }
    }
  }
}
