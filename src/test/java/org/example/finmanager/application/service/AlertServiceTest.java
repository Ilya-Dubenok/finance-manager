package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.AlertDto;
import org.example.finmanager.application.dto.result.BudgetExceededAlert;
import org.example.finmanager.application.dto.result.ExpensesExceedIncomeAlert;
import org.junit.jupiter.api.Test;

public class AlertServiceTest {

  private final AlertService service = new AlertService();

  @Test
  public void walletFromTheExampleHasOnlyTheBudgetAlertOfUtilities() {
    List<AlertDto> alerts = service.checkAll(FrExample.wallet());

    assertThat(alerts)
        .containsExactly(
            new BudgetExceededAlert(
                "Utilities", new BigDecimal("2500.00"), new BigDecimal("3000")));
  }

  @Test
  public void remainingExactlyZeroIsNotAnExcess() {
    WalletDto wallet = wallet(budget("Food", "100"), expense("Food", "100"), income("500"));

    assertThat(service.checkBudget(wallet, "Food")).isEmpty();
  }

  @Test
  public void oneCentOverTheLimitIsAnExcess() {
    WalletDto wallet = wallet(budget("Food", "100"), expense("Food", "100.01"), income("500"));

    assertThat(service.checkBudget(wallet, "Food"))
        .containsExactly(
            new BudgetExceededAlert("Food", new BigDecimal("100"), new BigDecimal("100.01")));
  }

  @Test
  public void checkBudgetLooksAtOneCategoryAndIgnoresCase() {
    WalletDto wallet =
        wallet(
            budget("Food", "1"), budget("Taxi", "1"), expense("Food", "5"), expense("Taxi", "5"));

    assertThat(service.checkBudget(wallet, "fOOD"))
        .extracting(alert -> ((BudgetExceededAlert) alert).category())
        .containsExactly("Food");
  }

  @Test
  public void categoryWithoutBudgetGivesNoAlert() {
    WalletDto wallet = wallet(expense("Food", "5"), income("10"));

    assertThat(service.checkBudget(wallet, "Food")).isEmpty();
  }

  @Test
  public void expensesGreaterThanIncomeGiveImbalanceAlert() {
    WalletDto wallet = wallet(expense("Food", "8300"), income("5000"));

    assertThat(service.checkAfterIncome(wallet))
        .containsExactly(
            new ExpensesExceedIncomeAlert(new BigDecimal("5000"), new BigDecimal("8300")));
  }

  @Test
  public void equalIncomeAndExpensesGiveNoImbalanceAlert() {
    WalletDto wallet = wallet(expense("Food", "10"), income("10"));

    assertThat(service.checkAfterIncome(wallet)).isEmpty();
  }

  @Test
  public void emptyWalletGivesNoAlerts() {
    assertThat(service.checkAll(WalletRules.newWallet("alice"))).isEmpty();
  }

  @Test
  public void afterExpenseBudgetAlertComesFirstThenImbalance() {
    WalletDto wallet = wallet(budget("Food", "1"), expense("Food", "5"));

    List<AlertDto> alerts = service.checkAfterExpense(wallet, "Food");

    assertThat(alerts).hasSize(2);
    assertThat(alerts.get(0)).isInstanceOf(BudgetExceededAlert.class);
    assertThat(alerts.get(1)).isInstanceOf(ExpensesExceedIncomeAlert.class);
  }

  @Test
  public void afterExpenseChecksOnlyTheBudgetOfThatCategory() {
    WalletDto wallet =
        wallet(budget("Taxi", "1"), expense("Taxi", "5"), expense("Food", "1"), income("100"));

    assertThat(service.checkAfterExpense(wallet, "Food")).isEmpty();
  }

  @Test
  public void checkAllListsBudgetAlertsAlphabeticallyBeforeImbalance() {
    WalletDto wallet =
        wallet(
            budget("Utilities", "1"),
            budget("food", "1"),
            expense("Utilities", "5"),
            expense("food", "5"));

    List<AlertDto> alerts = service.checkAll(wallet);

    assertThat(alerts).hasSize(3);
    assertThat(((BudgetExceededAlert) alerts.get(0)).category()).isEqualTo("food");
    assertThat(((BudgetExceededAlert) alerts.get(1)).category()).isEqualTo("Utilities");
    assertThat(alerts.get(2)).isInstanceOf(ExpensesExceedIncomeAlert.class);
  }

  private WalletDto wallet(Object... parts) {
    List<BudgetDto> budgets = new ArrayList<>();
    List<OperationDto> operations = new ArrayList<>();
    for (Object part : parts) {
      if (part instanceof BudgetDto budget) {
        budgets.add(budget);
      } else {
        operations.add((OperationDto) part);
      }
    }
    return WalletRules.newWallet("alice").toBuilder()
        .budgets(budgets)
        .operations(operations)
        .build();
  }

  private BudgetDto budget(String category, String limit) {
    return new BudgetDto(category, new BigDecimal(limit));
  }

  private OperationDto expense(String category, String amount) {
    return FrExample.operation(OperationType.EXPENSE, category, amount);
  }

  private OperationDto income(String amount) {
    return FrExample.operation(OperationType.INCOME, "Transfers", amount);
  }
}
