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

/** Сценарии среза 3.4 через весь стек: оповещения A-1 и A-2 в нужные моменты. */
public class AlertScenarioTest {

  private static final String ALICE = "alice";
  private static final String ALICE_PASSWORD = "alice-test-pw";
  private static final String USERS_FILE = ALICE + "=" + ALICE_PASSWORD + "\n";

  private static final String LOGIN = "1";
  private static final String LOGOUT = "7";
  private static final String OPERATIONS = "1";
  private static final String CATEGORIES = "2";
  private static final String STATISTICS = "3";
  private static final String BACK = TestConstants.CHOICE_LEAVE;
  private static final String ADD_INCOME = "1";
  private static final String ADD_EXPENSE = "2";
  private static final String CREATE_CATEGORY = "1";
  private static final String SET_BUDGET = "5";
  private static final String SUMMARY = "1";
  private static final String TYPE_INCOME = "1";
  private static final String TYPE_EXPENSE = "2";
  private static final String SKIP = "";

  private static final String UTILITIES_ALERT =
      "[!] Budget limit exceeded for category \"Utilities\": spent 3,000.00 against a limit of "
          + "2,500.00 (over by 500.00).";
  private static final String IMBALANCE_MARKER = "[!] Expenses (";

  private static final List<String> CATEGORIES_OF_THE_EXAMPLE =
      List.of(
          CATEGORIES,
          CREATE_CATEGORY, TYPE_EXPENSE, "Food",
          CREATE_CATEGORY, TYPE_EXPENSE, "Entertainment",
          CREATE_CATEGORY, TYPE_EXPENSE, "Utilities",
          CREATE_CATEGORY, TYPE_EXPENSE, "Taxi",
          CREATE_CATEGORY, TYPE_INCOME, "Salary",
          CREATE_CATEGORY, TYPE_INCOME, "Bonus",
          BACK);

  private static final List<String> EXPENSES_OF_THE_EXAMPLE =
      List.of(
          OPERATIONS,
          ADD_EXPENSE, "Food", "300", SKIP, SKIP,
          ADD_EXPENSE, "Food", "500", SKIP, SKIP,
          ADD_EXPENSE, "Entertainment", "3000", SKIP, SKIP,
          ADD_EXPENSE, "Utilities", "3000", SKIP, SKIP,
          ADD_EXPENSE, "Taxi", "1500", SKIP, SKIP);

  private static final List<String> INCOMES_OF_THE_EXAMPLE =
      List.of(
          ADD_INCOME, "Salary", "20000", SKIP, SKIP,
          ADD_INCOME, "Salary", "40000", SKIP, SKIP,
          ADD_INCOME, "Bonus", "3000", SKIP, SKIP,
          BACK);

  private static final List<String> BUDGETS_OF_THE_EXAMPLE =
      List.of(
          CATEGORIES,
          SET_BUDGET, "Food", "4000",
          SET_BUDGET, "Entertainment", "3000",
          SET_BUDGET, "Utilities", "2500",
          BACK);

  @TempDir private Path dataDir;

  private ScenarioApp app;

  @BeforeEach
  public void createApp() {
    app = new ScenarioApp(dataDir, USERS_FILE);
  }

  @Test
  public void everyExpenseBeforeAnyIncomeWarnsThatExpensesExceedIncome() {
    ScriptedConsoleIo io = run(CATEGORIES_OF_THE_EXAMPLE, EXPENSES_OF_THE_EXAMPLE);

    assertThat(io.output())
        .containsSubsequence(
            "[!] Expenses (300.00) exceed income (0.00).",
            "[!] Expenses (800.00) exceed income (0.00).",
            "[!] Expenses (3,800.00) exceed income (0.00).",
            "[!] Expenses (6,800.00) exceed income (0.00).",
            "[!] Expenses (8,300.00) exceed income (0.00).");
    assertThat(occurrences(io, IMBALANCE_MARKER)).isEqualTo(5);
  }

  @Test
  public void incomeThatCoversTheExpensesAddsNoImbalanceAlert() {
    ScriptedConsoleIo io =
        run(CATEGORIES_OF_THE_EXAMPLE, EXPENSES_OF_THE_EXAMPLE, INCOMES_OF_THE_EXAMPLE);

    assertThat(occurrences(io, IMBALANCE_MARKER)).isEqualTo(5);
  }

  @Test
  public void incomeThatDoesNotCoverTheExpensesStillWarns() {
    ScriptedConsoleIo io =
        run(
            CATEGORIES_OF_THE_EXAMPLE,
            List.of(
                OPERATIONS,
                ADD_EXPENSE, "Food", "100", SKIP, SKIP,
                ADD_INCOME, "Salary", "30", SKIP, SKIP));

    assertThat(io.output())
        .containsSubsequence(
            "[!] Expenses (100.00) exceed income (0.00).",
            "Income added: Salary, 30.00",
            "[!] Expenses (100.00) exceed income (30.00).");
  }

  @Test
  public void onlyTheBudgetOfUtilitiesIsExceededWhileBudgetsAreSet() {
    ScriptedConsoleIo io =
        run(
            CATEGORIES_OF_THE_EXAMPLE,
            EXPENSES_OF_THE_EXAMPLE,
            INCOMES_OF_THE_EXAMPLE,
            BUDGETS_OF_THE_EXAMPLE);

    assertThat(occurrences(io, UTILITIES_ALERT)).isEqualTo(1);
    assertThat(occurrences(io, "Budget limit exceeded")).isEqualTo(1);
    assertThat(io.output())
        .containsSubsequence(
            "Budget for category \"Utilities\": 2,500.00. Spent 3,000.00, remaining -500.00.",
            UTILITIES_ALERT);
  }

  @Test
  public void summaryEndsWithTheBudgetAlertOfTheExample() {
    ScriptedConsoleIo io =
        run(
            CATEGORIES_OF_THE_EXAMPLE,
            EXPENSES_OF_THE_EXAMPLE,
            INCOMES_OF_THE_EXAMPLE,
            BUDGETS_OF_THE_EXAMPLE,
            List.of(STATISTICS, SUMMARY, SKIP, SKIP));

    String newLine = System.lineSeparator();
    assertThat(io.output())
        .contains(
            "| Utilities     | 2,500.00 | 3,000.00 |          -500.00 |"
                + newLine
                + "+---------------+----------+----------+------------------+"
                + newLine
                + newLine
                + UTILITIES_ALERT);
    assertThat(occurrences(io, UTILITIES_ALERT)).isEqualTo(2);
    assertThat(occurrences(io, IMBALANCE_MARKER)).isEqualTo(5);
  }

  @Test
  public void expenseOverBudgetWarnsImmediately() {
    ScriptedConsoleIo io =
        run(
            CATEGORIES_OF_THE_EXAMPLE,
            List.of(
                OPERATIONS, ADD_INCOME, "Salary", "1000", SKIP, SKIP, BACK,
                CATEGORIES, SET_BUDGET, "Food", "100", BACK,
                OPERATIONS, ADD_EXPENSE, "Food", "150", SKIP, SKIP));

    assertThat(io.output())
        .contains(
            "[!] Budget limit exceeded for category \"Food\": spent 150.00 against a limit of "
                + "100.00 (over by 50.00).")
        .doesNotContain(IMBALANCE_MARKER);
  }

  @Test
  public void loweringTheLimitBelowSpentWarnsAndRaisingItDoesNot() {
    ScriptedConsoleIo io =
        run(
            CATEGORIES_OF_THE_EXAMPLE,
            List.of(
                OPERATIONS, ADD_INCOME, "Salary", "1000", SKIP, SKIP,
                ADD_EXPENSE, "Food", "300", SKIP, SKIP, BACK,
                CATEGORIES, SET_BUDGET, "Food", "200",
                SET_BUDGET, "Food", "300",
                SET_BUDGET, "Food", "400"));

    assertThat(occurrences(io, "Budget limit exceeded")).isEqualTo(1);
    assertThat(io.output()).contains("(over by 100.00)");
  }

  @Test
  public void noAlertsAreShownAtLogin() {
    ScriptedConsoleIo io =
        run(
            CATEGORIES_OF_THE_EXAMPLE,
            EXPENSES_OF_THE_EXAMPLE,
            List.of(BACK, LOGOUT, LOGIN, ALICE, ALICE_PASSWORD));

    assertThat(occurrences(io, IMBALANCE_MARKER)).isEqualTo(5);
  }

  @SafeVarargs
  private ScriptedConsoleIo run(List<String>... parts) {
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
