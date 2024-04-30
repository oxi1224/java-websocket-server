package io.github.oxi1224;

import java.util.HashMap;
import java.util.Scanner;

public class HttpParser {
  public String method;
  public String path;
  public String version;
  public String body = "";
  public HashMap<String, String> headers = new HashMap<String, String>();

  public HttpParser(Scanner s) {
    s.useDelimiter("\r\n");
    String[] splitHeader = s.nextLine().split(" ");
    if (splitHeader.length != 3) throw new Error("Given buffer is not a valid http header");
    method = splitHeader[0];
    path = splitHeader[1];
    version = splitHeader[2].substring(splitHeader[2].indexOf('/') + 1).trim();
    String line = s.nextLine(); 
    while (line != null && line.contains(":")) {
      String[] split = line.split(":", 2);
      if (split.length != 2) throw new Error("Tried splitting line by : but length != 2");
      headers.put(split[0].trim(), split[1].trim());
      if (!s.hasNextLine()) break;
      line = s.nextLine();
    }
    // while (line == "\r\n") line = r.readLine();
    // String rest = "";
    // while (line != null) {
      // rest += line;
      // line = r.readLine();
    // }
    // body = rest;
  }
}
