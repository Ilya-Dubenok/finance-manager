package org.example.finmanager.adapter.inbound.cli.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.result.BudgetStatusDto;
import org.example.finmanager.application.dto.result.CategoryAmountDto;
import org.example.finmanager.application.dto.result.SummaryDto;
import org.springframework.stereotype.Component;

/**
 * Рисует сводку: итоги, доходы и расходы по категориям, бюджеты, оповещения. Пустые блоки
 * пропускаются, блоки разделяются пустой строкой.
 */
@Component
@RequiredArgsConstructor
public class SummaryRenderer {

  private static final String LINE_BREAK = System.lineSeparator();
  private static final String BLOCK_SEPARATOR = LINE_BREAK + LINE_BREAK;
  private static final Set<Integer> AMOUNT_COLUMNS = Set.of(1);
  private static final Set<Integer> BUDGET_COLUMNS = Set.of(1, 2, 3);

  private final MessageCatalog messages;
  private final AlertRenderer alertRenderer;

  /**
   * Рисует сводку.
   *
   * @param summary сводка
   * @return текст для вывода без перевода строки в конце
   */
  public String render(SummaryDto summary) {
    List<String> blocks = new ArrayList<>();
    blocks.add(String.join(LINE_BREAK, headLines(summary)));
    if (summary.hasOperations()) {
      addAmountBlock(blocks, MessageKeys.SUMMARY_INCOME_TITLE, summary.incomeByCategory());
      addAmountBlock(blocks, MessageKeys.SUMMARY_EXPENSES_TITLE, summary.expenseByCategory());
    } else {
      blocks.add(messages.get(MessageKeys.INFO_NO_OPERATIONS));
    }
    if (!summary.budgets().isEmpty()) {
      blocks.add(budgetBlock(summary.budgets()));
    }
    if (!summary.alerts().isEmpty()) {
      blocks.add(String.join(LINE_BREAK, alertRenderer.render(summary.alerts())));
    }
    return String.join(BLOCK_SEPARATOR, blocks);
  }

  private List<String> headLines(SummaryDto summary) {
    List<String> lines = new ArrayList<>();
    PeriodDto period = summary.period();
    if (!PeriodFormatter.isUnbounded(period)) {
      String from = PeriodFormatter.bound(period.from());
      String to = PeriodFormatter.bound(period.to());
      lines.add(messages.get(MessageKeys.SUMMARY_PERIOD, from, to));
    }
    lines.add(messages.get(MessageKeys.SUMMARY_TOTAL_INCOME, summary.totalIncome()));
    lines.add(messages.get(MessageKeys.SUMMARY_TOTAL_EXPENSES, summary.totalExpense()));
    lines.add(messages.get(MessageKeys.SUMMARY_BALANCE, summary.balance()));
    return lines;
  }

  private void addAmountBlock(
      List<String> blocks, String titleKey, List<CategoryAmountDto> amounts) {
    if (amounts.isEmpty()) {
      return;
    }
    List<String> headers =
        List.of(
            messages.get(MessageKeys.TABLE_CATEGORY), messages.get(MessageKeys.TABLE_AMOUNT));
    List<List<String>> rows =
        amounts.stream()
            .map(item -> List.of(item.category(), MoneyFormatter.format(item.amount())))
            .toList();
    String table = TableRenderer.render(headers, rows, AMOUNT_COLUMNS);
    blocks.add(messages.get(titleKey) + LINE_BREAK + table);
  }

  private String budgetBlock(List<BudgetStatusDto> budgets) {
    List<String> headers =
        List.of(
            messages.get(MessageKeys.TABLE_CATEGORY),
            messages.get(MessageKeys.TABLE_LIMIT),
            messages.get(MessageKeys.TABLE_SPENT),
            messages.get(MessageKeys.TABLE_REMAINING));
    List<List<String>> rows =
        budgets.stream()
            .map(
                budget ->
                    List.of(
                        budget.category(),
                        MoneyFormatter.format(budget.limit()),
                        MoneyFormatter.format(budget.spent()),
                        MoneyFormatter.format(budget.remaining())))
            .toList();
    String table = TableRenderer.render(headers, rows, BUDGET_COLUMNS);
    return messages.get(MessageKeys.SUMMARY_BUDGETS_TITLE) + LINE_BREAK + table;
  }
}
