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

/** Сценарии среза 3.7 через весь стек: переименование и удаление категорий. */
public class EditingScenarioTest {

  private static final String ALICE = "alice";
  private static final String ALICE_PASSWORD = "alice-test-pw";
  private static final String USERS_FILE = ALICE + "=" + ALICE_PASSWORD + "\n";

  private static final String LOGIN = "1";
  private static final String OPERATIONS = "1";
  private static final String CATEGORIES = "2";
  private static final String STATISTICS = "3";
  private static final String BACK = TestConstants.CHOICE_LEAVE;
  private static final String ADD_EXPENSE = "2";
  private static final String LIST_OPERATIONS = "3";
  private static final String CREATE_CATEGORY = "1";
  private static final String LIST_CATEGORIES = "2";
  private static final String RENAME_CATEGORY = "3";
  private static final String DELETE_CATEGORY = "4";
  private static final String SET_BUDGET = "5";
  private static final String SUMMARY = "1";
  private static final String TYPE_INCOME = "1";
  private static final String TYPE_EXPENSE = "2";
  private static final String SKIP = "";
  private static final String CANCELLED = "Cancelled.";

  private static final List<String> LOGIN_ALICE = List.of(LOGIN, ALICE, ALICE_PASSWORD);

  /** Food с бюджетом 4000 и расходом 300, Gym с бюджетом 50 без операций, Salary. */
  private static final List<String> SETUP =
      List.of(
          CATEGORIES,
          CREATE_CATEGORY, TYPE_EXPENSE, "Food",
          CREATE_CATEGORY, TYPE_EXPENSE, "Gym",
          CREATE_CATEGORY, TYPE_INCOME, "Salary",
          SET_BUDGET, "Food", "4000",
          SET_BUDGET, "Gym", "50",
          BACK,
          OPERATIONS,
          ADD_EXPENSE, "Food", "300", SKIP, "lunch",
          BACK);

  @TempDir private Path dataDir;

  private ScenarioApp app;

  @BeforeEach
  public void createApp() {
    app = new ScenarioApp(dataDir, USERS_FILE);
  }

  @Test
  public void renamedCategoryKeepsItsOperationsAndBudgetUnderTheNewName() {
    ScriptedConsoleIo io =
        run(
            SETUP,
            List.of(
                CATEGORIES, RENAME_CATEGORY, TYPE_EXPENSE, "food", "Groceries",
                LIST_CATEGORIES, BACK,
                OPERATIONS, LIST_OPERATIONS, SKIP, SKIP, BACK,
                STATISTICS, SUMMARY, SKIP, SKIP));

    String output = io.output();
    assertThat(output)
        .contains("Category \"food\" (expense) renamed to \"Groceries\".")
        .contains("| Groceries          | 4,000.00 |");
    String afterRename = output.substring(output.indexOf("renamed to"));
    assertThat(afterRename)
        .contains("| Groceries |")
        .contains("lunch")
        .contains("| Groceries | 4,000.00 | 300.00 |")
        .doesNotContain("| Food ");
  }

  @Test
  public void oldNameIsNoLongerAvailableForOperations() {
    ScriptedConsoleIo io =
        run(
            SETUP,
            List.of(
                CATEGORIES, RENAME_CATEGORY, TYPE_EXPENSE, "Food", "Groceries", BACK,
                OPERATIONS, ADD_EXPENSE, "Food", "5", SKIP, SKIP));

    assertThat(io.output())
        .contains("Category \"Food\" not found. Available categories: Groceries, Gym, Transfers.");
  }

  @Test
  public void renameReportsSystemTakenAndBadNames() {
    ScriptedConsoleIo io =
        run(
            SETUP,
            List.of(
                CATEGORIES,
                RENAME_CATEGORY, TYPE_EXPENSE, "Transfers", "X",
                RENAME_CATEGORY, TYPE_EXPENSE, "Food", "gym",
                RENAME_CATEGORY, TYPE_EXPENSE, "Food", "a,b",
                RENAME_CATEGORY, TYPE_EXPENSE, "Cinema", "X"));

    assertThat(io.output())
        .contains("The \"Transfers\" category is a system category")
        .contains("Category \"gym\" (expense) already exists.")
        .contains("A category name cannot contain a comma or control characters.")
        .contains("Category \"Cinema\" not found.")
        .doesNotContain("renamed to");
  }

  @Test
  public void renameDialogCanBeCancelledAtEveryQuestion() {
    ScriptedConsoleIo io =
        run(
            SETUP,
            List.of(
                CATEGORIES,
                RENAME_CATEGORY, "cancel",
                RENAME_CATEGORY, TYPE_EXPENSE, "cancel",
                RENAME_CATEGORY, TYPE_EXPENSE, "Food", "cancel"));

    assertThat(occurrences(io, CANCELLED)).isEqualTo(3);
    assertThat(io.output()).doesNotContain("renamed to");
  }

  @Test
  public void emptyCategoryIsDeletedTogetherWithItsBudget() {
    ScriptedConsoleIo io =
        run(
            SETUP,
            List.of(
                CATEGORIES, DELETE_CATEGORY, TYPE_EXPENSE, "Gym", "yes", LIST_CATEGORIES, BACK,
                STATISTICS, SUMMARY, SKIP, SKIP));

    String output = io.output();
    assertThat(output)
        .contains("Delete category \"Gym\" (expense)? (yes/no): ")
        .contains("Category \"Gym\" (expense) deleted.");
    String afterDelete = output.substring(output.indexOf("deleted."));
    assertThat(afterDelete).doesNotContain("Gym").contains("| Food");
  }

  @Test
  public void categoryWithOperationsCannotBeDeleted() {
    ScriptedConsoleIo io =
        run(SETUP, List.of(CATEGORIES, DELETE_CATEGORY, TYPE_EXPENSE, "Food", "yes"));

    assertThat(io.output())
        .contains("Cannot delete category \"Food\": it has 1 operations.")
        .doesNotContain("deleted.");
  }

  @Test
  public void declinedConfirmationKeepsTheCategory() {
    ScriptedConsoleIo io =
        run(
            SETUP,
            List.of(
                CATEGORIES, DELETE_CATEGORY, TYPE_EXPENSE, "Gym", "maybe", "no", LIST_CATEGORIES));

    assertThat(io.output())
        .contains("Enter \"yes\" or \"no\".")
        .contains(CANCELLED)
        .doesNotContain("deleted.");
    String afterAnswer = io.output().substring(io.output().indexOf(CANCELLED));
    assertThat(afterAnswer).contains("| Gym");
  }

  @Test
  public void systemAndUnknownCategoriesCannotBeDeleted() {
    ScriptedConsoleIo io =
        run(
            SETUP,
            List.of(
                CATEGORIES,
                DELETE_CATEGORY, TYPE_INCOME, "Transfers", "yes",
                DELETE_CATEGORY, TYPE_EXPENSE, "Cinema", "yes"));

    assertThat(io.output())
        .contains("The \"Transfers\" category is a system category")
        .contains("Category \"Cinema\" not found.");
  }

  @Test
  public void renamingAndDeletingSurviveRestart() {
    run(
        SETUP,
        List.of(
            CATEGORIES,
            RENAME_CATEGORY, TYPE_EXPENSE, "Food", "Groceries",
            DELETE_CATEGORY, TYPE_EXPENSE, "Gym", "yes"));

    ScriptedConsoleIo second = run(List.of(CATEGORIES, LIST_CATEGORIES));

    assertThat(second.output())
        .contains("| Groceries          | 4,000.00 |")
        .doesNotContain("Gym")
        .doesNotContain("| Food ");
  }

  @SafeVarargs
  private ScriptedConsoleIo run(List<String>... parts) {
    List<String> input = new ArrayList<>(LOGIN_ALICE);
    Arrays.stream(parts).forEach(input::addAll);
    ScriptedConsoleIo io = new ScriptedConsoleIo(input.toArray(new String[0]));
    app.run(io);
    return io;
  }

  private int occurrences(ScriptedConsoleIo io, String part) {
    return io.output().split(Pattern.quote(part), -1).length - 1;
  }
}
