package org.example.finmanager.application.dto.result;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import org.example.finmanager.application.dto.command.PeriodDto;

/**
 * Сводка по кошельку.
 *
 * @param period период, за который считаются доходы и расходы
 * @param hasOperations есть ли в периоде хотя бы одна операция
 * @param totalIncome доходы за период
 * @param totalExpense расходы за период
 * @param balance баланс за всё время
 * @param incomeByCategory доходы по категориям за период, только ненулевые, по алфавиту
 * @param expenseByCategory расходы по категориям за период, только ненулевые, по алфавиту
 * @param budgets состояние всех бюджетов за всё время, по алфавиту
 * @param alerts оповещения по всем бюджетам и по соотношению доходов и расходов
 */
@Builder
public record SummaryDto(
    PeriodDto period,
    boolean hasOperations,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal balance,
    List<CategoryAmountDto> incomeByCategory,
    List<CategoryAmountDto> expenseByCategory,
    List<BudgetStatusDto> budgets,
    List<AlertDto> alerts) {

  /** Копирует списки, чтобы сводку нельзя было изменить снаружи. */
  public SummaryDto {
    incomeByCategory = List.copyOf(incomeByCategory);
    expenseByCategory = List.copyOf(expenseByCategory);
    budgets = List.copyOf(budgets);
    alerts = List.copyOf(alerts);
  }
}
