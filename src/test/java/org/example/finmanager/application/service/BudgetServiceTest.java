package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.example.finmanager.RefusalAssert;
import org.example.finmanager.application.dto.command.RemoveBudgetCommand;
import org.example.finmanager.application.dto.command.SetBudgetCommand;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.result.BudgetExceededAlert;
import org.example.finmanager.application.dto.result.BudgetResultDto;
import org.example.finmanager.application.dto.result.BudgetStatusDto;
import org.example.finmanager.application.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class BudgetServiceTest {

  private static final String LOGIN = FrExample.OWNER;

  private final FakeWalletPort wallets = new FakeWalletPort();
  private final BudgetService service = new BudgetService(wallets, new AlertService());

  @BeforeEach
  public void createWalletFromTheExample() {
    wallets.save(
        FrExample.wallet().toBuilder().budgets(List.of()).build());
  }

  @Test
  public void setsBudgetAndReportsSpentAndRemaining() {
    BudgetResultDto result = set("Food", "4000");

    BudgetStatusDto status = result.status();
    assertThat(status.category()).isEqualTo("Food");
    assertThat(status.limit()).isEqualByComparingTo("4000.00");
    assertThat(status.spent()).isEqualByComparingTo("800.00");
    assertThat(status.remaining()).isEqualByComparingTo("3200.00");
    assertThat(wallets.get(LOGIN).budgets())
        .containsExactly(new BudgetDto("Food", new BigDecimal("4000.00")));
  }

  @Test
  public void limitBelowSpentIsAllowedAndGivesNegativeRemaining() {
    BudgetStatusDto status = set("Utilities", "2500").status();

    assertThat(status.remaining()).isEqualByComparingTo("-500.00");
  }

  @Test
  public void spentEqualToLimitLeavesZeroRemaining() {
    assertThat(set("Entertainment", "3000").status().remaining()).isEqualByComparingTo("0.00");
  }

  @Test
  public void settingAgainReplacesTheLimitKeepingOneBudgetPerCategory() {
    set("Food", "4000");

    BudgetStatusDto status = set("Food", "900.5").status();

    assertThat(status.limit()).isEqualByComparingTo("900.50");
    assertThat(wallets.get(LOGIN).budgets()).hasSize(1);
  }

  @Test
  public void findsCategoryIgnoringCaseAndStoresItsCanonicalName() {
    BudgetStatusDto status = set("  fOOD ", "100").status();

    assertThat(status.category()).isEqualTo("Food");
    assertThat(wallets.get(LOGIN).budgets())
        .extracting(BudgetDto::category)
        .containsExactly("Food");
  }

  @Test
  public void budgetOfSeveralCategoriesAreIndependent() {
    set("Food", "4000");
    set("Taxi", "2000");

    assertThat(wallets.get(LOGIN).budgets()).hasSize(2);
  }

  @Test
  public void systemExpenseCategoryMayHaveBudget() {
    assertThat(set("Transfers", "10").status().category()).isEqualTo("Transfers");
  }

  @Test
  public void rejectsIncomeCategory() {
    RefusalAssert.assertRefusal(() -> set("Salary", "100"), ErrorCode.BUDGET_FOR_INCOME_CATEGORY);
  }

  @Test
  public void rejectsUnknownCategoryAndListsExpenseCategories() {
    RefusalAssert.assertRefusalWithArgs(
        () -> set("Cinema", "100"),
        ErrorCode.CATEGORY_NOT_FOUND,
        "Cinema",
        List.of("Entertainment", "Food", "Taxi", "Transfers", "Utilities"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "0.00"})
  public void rejectsLimitNotGreaterThanZero(String limit) {
    RefusalAssert.assertRefusal(
        () -> set("Food", limit), ErrorCode.BUDGET_LIMIT_NOT_POSITIVE);
  }

  @Test
  public void rejectsMoreThanTwoDecimalsAndTooLargeLimit() {
    RefusalAssert.assertRefusal(
        () -> set("Food", "10.999"), ErrorCode.AMOUNT_TOO_MANY_DECIMALS);
    RefusalAssert.assertRefusal(() -> set("Food", "1000000000"), ErrorCode.AMOUNT_TOO_LARGE);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  public void rejectsMissingCategory(String category) {
    RefusalAssert.assertRefusal(
        () -> service.setBudget(new SetBudgetCommand(LOGIN, category, BigDecimal.TEN)),
        ErrorCode.EMPTY_VALUE);
  }

  @Test
  public void rejectsMissingLimit() {
    RefusalAssert.assertRefusal(
        () -> service.setBudget(new SetBudgetCommand(LOGIN, "Food", null)),
        ErrorCode.EMPTY_VALUE);
  }

  @Test
  public void rejectedBudgetDoesNotChangeTheWallet() {
    RefusalAssert.assertRefusal(
        () -> set("Food", "0"), ErrorCode.BUDGET_LIMIT_NOT_POSITIVE);

    assertThat(wallets.get(LOGIN).budgets()).isEmpty();
  }

  @Test
  public void removesBudget() {
    set("Food", "4000");
    set("Taxi", "2000");

    service.removeBudget(new RemoveBudgetCommand(LOGIN, "food"));

    assertThat(wallets.get(LOGIN).budgets())
        .extracting(BudgetDto::category)
        .containsExactly("Taxi");
  }

  @Test
  public void removingBudgetOfCategoryWithoutOneIsRejected() {
    RefusalAssert.assertRefusalWithArgs(
        () -> service.removeBudget(new RemoveBudgetCommand(LOGIN, "Food")),
        ErrorCode.BUDGET_NOT_FOUND,
        "Food");
  }

  @Test
  public void removingBudgetOfUnknownOrIncomeCategoryIsReportedAsMissingCategory() {
    RefusalAssert.assertRefusal(
        () -> service.removeBudget(new RemoveBudgetCommand(LOGIN, "Cinema")),
        ErrorCode.CATEGORY_NOT_FOUND);
    RefusalAssert.assertRefusal(
        () -> service.removeBudget(new RemoveBudgetCommand(LOGIN, "Salary")),
        ErrorCode.CATEGORY_NOT_FOUND);
  }

  @Test
  public void removeRejectsMissingCategory() {
    RefusalAssert.assertRefusal(
        () -> service.removeBudget(new RemoveBudgetCommand(LOGIN, " ")), ErrorCode.EMPTY_VALUE);
  }

  @Test
  public void alertsWhenTheNewLimitIsBelowSpent() {
    BudgetResultDto result = set("Utilities", "2500");

    assertThat(result.alerts())
        .containsExactly(
            new BudgetExceededAlert(
                "Utilities", new BigDecimal("2500.00"), new BigDecimal("3000")));
  }

  @Test
  public void noAlertWhenSpentEqualsTheLimitOrIsBelowIt() {
    assertThat(set("Entertainment", "3000").alerts()).isEmpty();
    assertThat(set("Food", "4000").alerts()).isEmpty();
  }

  @Test
  public void budgetAlertDoesNotIncludeTheImbalanceRule() {
    assertThat(set("Utilities", "1").alerts())
        .allMatch(alert -> alert instanceof BudgetExceededAlert);
  }

  private BudgetResultDto set(String category, String limit) {
    return service.setBudget(new SetBudgetCommand(LOGIN, category, new BigDecimal(limit)));
  }
}
