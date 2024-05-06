package io.github.oxi1224.websocket.shared;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

class HttpResponseTest {
  @Test public void testParsing() throws IOException {
    String rawHeader = ("HTTP/1.1 200 OK\r\n" +
      "Host: localhost:8080\r\n" +
      "User-Agent: whatever\r\n" +
      "Accept: text/html\r\n" +
      "Connection: keep-alive\r\n" +
      "\r\n" +
      "Hello World"
    );
    HttpResponse req = HttpResponse.parse(new ByteArrayInputStream(rawHeader.getBytes()));
    assertEquals("1.1", req.getVersion(), "Wrong version provided");
    assertEquals(200, req.getStatusCode(), "Wrong status code provided");
    assertEquals("OK", req.getStatusMessage(), "Wrong status message provided");
    String header = req.getFirstHeader("Host");
    assertEquals("localhost:8080", header, "Invalid Host header provided");
    assertEquals("Hello World", req.getBody(), "Invalid body provided");
    assertEquals(rawHeader, req.toString(), "Incorrect parsing to string");
  }
}
