package org.example.finmanager.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.AlertDto;
import org.example.finmanager.application.dto.result.BudgetExceededAlert;
import org.example.finmanager.application.dto.result.BudgetStatusDto;
import org.example.finmanager.application.dto.result.ExpensesExceedIncomeAlert;
import org.example.finmanager.application.port.inbound.AlertPort;
import org.springframework.stereotype.Service;

/**
 * Вычисляет оповещения A-1 и A-2 по текущему состоянию кошелька. Состояния не хранит и ничего не
 * меняет: оповещение возникает, пока условие выполнено, и исчезает вместе с ним.
 */
@Service
public class AlertService implements AlertPort {

  @Override
  public List<AlertDto> checkAfterExpense(WalletDto wallet, String category) {
    List<AlertDto> alerts = new ArrayList<>(checkBudget(wallet, category));
    expensesExceedIncome(wallet).ifPresent(alerts::add);
    return alerts;
  }

  @Override
  public List<AlertDto> checkAfterIncome(WalletDto wallet) {
    return expensesExceedIncome(wallet).stream().toList();
  }

  @Override
  public List<AlertDto> checkBudget(WalletDto wallet, String category) {
    return WalletRules.findBudget(wallet, category)
        .flatMap(budget -> budgetExceeded(wallet, budget))
        .stream()
        .toList();
  }

  @Override
  public List<AlertDto> checkAll(WalletDto wallet) {
    List<AlertDto> alerts = new ArrayList<>();
    wallet.budgets().stream()
        .sorted(Comparator.comparing(BudgetDto::category, String.CASE_INSENSITIVE_ORDER))
        .map(budget -> budgetExceeded(wallet, budget))
        .flatMap(Optional::stream)
        .forEach(alerts::add);
    expensesExceedIncome(wallet).ifPresent(alerts::add);
    return alerts;
  }

  private Optional<AlertDto> budgetExceeded(WalletDto wallet, BudgetDto budget) {
    BudgetStatusDto status = WalletRules.budgetStatus(wallet, budget);
    if (status.remaining().signum() >= 0) {
      return Optional.empty();
    }
    return Optional.of(new BudgetExceededAlert(status.category(), status.limit(), status.spent()));
  }

  private Optional<AlertDto> expensesExceedIncome(WalletDto wallet) {
    BigDecimal income = WalletRules.sum(wallet.operations(), OperationType.INCOME);
    BigDecimal expense = WalletRules.sum(wallet.operations(), OperationType.EXPENSE);
    if (expense.compareTo(income) <= 0) {
      return Optional.empty();
    }
    return Optional.of(new ExpensesExceedIncomeAlert(income, expense));
  }
}
