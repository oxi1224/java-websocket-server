package io.github.oxi1224.websocket.shared.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HttpResponse {
  private String version;
  private int statusCode;
  private String statusMessage;
  private HeaderMap headers;
  private String body;

  public HttpResponse(
    String version,
    int statusCode,
    String statusMessage,
    HeaderMap headers,
    String body
  ) {
    this.version = version;
    this.statusCode = statusCode;
    this.statusMessage = statusMessage;
    this.headers = headers;
    this.body = body;
    if (!this.body.isBlank() && !this.headers.containsKey("Content-Length")) {
      this.headers.put(new HeaderMap.HeaderPair("Content-Length", Integer.toString(body.length())));
    }
  }
  
  /**
   * Reads an HTTP response from an {@link java.io.InputStream}
   * Note: The entire data first has to be in the stream
   * @return the parsed http response 
   */
  public static HttpResponse parse(InputStream stream) throws IllegalArgumentException, IOException {
    byte[] buff = new byte[stream.available()];
    stream.readNBytes(buff, 0, stream.available());
    Scanner s = new Scanner(new String(buff));

    s.useDelimiter("\r\n");
    if (!s.hasNextLine()) {
      s.close();
      throw new IllegalArgumentException("Invalid http response provided");
    }
    String[] splitHeader = s.nextLine().split(" ", 3);
    String version = splitHeader[0].substring(splitHeader[0].indexOf('/') + 1).trim();
    int statusCode = Integer.parseInt(splitHeader[1]);
    String statusMessage = splitHeader[2];
    if (version.isBlank() || statusCode < 100 || statusCode > 599) {
      s.close();
      throw new IllegalArgumentException("Invalid http response provided");
    }
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
    return new HttpResponse(version, statusCode, statusMessage, headers, body);
  }
  
  /**
   * Converts the class into a valid string representation
   * @return the converted string
   */
  public String toString() {
    String out = "";
    out += String.format("HTTP/%s %s %s\r\n", version, statusCode, statusMessage);
    for (Map.Entry<String, ArrayList<String>> entry : headers.entrySet()) {
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
  
  /**
   * Converts the class into a byte[]
   * @return the converted data
   */
  public byte[] getBytes() {
    return toString().getBytes();
  }

  public List<String> getHeader(String key) { return headers.get(key); }
  /**
   * Gets the first value of the specified header
   * @param key - the header name
   * @return the first value of header
   */
  public String getFirstHeaderValue(String key) { return headers.getFirstValue(key); }
  public String getVersion() { return version; }
  public int getStatusCode() { return statusCode; }
  public String getStatusMessage() { return statusMessage; }
  public String getBody() { return body; }
}
