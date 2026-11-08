package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.example.finmanager.RefusalAssert;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.AlertDto;
import org.example.finmanager.application.dto.result.BudgetExceededAlert;
import org.example.finmanager.application.dto.result.BudgetStatusDto;
import org.example.finmanager.application.dto.result.CategoryAmountDto;
import org.example.finmanager.application.dto.result.CategoryTotalRowDto;
import org.example.finmanager.application.dto.result.CategoryTotalsDto;
import org.example.finmanager.application.dto.result.ExpensesExceedIncomeAlert;
import org.example.finmanager.application.dto.result.SummaryDto;
import org.example.finmanager.application.exception.ErrorCode;
import org.junit.jupiter.api.Test;

public class StatisticsServiceTest {

  private static final String LOGIN = FrExample.OWNER;
  private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
  private static final LocalDate NOV_5 = LocalDate.of(2026, 11, 5);
  private static final LocalDate NOV_8 = FrExample.DATE;

  private final FakeWalletPort wallets = new FakeWalletPort();
  private final StatisticsService service = new StatisticsService(wallets, new AlertService());

  @Test
  public void balanceOfNewWalletIsZero() {
    assertThat(service.balance(LOGIN)).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  public void balanceIsIncomeMinusExpensesOfTheRequestedUser() {
    save(
        LOGIN,
        operation(OperationType.INCOME, "63000.00"),
        operation(OperationType.EXPENSE, "8300.00"));
    save("bob", operation(OperationType.INCOME, "1.00"));

    assertThat(service.balance(LOGIN)).isEqualByComparingTo("54700.00");
    assertThat(service.balance("bob")).isEqualByComparingTo("1.00");
  }

  @Test
  public void balanceCanBeNegative() {
    save(LOGIN, operation(OperationType.EXPENSE, "5.50"));

    assertThat(service.balance(LOGIN)).isEqualByComparingTo("-5.50");
  }

  @Test
  public void summaryOfTheExampleFromTheRequirements() {
    wallets.save(FrExample.wallet());

    SummaryDto summary = service.summary(LOGIN, null);

    assertThat(summary.hasOperations()).isTrue();
    assertThat(summary.totalIncome()).isEqualByComparingTo("63000.00");
    assertThat(summary.totalExpense()).isEqualByComparingTo("8300.00");
    assertThat(summary.balance()).isEqualByComparingTo("54700.00");
    assertThat(summary.incomeByCategory())
        .extracting(CategoryAmountDto::category)
        .containsExactly("Bonus", "Salary");
    assertThat(summary.incomeByCategory())
        .extracting(CategoryAmountDto::amount)
        .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
        .containsExactly(new BigDecimal("3000"), new BigDecimal("60000"));
    assertThat(summary.expenseByCategory())
        .extracting(CategoryAmountDto::category)
        .containsExactly("Entertainment", "Food", "Taxi", "Utilities");
    assertThat(summary.expenseByCategory())
        .extracting(CategoryAmountDto::amount)
        .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
        .containsExactly(
            new BigDecimal("3000"),
            new BigDecimal("800"),
            new BigDecimal("1500"),
            new BigDecimal("3000"));
  }

  @Test
  public void summaryShowsBudgetsWithRemainingLimitForTheWholeTime() {
    wallets.save(FrExample.wallet());

    List<BudgetStatusDto> budgets = service.summary(LOGIN, null).budgets();

    assertThat(budgets).extracting(BudgetStatusDto::category)
        .containsExactly("Entertainment", "Food", "Utilities");
    assertThat(budgets.get(0).remaining()).isEqualByComparingTo("0.00");
    assertThat(budgets.get(1).spent()).isEqualByComparingTo("800.00");
    assertThat(budgets.get(1).remaining()).isEqualByComparingTo("3200.00");
    assertThat(budgets.get(2).remaining()).isEqualByComparingTo("-500.00");
  }

  @Test
  public void periodFiltersTotalsAndCategoriesButNotBalanceAndBudgets() {
    save(
        LOGIN,
        dated(OperationType.INCOME, "Transfers", "1000", NOV_1),
        dated(OperationType.EXPENSE, "Transfers", "100", NOV_5),
        dated(OperationType.EXPENSE, "Transfers", "40", NOV_8));
    PeriodDto period = new PeriodDto(NOV_5, NOV_5);

    SummaryDto summary = service.summary(LOGIN, period);

    assertThat(summary.period()).isEqualTo(period);
    assertThat(summary.totalIncome()).isEqualByComparingTo("0");
    assertThat(summary.totalExpense()).isEqualByComparingTo("100");
    assertThat(summary.incomeByCategory()).isEmpty();
    assertThat(summary.expenseByCategory()).hasSize(1);
    assertThat(summary.balance()).isEqualByComparingTo("860");
  }

  @Test
  public void summaryWithoutOperationsInPeriodHasNoOperationsButKeepsBudgets() {
    wallets.save(FrExample.wallet());
    PeriodDto period = new PeriodDto(NOV_1, NOV_5);

    SummaryDto summary = service.summary(LOGIN, period);

    assertThat(summary.hasOperations()).isFalse();
    assertThat(summary.totalIncome()).isEqualByComparingTo("0");
    assertThat(summary.incomeByCategory()).isEmpty();
    assertThat(summary.budgets()).hasSize(3);
    assertThat(summary.balance()).isEqualByComparingTo("54700.00");
  }

  @Test
  public void summaryOfTheExampleHasExactlyTheBudgetAlertOfUtilities() {
    wallets.save(FrExample.wallet());

    List<AlertDto> alerts = service.summary(LOGIN, null).alerts();

    assertThat(alerts)
        .containsExactly(
            new BudgetExceededAlert(
                "Utilities", new BigDecimal("2500.00"), new BigDecimal("3000")));
  }

  @Test
  public void summaryAlertsIgnoreThePeriod() {
    wallets.save(FrExample.wallet());

    List<AlertDto> alerts = service.summary(LOGIN, new PeriodDto(NOV_1, NOV_5)).alerts();

    assertThat(alerts).hasSize(1);
  }

  @Test
  public void summaryListsBudgetAlertsAlphabeticallyThenImbalance() {
    WalletDto wallet = FrExample.wallet();
    wallets.save(
        wallet.toBuilder()
            .budgets(
                List.of(
                    new BudgetDto("Utilities", new BigDecimal("1.00")),
                    new BudgetDto("Food", new BigDecimal("1.00"))))
            .operations(
                List.of(
                    FrExample.operation(OperationType.EXPENSE, "Food", "5"),
                    FrExample.operation(OperationType.EXPENSE, "Utilities", "5")))
            .build());

    List<AlertDto> alerts = service.summary(LOGIN, null).alerts();

    assertThat(alerts).hasSize(3);
    assertThat(alerts.get(0)).isEqualTo(
        new BudgetExceededAlert("Food", new BigDecimal("1.00"), new BigDecimal("5")));
    assertThat(alerts.get(1)).isInstanceOf(BudgetExceededAlert.class);
    assertThat(alerts.get(2)).isInstanceOf(ExpensesExceedIncomeAlert.class);
  }

  @Test
  public void summaryRejectsPeriodWithStartAfterEnd() {
    PeriodDto reversed = new PeriodDto(NOV_8, NOV_1);

    RefusalAssert.assertRefusal(() -> service.summary(LOGIN, reversed), ErrorCode.PERIOD_INVALID);
  }

  @Test
  public void totalsForSeveralCategoriesOfBothTypes() {
    wallets.save(FrExample.wallet());

    CategoryTotalsDto totals =
        service.totalsByCategories(LOGIN, List.of("Food", "Salary"), null);

    assertThat(totals.anyCategoryFound()).isTrue();
    assertThat(totals.notFound()).isEmpty();
    assertThat(totals.rows())
        .extracting(CategoryTotalRowDto::category)
        .containsExactly("Food", "Salary");
    assertThat(totals.rows())
        .extracting(CategoryTotalRowDto::type)
        .containsExactly(OperationType.EXPENSE, OperationType.INCOME);
    assertThat(totals.incomeTotal()).isEqualByComparingTo("60000");
    assertThat(totals.expenseTotal()).isEqualByComparingTo("800");
  }

  @Test
  public void totalsCalculateFoundCategoriesAndReportMissingOnes() {
    wallets.save(FrExample.wallet());

    CategoryTotalsDto totals =
        service.totalsByCategories(LOGIN, List.of("Food", "Cinema", "Salary"), null);

    assertThat(totals.anyCategoryFound()).isTrue();
    assertThat(totals.notFound()).containsExactly("Cinema");
    assertThat(totals.rows()).hasSize(2);
    assertThat(totals.incomeTotal()).isEqualByComparingTo("60000");
    assertThat(totals.expenseTotal()).isEqualByComparingTo("800");
  }

  @Test
  public void totalsReportThatNoCategoryWasFound() {
    wallets.save(FrExample.wallet());

    CategoryTotalsDto totals = service.totalsByCategories(LOGIN, List.of("Cinema"), null);

    assertThat(totals.anyCategoryFound()).isFalse();
    assertThat(totals.notFound()).containsExactly("Cinema");
    assertThat(totals.rows()).isEmpty();
    assertThat(totals.incomeTotal()).isEqualByComparingTo("0");
  }

  @Test
  public void totalsMatchNamesIgnoringCaseAndIgnoreRepeats() {
    wallets.save(FrExample.wallet());

    CategoryTotalsDto totals =
        service.totalsByCategories(LOGIN, List.of("food", " FOOD ", "Food", "  "), null);

    assertThat(totals.rows()).hasSize(1);
    assertThat(totals.rows().get(0).category()).isEqualTo("Food");
    assertThat(totals.expenseTotal()).isEqualByComparingTo("800");
  }

  @Test
  public void totalsCountBothCategoriesWhenTheSameNameExistsInBothTypes() {
    List<OperationDto> operations =
        List.of(
            operation(OperationType.INCOME, "10"),
            operation(OperationType.EXPENSE, "4"));
    save(LOGIN, operations.toArray(new OperationDto[0]));

    CategoryTotalsDto totals = service.totalsByCategories(LOGIN, List.of("transfers"), null);

    assertThat(totals.rows()).hasSize(2);
    assertThat(totals.incomeTotal()).isEqualByComparingTo("10");
    assertThat(totals.expenseTotal()).isEqualByComparingTo("4");
  }

  @Test
  public void totalsDoNotListFoundCategoriesWithoutOperationsInPeriod() {
    wallets.save(FrExample.wallet());
    PeriodDto period = new PeriodDto(NOV_1, NOV_5);

    CategoryTotalsDto totals = service.totalsByCategories(LOGIN, List.of("Food"), period);

    assertThat(totals.anyCategoryFound()).isTrue();
    assertThat(totals.rows()).isEmpty();
    assertThat(totals.expenseTotal()).isEqualByComparingTo("0");
  }

  @Test
  public void totalsRespectThePeriod() {
    save(
        LOGIN,
        dated(OperationType.EXPENSE, "Transfers", "100", NOV_1),
        dated(OperationType.EXPENSE, "Transfers", "40", NOV_8));

    CategoryTotalsDto totals =
        service.totalsByCategories(LOGIN, List.of("Transfers"), new PeriodDto(NOV_8, null));

    assertThat(totals.expenseTotal()).isEqualByComparingTo("40");
  }

  @Test
  public void totalsRejectEmptyNameListAndBadPeriod() {
    RefusalAssert.assertRefusal(
        () -> service.totalsByCategories(LOGIN, List.of(" ", ""), null), ErrorCode.EMPTY_VALUE);
    RefusalAssert.assertRefusal(
        () -> service.totalsByCategories(LOGIN, List.of("Food"), new PeriodDto(NOV_8, NOV_1)),
        ErrorCode.PERIOD_INVALID);
  }

  private void save(String login, OperationDto... operations) {
    WalletDto wallet = wallets.get(login);
    List<OperationDto> all = new ArrayList<>(wallet.operations());
    all.addAll(List.of(operations));
    wallets.save(wallet.toBuilder().operations(all).build());
  }

  private OperationDto operation(OperationType type, String amount) {
    return FrExample.operation(type, WalletRules.SYSTEM_CATEGORY_NAME, amount);
  }

  private OperationDto dated(OperationType type, String category, String amount, LocalDate date) {
    return new OperationDto(type, category, new BigDecimal(amount), date, null, null);
  }
}
