package io.github.oxi1224.websocket.shared;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

class HttpParserTest {
  @Test public void checkParser() throws IOException {
    String rawHeader = "GET /chat HTTP/1.1\r\nHost: example.com:8000\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Version: 13";
    Scanner s = new Scanner(new ByteArrayInputStream(rawHeader.getBytes()), StandardCharsets.UTF_8);
    HttpParser parser = new HttpParser(s);
    assertEquals("GET", parser.method, String.format("Provided method %s expected GET", parser.method));
    assertEquals("/chat", parser.path, String.format("Expected path /chat given %s", parser.path));
    assertEquals("1.1", parser.version, String.format("Expected version 1.1 given %s", parser.version));
    String tHeader = parser.headers.get("Upgrade");
    assertEquals("websocket", tHeader, String.format("Expected Upgrade header to be websocket given %s", tHeader));
    assertEquals("", parser.body, String.format("Expected body to be empty given %s", parser.body)); 
  }
}