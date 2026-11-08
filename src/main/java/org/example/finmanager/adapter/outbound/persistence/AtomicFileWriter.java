package org.example.finmanager.adapter.outbound.persistence;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Component;

/**
 * Атомарная запись файла: содержимое пишется во временный файл рядом, затем файл перемещается на
 * место с заменой. Если запись не удалась, прежний файл остаётся нетронутым.
 */
@Component
public class AtomicFileWriter {

  private static final String TEMP_PREFIX = "write-";
  private static final String TEMP_SUFFIX = ".tmp";

  /**
   * Записывает файл целиком.
   *
   * @param target итоговый файл; недостающие каталоги создаются
   * @param content содержимое
   * @throws IOException если запись или перемещение не удались
   */
  public void write(Path target, byte[] content) throws IOException {
    Path directory = target.toAbsolutePath().getParent();
    Files.createDirectories(directory);
    Path temp = Files.createTempFile(directory, TEMP_PREFIX, TEMP_SUFFIX);
    try {
      Files.write(temp, content);
      move(temp, target);
    } catch (IOException | RuntimeException e) {
      deleteQuietly(temp, e);
      throw e;
    }
  }

  private void move(Path temp, Path target) throws IOException {
    try {
      Files.move(
          temp,
          target,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private void deleteQuietly(Path temp, Exception cause) {
    try {
      Files.deleteIfExists(temp);
    } catch (IOException e) {
      cause.addSuppressed(e);
    }
  }
}
