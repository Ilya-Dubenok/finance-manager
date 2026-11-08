package org.example.finmanager;

import static org.assertj.core.api.Assertions.assertThat;

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

/** Сценарии среза 3.2 через весь стек: категории, доходы, расходы, список операций, баланс. */
public class WalletScenarioTest {

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
  private static final String BACK = TestConstants.CHOICE_LEAVE;
  private static final String ADD_INCOME = "1";
  private static final String ADD_EXPENSE = "2";
  private static final String LIST_OPERATIONS = "3";
  private static final String CREATE_CATEGORY = "1";
  private static final String LIST_CATEGORIES = "2";
  private static final String TYPE_INCOME = "1";
  private static final String TYPE_EXPENSE = "2";
  private static final String SKIP = "";

  private static final List<String> CREATE_FOOD =
      List.of(CATEGORIES, CREATE_CATEGORY, TYPE_EXPENSE, "Food", BACK);
  private static final List<String> CREATE_FOOD_AND_SALARY =
      List.of(
          CATEGORIES,
          CREATE_CATEGORY,
          TYPE_EXPENSE,
          "Food",
          CREATE_CATEGORY,
          TYPE_INCOME,
          "Salary",
          BACK);

  private static final String TODAY_TEXT = "08.11.2026";
  private static final String CANCELLED = "Cancelled.";
  private static final String NO_OPERATIONS = "No operations found for the selected period.";

  @TempDir private Path dataDir;

  private ScenarioApp app;

  @BeforeEach
  public void createApp() {
    app = new ScenarioApp(dataDir, USERS_FILE);
  }

  @Test
  public void newUserStartsWithZeroBalance() {
    ScriptedConsoleIo io = run();

    assertThat(io.output()).contains(mainTitle(ALICE, "0.00"));
  }

  @Test
  public void createsCategoriesAndListsThemAlphabeticallyWithSystemOnesMarked() {
    ScriptedConsoleIo io =
        run(
            CATEGORIES,
            CREATE_CATEGORY,
            TYPE_EXPENSE,
            "Food",
            CREATE_CATEGORY,
            TYPE_INCOME,
            "Salary",
            CREATE_CATEGORY,
            TYPE_EXPENSE,
            "apple",
            LIST_CATEGORIES);

    assertThat(io.output())
        .contains("Category \"Food\" (expense) created.")
        .contains("Category \"Salary\" (income) created.")
        .containsSubsequence(
            "Income categories",
            "| Salary",
            "| Transfers (system)",
            "Expense categories",
            "| apple",
            "| Food",
            "| Transfers (system)");
  }

  @Test
  public void categoryDialogReportsDuplicatesSystemNamesAndBadNames() {
    ScriptedConsoleIo io =
        run(
            CATEGORIES,
            CREATE_CATEGORY,
            TYPE_EXPENSE,
            "Food",
            CREATE_CATEGORY,
            TYPE_EXPENSE,
            "food",
            CREATE_CATEGORY,
            TYPE_INCOME,
            "TRANSFERS",
            CREATE_CATEGORY,
            TYPE_EXPENSE,
            "a,b",
            CREATE_CATEGORY,
            TYPE_EXPENSE,
            "n".repeat(51),
            CREATE_CATEGORY,
            TYPE_INCOME,
            "Food");

    assertThat(io.output())
        .contains("Category \"food\" (expense) already exists.")
        .contains(
            "The \"Transfers\" category is a system category: "
                + "it cannot be created, renamed or deleted.")
        .contains("A category name cannot contain a comma or control characters.")
        .contains("The value is too long: at most 50 characters.")
        .contains("Category \"Food\" (income) created.");
  }

  @Test
  public void categoryTypeIsAskedAgainUntilItIsOneOrTwo() {
    ScriptedConsoleIo io = run(CATEGORIES, CREATE_CATEGORY, "3", TYPE_EXPENSE, "Food");

    assertThat(io.output())
        .contains("Choose 1 (income) or 2 (expense).")
        .contains("Category \"Food\" (expense) created.");
  }

  @Test
  public void categoryDialogCanBeCancelledAtAnyQuestion() {
    ScriptedConsoleIo io =
        run(
            CATEGORIES,
            CREATE_CATEGORY,
            "cancel",
            CREATE_CATEGORY,
            TYPE_EXPENSE,
            "cancel");

    assertThat(occurrences(io, CANCELLED)).isEqualTo(2);
    assertThat(io.output()).doesNotContain("created.");
  }

  @Test
  public void exampleFromTheRequirementsGivesTheExpectedBalance() {
    ScriptedConsoleIo io =
        run(
            CATEGORIES,
            CREATE_CATEGORY, TYPE_EXPENSE, "Food",
            CREATE_CATEGORY, TYPE_EXPENSE, "Entertainment",
            CREATE_CATEGORY, TYPE_EXPENSE, "Utilities",
            CREATE_CATEGORY, TYPE_EXPENSE, "Taxi",
            CREATE_CATEGORY, TYPE_INCOME, "Salary",
            CREATE_CATEGORY, TYPE_INCOME, "Bonus",
            BACK,
            OPERATIONS,
            ADD_EXPENSE, "Food", "300", SKIP, SKIP,
            ADD_EXPENSE, "Food", "500", SKIP, SKIP,
            ADD_EXPENSE, "Entertainment", "3000", SKIP, SKIP,
            ADD_EXPENSE, "Utilities", "3000", SKIP, SKIP,
            ADD_EXPENSE, "Taxi", "1500", SKIP, SKIP,
            ADD_INCOME, "Salary", "20000", SKIP, SKIP,
            ADD_INCOME, "Salary", "40000", SKIP, SKIP,
            ADD_INCOME, "Bonus", "3000", SKIP, SKIP,
            BACK);

    assertThat(io.output())
        .contains("Expense added: Food, 300.00 (" + TODAY_TEXT + ").")
        .contains("Expense added: Taxi, 1,500.00 (" + TODAY_TEXT + ").")
        .contains("Income added: Salary, 40,000.00 (" + TODAY_TEXT + ").")
        .contains("Income added: Bonus, 3,000.00 (" + TODAY_TEXT + ").")
        .containsSubsequence(mainTitle(ALICE, "0.00"), mainTitle(ALICE, "54,700.00"));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "0 | The amount must be greater than zero.",
        "-5 | The amount must be greater than zero.",
        "10.999 | The amount cannot have more than two decimal places.",
        "1000000000 | The amount cannot exceed 999,999,999.99."
      })
  public void businessRulesForAmountEndTheDialogWithMessage(String amount, String message) {
    ScriptedConsoleIo io =
        runAfter(CREATE_FOOD, OPERATIONS, ADD_EXPENSE, "Food", amount, SKIP, SKIP);

    assertThat(io.output()).contains(message).doesNotContain("Expense added");
    assertThat(occurrences(io, "=== Operations ===")).isEqualTo(2);
  }

  @Test
  public void amountQuestionIsAskedAgainWhenFormatIsWrong() {
    ScriptedConsoleIo io =
        runAfter(CREATE_FOOD, OPERATIONS, ADD_EXPENSE, "Food", "abc", "1e3", "1500,5", SKIP, SKIP);

    assertThat(io.output())
        .contains("Cannot recognize the amount \"abc\". Example: 1500 or 1500.50.")
        .contains("Cannot recognize the amount \"1e3\".")
        .contains("Expense added: Food, 1,500.50 (" + TODAY_TEXT + ").");
  }

  @Test
  public void dateQuestionIsAskedAgainWhenFormatIsWrong() {
    ScriptedConsoleIo io =
        runAfter(CREATE_FOOD, OPERATIONS, ADD_EXPENSE, "Food", "5", "2030-01-01", SKIP, SKIP);

    assertThat(io.output())
        .contains("Cannot recognize the date \"2030-01-01\". Format: dd.mm.yyyy")
        .contains("Expense added: Food, 5.00 (" + TODAY_TEXT + ").");
  }

  @Test
  public void futureDateIsRejectedAndGivenDateIsUsed() {
    ScriptedConsoleIo io =
        runAfter(
            CREATE_FOOD,
            OPERATIONS,
            ADD_EXPENSE, "Food", "5", "09.11.2026", SKIP,
            ADD_EXPENSE, "Food", "6", "05.11.2026", SKIP);

    assertThat(io.output())
        .contains("The operation date cannot be in the future.")
        .contains("Expense added: Food, 6.00 (05.11.2026).")
        .doesNotContain("Expense added: Food, 5.00");
  }

  @Test
  public void unknownCategoryIsReportedWithAvailableOnes() {
    ScriptedConsoleIo io =
        runAfter(CREATE_FOOD, OPERATIONS, ADD_EXPENSE, "Taxi", "5", SKIP, SKIP);

    assertThat(io.output())
        .contains("Category \"Taxi\" not found. Available categories: Food, Transfers.");
  }

  @Test
  public void categoryOfTheOtherTypeIsNotFound() {
    ScriptedConsoleIo io =
        runAfter(CREATE_FOOD, OPERATIONS, ADD_INCOME, "Food", "5", SKIP, SKIP);

    assertThat(io.output())
        .contains("Category \"Food\" not found. Available categories: Transfers.");
  }

  @Test
  public void operationDialogCanBeCancelled() {
    ScriptedConsoleIo io =
        runAfter(
            CREATE_FOOD,
            OPERATIONS,
            ADD_EXPENSE, "Food", "cancel",
            LIST_OPERATIONS, SKIP, SKIP);

    assertThat(io.output()).contains(CANCELLED).contains(NO_OPERATIONS);
  }

  @Test
  public void listsOperationsByDateWithShortenedComments() {
    ScriptedConsoleIo io =
        runAfter(
            CREATE_FOOD_AND_SALARY,
            OPERATIONS,
            ADD_EXPENSE, "Food", "300", "05.11.2026", "lunch",
            ADD_INCOME, "Salary", "1000", "01.11.2026", SKIP,
            ADD_EXPENSE, "Food", "40", "08.11.2026", "x".repeat(40),
            LIST_OPERATIONS, SKIP, SKIP);

    assertThat(io.output())
        .contains("| Counterparty | Comment")
        .contains("1,000.00")
        .contains("x".repeat(27) + "...")
        .doesNotContain("x".repeat(28))
        .containsSubsequence("| 1 | 01.11.2026", "| 2 | 05.11.2026", "| 3 | 08.11.2026");
  }

  @Test
  public void periodFiltersTheListInclusively() {
    ScriptedConsoleIo io =
        runAfter(
            CREATE_FOOD,
            OPERATIONS,
            ADD_EXPENSE, "Food", "1", "01.11.2026", SKIP,
            ADD_EXPENSE, "Food", "2", "05.11.2026", SKIP,
            ADD_EXPENSE, "Food", "3", "08.11.2026", SKIP,
            LIST_OPERATIONS, "05.11.2026", "05.11.2026");

    assertThat(io.output()).contains("| 1 | 05.11.2026").doesNotContain("| 2 |");
  }

  @Test
  public void emptyResultIsReportedInsteadOfAnEmptyTable() {
    ScriptedConsoleIo io = runAfter(CREATE_FOOD, OPERATIONS, LIST_OPERATIONS, SKIP, SKIP);

    assertThat(io.output()).contains(NO_OPERATIONS).doesNotContain("| Counterparty");
  }

  @Test
  public void reversedPeriodIsRejected() {
    ScriptedConsoleIo io =
        runAfter(CREATE_FOOD, OPERATIONS, LIST_OPERATIONS, "08.11.2026", "01.11.2026");

    assertThat(io.output()).contains("The start of the period cannot be later than its end.");
  }

  @Test
  public void walletSurvivesLogoutAndLoginAndWalletsAreIndependentPerUser() {
    ScriptedConsoleIo io =
        runAfter(
            CREATE_FOOD,
            OPERATIONS,
            ADD_EXPENSE, "Food", "100", SKIP, SKIP,
            BACK,
            LOGOUT,
            LOGIN, ALICE, ALICE_PASSWORD,
            LOGOUT,
            LOGIN, BOB, BOB_PASSWORD);

    assertThat(occurrences(io, mainTitle(ALICE, "-100.00"))).isEqualTo(2);
    assertThat(io.output()).contains(mainTitle(BOB, "0.00"));
  }

  private ScriptedConsoleIo run(String... steps) {
    return runAfter(List.of(), steps);
  }

  private ScriptedConsoleIo runAfter(List<String> setup, String... steps) {
    List<String> input = new ArrayList<>(List.of(LOGIN, ALICE, ALICE_PASSWORD));
    input.addAll(setup);
    input.addAll(Arrays.asList(steps));
    ScriptedConsoleIo io = new ScriptedConsoleIo(input.toArray(new String[0]));
    app.run(io);
    return io;
  }

  private String mainTitle(String login, String balance) {
    return "=== Finance Manager | " + login + " | Balance: " + balance + " ===";
  }

  private int occurrences(ScriptedConsoleIo io, String part) {
    return io.output().split(Pattern.quote(part), -1).length - 1;
  }
}
