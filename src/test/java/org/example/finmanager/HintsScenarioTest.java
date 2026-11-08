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

/** Сценарии среза 3.9: подсказки и примеры в диалогах, справка. */
public class HintsScenarioTest {

  private static final String ALICE = "alice";
  private static final String ALICE_PASSWORD = "alice-test-pw";
  private static final String USERS_FILE = ALICE + "=" + ALICE_PASSWORD + "\n";

  private static final String LOGIN = "1";
  private static final String GUEST_HELP = "2";
  private static final String OPERATIONS = "1";
  private static final String CATEGORIES = "2";
  private static final String BACK = TestConstants.CHOICE_LEAVE;
  private static final String ADD_INCOME = "1";
  private static final String ADD_EXPENSE = "2";
  private static final String CREATE_CATEGORY = "1";
  private static final String RENAME_CATEGORY = "3";
  private static final String DELETE_CATEGORY = "4";
  private static final String SET_BUDGET = "5";
  private static final String REMOVE_BUDGET = "6";
  private static final String TYPE_INCOME = "1";
  private static final String TYPE_EXPENSE = "2";
  private static final String CANCEL = "cancel";

  private static final List<String> SIGN_IN_AND_CREATE_CATEGORIES =
      List.of(
          LOGIN,
          ALICE,
          ALICE_PASSWORD,
          CATEGORIES,
          CREATE_CATEGORY,
          TYPE_EXPENSE,
          "Food",
          CREATE_CATEGORY,
          TYPE_INCOME,
          "Salary",
          BACK);

  private static final String EXPENSE_HINT = "Available categories: Food, Transfers.";
  private static final String INCOME_HINT = "Available categories: Salary, Transfers.";
  private static final String HINT_PART = "Available categories";
  private static final String CATEGORY_PROMPT = "Category: ";
  private static final String AMOUNT_PROMPT = "Amount (example: 1500.50):";
  private static final String DATE_PROMPT = "Date (dd.mm.yyyy, Enter - today):";
  private static final String LIMIT_PROMPT = "Limit (example: 4000):";

  @TempDir private Path dataDir;

  private ScenarioApp app;

  @BeforeEach
  public void createApp() {
    app = new ScenarioApp(dataDir, USERS_FILE);
  }

  @Test
  public void operationDialogsListTheCategoriesOfTheirTypeBeforeTheQuestion() {
    ScriptedConsoleIo io = signedIn(OPERATIONS, ADD_INCOME, CANCEL, ADD_EXPENSE, CANCEL);

    String incomeQuestion = INCOME_HINT + System.lineSeparator() + CATEGORY_PROMPT;
    String expenseQuestion = EXPENSE_HINT + System.lineSeparator() + CATEGORY_PROMPT;
    assertThat(io.output()).contains(incomeQuestion).contains(expenseQuestion);
    assertThat(occurrences(io, INCOME_HINT)).isEqualTo(1);
    assertThat(occurrences(io, EXPENSE_HINT)).isEqualTo(1);
  }

  @Test
  public void budgetAndCategoryDialogsListCategoriesToo() {
    ScriptedConsoleIo io =
        signedIn(
            CATEGORIES,
            SET_BUDGET,
            CANCEL,
            REMOVE_BUDGET,
            CANCEL,
            RENAME_CATEGORY,
            TYPE_INCOME,
            CANCEL,
            DELETE_CATEGORY,
            TYPE_EXPENSE,
            CANCEL);

    assertThat(occurrences(io, EXPENSE_HINT)).isEqualTo(3);
    assertThat(occurrences(io, INCOME_HINT)).isEqualTo(1);
  }

  @Test
  public void renameDialogShowsNoHintUntilTheTypeIsKnown() {
    ScriptedConsoleIo io = signedIn(CATEGORIES, RENAME_CATEGORY, CANCEL);

    assertThat(io.output()).doesNotContain(HINT_PART);
  }

  @Test
  public void amountAndDateQuestionsCarryExamplesOfTheExpectedFormat() {
    ScriptedConsoleIo io = signedIn(OPERATIONS, ADD_EXPENSE, "Food", "1500", CANCEL);

    assertThat(io.output()).contains(AMOUNT_PROMPT).contains(DATE_PROMPT);
  }

  @Test
  public void budgetLimitQuestionCarriesAnExample() {
    ScriptedConsoleIo io = signedIn(CATEGORIES, SET_BUDGET, "Food", CANCEL);

    assertThat(io.output()).contains(LIMIT_PROMPT);
  }

  @Test
  public void helpDescribesFormatsReportsAndHints() {
    ScriptedConsoleIo io = run(GUEST_HELP);

    assertThat(io.output())
        .contains("Period: two dates")
        .contains("\"exports\" folder")
        .contains("Import replaces the whole wallet")
        .contains("available categories are shown");
  }

  private ScriptedConsoleIo signedIn(String... actions) {
    List<String> input = new ArrayList<>(SIGN_IN_AND_CREATE_CATEGORIES);
    input.addAll(Arrays.asList(actions));
    return run(input.toArray(String[]::new));
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
