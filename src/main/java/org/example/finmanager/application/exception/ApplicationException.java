package org.example.finmanager.application.exception;

import java.util.List;
import lombok.Getter;

/** Отказ выполнить команду: код причины и значения для подстановки в сообщение. */
@Getter
public class ApplicationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final ErrorCode code;
  private final transient List<Object> args;

  /**
   * Создаёт исключение.
   *
   * @param code причина отказа
   * @param args значения для подстановки в сообщение каталога
   */
  public ApplicationException(ErrorCode code, Object... args) {
    super(code.name());
    this.code = code;
    this.args = List.of(args);
  }
}
