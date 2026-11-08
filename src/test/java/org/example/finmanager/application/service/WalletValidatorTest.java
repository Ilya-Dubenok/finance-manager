package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.junit.jupiter.api.Test;

public class WalletValidatorTest {

  private static final String OWNER = "alice";
  private static final LocalDate DATE = LocalDate.of(2026, 11, 8);
  private static final CategoryDto FOOD = new CategoryDto("Food", OperationType.EXPENSE);
  private static final CategoryDto SALARY = new CategoryDto("Salary", OperationType.INCOME);

  @Test
  public void walletFromTheExampleIsValid() {
    assertThat(WalletValidator.validate(FrExample.wallet())).isEmpty();
  }

  @Test
  public void newWalletIsValid() {
    assertThat(WalletValidator.validate(WalletRules.newWallet(OWNER))).isEmpty();
  }

  @Test
  public void reportsCategoryNameThatBreaksTheNameRules() {
    WalletDto wallet = wallet(List.of(new CategoryDto("a,b", OperationType.EXPENSE)));

    assertThat(WalletValidator.validate(wallet))
        .containsExactly("category #1 (a,b): NAME_INVALID_CHARS");
  }

  @Test
  public void reportsTooLongAndEmptyNames() {
    WalletDto wallet =
        wallet(
            List.of(
                new CategoryDto("n".repeat(51), OperationType.EXPENSE),
                new CategoryDto("  ", OperationType.INCOME)));

    assertThat(WalletValidator.validate(wallet))
        .containsExactly(
            "category #1 (" + "n".repeat(51) + "): VALUE_TOO_LONG",
            "category #2 (  ): EMPTY_VALUE");
  }

  @Test
  public void reportsReservedWordAsCategoryName() {
    WalletDto wallet = wallet(List.of(new CategoryDto("Cancel", OperationType.EXPENSE)));

    assertThat(WalletValidator.validate(wallet))
        .containsExactly("category #1 (Cancel): reserved name");
  }

  @Test
  public void reportsDuplicateCategoryInTheSameTypeIgnoringCaseButNotInTheOther() {
    WalletDto wallet =
        wallet(
            List.of(
                FOOD,
                new CategoryDto("food", OperationType.EXPENSE),
                new CategoryDto("Food", OperationType.INCOME)));

    assertThat(WalletValidator.validate(wallet)).containsExactly("category #2 (food): duplicate");
  }

  @Test
  public void reportsMissingFieldsOfCategory() {
    WalletDto wallet = wallet(List.of(new CategoryDto(null, OperationType.EXPENSE)));

    assertThat(WalletValidator.validate(wallet))
        .containsExactly("category #1: required fields are missing");
  }

  @Test
  public void reportsBudgetForUnknownOrIncomeCategory() {
    WalletDto wallet =
        walletWith(
            List.of(FOOD, SALARY),
            List.of(budget("Salary", "10"), budget("Taxi", "10")),
            List.of());

    assertThat(WalletValidator.validate(wallet))
        .containsExactly(
            "budget #1 (Salary): category does not exist",
            "budget #2 (Taxi): category does not exist");
  }

  @Test
  public void reportsBudgetLimitThatBreaksTheAmountRules() {
    WalletDto wallet =
        walletWith(
            List.of(FOOD),
            List.of(budget("Food", "0"), budget("food", "1.234")),
            List.of());

    assertThat(WalletValidator.validate(wallet))
        .containsExactly(
            "budget #1 (Food): AMOUNT_NOT_POSITIVE",
            "budget #2 (food): AMOUNT_TOO_MANY_DECIMALS",
            "budget #2 (food): duplicate");
  }

  @Test
  public void reportsOperationWithUnknownCategoryOrWrongType() {
    WalletDto wallet =
        walletWith(
            List.of(FOOD, SALARY),
            List.of(),
            List.of(
                operation(OperationType.EXPENSE, "Taxi", "5"),
                operation(OperationType.INCOME, "Food", "5")));

    assertThat(WalletValidator.validate(wallet))
        .containsExactly(
            "operation #1 (Taxi): category does not exist",
            "operation #2 (Food): category does not exist");
  }

  @Test
  public void reportsOperationWithBadAmountAndComment() {
    OperationDto bad =
        new OperationDto(
            OperationType.EXPENSE, "Food", new BigDecimal("-1"), DATE, "x".repeat(101), null);
    WalletDto wallet = walletWith(List.of(FOOD), List.of(), List.of(bad));

    assertThat(WalletValidator.validate(wallet))
        .containsExactly(
            "operation #1 (Food): AMOUNT_NOT_POSITIVE", "operation #1 (Food): VALUE_TOO_LONG");
  }

  @Test
  public void reportsOperationWithMissingFields() {
    OperationDto withoutDate =
        new OperationDto(OperationType.EXPENSE, "Food", BigDecimal.TEN, null, null, null);
    WalletDto wallet = walletWith(List.of(FOOD), List.of(), List.of(withoutDate));

    assertThat(WalletValidator.validate(wallet))
        .containsExactly("operation #1: required fields are missing");
  }

  @Test
  public void collectsAllProblemsInOrder() {
    WalletDto wallet =
        walletWith(
            List.of(new CategoryDto("a,b", OperationType.EXPENSE)),
            List.of(budget("Nowhere", "1")),
            List.of(operation(OperationType.EXPENSE, "Nowhere", "1")));

    assertThat(WalletValidator.validate(wallet)).hasSize(3);
  }

  private WalletDto wallet(List<CategoryDto> categories) {
    return walletWith(categories, List.of(), List.of());
  }

  private WalletDto walletWith(
      List<CategoryDto> categories, List<BudgetDto> budgets, List<OperationDto> operations) {
    return new WalletDto(OWNER, categories, budgets, operations);
  }

  private BudgetDto budget(String category, String limit) {
    return new BudgetDto(category, new BigDecimal(limit));
  }

  private OperationDto operation(OperationType type, String category, String amount) {
    return new OperationDto(type, category, new BigDecimal(amount), DATE, null, null);
  }
}
