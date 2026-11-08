package org.example.finmanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.example.finmanager.adapter.inbound.cli.TestConstants;
import org.example.finmanager.adapter.inbound.cli.io.ScriptedConsoleIo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Сценарии среза 3.5 через весь стек: сохранение и загрузка кошельков (спецификация, S-3). */
public class PersistenceScenarioTest {

  private static final String ALICE = "alice";
  private static final String BOB = "bob";
  private static final String ALICE_PASSWORD = "alice-test-pw";
  private static final String BOB_PASSWORD = "bob-test-pw";
  private static final String USERS_FILE =
      ALICE + "=" + ALICE_PASSWORD + "\n" + BOB + "=" + BOB_PASSWORD + "\n";

  private static final String LOGIN = "1";
  private static final String LOGOUT = "7";
  private static final String OPERATIONS = "1";
  private static final String CATEGORIES = "2";
  private static final String STATISTICS = "3";
  private static final String EXIT_APPLICATION = TestConstants.CHOICE_LEAVE;
  private static final String BACK = TestConstants.CHOICE_LEAVE;
  private static final String ADD_INCOME = "1";
  private static final String ADD_EXPENSE = "2";
  private static final String LIST_OPERATIONS = "3";
  private static final String CREATE_CATEGORY = "1";
  private static final String LIST_CATEGORIES = "2";
  private static final String SET_BUDGET = "5";
  private static final String SUMMARY = "1";
  private static final String TYPE_INCOME = "1";
  private static final String TYPE_EXPENSE = "2";
  private static final String SKIP = "";

  private static final List<String> LOGIN_ALICE = List.of(LOGIN, ALICE, ALICE_PASSWORD);
  private static final List<String> LOGIN_BOB = List.of(LOGIN, BOB, BOB_PASSWORD);

  private static final List<String> SOME_DATA =
      List.of(
          CATEGORIES,
          CREATE_CATEGORY, TYPE_EXPENSE, "Food",
          CREATE_CATEGORY, TYPE_INCOME, "Salary",
          SET_BUDGET, "Food", "4000",
          BACK,
          OPERATIONS,
          ADD_INCOME, "Salary", "5000", "01.11.2026", "advance",
          ADD_EXPENSE, "Food", "800", SKIP, SKIP,
          BACK);

  private static final String BALANCE_4200 = "Balance: 4,200.00 ===";

  @TempDir private Path dataDir;

  private ScenarioApp app;

  @BeforeEach
  public void createApp() {
    app = new ScenarioApp(dataDir, USERS_FILE);
  }

  @Test
  public void dataSurvivesRestartAfterExitFromTheMenu() {
    run(LOGIN_ALICE, SOME_DATA, List.of(EXIT_APPLICATION));

    ScriptedConsoleIo second =
        run(
            LOGIN_ALICE,
            List.of(
                CATEGORIES, LIST_CATEGORIES, BACK,
                OPERATIONS, LIST_OPERATIONS, SKIP, SKIP, BACK,
                STATISTICS, SUMMARY, SKIP, SKIP));

    assertThat(dataDir.resolve("wallets").resolve("alice.json")).exists();
    assertThat(second.output())
        .contains(BALANCE_4200)
        .contains("| Food               | 4,000.00 |")
        .containsSubsequence(
            "| 1 | 01.11.2026 | income  | Salary", "| 2 | 08.11.2026 | expense | Food")
        .contains("advance")
        .contains("| Food     | 4,000.00 | 800.00 |         3,200.00 |");
  }

  @Test
  public void endOfInputSavesTheDataToo() {
    run(LOGIN_ALICE, SOME_DATA);

    ScriptedConsoleIo second = run(LOGIN_ALICE);

    assertThat(second.output()).contains(BALANCE_4200);
  }

  @Test
  public void logoutSavesTheDataBeforeReturningToTheGuestMenu() {
    run(LOGIN_ALICE, SOME_DATA, List.of(LOGOUT));

    assertThat(dataDir.resolve("wallets").resolve("alice.json")).exists();
    assertThat(run(LOGIN_ALICE).output()).contains(BALANCE_4200);
  }

  @Test
  public void everyUserHasHisOwnFile() {
    run(LOGIN_ALICE, SOME_DATA, List.of(LOGOUT), LOGIN_BOB, List.of(OPERATIONS), List.of(
        ADD_INCOME, "Transfers", "7", SKIP, SKIP));

    assertThat(dataDir.resolve("wallets").resolve("alice.json")).exists();
    assertThat(dataDir.resolve("wallets").resolve("bob.json")).exists();
    assertThat(run(LOGIN_BOB).output()).contains("| bob | Balance: 7.00 ===");
    assertThat(run(LOGIN_ALICE).output()).contains(BALANCE_4200);
  }

  @Test
  public void userWhoOnlyLoggedInGetsNoFile() {
    run(LOGIN_ALICE);

    assertThat(dataDir.resolve("wallets").resolve("alice.json")).doesNotExist();
  }

  @Test
  public void corruptedFileBlocksLoginAndStaysUntouched() throws IOException {
    Path file = writeWallet("alice", "garbage");

    ScriptedConsoleIo io = run(LOGIN_ALICE);

    assertThat(io.output())
        .contains("The wallet file of user \"alice\" is corrupted:")
        .contains("Login is not possible. Fix or delete the file " + file)
        .doesNotContain("| alice |");
    assertThat(file).hasContent("garbage");
  }

  @Test
  public void contentThatBreaksTheRulesBlocksLoginAndStaysUntouched() throws IOException {
    String content =
        "{\"version\": 1, \"operations\": [{\"type\": \"EXPENSE\", \"category\": \"Nowhere\", "
            + "\"amount\": \"5.00\", \"date\": \"2026-11-08\"}]}";
    Path file = writeWallet("alice", content);

    ScriptedConsoleIo io = run(LOGIN_ALICE);

    assertThat(io.output())
        .contains("operation #1 (Nowhere): category does not exist")
        .doesNotContain("| alice |");
    assertThat(file).hasContent(content);
  }

  @Test
  public void afterTheFileIsRemovedLoginWorksWithAnEmptyWallet() throws IOException {
    Path file = writeWallet("alice", "garbage");
    run(LOGIN_ALICE);

    Files.delete(file);

    assertThat(run(LOGIN_ALICE).output()).contains("| alice | Balance: 0.00 ===");
  }

  @Test
  public void corruptedFileOfOneUserDoesNotAffectTheOther() throws IOException {
    writeWallet("alice", "garbage");

    assertThat(run(LOGIN_BOB).output()).contains("| bob | Balance: 0.00 ===");
  }

  @Test
  public void failedSaveOnExitOffersToRetryAndThenExitsWithoutSaving() throws IOException {
    blockWalletsDirectory();

    ScriptedConsoleIo io =
        run(
            LOGIN_ALICE,
            List.of(OPERATIONS, ADD_EXPENSE, "Transfers", "5", SKIP, SKIP, BACK),
            List.of(EXIT_APPLICATION, "yes", "no"));

    assertThat(occurrences(io, "Cannot save the data:")).isEqualTo(2);
    assertThat(occurrences(io, "Retry? (yes - retry, no - exit without saving)")).isEqualTo(2);
    assertThat(io.output()).endsWith(TestConstants.GOODBYE + System.lineSeparator());
  }

  @Test
  public void failedSaveOnLogoutLetsTheUserLeaveWithoutSaving() throws IOException {
    blockWalletsDirectory();

    ScriptedConsoleIo io =
        run(
            LOGIN_ALICE,
            List.of(OPERATIONS, ADD_EXPENSE, "Transfers", "5", SKIP, SKIP, BACK),
            List.of(LOGOUT, "no"),
            LOGIN_BOB);

    assertThat(occurrences(io, "Cannot save the data:")).isEqualTo(1);
    assertThat(io.output()).contains("| bob | Balance: 0.00 ===");
  }

  @Test
  public void cancelWordAtTheRetryQuestionKeepsTheUserInTheMenu() throws IOException {
    blockWalletsDirectory();

    ScriptedConsoleIo io =
        run(
            LOGIN_ALICE,
            List.of(OPERATIONS, ADD_EXPENSE, "Transfers", "5", SKIP, SKIP, BACK),
            List.of(LOGOUT, "cancel"));

    assertThat(io.output()).contains("Cancelled.");
    assertThat(occurrences(io, "| alice | Balance: -5.00 ===")).isEqualTo(2);
  }

  @Test
  public void shutdownSaveFailureAfterEndOfInputDoesNotBreakTheExit() throws IOException {
    blockWalletsDirectory();

    ScriptedConsoleIo io =
        run(LOGIN_ALICE, List.of(OPERATIONS, ADD_EXPENSE, "Transfers", "5", SKIP, SKIP));

    assertThat(io.output()).endsWith(TestConstants.GOODBYE + System.lineSeparator());
  }

  private void blockWalletsDirectory() throws IOException {
    Files.writeString(dataDir.resolve("wallets"), "a file where a directory is needed");
  }

  private Path writeWallet(String login, String content) throws IOException {
    Path directory = dataDir.resolve("wallets");
    Files.createDirectories(directory);
    Path file = directory.resolve(login + ".json");
    Files.writeString(file, content);
    return file;
  }

  @SafeVarargs
  private ScriptedConsoleIo run(List<String>... parts) {
    List<String> input = new ArrayList<>();
    Arrays.stream(parts).forEach(input::addAll);
    ScriptedConsoleIo io = new ScriptedConsoleIo(input.toArray(new String[0]));
    app.run(io);
    return io;
  }

  private int occurrences(ScriptedConsoleIo io, String part) {
    return io.output().split(Pattern.quote(part), -1).length - 1;
  }
}
