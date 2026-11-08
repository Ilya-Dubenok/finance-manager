package org.example.finmanager.adapter.inbound.cli.input;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Разбирает путь к файлу из текста. Кавычки по краям (так часто вставляют путь с пробелами)
 * отбрасываются. Проверяется только синтаксис пути, существование файла — нет.
 */
@UtilityClass
public class PathParser {

  private static final String QUOTE = "\"";

  /**
   * Разбирает путь.
   *
   * @param text введённый текст
   * @return путь или пустой результат, если текст пуст или не является допустимым путём
   */
  public Optional<Path> parse(String text) {
    String unquoted = strip(text.trim());
    if (unquoted.isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.of(Path.of(unquoted));
    } catch (InvalidPathException e) {
      return Optional.empty();
    }
  }

  private String strip(String text) {
    boolean quoted =
        text.length() >= 2 && text.startsWith(QUOTE) && text.endsWith(QUOTE);
    return quoted ? text.substring(1, text.length() - 1).trim() : text;
  }
}
