package io.github.oxi1224.websocket.shared.http;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

class HttpRequestTest {
  @Test public void testParsing() throws IOException {
    String rawHeader = ("GET / HTTP/1.1\r\n" +
      "Host: localhost:8080\r\n" +
      "User-Agent: whatever\r\n" +
      "Accept: text/html\r\n" +
      "Connection: keep-alive\r\n" +
      "\r\n" +
      "Hello World"
    );
    HttpRequest req = HttpRequest.parse(new ByteArrayInputStream(rawHeader.getBytes()));
    assertEquals("GET", req.getMethod(), "Wrong method provided");
    assertEquals("/", req.getPath(), "Wrong path provided");
    assertEquals("1.1", req.getVersion(), "Wrong version provided");
    String header = req.getFirstValue("Host");
    assertEquals("localhost:8080", header, "Invalid Host header provided");
    assertEquals("Hello World", req.getBody(), "Invalid body provided");
    assertEquals(rawHeader, req.toString(), "Incorrect parsing to string");
  }
}
