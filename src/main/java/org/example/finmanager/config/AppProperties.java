package org.example.finmanager.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Настройки приложения.
 *
 * <p>Каталог данных берётся из системного свойства {@code finmanager.data-dir}, затем из
 * переменной окружения {@code FINMANAGER_DATA_DIR} (Spring сопоставляет имена сам), иначе это
 * {@code ./data}.
 */
@Component
public class AppProperties {

  /** Имя системного свойства с каталогом данных. */
  public static final String DATA_DIR_PROPERTY = "finmanager.data-dir";

  private static final String DEFAULT_DATA_DIR = "data";

  private final Path dataDir;

  /**
   * Читает настройки из окружения Spring.
   *
   * @param environment системные свойства и переменные окружения
   */
  @Autowired
  public AppProperties(Environment environment) {
    this(Path.of(environment.getProperty(DATA_DIR_PROPERTY, DEFAULT_DATA_DIR)));
  }

  /**
   * Задаёт каталог данных явно, например в тестах.
   *
   * @param dataDir каталог с {@code users.properties}, {@code wallets} и {@code exports}
   */
  public AppProperties(Path dataDir) {
    this.dataDir = dataDir;
  }

  /** Возвращает каталог данных. */
  public Path dataDir() {
    return dataDir;
  }
}
