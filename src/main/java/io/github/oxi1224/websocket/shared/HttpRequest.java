package io.github.oxi1224.websocket.shared;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HttpRequest {
  private String method;
  private String path;
  private String version;
  private HeaderMap headers;
  private String body;

  public static class HeaderMap extends LinkedHashMap<String, List<String>> {
    public HeaderMap() {
      super();
    }
  }
    
  public HttpRequest(
    String method,
    String path,
    String version,
    HeaderMap headers,
    String body
  ) {
    this.method = method;
    this.path = path;
    this.version = version;
    this.headers = headers;
    this.body = body;
  }

  public static HttpRequest parse(InputStream stream) throws IllegalArgumentException, IOException {
    byte[] buff = new byte[stream.available()];
    stream.readNBytes(buff, 0, stream.available());
    Scanner s = new Scanner(new String(buff));
    s.useDelimiter("\r\n");
    if (!s.hasNextLine()) {
      s.close();
      throw new IllegalArgumentException("Invalid http request provided");
    }
    String[] splitHeader = s.nextLine().split(" ");
    if (splitHeader.length != 3) {
      s.close();
      throw new IllegalArgumentException("Invalid http request provided");
    }
    String method = splitHeader[0].trim();
    String path = splitHeader[1].trim();
    String version = splitHeader[2].substring(splitHeader[2].indexOf('/') + 1).trim();
    HeaderMap headers = new HeaderMap();
    String line = s.nextLine(); 
    while (line.contains(":") && !line.trim().isBlank()) {
      String[] split = line.split(":", 2);
      String header = split[0].trim();
      String value = split[1].trim();
      if (headers.containsKey(header)) {
        headers.get(header).add(value);
      } else {
        ArrayList<String> set = new ArrayList<String>();
        set.add(value);
        headers.put(header, set);
      }
      if (!s.hasNextLine()) break;
      line = s.nextLine();
    }
    String body = "";
    if (s.hasNextLine()) {
      body = s.nextLine();
      while (s.hasNextLine()) {
        body += line;
        line = s.nextLine();
      }
    }
    s.close();
    return new HttpRequest(method, path, version, headers, body);
  }

  public String toString() {
    String out = "";
    out += String.format("%s %s HTTP/%s\r\n", method, path, version);
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      out += entry.getKey() + ": ";
      for (Iterator<String> i = entry.getValue().iterator(); i.hasNext();) {
        out += i.next();
        if (i.hasNext()) out += ";";
      }
      out += "\r\n";
    }
    out += "\r\n";
    if (!body.isBlank()) out += body;
    return out;
  }

  public byte[] getBytes() {
    return toString().getBytes();
  }

  public List<String> getHeader(String key) { return headers.get(key); }
  public String getFirstHeader(String key) { return headers.get(key).iterator().next(); }
  public String getMethod() { return method; }
  public String getPath() { return path; }
  public String getVersion() { return version; }
  public HeaderMap getHeaderMap() { return headers; }
  public String getBody() { return body; }
}
