package org.example.finmanager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import org.example.finmanager.adapter.inbound.cli.CliApplication;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.io.ScriptedConsoleIo;
import org.example.finmanager.config.AppConfig;
import org.example.finmanager.config.AppProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * Поднимает приложение с боевыми бинами на временном каталоге данных и прогоняет сценарий со
 * скриптовым вводом: подменены только {@code ConsoleIo} и часы (всегда {@link #TODAY}).
 */
public class ScenarioApp {

  /** «Сегодня» во всех сценариях. */
  public static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

  private static final String USERS_FILE_NAME = "users.properties";
  private static final String PROPERTY_SOURCE_NAME = "scenario";
  private static final String FIXED_CLOCK_BEAN = "fixedClock";
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);

  private final Path dataDir;

  /**
   * Готовит каталог данных с заданным файлом пользователей.
   *
   * @param dataDir существующий каталог (обычно временный)
   * @param usersFile содержимое {@code users.properties}
   */
  public ScenarioApp(Path dataDir, String usersFile) {
    this.dataDir = dataDir;
    try {
      Files.writeString(dataDir.resolve(USERS_FILE_NAME), usersFile);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Запускает приложение и возвращается, когда цикл команд закончился.
   *
   * @param io скриптовой ввод-вывод; по нему же читается вывод
   */
  public void run(ScriptedConsoleIo io) {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      Map<String, Object> settings = Map.of(AppProperties.DATA_DIR_PROPERTY, dataDir.toString());
      MapPropertySource source = new MapPropertySource(PROPERTY_SOURCE_NAME, settings);
      context.getEnvironment().getPropertySources().addFirst(source);
      context.register(AppConfig.class);
      context.registerBean(ConsoleIo.class, () -> io, definition -> definition.setPrimary(true));
      context.registerBean(
          FIXED_CLOCK_BEAN, Clock.class, () -> CLOCK, definition -> definition.setPrimary(true));
      context.refresh();

      context.getBean(CliApplication.class).run();
    }
  }
}
