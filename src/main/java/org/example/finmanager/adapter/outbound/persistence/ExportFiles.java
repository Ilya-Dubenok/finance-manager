package org.example.finmanager.adapter.outbound.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.application.exception.StorageFileExistsException;
import org.example.finmanager.application.exception.StorageIoException;
import org.example.finmanager.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Общие действия экспорта: путь по умолчанию
 * {@code <каталог данных>/exports/<login>-<время>.<расширение>} и безопасная запись с вопросом о
 * перезаписи.
 */
@Component
@RequiredArgsConstructor
public class ExportFiles {

  private static final String EXPORTS_DIRECTORY = "exports";
  private static final String NAME_SEPARATOR = "-";
  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private final AppProperties properties;
  private final Clock clock;
  private final AtomicFileWriter fileWriter;

  /**
   * Выбирает файл для экспорта.
   *
   * @param login логин владельца
   * @param extension расширение с точкой
   * @param requested путь, который задал пользователь, или {@code null}
   * @return заданный путь или путь по умолчанию
   */
  public Path targetFor(String login, String extension, Path requested) {
    if (requested != null) {
      return requested;
    }
    String stamp = STAMP.format(LocalDateTime.now(clock));
    String name = login + NAME_SEPARATOR + stamp + extension;
    return properties.dataDir().resolve(EXPORTS_DIRECTORY).resolve(name);
  }

  /**
   * Записывает файл атомарно.
   *
   * @param target файл
   * @param content содержимое
   * @param overwrite можно ли заменить существующий файл
   * @throws StorageFileExistsException если файл есть, а заменять его нельзя
   * @throws StorageIoException если запись не удалась
   */
  public void write(Path target, byte[] content, boolean overwrite) {
    if (!overwrite && Files.exists(target)) {
      throw new StorageFileExistsException(target);
    }
    try {
      fileWriter.write(target, content);
    } catch (IOException e) {
      throw new StorageIoException(target, e.toString());
    }
  }
}
