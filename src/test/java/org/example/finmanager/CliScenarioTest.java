package org.example.finmanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.regex.Pattern;
import org.example.finmanager.adapter.inbound.cli.TestConstants;
import org.example.finmanager.adapter.inbound.cli.io.ScriptedConsoleIo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Сценарии среза 3.1 через весь стек: меню, вход, выход, справка, устойчивость цикла. */
public class CliScenarioTest {

  private static final String ALICE = "alice";
  private static final String BOB = "bob";
  private static final String ALICE_PASSWORD = "alice-test-pw";
  private static final String BOB_PASSWORD = "bob-test-pw";
  private static final String USERS_FILE =
      ALICE + "=" + ALICE_PASSWORD + "\n" + BOB + "=" + BOB_PASSWORD + "\n";

  private static final String CHOICE_LOGIN = "1";
  private static final String CHOICE_GUEST_HELP = "2";
  private static final String CHOICE_MAIN_HELP = "6";
  private static final String CHOICE_LOGOUT = "7";

  private static final String ALICE_MAIN_TITLE = "=== Finance Manager | alice | Balance: 0.00 ===";
  private static final String BOB_MAIN_TITLE = "=== Finance Manager | bob | Balance: 0.00 ===";
  private static final String BAD_CREDENTIALS = "Invalid login or password.";
  private static final String EMPTY_VALUE = "The value must not be empty.";
  private static final String CANCELLED = "Cancelled.";
  private static final String HELP_TITLE = "=== Help ===";

  @TempDir private Path dataDir;

  private ScenarioApp app;

  @BeforeEach
  public void createApp() {
    app = new ScenarioApp(dataDir, USERS_FILE);
  }

  @Test
  public void guestMenuOffersLoginHelpAndExit() {
    ScriptedConsoleIo io = run(TestConstants.CHOICE_LEAVE);

    assertThat(io.output())
        .contains(TestConstants.GUEST_MENU_TITLE)
        .contains("1. Log in")
        .contains("2. Help")
        .contains("0. Exit")
        .endsWith(TestConstants.GOODBYE + System.lineSeparator());
  }

  @Test
  public void keepsAskingAfterInvalidChoice() {
    ScriptedConsoleIo io = run("x", TestConstants.CHOICE_LEAVE);

    assertThat(io.output())
        .contains("No such option: x. Enter a number from 0 to 2.")
        .contains(TestConstants.GOODBYE);
  }

  @Test
  public void exitsGracefullyWhenInputEndsInGuestMenu() {
    ScriptedConsoleIo io = run();

    assertThat(io.output()).endsWith(TestConstants.GOODBYE + System.lineSeparator());
  }

  @Test
  public void loginShowsMainMenuWithLogin() {
    ScriptedConsoleIo io = run(CHOICE_LOGIN, ALICE, ALICE_PASSWORD, TestConstants.CHOICE_LEAVE);

    assertThat(io.output())
        .contains(ALICE_MAIN_TITLE)
        .contains("1. Operations")
        .contains("2. Categories and budgets")
        .contains("3. Statistics")
        .contains("6. Help")
        .contains("7. Log out")
        .contains("0. Exit the application");
  }

  @Test
  public void passwordIsReadWithoutEchoAndNeverPrinted() {
    ScriptedConsoleIo io = run(CHOICE_LOGIN, ALICE, ALICE_PASSWORD, TestConstants.CHOICE_LEAVE);

    assertThat(io.passwordReads()).isEqualTo(1);
    assertThat(io.output()).doesNotContain(ALICE_PASSWORD);
  }

  @Test
  public void wrongPasswordReturnsToGuestMenu() {
    ScriptedConsoleIo io = run(CHOICE_LOGIN, ALICE, "wrong", TestConstants.CHOICE_LEAVE);

    assertThat(io.output()).contains(BAD_CREDENTIALS).doesNotContain(ALICE_MAIN_TITLE);
    assertThat(occurrences(io, TestConstants.GUEST_MENU_TITLE)).isEqualTo(2);
  }

  @Test
  public void unknownLoginGetsTheSameMessageAsWrongPassword() {
    ScriptedConsoleIo io = run(CHOICE_LOGIN, "nobody", ALICE_PASSWORD, TestConstants.CHOICE_LEAVE);

    assertThat(io.output()).contains(BAD_CREDENTIALS);
  }

  @Test
  public void loginIsCaseSensitive() {
    ScriptedConsoleIo io = run(CHOICE_LOGIN, "ALICE", ALICE_PASSWORD, TestConstants.CHOICE_LEAVE);

    assertThat(io.output()).contains(BAD_CREDENTIALS).doesNotContain(ALICE_MAIN_TITLE);
  }

  @Test
  public void loginAttemptsAreNotLimited() {
    ScriptedConsoleIo io =
        run(
            CHOICE_LOGIN, ALICE, "x1",
            CHOICE_LOGIN, ALICE, "x2",
            CHOICE_LOGIN, ALICE, "x3",
            CHOICE_LOGIN, ALICE, ALICE_PASSWORD,
            TestConstants.CHOICE_LEAVE);

    assertThat(occurrences(io, BAD_CREDENTIALS)).isEqualTo(3);
    assertThat(io.output()).contains(ALICE_MAIN_TITLE);
  }

  @Test
  public void emptyLoginIsAskedAgain() {
    ScriptedConsoleIo io = run(CHOICE_LOGIN, "", ALICE, ALICE_PASSWORD, TestConstants.CHOICE_LEAVE);

    assertThat(occurrences(io, EMPTY_VALUE)).isEqualTo(1);
    assertThat(io.output()).contains(ALICE_MAIN_TITLE);
  }

  @Test
  public void cancelWordAtLoginReturnsToGuestMenu() {
    ScriptedConsoleIo io = run(CHOICE_LOGIN, "cancel", TestConstants.CHOICE_LEAVE);

    assertThat(io.output()).contains(CANCELLED).doesNotContain(ALICE_MAIN_TITLE);
    assertThat(occurrences(io, TestConstants.GUEST_MENU_TITLE)).isEqualTo(2);
  }

  @Test
  public void cancelWordAtPasswordReturnsToGuestMenu() {
    ScriptedConsoleIo io = run(CHOICE_LOGIN, ALICE, "cancel", TestConstants.CHOICE_LEAVE);

    assertThat(io.output()).contains(CANCELLED).doesNotContain(ALICE_MAIN_TITLE);
  }

  @Test
  public void mainMenuReportsUnknownItemWithItsOwnRange() {
    ScriptedConsoleIo io =
        run(CHOICE_LOGIN, ALICE, ALICE_PASSWORD, "9", TestConstants.CHOICE_LEAVE);

    assertThat(io.output()).contains("No such option: 9. Enter a number from 0 to 7.");
  }

  @Test
  public void logoutReturnsToGuestMenuAndAnotherUserCanLogIn() {
    ScriptedConsoleIo io =
        run(
            CHOICE_LOGIN, ALICE, ALICE_PASSWORD,
            CHOICE_LOGOUT,
            CHOICE_LOGIN, BOB, BOB_PASSWORD,
            TestConstants.CHOICE_LEAVE);

    assertThat(io.output()).contains(ALICE_MAIN_TITLE).contains(BOB_MAIN_TITLE);
    assertThat(occurrences(io, TestConstants.GUEST_MENU_TITLE)).isEqualTo(2);
  }

  @Test
  public void exitFromMainMenuEndsTheApplication() {
    ScriptedConsoleIo io = run(CHOICE_LOGIN, ALICE, ALICE_PASSWORD, TestConstants.CHOICE_LEAVE);

    assertThat(occurrences(io, TestConstants.GUEST_MENU_TITLE)).isEqualTo(1);
    assertThat(io.output()).endsWith(TestConstants.GOODBYE + System.lineSeparator());
  }

  @Test
  public void exitAfterLogoutLeavesFromGuestMenu() {
    ScriptedConsoleIo io =
        run(CHOICE_LOGIN, ALICE, ALICE_PASSWORD, CHOICE_LOGOUT, TestConstants.CHOICE_LEAVE);

    assertThat(occurrences(io, TestConstants.GUEST_MENU_TITLE)).isEqualTo(2);
    assertThat(io.output()).endsWith(TestConstants.GOODBYE + System.lineSeparator());
  }

  @Test
  public void exitsGracefullyWhenInputEndsInMainMenu() {
    ScriptedConsoleIo io = run(CHOICE_LOGIN, ALICE, ALICE_PASSWORD);

    assertThat(io.output())
        .contains(ALICE_MAIN_TITLE)
        .endsWith(TestConstants.GOODBYE + System.lineSeparator());
  }

  @Test
  public void helpIsAvailableBeforeLogin() {
    ScriptedConsoleIo io = run(CHOICE_GUEST_HELP, TestConstants.CHOICE_LEAVE);

    assertThat(io.output())
        .contains(HELP_TITLE)
        .contains("\n  Enter a menu number")
        .contains("Example: adding an expense");
    assertThat(occurrences(io, TestConstants.GUEST_MENU_TITLE)).isEqualTo(2);
  }

  @Test
  public void helpIsAvailableInMainMenu() {
    ScriptedConsoleIo io =
        run(CHOICE_LOGIN, ALICE, ALICE_PASSWORD, CHOICE_MAIN_HELP, TestConstants.CHOICE_LEAVE);

    assertThat(io.output()).contains(HELP_TITLE);
    assertThat(occurrences(io, ALICE_MAIN_TITLE)).isEqualTo(2);
  }

  private ScriptedConsoleIo run(String... input) {
    ScriptedConsoleIo io = new ScriptedConsoleIo(input);
    app.run(io);
    return io;
  }

  private int occurrences(ScriptedConsoleIo io, String part) {
    return io.output().split(Pattern.quote(part), -1).length - 1;
  }
}
