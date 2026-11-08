package org.example.finmanager.adapter.inbound.cli.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.result.CategoryTotalsDto;
import org.springframework.stereotype.Component;

/** Рисует подсчёт по выбранным категориям: период, таблица, итоги, предупреждение. */
@Component
@RequiredArgsConstructor
public class TotalsRenderer {

  private static final String LINE_BREAK = System.lineSeparator();
  private static final Set<Integer> AMOUNT_COLUMN = Set.of(2);

  private final MessageCatalog messages;

  /**
   * Рисует подсчёт.
   *
   * @param totals результат подсчёта
   * @return текст для вывода без перевода строки в конце
   */
  public String render(CategoryTotalsDto totals) {
    if (!totals.anyCategoryFound()) {
      return warning(MessageKeys.WARNING_NO_CATEGORY_FOUND);
    }
    List<String> lines = new ArrayList<>();
    if (totals.rows().isEmpty()) {
      lines.add(messages.get(MessageKeys.INFO_NO_OPERATIONS));
    } else {
      lines.add(periodLine(totals.period()));
      lines.add(table(totals));
      lines.add(messages.get(MessageKeys.TOTALS_INCOME, totals.incomeTotal()));
      lines.add(messages.get(MessageKeys.TOTALS_EXPENSES, totals.expenseTotal()));
    }
    if (!totals.notFound().isEmpty()) {
      lines.add(warning(MessageKeys.WARNING_CATEGORIES_NOT_FOUND, totals.notFound()));
    }
    return String.join(LINE_BREAK, lines);
  }

  private String periodLine(PeriodDto period) {
    if (PeriodFormatter.isUnbounded(period)) {
      return messages.get(MessageKeys.TOTALS_PERIOD_ALL);
    }
    String from = PeriodFormatter.bound(period.from());
    String to = PeriodFormatter.bound(period.to());
    return messages.get(MessageKeys.SUMMARY_PERIOD, from, to);
  }

  private String table(CategoryTotalsDto totals) {
    List<String> headers =
        List.of(
            messages.get(MessageKeys.TABLE_CATEGORY),
            messages.get(MessageKeys.TABLE_TYPE),
            messages.get(MessageKeys.TABLE_AMOUNT));
    List<List<String>> rows =
        totals.rows().stream()
            .map(
                row ->
                    List.of(
                        row.category(),
                        TypeFormatter.format(row.type()),
                        MoneyFormatter.format(row.amount())))
            .toList();
    return TableRenderer.render(headers, rows, AMOUNT_COLUMN);
  }

  private String warning(String key, Object... args) {
    return WarningFormatter.format(messages.get(key, args));
  }
}
