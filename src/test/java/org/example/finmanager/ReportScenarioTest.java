package org.example.finmanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

/** Сценарии среза 3.8 через весь стек: экспорт и импорт (спецификация, UC-16…UC-18, S-6). */
public class ReportScenarioTest {

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
  private static final String REPORTS = "5";
  private static final String BACK = TestConstants.CHOICE_LEAVE;
  private static final String EXPORT_JSON = "1";
  private static final String EXPORT_CSV = "2";
  private static final String IMPORT = "3";
  private static final String ADD_INCOME = "1";
  private static final String ADD_EXPENSE = "2";
  private static final String CREATE_CATEGORY = "1";
  private static final String SET_BUDGET = "5";
  private static final String TYPE_INCOME = "1";
  private static final String TYPE_EXPENSE = "2";
  private static final String SKIP = "";
  private static final String YES = "yes";
  private static final String NO = "no";

  private static final List<String> LOGIN_ALICE = List.of(LOGIN, ALICE, ALICE_PASSWORD);
  private static final List<String> LOGIN_BOB = List.of(LOGIN, BOB, BOB_PASSWORD);

  /** Alice: категории Food и Salary, бюджет Food 4000, доход 5000 и расход 800 (баланс 4200). */
  private static final List<String> ALICE_DATA =
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

  private static final String ALICE_BALANCE = "| alice | Balance: 4,200.00 ===";
  private static final String BOB_BALANCE = "| bob | Balance: 4,200.00 ===";
  private static final String BOB_EMPTY_BALANCE = "| bob | Balance: 0.00 ===";
  private static final String CANCELLED = "Cancelled.";

  @TempDir private Path dataDir;

  private ScenarioApp app;
  private Path snapshot;

  @BeforeEach
  public void createApp() {
    app = new ScenarioApp(dataDir, USERS_FILE);
    snapshot = dataDir.resolve("out").resolve("snapshot.json");
  }

  @Test
  public void snapshotOfAliceImportedByBobReplacesBobWalletAndNotesTheOwner() {
    run(LOGIN_ALICE, ALICE_DATA, List.of(REPORTS, EXPORT_JSON, snapshot.toString(), BACK, LOGOUT));

    ScriptedConsoleIo bob =
        run(LOGIN_BOB, List.of(REPORTS, IMPORT, snapshot.toString(), YES, BACK));

    assertThat(snapshot).exists();
    assertThat(bob.output())
        .contains(BOB_EMPTY_BALANCE)
        .contains("Imported: 4 categories, 1 budgets, 2 operations.")
        .contains("Note: the snapshot belongs to \"alice\"")
        .contains("imported into the wallet of \"bob\"")
        .contains(BOB_BALANCE);
    assertThat(run(LOGIN_BOB).output()).contains(BOB_BALANCE);
  }

  @Test
  public void importOfOwnSnapshotHasNoOwnerNote() {
    run(LOGIN_ALICE, ALICE_DATA, List.of(REPORTS, EXPORT_JSON, snapshot.toString(), BACK));

    ScriptedConsoleIo io =
        run(LOGIN_ALICE, List.of(REPORTS, IMPORT, snapshot.toString(), YES, BACK));

    assertThat(io.output()).contains("Imported: 4 categories").doesNotContain("Note: the snapshot");
    assertThat(io.output()).contains(ALICE_BALANCE);
  }

  @Test
  public void exportWithoutPathGoesToTheExportsDirectoryUnderTheDefaultName() {
    ScriptedConsoleIo io = run(LOGIN_ALICE, List.of(REPORTS, EXPORT_JSON, SKIP, BACK));

    Path expected = dataDir.resolve("exports").resolve("alice-20261108-120000.json");
    assertThat(expected).exists();
    assertThat(io.output()).contains("Snapshot exported to " + expected + ".");
  }

  @Test
  public void existingFileIsReplacedOnlyAfterConfirmation() throws IOException {
    run(LOGIN_ALICE, List.of(REPORTS, EXPORT_JSON, snapshot.toString(), BACK));
    String emptyWalletSnapshot = Files.readString(snapshot);

    ScriptedConsoleIo declined =
        run(LOGIN_ALICE, ALICE_DATA, List.of(REPORTS, EXPORT_JSON, snapshot.toString(), NO, BACK));

    assertThat(declined.output()).contains("already exists. Overwrite it? (yes/no):");
    assertThat(declined.output()).contains(CANCELLED);
    assertThat(snapshot).hasContent(emptyWalletSnapshot);

    ScriptedConsoleIo accepted =
        run(LOGIN_ALICE, List.of(REPORTS, EXPORT_JSON, snapshot.toString(), YES, BACK));

    assertThat(accepted.output()).contains("Snapshot exported to " + snapshot + ".");
    assertThat(Files.readString(snapshot)).contains("Salary").contains("advance");
  }

  @Test
  public void csvContainsTheHeaderAndAllOperationsAndStartsWithBom() throws IOException {
    Path csv = dataDir.resolve("out").resolve("operations.csv");

    ScriptedConsoleIo io =
        run(LOGIN_ALICE, ALICE_DATA, List.of(REPORTS, EXPORT_CSV, csv.toString(), BACK));

    assertThat(io.output()).contains("Operations exported to " + csv + ".");
    String text = new String(Files.readAllBytes(csv), StandardCharsets.UTF_8);
    assertThat(text.charAt(0)).isEqualTo((char) 0xFEFF);
    assertThat(text)
        .contains("date;type;category;amount;counterparty;comment")
        .contains("01.11.2026;INCOME;Salary;5000.00;;advance")
        .contains("08.11.2026;EXPENSE;Food;800.00;;");
  }

  @Test
  public void csvOfAnEmptyWalletIsJustTheHeader() throws IOException {
    Path csv = dataDir.resolve("empty.csv");

    run(LOGIN_ALICE, List.of(REPORTS, EXPORT_CSV, csv.toString(), BACK));

    String text = new String(Files.readAllBytes(csv), StandardCharsets.UTF_8);
    assertThat(text.lines().count()).isEqualTo(1);
  }

  @Test
  public void snapshotWithAnUnknownCategoryIsRejectedAndTheWalletIsNotChanged()
      throws IOException {
    Path broken = dataDir.resolve("broken.json");
    Files.writeString(
        broken,
        "{\"version\": 1, \"operations\": [{\"type\": \"EXPENSE\", \"category\": \"Nowhere\", "
            + "\"amount\": \"5\", \"date\": \"2026-11-08\"}]}");

    ScriptedConsoleIo io =
        run(LOGIN_ALICE, ALICE_DATA, List.of(REPORTS, IMPORT, broken.toString(), YES, BACK));

    assertThat(io.output())
        .contains("Import rejected. Errors (first 1 shown):")
        .contains("Nowhere")
        .doesNotContain("Imported:");
    assertThat(run(LOGIN_ALICE).output()).contains(ALICE_BALANCE);
  }

  @Test
  public void malformedFileIsRejectedWithTheReason() throws IOException {
    Path broken = dataDir.resolve("garbage.json");
    Files.writeString(broken, "garbage");

    ScriptedConsoleIo io =
        run(LOGIN_ALICE, ALICE_DATA, List.of(REPORTS, IMPORT, broken.toString(), YES, BACK));

    assertThat(io.output()).contains("Import rejected.").doesNotContain("Imported:");
    assertThat(run(LOGIN_ALICE).output()).contains(ALICE_BALANCE);
  }

  @Test
  public void missingFileIsReportedAndTheWalletIsNotChanged() {
    Path missing = dataDir.resolve("missing.json");

    ScriptedConsoleIo io =
        run(LOGIN_ALICE, ALICE_DATA, List.of(REPORTS, IMPORT, missing.toString(), YES, BACK));

    assertThat(io.output()).contains("Cannot read the file \"" + missing + "\"");
    assertThat(run(LOGIN_ALICE).output()).contains(ALICE_BALANCE);
  }

  @Test
  public void declinedConfirmationLeavesTheWalletAlone() {
    run(LOGIN_ALICE, List.of(REPORTS, EXPORT_JSON, snapshot.toString(), BACK));

    ScriptedConsoleIo io =
        run(LOGIN_ALICE, ALICE_DATA, List.of(REPORTS, IMPORT, snapshot.toString(), NO, BACK));

    assertThat(io.output()).contains(CANCELLED).doesNotContain("Imported:");
    assertThat(run(LOGIN_ALICE).output()).contains(ALICE_BALANCE);
  }

  @Test
  public void importedWalletOverBudgetShowsTheAlert() throws IOException {
    Path overspent = dataDir.resolve("overspent.json");
    Files.writeString(
        overspent,
        "{\"version\": 1, \"categories\": [{\"name\": \"Food\", \"type\": \"EXPENSE\"}], "
            + "\"budgets\": [{\"category\": \"Food\", \"limit\": \"100\"}], "
            + "\"operations\": [{\"type\": \"EXPENSE\", \"category\": \"Food\", "
            + "\"amount\": \"250\", \"date\": \"2026-11-08\"}]}");

    ScriptedConsoleIo io =
        run(LOGIN_ALICE, List.of(REPORTS, IMPORT, overspent.toString(), YES, BACK));

    assertThat(io.output())
        .contains("Imported:")
        .contains("[!] Budget limit exceeded for category \"Food\"");
  }

  @Test
  public void everyDialogCanBeCancelled() {
    ScriptedConsoleIo io =
        run(
            LOGIN_ALICE,
            List.of(
                REPORTS,
                EXPORT_JSON, "cancel",
                EXPORT_CSV, "cancel",
                IMPORT, "cancel",
                BACK));

    assertThat(occurrences(io, CANCELLED)).isEqualTo(3);
  }

  @Test
  public void reportsMenuIsListedInTheMainMenu() {
    ScriptedConsoleIo io = run(LOGIN_ALICE, List.of(REPORTS, BACK));

    assertThat(io.output())
        .contains("5. Reports")
        .contains("=== Reports ===")
        .contains("1. Export JSON snapshot")
        .contains("2. Export operations to CSV")
        .contains("3. Import JSON snapshot");
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
