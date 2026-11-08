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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Сценарии среза 3.6 через весь стек: переводы между пользователями (спецификация, S-2). */
public class TransferScenarioTest {

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
  private static final String TRANSFER = "4";
  private static final String BACK = TestConstants.CHOICE_LEAVE;
  private static final String ADD_INCOME = "1";
  private static final String LIST_OPERATIONS = "3";
  private static final String SET_BUDGET = "5";
  private static final String SKIP = "";

  private static final List<String> LOGIN_ALICE = List.of(LOGIN, ALICE, ALICE_PASSWORD);
  private static final List<String> LOGIN_BOB = List.of(LOGIN, BOB, BOB_PASSWORD);

  /** Alice получает 1000 доходом в системной категории. */
  private static final List<String> ALICE_HAS_THOUSAND =
      List.of(OPERATIONS, ADD_INCOME, "Transfers", "1000", SKIP, SKIP, BACK);

  private static final String BOB_BALANCE_400 = "| bob | Balance: 400.00 ===";

  @TempDir private Path dataDir;

  private ScenarioApp app;

  @BeforeEach
  public void createApp() {
    app = new ScenarioApp(dataDir, USERS_FILE);
  }

  @Test
  public void transferChangesBothBalancesEvenIfTheRecipientNeverLoggedIn() {
    ScriptedConsoleIo alice =
        run(LOGIN_ALICE, ALICE_HAS_THOUSAND, List.of(TRANSFER, BOB, "400", "for lunch", LOGOUT));
    ScriptedConsoleIo bob = run(LOGIN_BOB, List.of(OPERATIONS, LIST_OPERATIONS, SKIP, SKIP));

    assertThat(alice.output())
        .contains("Transferred 400.00 to bob.")
        .contains("| alice | Balance: 600.00 ===");
    assertThat(bob.output())
        .contains(BOB_BALANCE_400)
        .containsSubsequence("income", "Transfers", "400.00", "alice", "for lunch");
  }

  @Test
  public void senderSeesTheTransferInTheOperationList() {
    ScriptedConsoleIo io =
        run(
            LOGIN_ALICE,
            ALICE_HAS_THOUSAND,
            List.of(TRANSFER, BOB, "400", "gift", OPERATIONS, LIST_OPERATIONS, SKIP, SKIP));

    assertThat(io.output()).containsSubsequence("expense", "Transfers", "400.00", "bob", "gift");
  }

  @Test
  public void transferTooBigIsRejectedWithBalanceAndAmountAndChangesNothing() {
    ScriptedConsoleIo io =
        run(
            LOGIN_ALICE,
            ALICE_HAS_THOUSAND,
            List.of(TRANSFER, BOB, "400", SKIP, TRANSFER, BOB, "700", SKIP));

    assertThat(io.output())
        .contains("Insufficient funds for the transfer: balance 600.00, transfer amount 700.00.");
    assertThat(run(LOGIN_ALICE).output()).contains("| alice | Balance: 600.00 ===");
    assertThat(run(LOGIN_BOB).output()).contains(BOB_BALANCE_400);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "carol | User \"carol\" not found.",
        "alice | You cannot transfer money to yourself."
      })
  public void wrongRecipientIsRejected(String recipient, String message) {
    ScriptedConsoleIo io =
        run(LOGIN_ALICE, ALICE_HAS_THOUSAND, List.of(TRANSFER, recipient, "1", SKIP));

    assertThat(io.output()).contains(message).doesNotContain("Transferred");
    assertThat(io.output()).contains("| alice | Balance: 1,000.00 ===");
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "0 | The amount must be greater than zero.",
        "10.999 | The amount cannot have more than two decimal places.",
        "1000000000 | The amount cannot exceed 999,999,999.99."
      })
  public void invalidAmountEndsTheDialogWithMessage(String amount, String message) {
    ScriptedConsoleIo io =
        run(LOGIN_ALICE, ALICE_HAS_THOUSAND, List.of(TRANSFER, BOB, amount, SKIP));

    assertThat(io.output()).contains(message).doesNotContain("Transferred");
  }

  @Test
  public void amountQuestionIsAskedAgainWhenFormatIsWrong() {
    ScriptedConsoleIo io =
        run(LOGIN_ALICE, ALICE_HAS_THOUSAND, List.of(TRANSFER, BOB, "abc", "5", SKIP));

    assertThat(io.output())
        .contains("Cannot recognize the amount \"abc\".")
        .contains("Transferred 5.00 to bob.");
  }

  @Test
  public void dialogCanBeCancelled() {
    ScriptedConsoleIo io =
        run(LOGIN_ALICE, ALICE_HAS_THOUSAND, List.of(TRANSFER, "cancel", TRANSFER, BOB, "cancel"));

    assertThat(occurrences(io, "Cancelled.")).isEqualTo(2);
    assertThat(io.output()).doesNotContain("Transferred");
  }

  @Test
  public void damagedWalletOfTheRecipientRejectsTheTransferAndChangesNothing() throws IOException {
    Path directory = dataDir.resolve("wallets");
    Files.createDirectories(directory);
    Path bobFile = directory.resolve("bob.json");
    Files.writeString(bobFile, "garbage");

    ScriptedConsoleIo io =
        run(LOGIN_ALICE, ALICE_HAS_THOUSAND, List.of(TRANSFER, BOB, "100", SKIP));

    assertThat(io.output())
        .contains("The wallet file of user \"bob\" is corrupted:")
        .doesNotContain("Transferred")
        .contains("| alice | Balance: 1,000.00 ===");
    assertThat(bobFile).hasContent("garbage");
  }

  @Test
  public void exceededBudgetOfTheTransfersCategoryWarnsTheSender() {
    ScriptedConsoleIo io =
        run(
            LOGIN_ALICE,
            ALICE_HAS_THOUSAND,
            List.of(CATEGORIES, SET_BUDGET, "Transfers", "100", BACK),
            List.of(TRANSFER, BOB, "150", SKIP));

    assertThat(io.output())
        .contains(
            "[!] Budget limit exceeded for category \"Transfers\": spent 150.00 against a limit "
                + "of 100.00 (over by 50.00).");
  }

  @Test
  public void recipientGetsNoAlertsAndHisBalanceIncreases() {
    ScriptedConsoleIo alice =
        run(LOGIN_ALICE, ALICE_HAS_THOUSAND, List.of(TRANSFER, BOB, "400", SKIP, LOGOUT));

    assertThat(alice.output()).doesNotContain("[!]");
  }

  @Test
  public void transferWritesBothWalletsOnExit() {
    run(LOGIN_ALICE, ALICE_HAS_THOUSAND, List.of(TRANSFER, BOB, "400", SKIP));

    assertThat(dataDir.resolve("wallets").resolve("bob.json")).exists();
    assertThat(dataDir.resolve("wallets").resolve("alice.json")).exists();
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
