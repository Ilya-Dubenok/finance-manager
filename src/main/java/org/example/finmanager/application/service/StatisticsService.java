package org.example.finmanager.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.model.BudgetDto;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.model.WalletDto;
import org.example.finmanager.application.dto.result.BudgetStatusDto;
import org.example.finmanager.application.dto.result.CategoryAmountDto;
import org.example.finmanager.application.dto.result.CategoryTotalRowDto;
import org.example.finmanager.application.dto.result.CategoryTotalsDto;
import org.example.finmanager.application.dto.result.SummaryDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;
import org.example.finmanager.application.port.inbound.AlertPort;
import org.example.finmanager.application.port.inbound.StatisticsPort;
import org.example.finmanager.application.port.inbound.WalletPort;
import org.springframework.stereotype.Service;

/** Подсчёты по кошельку: баланс, сводка, итоги по выбранным категориям. */
@Service
@RequiredArgsConstructor
public class StatisticsService implements StatisticsPort {

  private static final String EMPTY = "";

  private final WalletPort wallets;
  private final AlertPort alerts;

  @Override
  public BigDecimal balance(String login) {
    WalletDto wallet = wallets.get(login);
    return WalletRules.balance(wallet);
  }

  @Override
  public SummaryDto summary(String login, PeriodDto period) {
    PeriodDto filter = validPeriod(period);
    WalletDto wallet = wallets.get(login);
    List<OperationDto> operations = WalletRules.operationsInPeriod(wallet, filter);
    return SummaryDto.builder()
        .period(filter)
        .hasOperations(!operations.isEmpty())
        .totalIncome(WalletRules.sum(operations, OperationType.INCOME))
        .totalExpense(WalletRules.sum(operations, OperationType.EXPENSE))
        .balance(WalletRules.balance(wallet))
        .incomeByCategory(amountsByCategory(operations, OperationType.INCOME))
        .expenseByCategory(amountsByCategory(operations, OperationType.EXPENSE))
        .budgets(budgetStatuses(wallet))
        .alerts(alerts.checkAll(wallet))
        .build();
  }

  @Override
  public CategoryTotalsDto totalsByCategories(
      String login, List<String> categoryNames, PeriodDto period) {
    PeriodDto filter = validPeriod(period);
    Map<String, String> names = distinctNames(categoryNames);
    if (names.isEmpty()) {
      throw new ApplicationException(ErrorCode.EMPTY_VALUE);
    }
    WalletDto wallet = wallets.get(login);
    List<OperationDto> operations = WalletRules.operationsInPeriod(wallet, filter);

    List<String> notFound = new ArrayList<>();
    List<CategoryDto> found = new ArrayList<>();
    for (String name : names.values()) {
      List<CategoryDto> matches = categoriesNamed(wallet, name);
      if (matches.isEmpty()) {
        notFound.add(name);
      }
      found.addAll(matches);
    }

    List<CategoryTotalRowDto> rows = totalRows(found, operations);
    return new CategoryTotalsDto(
        filter,
        !found.isEmpty(),
        rows,
        totalOf(rows, OperationType.INCOME),
        totalOf(rows, OperationType.EXPENSE),
        notFound);
  }

  private PeriodDto validPeriod(PeriodDto period) {
    PeriodDto filter = period == null ? new PeriodDto(null, null) : period;
    InputRules.requirePeriod(filter);
    return filter;
  }

  private List<CategoryAmountDto> amountsByCategory(
      List<OperationDto> operations, OperationType type) {
    Map<String, BigDecimal> sums = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (OperationDto operation : operations) {
      if (operation.type() == type) {
        sums.merge(operation.category(), operation.amount(), BigDecimal::add);
      }
    }
    return sums.entrySet().stream()
        .map(entry -> new CategoryAmountDto(entry.getKey(), entry.getValue()))
        .toList();
  }

  private List<BudgetStatusDto> budgetStatuses(WalletDto wallet) {
    return wallet.budgets().stream()
        .sorted(Comparator.comparing(BudgetDto::category, String.CASE_INSENSITIVE_ORDER))
        .map(budget -> WalletRules.budgetStatus(wallet, budget))
        .toList();
  }

  private Map<String, String> distinctNames(List<String> categoryNames) {
    Map<String, String> names = new LinkedHashMap<>();
    for (String name : categoryNames) {
      String trimmed = name == null ? EMPTY : name.trim();
      if (!trimmed.isEmpty()) {
        names.putIfAbsent(trimmed.toLowerCase(), trimmed);
      }
    }
    return names;
  }

  private List<CategoryDto> categoriesNamed(WalletDto wallet, String name) {
    return wallet.categories().stream()
        .filter(category -> category.name().equalsIgnoreCase(name))
        .toList();
  }

  private List<CategoryTotalRowDto> totalRows(
      List<CategoryDto> categories, List<OperationDto> operations) {
    return categories.stream()
        .sorted(Comparator.comparing(CategoryDto::name, String.CASE_INSENSITIVE_ORDER))
        .map(category -> totalRow(category, operations))
        .filter(row -> row.amount().signum() != 0)
        .toList();
  }

  private CategoryTotalRowDto totalRow(CategoryDto category, List<OperationDto> operations) {
    BigDecimal amount =
        operations.stream()
            .filter(operation -> operation.type() == category.type())
            .filter(operation -> operation.category().equalsIgnoreCase(category.name()))
            .map(OperationDto::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new CategoryTotalRowDto(category.name(), category.type(), amount);
  }

  private BigDecimal totalOf(List<CategoryTotalRowDto> rows, OperationType type) {
    return rows.stream()
        .filter(row -> row.type() == type)
        .map(CategoryTotalRowDto::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
