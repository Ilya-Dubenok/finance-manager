package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.BudgetStatusDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.junit.jupiter.api.Test;

public class WalletRulesTest {

  private static final String OWNER = "alice";
  private static final String TRANSFERS = "Transfers";
  private static final LocalDate DATE = LocalDate.of(2026, 11, 8);

  @Test
  public void newWalletHasSystemCategoriesOfBothTypesAndNoOperations() {
    WalletDto wallet = WalletRules.newWallet(OWNER);

    assertThat(wallet.owner()).isEqualTo(OWNER);
    assertThat(wallet.categories())
        .containsExactlyInAnyOrder(
            new CategoryDto(TRANSFERS, OperationType.INCOME),
            new CategoryDto(TRANSFERS, OperationType.EXPENSE));
    assertThat(wallet.operations()).isEmpty();
  }

  @Test
  public void findCategoryIgnoresCaseButRespectsType() {
    WalletDto wallet = walletWithCategory(OperationType.EXPENSE, "Food");

    assertThat(WalletRules.findCategory(wallet, OperationType.EXPENSE, "fOOD"))
        .contains(new CategoryDto("Food", OperationType.EXPENSE));
    assertThat(WalletRules.findCategory(wallet, OperationType.INCOME, "Food")).isEmpty();
    assertThat(WalletRules.findCategory(wallet, OperationType.EXPENSE, "Taxi")).isEmpty();
  }

  @Test
  public void categoryNamesAreSortedIgnoringCase() {
    WalletDto wallet = walletWithCategory(OperationType.EXPENSE, "banana");
    wallet = withCategory(wallet, OperationType.EXPENSE, "Apple");
    wallet = withCategory(wallet, OperationType.INCOME, "Salary");

    List<String> names = WalletRules.categoryNames(wallet, OperationType.EXPENSE);

    assertThat(names).containsExactly("Apple", "banana", TRANSFERS);
  }

  @Test
  public void systemNameIsRecognizedIgnoringCase() {
    assertThat(WalletRules.isSystemName("transfers")).isTrue();
    assertThat(WalletRules.isSystemName("TRANSFERS")).isTrue();
    assertThat(WalletRules.isSystemName("Food")).isFalse();
  }

  @Test
  public void balanceOfEmptyWalletIsZero() {
    BigDecimal balance = WalletRules.balance(WalletRules.newWallet(OWNER));

    assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  public void balanceIsIncomeMinusExpenses() {
    WalletDto wallet =
        walletWithOperations(
            operation(OperationType.INCOME, "100.50"),
            operation(OperationType.EXPENSE, "40.25"),
            operation(OperationType.INCOME, "0.75"));

    assertThat(WalletRules.balance(wallet)).isEqualByComparingTo("61.00");
  }

  @Test
  public void balanceCanBeNegative() {
    WalletDto wallet = walletWithOperations(operation(OperationType.EXPENSE, "10.00"));

    assertThat(WalletRules.balance(wallet)).isEqualByComparingTo("-10.00");
  }

  @Test
  public void newWalletHasNoBudgets() {
    assertThat(WalletRules.newWallet(OWNER).budgets()).isEmpty();
  }

  @Test
  public void findBudgetIgnoresCase() {
    WalletDto wallet = FrExample.wallet();

    assertThat(WalletRules.findBudget(wallet, "fOOD"))
        .contains(new BudgetDto("Food", new BigDecimal("4000.00")));
    assertThat(WalletRules.findBudget(wallet, "Taxi")).isEmpty();
  }

  @Test
  public void spentSumsOnlyExpensesOfTheCategory() {
    WalletDto wallet = FrExample.wallet();
    wallet =
        wallet.toBuilder()
            .operations(
                List.of(
                    FrExample.operation(OperationType.EXPENSE, "Food", "300"),
                    FrExample.operation(OperationType.EXPENSE, "food", "500"),
                    FrExample.operation(OperationType.INCOME, "Food", "9999"),
                    FrExample.operation(OperationType.EXPENSE, "Taxi", "1")))
            .build();

    assertThat(WalletRules.spent(wallet, "Food")).isEqualByComparingTo("800");
    assertThat(WalletRules.spent(wallet, "Utilities")).isEqualByComparingTo("0");
  }

  @Test
  public void budgetStatusIsLimitMinusSpentForTheWholeTime() {
    WalletDto wallet = FrExample.wallet();
    BudgetDto utilities = new BudgetDto("Utilities", new BigDecimal("2500.00"));

    BudgetStatusDto status = WalletRules.budgetStatus(wallet, utilities);

    assertThat(status.spent()).isEqualByComparingTo("3000");
    assertThat(status.remaining()).isEqualByComparingTo("-500");
  }

  @Test
  public void operationsInPeriodAreFilteredInclusivelyAndSortedByDate() {
    LocalDate early = LocalDate.of(2026, 11, 1);
    WalletDto wallet =
        walletWithOperations(
            dated("3", DATE), dated("1", early), dated("2", early), dated("4", DATE.plusDays(3)));

    List<OperationDto> all = WalletRules.operationsInPeriod(wallet, new PeriodDto(null, null));
    List<OperationDto> window = WalletRules.operationsInPeriod(wallet, new PeriodDto(early, DATE));

    assertThat(all).extracting(operation -> operation.amount().toPlainString())
        .containsExactly("1", "2", "3", "4");
    assertThat(window).extracting(operation -> operation.amount().toPlainString())
        .containsExactly("1", "2", "3");
  }

  @Test
  public void sumAddsOperationsOfOneTypeOnly() {
    List<OperationDto> operations =
        List.of(
            operation(OperationType.INCOME, "5"),
            operation(OperationType.EXPENSE, "2"),
            operation(OperationType.INCOME, "1"));

    assertThat(WalletRules.sum(operations, OperationType.INCOME)).isEqualByComparingTo("6");
    assertThat(WalletRules.sum(List.of(), OperationType.EXPENSE)).isEqualByComparingTo("0");
  }

  @Test
  public void categoryNotFoundCarriesNameAndAvailableCategories() {
    ApplicationException refusal =
        WalletRules.categoryNotFound(FrExample.wallet(), OperationType.INCOME, "Cinema");

    assertThat(refusal.getCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    assertThat(refusal.getArgs())
        .containsExactly("Cinema", List.of("Bonus", "Salary", "Transfers"));
  }

  private OperationDto dated(String amount, LocalDate date) {
    return new OperationDto(
        OperationType.EXPENSE, TRANSFERS, new BigDecimal(amount), date, null, null);
  }

  private WalletDto walletWithCategory(OperationType type, String name) {
    return withCategory(WalletRules.newWallet(OWNER), type, name);
  }

  private WalletDto withCategory(WalletDto wallet, OperationType type, String name) {
    List<CategoryDto> categories = new ArrayList<>(wallet.categories());
    categories.add(new CategoryDto(name, type));
    return wallet.toBuilder().categories(categories).build();
  }

  private WalletDto walletWithOperations(OperationDto... operations) {
    return WalletRules.newWallet(OWNER).toBuilder().operations(List.of(operations)).build();
  }

  private OperationDto operation(OperationType type, String amount) {
    return new OperationDto(type, TRANSFERS, new BigDecimal(amount), DATE, null, null);
  }
}
