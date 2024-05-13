package io.github.oxi1224.websocket.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class ClassScannerTest {
  @Test
  public void testScanning() {
    List<Class<?>> found = ClassScanner.findAllWithAnnotation(MessageHandler.class, "io.github.oxi1224");
    assertEquals("io.github.oxi1224.Main", found.get(0).getName(), "Invalid name received");
  }
}
