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

/** Сценарии среза 3.3 через весь стек: бюджеты, сводка, подсчёт по выбранным категориям. */
public class BudgetScenarioTest {

  private static final String ALICE = "alice";
  private static final String ALICE_PASSWORD = "alice-test-pw";
  private static final String USERS_FILE = ALICE + "=" + ALICE_PASSWORD + "\n";

  private static final String LOGIN = "1";
  private static final String OPERATIONS = "1";
  private static final String CATEGORIES = "2";
  private static final String STATISTICS = "3";
  private static final String BACK = TestConstants.CHOICE_LEAVE;
  private static final String ADD_INCOME = "1";
  private static final String ADD_EXPENSE = "2";
  private static final String CREATE_CATEGORY = "1";
  private static final String LIST_CATEGORIES = "2";
  private static final String SET_BUDGET = "5";
  private static final String REMOVE_BUDGET = "6";
  private static final String SUMMARY = "1";
  private static final String TOTALS = "2";
  private static final String TYPE_INCOME = "1";
  private static final String TYPE_EXPENSE = "2";
  private static final String SKIP = "";

  /** Шаги 1–3 примера из требований: категории, расходы, доходы. */
  private static final List<String> EXAMPLE_SETUP =
      List.of(
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

  /** Шаг 4 примера: бюджеты Food 4000, Entertainment 3000, Utilities 2500. */
  private static final List<String> EXAMPLE_BUDGETS =
      List.of(
          CATEGORIES,
          SET_BUDGET, "Food", "4000",
          SET_BUDGET, "Entertainment", "3000",
          SET_BUDGET, "Utilities", "2500",
          BACK);

  private static final String NO_OPERATIONS = "No operations found for the selected period.";
  private static final String CANCELLED = "Cancelled.";

  @TempDir private Path dataDir;

  private ScenarioApp app;

  @BeforeEach
  public void createApp() {
    app = new ScenarioApp(dataDir, USERS_FILE);
  }

  @Test
  public void exampleFromTheRequirementsGivesTheExpectedBudgetMessagesAndSummary() {
    ScriptedConsoleIo io =
        runAfter(EXAMPLE_SETUP, EXAMPLE_BUDGETS, List.of(STATISTICS, SUMMARY, SKIP, SKIP));

    assertThat(io.output())
        .contains("Budget for category \"Food\": 4,000.00. Spent 800.00, remaining 3,200.00.")
        .contains(
            "Budget for category \"Entertainment\": 3,000.00. Spent 3,000.00, remaining 0.00.")
        .contains(
            "Budget for category \"Utilities\": 2,500.00. Spent 3,000.00, remaining -500.00.")
        .containsSubsequence(
            "Total income: 63,000.00",
            "Total expenses: 8,300.00",
            "Balance: 54,700.00",
            "Income by category",
            "| Bonus",
            "3,000.00",
            "| Salary",
            "60,000.00",
            "Expenses by category",
            "| Entertainment",
            "| Food",
            "800.00",
            "| Taxi",
            "1,500.00",
            "| Utilities",
            "Budgets by category",
            "| Entertainment",
            "| Food",
            "3,200.00",
            "| Utilities",
            "-500.00");
  }

  @Test
  public void summaryTablesMatchTheRequirementsExactly() {
    ScriptedConsoleIo io =
        runAfter(EXAMPLE_SETUP, EXAMPLE_BUDGETS, List.of(STATISTICS, SUMMARY, SKIP, SKIP));

    String newLine = System.lineSeparator();
    String budgets =
        String.join(
            newLine,
            "Budgets by category",
            "+---------------+----------+----------+------------------+",
            "| Category      |    Limit |    Spent | Remaining budget |",
            "+---------------+----------+----------+------------------+",
            "| Entertainment | 3,000.00 | 3,000.00 |             0.00 |",
            "| Food          | 4,000.00 |   800.00 |         3,200.00 |",
            "| Utilities     | 2,500.00 | 3,000.00 |          -500.00 |",
            "+---------------+----------+----------+------------------+");
    assertThat(io.output()).contains(budgets);
  }

  @Test
  public void categoryListShowsBudgetLimitsOrDashInTheExpenseTable() {
    ScriptedConsoleIo io =
        runAfter(EXAMPLE_SETUP, EXAMPLE_BUDGETS, List.of(CATEGORIES, LIST_CATEGORIES));

    String newLine = System.lineSeparator();
    String expenses =
        String.join(
            newLine,
            "Expense categories",
            "+--------------------+----------+",
            "| Category           |   Budget |",
            "+--------------------+----------+",
            "| Entertainment      | 3,000.00 |",
            "| Food               | 4,000.00 |",
            "| Taxi               |        - |",
            "| Transfers (system) |        - |",
            "| Utilities          | 2,500.00 |",
            "+--------------------+----------+");
    assertThat(io.output()).contains(expenses);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "Salary | A budget can only be set for an expense category.",
        "Cinema | Category \"Cinema\" not found. Available categories: Entertainment, Food, "
            + "Taxi, Transfers, Utilities.",
      })
  public void budgetForUnsuitableCategoryIsRejected(String category, String message) {
    ScriptedConsoleIo io = runAfter(EXAMPLE_SETUP, List.of(CATEGORIES, SET_BUDGET, category, "10"));

    assertThat(io.output()).contains(message).doesNotContain("Budget for category");
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "0 | The budget limit must be greater than zero.",
        "-1 | The budget limit must be greater than zero.",
        "10.999 | The amount cannot have more than two decimal places.",
        "1000000000 | The amount cannot exceed 999,999,999.99."
      })
  public void invalidLimitEndsTheDialogWithMessage(String limit, String message) {
    ScriptedConsoleIo io = runAfter(EXAMPLE_SETUP, List.of(CATEGORIES, SET_BUDGET, "Food", limit));

    assertThat(io.output()).contains(message).doesNotContain("Budget for category");
  }

  @Test
  public void limitQuestionIsAskedAgainWhenFormatIsWrong() {
    ScriptedConsoleIo io =
        runAfter(EXAMPLE_SETUP, List.of(CATEGORIES, SET_BUDGET, "Food", "abc", "100"));

    assertThat(io.output())
        .contains("Cannot recognize the amount \"abc\".")
        .contains("Budget for category \"Food\": 100.00. Spent 800.00, remaining -700.00.");
  }

  @Test
  public void settingBudgetAgainReplacesTheOldOne() {
    ScriptedConsoleIo io =
        runAfter(
            EXAMPLE_SETUP,
            EXAMPLE_BUDGETS,
            List.of(CATEGORIES, SET_BUDGET, "food", "5000", LIST_CATEGORIES));

    assertThat(io.output())
        .contains("Budget for category \"Food\": 5,000.00.")
        .contains("| Food               | 5,000.00 |")
        .doesNotContain("| Food               | 4,000.00 |");
  }

  @Test
  public void budgetIsRemovedAfterConfirmation() {
    ScriptedConsoleIo io =
        runAfter(
            EXAMPLE_SETUP,
            EXAMPLE_BUDGETS,
            List.of(CATEGORIES, REMOVE_BUDGET, "Food", "yes", LIST_CATEGORIES));

    assertThat(io.output())
        .contains("Delete the budget of category \"Food\"? (yes/no): ")
        .contains("Budget of category \"Food\" deleted.")
        .contains("| Food               |        - |");
  }

  @Test
  public void declinedConfirmationKeepsTheBudget() {
    ScriptedConsoleIo io =
        runAfter(
            EXAMPLE_SETUP,
            EXAMPLE_BUDGETS,
            List.of(CATEGORIES, REMOVE_BUDGET, "Food", "no", LIST_CATEGORIES));

    assertThat(io.output())
        .contains(CANCELLED)
        .doesNotContain("deleted.")
        .contains("| Food               | 4,000.00 |");
  }

  @Test
  public void confirmationIsAskedAgainForAnswersOtherThanYesAndNo() {
    ScriptedConsoleIo io =
        runAfter(
            EXAMPLE_SETUP,
            EXAMPLE_BUDGETS,
            List.of(CATEGORIES, REMOVE_BUDGET, "Food", "maybe", "yes"));

    assertThat(io.output())
        .contains("Enter \"yes\" or \"no\".")
        .contains("Budget of category \"Food\" deleted.");
  }

  @Test
  public void removingMissingBudgetOrCategoryIsReported() {
    ScriptedConsoleIo io =
        runAfter(
            EXAMPLE_SETUP,
            List.of(
                CATEGORIES,
                REMOVE_BUDGET, "Food", "yes",
                REMOVE_BUDGET, "Cinema", "yes"));

    assertThat(io.output())
        .contains("No budget is set for category \"Food\".")
        .contains("Category \"Cinema\" not found.");
  }

  @Test
  public void summaryWithPeriodShowsItAndFiltersTotalsButNotBalanceAndBudgets() {
    ScriptedConsoleIo io =
        runAfter(
            List.of(
                CATEGORIES,
                CREATE_CATEGORY, TYPE_EXPENSE, "Food",
                CREATE_CATEGORY, TYPE_INCOME, "Salary",
                SET_BUDGET, "Food", "1000",
                BACK,
                OPERATIONS,
                ADD_INCOME, "Salary", "1000", "01.11.2026", SKIP,
                ADD_EXPENSE, "Food", "100", "05.11.2026", SKIP,
                ADD_EXPENSE, "Food", "40", "08.11.2026", SKIP,
                BACK),
            List.of(STATISTICS, SUMMARY, "05.11.2026", "07.11.2026"));

    String output = io.output();
    int summaryStart = output.lastIndexOf("Period: 05.11.2026 - 07.11.2026");
    assertThat(summaryStart).isPositive();
    assertThat(output.substring(summaryStart))
        .contains("Total income: 0.00")
        .contains("Total expenses: 100.00")
        .contains("Balance: 860.00")
        .doesNotContain("Income by category")
        .contains("Expenses by category")
        .contains("| Food")
        .contains("1,000.00");
  }

  @Test
  public void summaryWithoutOperationsInPeriodShowsMessageAndKeepsBudgets() {
    ScriptedConsoleIo io =
        runAfter(
            EXAMPLE_SETUP,
            EXAMPLE_BUDGETS,
            List.of(STATISTICS, SUMMARY, "01.01.2020", "02.01.2020"));

    String output = io.output();
    String summary = output.substring(output.lastIndexOf("Period: 01.01.2020 - 02.01.2020"));
    assertThat(summary)
        .contains(NO_OPERATIONS)
        .doesNotContain("Income by category")
        .contains("Budgets by category")
        .contains("Balance: 54,700.00");
  }

  @Test
  public void reversedPeriodIsRejectedForSummary() {
    ScriptedConsoleIo io = runAfter(List.of(STATISTICS, SUMMARY, "08.11.2026", "01.11.2026"));

    assertThat(io.output()).contains("The start of the period cannot be later than its end.");
  }

  @Test
  public void totalsForSelectedCategoriesWarnAboutMissingOnes() {
    ScriptedConsoleIo io =
        runAfter(EXAMPLE_SETUP, List.of(STATISTICS, TOTALS, "Food, Cinema, Salary", SKIP, SKIP));

    String newLine = System.lineSeparator();
    String expected =
        String.join(
            newLine,
            "Period: all time",
            "+----------+---------+-----------+",
            "| Category | Type    |    Amount |",
            "+----------+---------+-----------+",
            "| Food     | expense |    800.00 |",
            "| Salary   | income  | 60,000.00 |",
            "+----------+---------+-----------+",
            "Total income for selected categories: 60,000.00",
            "Total expenses for selected categories: 800.00",
            "[!] Categories not found: Cinema. Totals calculated for the rest.");
    assertThat(io.output()).contains(expected);
  }

  @Test
  public void totalsForOnlyMissingCategoriesReportThatNoneWasFound() {
    ScriptedConsoleIo io =
        runAfter(EXAMPLE_SETUP, List.of(STATISTICS, TOTALS, "Cinema", SKIP, SKIP));

    assertThat(io.output()).contains("[!] None of the specified categories was found.");
  }

  @Test
  public void totalsCountRepeatedNamesOnce() {
    ScriptedConsoleIo io =
        runAfter(EXAMPLE_SETUP, List.of(STATISTICS, TOTALS, "Food, food,FOOD", SKIP, SKIP));

    assertThat(io.output())
        .contains("Total expenses for selected categories: 800.00")
        .doesNotContain("1,600.00");
  }

  @Test
  public void totalsQuestionIsAskedAgainWhenNoNamesRemain() {
    ScriptedConsoleIo io =
        runAfter(EXAMPLE_SETUP, List.of(STATISTICS, TOTALS, " , ,", "Taxi", SKIP, SKIP));

    assertThat(io.output())
        .contains("The value must not be empty.")
        .contains("Total expenses for selected categories: 1,500.00");
  }

  @Test
  public void totalsRespectThePeriodAndReportWhenNothingMatches() {
    ScriptedConsoleIo io =
        runAfter(
            EXAMPLE_SETUP, List.of(STATISTICS, TOTALS, "Food", "01.01.2020", "02.01.2020"));

    String output = io.output();
    assertThat(output.substring(output.lastIndexOf("Category names, separated by commas")))
        .contains(NO_OPERATIONS)
        .doesNotContain("Total expenses for selected categories");
  }

  @Test
  public void totalsDialogCanBeCancelled() {
    ScriptedConsoleIo io = runAfter(List.of(STATISTICS, TOTALS, "cancel"));

    assertThat(occurrences(io, CANCELLED)).isEqualTo(1);
  }

  @SafeVarargs
  private ScriptedConsoleIo runAfter(List<String>... parts) {
    List<String> input = new ArrayList<>(List.of(LOGIN, ALICE, ALICE_PASSWORD));
    Arrays.stream(parts).forEach(input::addAll);
    ScriptedConsoleIo io = new ScriptedConsoleIo(input.toArray(new String[0]));
    app.run(io);
    return io;
  }

  private int occurrences(ScriptedConsoleIo io, String part) {
    return io.output().split(Pattern.quote(part), -1).length - 1;
  }
}
