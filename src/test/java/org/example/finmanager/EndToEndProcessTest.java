package org.example.finmanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.example.finmanager.adapter.inbound.cli.TestConstants;
import org.example.finmanager.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Сквозные тесты: приложение запускается отдельным процессом JVM с настоящими {@code System.in} и
 * {@code System.out}, как из командной строки. Проверяются точка входа, код выхода, файлы данных и
 * сохранение между запусками.
 */
public class EndToEndProcessTest {

  private static final String CLASSPATH_PROPERTY = "finmanager.e2e.classpath";
  private static final String MAIN_CLASS = FinanceManagerApplication.class.getName();
  private static final long TIMEOUT_SECONDS = 60;
  private static final String LINE_BREAK = "\n";

  private static final String ALICE = "alice";
  private static final String ALICE_PASSWORD = "alice-test-pw";
  private static final String USERS_FILE_NAME = "users.properties";
  private static final String WALLET_FILE = "wallets/alice.json";

  private static final String LOGIN = "1";
  private static final String OPERATIONS = "1";
  private static final String CATEGORIES = "2";
  private static final String LOGOUT = "7";
  private static final String EXIT = TestConstants.CHOICE_LEAVE;
  private static final String CREATE_CATEGORY = "1";
  private static final String ADD_INCOME = "1";
  private static final String TYPE_INCOME = "1";
  private static final String SKIP = "";

  private static final List<String> LOG_IN = List.of(LOGIN, ALICE, ALICE_PASSWORD);
  private static final List<String> ADD_SALARY =
      List.of(
          CATEGORIES,
          CREATE_CATEGORY,
          TYPE_INCOME,
          "Salary",
          EXIT,
          OPERATIONS,
          ADD_INCOME,
          "Salary",
          "1500",
          SKIP,
          SKIP,
          EXIT);

  private static final String INCOME_ADDED = "Income added: Salary, 1,500.00";
  private static final String BALANCE_AFTER_SALARY = "Balance: 1,500.00";
  private static final String BALANCE_BEFORE_SALARY = "Balance: 0.00";

  @TempDir private Path dataDir;
  @TempDir private Path workDir;

  @Test
  public void dataSurvivesRestartAfterExplicitExit() throws Exception {
    writeUsers();

    Launch first = launch(lines(LOG_IN, ADD_SALARY, List.of(EXIT)));
    Launch second = launch(lines(LOG_IN, List.of(EXIT)));

    assertThat(first.exitCode()).isZero();
    assertThat(first.out()).contains(BALANCE_BEFORE_SALARY).contains(INCOME_ADDED);
    assertThat(first.out()).endsWith(TestConstants.GOODBYE + System.lineSeparator());
    assertThat(dataDir.resolve(WALLET_FILE)).exists();
    assertThat(second.exitCode()).isZero();
    assertThat(second.out()).contains(BALANCE_AFTER_SALARY);
  }

  @Test
  public void endOfInputSavesTheWorkAndExitsNormally() throws Exception {
    writeUsers();

    Launch first = launch(lines(LOG_IN, ADD_SALARY));
    Launch second = launch(lines(LOG_IN, List.of(LOGOUT, EXIT)));

    assertThat(first.exitCode()).isZero();
    assertThat(first.err()).isEmpty();
    assertThat(first.out()).contains(TestConstants.GOODBYE);
    assertThat(second.out()).contains(BALANCE_AFTER_SALARY);
  }

  @Test
  public void firstLaunchCreatesUsersFileAndStopsAtEndOfInput() throws Exception {
    Launch launch = launch("");

    assertThat(launch.exitCode()).isZero();
    assertThat(dataDir.resolve(USERS_FILE_NAME)).exists();
    assertThat(launch.out()).contains(TestConstants.GUEST_MENU_TITLE);
  }

  @Test
  public void brokenUsersFileStopsTheProcessWithCodeOne() throws Exception {
    Files.writeString(dataDir.resolve(USERS_FILE_NAME), "line without separator\n");

    Launch launch = launch("");

    assertThat(launch.exitCode()).isEqualTo(1);
    assertThat(launch.out()).isEmpty();
    assertThat(launch.err()).contains("line 1");
  }

  @Test
  public void logsGoToTheFileAndNotToTheTerminal() throws Exception {
    writeUsers();

    Launch launch = launch(lines(LOG_IN, List.of(EXIT)));

    assertThat(launch.out()).doesNotContain("INFO");
    assertThat(workDir.resolve("logs/finmanager.log")).exists();
  }

  private void writeUsers() throws IOException {
    Files.writeString(
        dataDir.resolve(USERS_FILE_NAME), ALICE + "=" + ALICE_PASSWORD + LINE_BREAK);
  }

  @SafeVarargs
  private String lines(List<String>... parts) {
    StringBuilder text = new StringBuilder();
    for (List<String> part : parts) {
      part.forEach(line -> text.append(line).append(LINE_BREAK));
    }
    return text.toString();
  }

  private Launch launch(String input) throws IOException, InterruptedException {
    Path inputFile = Files.createTempFile(workDir, "input", ".txt");
    Files.writeString(inputFile, input, StandardCharsets.UTF_8);

    ProcessBuilder builder = new ProcessBuilder(command());
    builder.directory(workDir.toFile());
    builder.redirectInput(inputFile.toFile());
    Path outFile = Files.createTempFile(workDir, "out", ".txt");
    builder.redirectOutput(outFile.toFile());
    Path errFile = Files.createTempFile(workDir, "err", ".txt");
    builder.redirectError(errFile.toFile());
    Process process = builder.start();
    if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new AssertionError("The application did not finish in " + TIMEOUT_SECONDS + " s");
    }
    String out = Files.readString(outFile, StandardCharsets.UTF_8);
    String err = Files.readString(errFile, StandardCharsets.UTF_8);
    return new Launch(process.exitValue(), out, err);
  }

  private List<String> command() {
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    String dataDirOption = "-D" + AppProperties.DATA_DIR_PROPERTY + "=" + dataDir;
    String classpath = System.getProperty(CLASSPATH_PROPERTY);
    return List.of(
        java,
        dataDirOption,
        "-Dstdout.encoding=UTF-8",
        "-Dstdin.encoding=UTF-8",
        "-cp",
        classpath,
        MAIN_CLASS);
  }

  /** Итог запуска: код выхода и всё, что процесс вывел в stdout и stderr. */
  public record Launch(int exitCode, String out, String err) {}
}
