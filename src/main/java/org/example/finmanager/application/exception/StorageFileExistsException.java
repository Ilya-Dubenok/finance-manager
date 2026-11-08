package org.example.finmanager.application.exception;

import java.nio.file.Path;
import lombok.Getter;

/** Файл уже существует, а заменять его не разрешено. */
@Getter
public class StorageFileExistsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient Path file;

  /**
   * Создаёт исключение.
   *
   * @param file существующий файл
   */
  public StorageFileExistsException(Path file) {
    super(file.toString());
    this.file = file;
  }
}
