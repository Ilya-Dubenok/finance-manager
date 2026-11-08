package org.example.finmanager.adapter.inbound.cli.render;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.result.AlertDto;
import org.example.finmanager.application.dto.result.BudgetExceededAlert;
import org.example.finmanager.application.dto.result.ExpensesExceedIncomeAlert;
import org.springframework.stereotype.Component;

/** Строит тексты оповещений с маркером {@code [!]}; текст выбирается по типу оповещения. */
@Component
@RequiredArgsConstructor
public class AlertRenderer {

  private final MessageCatalog messages;

  /**
   * Строит строки для вывода, по одной на оповещение, в порядке списка.
   *
   * @param alerts оповещения
   * @return строки с маркером; пустой список, если оповещений нет
   */
  public List<String> render(List<AlertDto> alerts) {
    return alerts.stream().map(this::text).map(WarningFormatter::format).toList();
  }

  private String text(AlertDto alert) {
    return switch (alert) {
      case BudgetExceededAlert exceeded -> budgetExceeded(exceeded);
      case ExpensesExceedIncomeAlert imbalance ->
          messages.get(
              MessageKeys.ALERT_EXPENSES_EXCEED_INCOME, imbalance.expense(), imbalance.income());
    };
  }

  private String budgetExceeded(BudgetExceededAlert alert) {
    BigDecimal over = alert.spent().subtract(alert.limit());
    return messages.get(
        MessageKeys.ALERT_BUDGET_EXCEEDED, alert.category(), alert.spent(), alert.limit(), over);
  }
}
