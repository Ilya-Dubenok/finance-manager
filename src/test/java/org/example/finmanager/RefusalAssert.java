package org.example.finmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;

/** Проверки отказа сервиса: код причины и, при необходимости, аргументы сообщения. */
@UtilityClass
public class RefusalAssert {

  /**
   * Проверяет, что вызов отказал с заданным кодом.
   *
   * @param call проверяемый вызов
   * @param expected ожидаемый код
   */
  public void assertRefusal(ThrowingCallable call, ErrorCode expected) {
    assertThatThrownBy(call)
        .isInstanceOfSatisfying(
            ApplicationException.class, e -> assertThat(e.getCode()).isEqualTo(expected));
  }

  /**
   * Проверяет, что вызов отказал с заданным кодом и аргументами сообщения.
   *
   * @param call проверяемый вызов
   * @param expected ожидаемый код
   * @param args ожидаемые аргументы сообщения по порядку
   */
  public void assertRefusalWithArgs(ThrowingCallable call, ErrorCode expected, Object... args) {
    assertThatThrownBy(call)
        .isInstanceOfSatisfying(
            ApplicationException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(expected);
              assertThat(e.getArgs()).containsExactly(args);
            });
  }
}
