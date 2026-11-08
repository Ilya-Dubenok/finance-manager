package org.example.finmanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.example.finmanager.adapter.inbound.cli.TestConstants;
import org.example.finmanager.config.AppProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Запуск приложения целиком, с настоящим {@code SystemConsoleIo} и подменёнными потоками JVM. */
public class FinanceManagerApplicationTest {

  private static final String USERS_FILE_NAME = "users.properties";

  private final InputStream originalIn = System.in;
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private final String originalDataDir = System.getProperty(AppProperties.DATA_DIR_PROPERTY);
  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private final ByteArrayOutputStream err = new ByteArrayOutputStream();

  @TempDir private Path dataDir;

  @BeforeEach
  public void redirectSystemStreams() {
    System.setProperty(AppProperties.DATA_DIR_PROPERTY, dataDir.toString());
    System.setIn(new ByteArrayInputStream(new byte[0]));
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  public void restoreSystemStreams() {
    System.setIn(originalIn);
    System.setOut(originalOut);
    System.setErr(originalErr);
    if (originalDataDir == null) {
      System.clearProperty(AppProperties.DATA_DIR_PROPERTY);
    } else {
      System.setProperty(AppProperties.DATA_DIR_PROPERTY, originalDataDir);
    }
  }

  @Test
  public void firstRunCreatesUsersFileAndExitsNormallyWhenInputEnds() {
    int exitCode = new FinanceManagerApplication().run();

    assertThat(exitCode).isZero();
    assertThat(dataDir.resolve(USERS_FILE_NAME)).exists();
    assertThat(out.toString(StandardCharsets.UTF_8)).contains(TestConstants.GOODBYE);
    assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
  }

  @Test
  public void invalidUsersFileStopsStartupWithLineDetailsAndCodeOne() throws IOException {
    Path file = dataDir.resolve(USERS_FILE_NAME);
    Files.writeString(file, "alice=pw-one\nline without separator\n");

    int exitCode = new FinanceManagerApplication().run();

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString(StandardCharsets.UTF_8))
        .contains(file.toString())
        .contains("line 2")
        .contains("missing \"=\"");
    assertThat(out.toString(StandardCharsets.UTF_8)).isEmpty();
  }
}
