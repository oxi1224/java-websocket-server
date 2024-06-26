package io.github.oxi1224.websocket.messages;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.github.oxi1224.websocket.shared.util.Pair;

/**
 * Utility class for easy invoking of Method
 */
public class HandlerPair extends Pair<Object, Method> {
  public HandlerPair(Object object, Method method) {
    super(object, method);
  }

  public <T> void invoke(T data) {
    try {
      getValue().invoke(getKey(), data);
    } catch (InvocationTargetException | IllegalAccessException e) {
      System.out.println("Encountered exception when invoking message handler");
      e.printStackTrace();
    }
  }
}
