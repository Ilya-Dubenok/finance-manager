package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.example.finmanager.RefusalAssert;
import org.example.finmanager.application.dto.command.CreateCategoryCommand;
import org.example.finmanager.application.dto.command.DeleteCategoryCommand;
import org.example.finmanager.application.dto.command.RenameCategoryCommand;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Переименование и удаление категорий (спецификация, UC-09 и UC-10). */
public class CategoryEditingTest {

  private static final String LOGIN = FrExample.OWNER;
  private static final OperationType EXPENSE = OperationType.EXPENSE;
  private static final OperationType INCOME = OperationType.INCOME;

  private final FakeWalletPort wallets = new FakeWalletPort();
  private final CategoryService service = new CategoryService(wallets);

  @BeforeEach
  public void createWalletFromTheExample() {
    wallets.save(FrExample.wallet());
  }

  @Test
  public void renameChangesCategoryItsOperationsAndItsBudget() {
    CategoryDto renamed = rename(EXPENSE, "Food", "Groceries");

    assertThat(renamed).isEqualTo(new CategoryDto("Groceries", EXPENSE));
    WalletDto wallet = wallets.get(LOGIN);
    assertThat(wallet.categories())
        .contains(renamed)
        .doesNotContain(new CategoryDto("Food", EXPENSE));
    assertThat(operationsOf(wallet, EXPENSE, "Groceries")).hasSize(2);
    assertThat(operationsOf(wallet, EXPENSE, "Food")).isEmpty();
    assertThat(wallet.budgets()).contains(new BudgetDto("Groceries", new BigDecimal("4000.00")));
    assertThat(WalletRules.findBudget(wallet, "Food")).isEmpty();
  }

  @Test
  public void renameDoesNotTouchOperationsOfTheSameNameInTheOtherType() {
    WalletDto wallet = wallets.get(LOGIN);
    List<CategoryDto> categories = new ArrayList<>(wallet.categories());
    categories.add(new CategoryDto("Food", INCOME));
    List<OperationDto> operations = new ArrayList<>(wallet.operations());
    operations.add(FrExample.operation(INCOME, "Food", "7"));
    wallets.save(wallet.toBuilder().categories(categories).operations(operations).build());

    rename(EXPENSE, "Food", "Groceries");

    WalletDto updated = wallets.get(LOGIN);
    assertThat(operationsOf(updated, INCOME, "Food")).hasSize(1);
    assertThat(updated.categories()).contains(new CategoryDto("Food", INCOME));
  }

  @Test
  public void renameToTheSameNameWithAnotherCaseIsAllowed() {
    rename(EXPENSE, "Food", "FOOD");

    WalletDto wallet = wallets.get(LOGIN);
    assertThat(operationsOf(wallet, EXPENSE, "FOOD")).hasSize(2);
    assertThat(wallet.budgets()).extracting(BudgetDto::category).contains("FOOD");
  }

  @Test
  public void renameToTheVeryNameSucceedsAndChangesNothing() {
    WalletDto before = wallets.get(LOGIN);

    rename(EXPENSE, "Food", "Food");

    assertThat(wallets.get(LOGIN).categories())
        .containsExactlyInAnyOrderElementsOf(before.categories());
  }

  @Test
  public void renameFindsCategoryIgnoringCaseAndTrimsBothNames() {
    CategoryDto renamed = rename(EXPENSE, " fOOD ", "  Groceries  ");

    assertThat(renamed.name()).isEqualTo("Groceries");
  }

  @Test
  public void renameMayUseAnExistingNameOfTheOtherType() {
    CategoryDto renamed = rename(INCOME, "Bonus", "Food");

    assertThat(renamed).isEqualTo(new CategoryDto("Food", INCOME));
  }

  @Test
  public void renameRejectsUnknownCategoryAndListsAvailableOnes() {
    RefusalAssert.assertRefusalWithArgs(
        () -> rename(EXPENSE, "Cinema", "X"),
        ErrorCode.CATEGORY_NOT_FOUND,
        "Cinema",
        List.of("Entertainment", "Food", "Taxi", "Transfers", "Utilities"));
  }

  @Test
  public void renameRejectsSystemCategoryAndSystemNameAsTarget() {
    RefusalAssert.assertRefusal(
        () -> rename(EXPENSE, "transfers", "X"), ErrorCode.SYSTEM_CATEGORY);
    RefusalAssert.assertRefusal(
        () -> rename(EXPENSE, "Food", "TRANSFERS"), ErrorCode.SYSTEM_CATEGORY);
  }

  @Test
  public void renameRejectsReservedWord() {
    RefusalAssert.assertRefusalWithArgs(
        () -> rename(EXPENSE, "Food", "Cancel"), ErrorCode.RESERVED_NAME, "Cancel");
  }

  @Test
  public void renameRejectsNameTakenInTheSameType() {
    RefusalAssert.assertRefusalWithArgs(
        () -> rename(EXPENSE, "Taxi", "entertainment"),
        ErrorCode.CATEGORY_ALREADY_EXISTS,
        "entertainment",
        EXPENSE);
  }

  @Test
  public void renameRejectsBadNames() {
    RefusalAssert.assertRefusal(() -> rename(EXPENSE, "Food", "a,b"), ErrorCode.NAME_INVALID_CHARS);
    RefusalAssert.assertRefusalWithArgs(
        () -> rename(EXPENSE, "Food", "n".repeat(51)), ErrorCode.VALUE_TOO_LONG, 50);
    RefusalAssert.assertRefusal(() -> rename(EXPENSE, "Food", "  "), ErrorCode.EMPTY_VALUE);
    RefusalAssert.assertRefusal(() -> rename(EXPENSE, " ", "X"), ErrorCode.EMPTY_VALUE);
  }

  @Test
  public void rejectedRenameChangesNothing() {
    WalletDto before = wallets.get(LOGIN);

    RefusalAssert.assertRefusal(
        () -> rename(EXPENSE, "Taxi", "Food"), ErrorCode.CATEGORY_ALREADY_EXISTS);

    assertThat(wallets.get(LOGIN)).isSameAs(before);
  }

  @Test
  public void deleteRemovesEmptyCategoryTogetherWithItsBudget() {
    WalletDto wallet = wallets.get(LOGIN);
    List<CategoryDto> categories = new ArrayList<>(wallet.categories());
    categories.add(new CategoryDto("Gym", EXPENSE));
    List<BudgetDto> budgets = new ArrayList<>(wallet.budgets());
    budgets.add(new BudgetDto("Gym", new BigDecimal("50.00")));
    wallets.save(wallet.toBuilder().categories(categories).budgets(budgets).build());

    delete(EXPENSE, "gym");

    WalletDto updated = wallets.get(LOGIN);
    assertThat(updated.categories()).doesNotContain(new CategoryDto("Gym", EXPENSE));
    assertThat(WalletRules.findBudget(updated, "Gym")).isEmpty();
    assertThat(updated.budgets()).hasSize(3);
  }

  @Test
  public void deleteRefusesCategoryWithOperationsAndReportsTheirNumber() {
    RefusalAssert.assertRefusalWithArgs(
        () -> delete(EXPENSE, "Food"), ErrorCode.CATEGORY_HAS_OPERATIONS, "Food", 2L);
    RefusalAssert.assertRefusalWithArgs(
        () -> delete(INCOME, "Bonus"), ErrorCode.CATEGORY_HAS_OPERATIONS, "Bonus", 1L);
  }

  @Test
  public void deleteRefusesSystemAndUnknownCategories() {
    RefusalAssert.assertRefusal(() -> delete(INCOME, "Transfers"), ErrorCode.SYSTEM_CATEGORY);
    RefusalAssert.assertRefusal(() -> delete(EXPENSE, "Cinema"), ErrorCode.CATEGORY_NOT_FOUND);
    RefusalAssert.assertRefusal(() -> delete(EXPENSE, " "), ErrorCode.EMPTY_VALUE);
  }

  @Test
  public void deleteLooksOnlyAtTheGivenType() {
    RefusalAssert.assertRefusal(() -> delete(INCOME, "Food"), ErrorCode.CATEGORY_NOT_FOUND);
  }

  @Test
  public void deleteOfIncomeCategoryKeepsExpenseCategoryWithTheSameNameAndItsBudget() {
    WalletDto wallet = wallets.get(LOGIN);
    List<CategoryDto> categories = new ArrayList<>(wallet.categories());
    categories.add(new CategoryDto("Food", INCOME));
    wallets.save(wallet.toBuilder().categories(categories).build());

    delete(INCOME, "Food");

    WalletDto updated = wallets.get(LOGIN);
    assertThat(updated.categories()).contains(new CategoryDto("Food", EXPENSE));
    assertThat(WalletRules.findBudget(updated, "Food")).isPresent();
  }

  @Test
  public void deletedNameCanBeCreatedAgain() {
    service.createCategory(new CreateCategoryCommand(LOGIN, EXPENSE, "Gym"));
    delete(EXPENSE, "Gym");
    service.createCategory(new CreateCategoryCommand(LOGIN, EXPENSE, "Gym"));

    assertThat(WalletRules.findCategory(wallets.get(LOGIN), EXPENSE, "Gym")).isPresent();
  }

  private CategoryDto rename(OperationType type, String name, String newName) {
    return service.renameCategory(new RenameCategoryCommand(LOGIN, type, name, newName));
  }

  private void delete(OperationType type, String name) {
    service.deleteCategory(new DeleteCategoryCommand(LOGIN, type, name));
  }

  private List<OperationDto> operationsOf(WalletDto wallet, OperationType type, String category) {
    return wallet.operations().stream()
        .filter(operation -> operation.type() == type && operation.category().equals(category))
        .toList();
  }
}
