package org.example.finmanager.application.exception;

import java.nio.file.Path;
import lombok.Getter;

/** Файл хранилища прочитан, но его содержимое не соответствует формату. */
@Getter
public class StorageFormatException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient Path file;

  /**
   * Создаёт исключение.
   *
   * @param file файл с ошибкой
   * @param reason что именно не так, техническим языком
   */
  public StorageFormatException(Path file, String reason) {
    super(reason);
    this.file = file;
  }
}
