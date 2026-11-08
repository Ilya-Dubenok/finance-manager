package org.example.finmanager.application.exception;

import java.nio.file.Path;
import lombok.Getter;

/** Файл хранилища не удалось прочитать или записать. */
@Getter
public class StorageIoException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient Path file;

  /**
   * Создаёт исключение.
   *
   * @param file файл, с которым не удалось работать
   * @param reason причина, техническим языком
   */
  public StorageIoException(Path file, String reason) {
    super(reason);
    this.file = file;
  }
}
