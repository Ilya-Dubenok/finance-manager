package org.example.finmanager.adapter.outbound.user;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.example.finmanager.application.dto.model.UserDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.port.outbound.UserRepositoryPort;
import org.example.finmanager.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Пользователи из файла {@code users.properties} в каталоге данных.
 *
 * <p>Файл читается один раз при создании. Если его нет, создаётся копия файла по умолчанию из
 * ресурсов. Файл только читается, обратно не записывается.
 */
@Slf4j
@Component
public class PropertiesUserRepository implements UserRepositoryPort {

  private static final String USERS_FILE_NAME = "users.properties";
  private static final String DEFAULT_USERS_RESOURCE = "/default-users.properties";
  private static final String MISSING_RESOURCE_MESSAGE = "Missing classpath resource ";

  private final Map<String, UserDto> users;

  /**
   * Загружает пользователей.
   *
   * @param properties настройки с каталогом данных
   * @param parser разбор содержимого файла
   * @throws ApplicationException если файл нельзя прочитать или он содержит ошибки
   */
  public PropertiesUserRepository(AppProperties properties, UsersFileParser parser) {
    Path file = properties.dataDir().resolve(USERS_FILE_NAME);
    createDefaultFileIfMissing(file);
    List<String> lines = readLines(file);
    this.users = parser.parse(file.toString(), lines);
    log.info("Loaded {} users from {}", users.size(), file);
  }

  @Override
  public Optional<UserDto> findByLogin(String login) {
    return Optional.ofNullable(users.get(login));
  }

  private void createDefaultFileIfMissing(Path file) {
    if (Files.exists(file)) {
      return;
    }
    try (InputStream defaults = getClass().getResourceAsStream(DEFAULT_USERS_RESOURCE)) {
      if (defaults == null) {
        throw new IllegalStateException(MISSING_RESOURCE_MESSAGE + DEFAULT_USERS_RESOURCE);
      }
      Files.createDirectories(file.getParent());
      Files.copy(defaults, file);
      log.info("Created default users file {}", file);
    } catch (IOException e) {
      throw new ApplicationException(ErrorCode.USERS_FILE_IO, file, e.toString());
    }
  }

  private List<String> readLines(Path file) {
    try {
      return Files.readAllLines(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new ApplicationException(ErrorCode.USERS_FILE_IO, file, e.toString());
    }
  }
}
