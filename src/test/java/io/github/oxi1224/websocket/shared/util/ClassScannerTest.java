package io.github.oxi1224.websocket.shared.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.oxi1224.websocket.shared.Handler;

class ClassScannerTest {
  @Test
  public void testScanning() {
    List<Class<?>> found = ClassScanner.findAllWithAnnotation(Handler.class, "io.github.oxi1224");
    // assertEquals("io.github.oxi1224.Main", found.get(0).getName(), "Invalid name received");
  }
}
