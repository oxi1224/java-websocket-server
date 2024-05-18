package io.github.oxi1224.websocket.messages;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
/**
 * An annotation that must be present in all valid handlers
 * @see io.github.oxi1224.websocket.messages.MessageHandler
 */
public @interface Handler {
  /**
   * The message ID the handler responds to
   */
  public String id() default DefaultHandlerID.DEFAULT;
}
