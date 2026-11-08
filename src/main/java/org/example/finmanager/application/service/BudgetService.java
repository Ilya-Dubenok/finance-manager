package org.example.finmanager.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finmanager.application.dto.command.RemoveBudgetCommand;
import org.example.finmanager.application.dto.command.SetBudgetCommand;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.AlertDto;
import org.example.finmanager.application.dto.result.BudgetResultDto;
import org.example.finmanager.application.dto.result.BudgetStatusDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.port.inbound.AlertPort;
import org.example.finmanager.application.port.inbound.BudgetPort;
import org.example.finmanager.application.port.inbound.WalletPort;
import org.springframework.stereotype.Service;

/** Бюджеты на расходные категории: один лимит на категорию, без периода. */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService implements BudgetPort {

  private static final int AMOUNT_SCALE = 2;

  private final WalletPort wallets;
  private final AlertPort alerts;

  @Override
  public BudgetResultDto setBudget(SetBudgetCommand command) {
    InputRules.requireNotBlank(command.category());
    WalletDto wallet = wallets.get(command.login());
    CategoryDto category = findExpenseCategoryForBudget(wallet, command.category().trim());
    BigDecimal limit = validLimit(command.limit());

    List<BudgetDto> budgets = new ArrayList<>(wallet.budgets());
    budgets.removeIf(budget -> budget.category().equalsIgnoreCase(category.name()));
    BudgetDto budget = new BudgetDto(category.name(), limit);
    budgets.add(budget);
    WalletDto updated = wallet.toBuilder().budgets(budgets).build();
    wallets.save(updated);

    log.info("Set budget {} for '{}' of '{}'", limit, category.name(), command.login());
    BudgetStatusDto status = WalletRules.budgetStatus(updated, budget);
    List<AlertDto> budgetAlerts = alerts.checkBudget(updated, category.name());
    return new BudgetResultDto(status, budgetAlerts);
  }

  @Override
  public void removeBudget(RemoveBudgetCommand command) {
    InputRules.requireNotBlank(command.category());
    WalletDto wallet = wallets.get(command.login());
    String name = command.category().trim();
    CategoryDto category =
        WalletRules.findCategory(wallet, OperationType.EXPENSE, name)
            .orElseThrow(() -> WalletRules.categoryNotFound(wallet, OperationType.EXPENSE, name));
    BudgetDto budget =
        WalletRules.findBudget(wallet, category.name())
            .orElseThrow(
                () -> new ApplicationException(ErrorCode.BUDGET_NOT_FOUND, category.name()));

    List<BudgetDto> budgets = new ArrayList<>(wallet.budgets());
    budgets.remove(budget);
    wallets.save(wallet.toBuilder().budgets(budgets).build());
    log.info("Removed budget of '{}' for '{}'", category.name(), command.login());
  }

  private CategoryDto findExpenseCategoryForBudget(WalletDto wallet, String name) {
    return WalletRules.findCategory(wallet, OperationType.EXPENSE, name)
        .orElseThrow(() -> notExpenseCategory(wallet, name));
  }

  private ApplicationException notExpenseCategory(WalletDto wallet, String name) {
    if (WalletRules.findCategory(wallet, OperationType.INCOME, name).isPresent()) {
      return new ApplicationException(ErrorCode.BUDGET_FOR_INCOME_CATEGORY);
    }
    return WalletRules.categoryNotFound(wallet, OperationType.EXPENSE, name);
  }

  private BigDecimal validLimit(BigDecimal limit) {
    if (limit == null) {
      throw new ApplicationException(ErrorCode.EMPTY_VALUE);
    }
    if (limit.signum() <= 0) {
      throw new ApplicationException(ErrorCode.BUDGET_LIMIT_NOT_POSITIVE);
    }
    InputRules.requireAmount(limit);
    return limit.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
  }
}
