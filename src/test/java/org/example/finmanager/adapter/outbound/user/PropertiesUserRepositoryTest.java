package org.example.finmanager.adapter.outbound.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.example.finmanager.application.dto.model.UserDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PropertiesUserRepositoryTest {

  private static final String USERS_FILE_NAME = "users.properties";
  private static final String CUSTOM_USERS = "dave=dave-test-pw\n";

  @TempDir private Path tempDir;

  private final UsersFileParser parser = new UsersFileParser();

  @Test
  public void createsDefaultFileWhenMissing() {
    Path dataDir = tempDir.resolve("data");

    PropertiesUserRepository repository = repositoryFor(dataDir);

    assertThat(dataDir.resolve(USERS_FILE_NAME)).exists();
    assertThat(repository.findByLogin("alice")).isPresent();
    assertThat(repository.findByLogin("bob")).isPresent();
    assertThat(repository.findByLogin("carol")).isPresent();
  }

  @Test
  public void readsExistingFileAndDoesNotOverwriteIt() throws IOException {
    Path file = tempDir.resolve(USERS_FILE_NAME);
    Files.writeString(file, CUSTOM_USERS);

    PropertiesUserRepository repository = repositoryFor(tempDir);

    Optional<UserDto> dave = repository.findByLogin("dave");
    assertThat(dave).isPresent();
    assertThat(dave.get().password()).isEqualTo("dave-test-pw");
    assertThat(repository.findByLogin("alice")).isEmpty();
    assertThat(file).hasContent(CUSTOM_USERS);
  }

  @Test
  public void findsLoginsCaseSensitively() throws IOException {
    Files.writeString(tempDir.resolve(USERS_FILE_NAME), CUSTOM_USERS);

    PropertiesUserRepository repository = repositoryFor(tempDir);

    assertThat(repository.findByLogin("DAVE")).isEmpty();
  }

  @Test
  public void failsWithLineDetailsWhenFileIsInvalid() throws IOException {
    Path file = tempDir.resolve(USERS_FILE_NAME);
    Files.writeString(file, "dave=ok-pw\nx=short-login\n");

    assertThatThrownBy(() -> repositoryFor(tempDir))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            e -> {
              assertThat(e.getCode()).isEqualTo(ErrorCode.USERS_FILE_BAD_LOGIN);
              assertThat(e.getArgs()).containsExactly(file.toString(), 2, "x");
            });
  }

  @Test
  public void failsWhenFileIsNotValidUtf8() throws IOException {
    Files.write(tempDir.resolve(USERS_FILE_NAME), new byte[] {(byte) 0xC3, (byte) 0x28});

    assertThatThrownBy(() -> repositoryFor(tempDir))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.USERS_FILE_IO));
  }

  @Test
  public void failsWhenDataDirectoryCannotBeCreated() throws IOException {
    Path blocker = tempDir.resolve("blocker");
    Files.writeString(blocker, "not a directory");

    assertThatThrownBy(() -> repositoryFor(blocker))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            e -> assertThat(e.getCode()).isEqualTo(ErrorCode.USERS_FILE_IO));
  }

  private PropertiesUserRepository repositoryFor(Path dataDir) {
    return new PropertiesUserRepository(new AppProperties(dataDir), parser);
  }
}
