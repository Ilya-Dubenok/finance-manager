package org.example.finmanager.adapter.inbound.cli.render;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.dto.OperationRow;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.springframework.stereotype.Component;

/** Рисует таблицу операций: номер, дата, тип, категория, сумма, контрагент, комментарий. */
@Component
@RequiredArgsConstructor
public class OperationTableRenderer {

  private static final int NUMBER_COLUMN = 0;
  private static final int AMOUNT_COLUMN = 4;
  private static final Set<Integer> RIGHT_ALIGNED = Set.of(NUMBER_COLUMN, AMOUNT_COLUMN);

  private final MessageCatalog messages;

  /**
   * Рисует таблицу.
   *
   * @param rows строки в порядке показа; номера считаются с единицы
   * @return таблица для вывода
   */
  public String render(List<OperationRow> rows) {
    List<String> headers =
        List.of(
            messages.get(MessageKeys.TABLE_NUMBER),
            messages.get(MessageKeys.TABLE_DATE),
            messages.get(MessageKeys.TABLE_TYPE),
            messages.get(MessageKeys.TABLE_CATEGORY),
            messages.get(MessageKeys.TABLE_AMOUNT),
            messages.get(MessageKeys.TABLE_COUNTERPARTY),
            messages.get(MessageKeys.TABLE_COMMENT));
    List<List<String>> cells =
        IntStream.range(0, rows.size()).mapToObj(index -> cellsOf(index + 1, rows)).toList();
    return TableRenderer.render(headers, cells, RIGHT_ALIGNED);
  }

  private List<String> cellsOf(int number, List<OperationRow> rows) {
    OperationRow row = rows.get(number - 1);
    return Arrays.asList(
        String.valueOf(number),
        row.date(),
        row.type(),
        row.category(),
        row.amount(),
        row.counterparty(),
        row.comment());
  }
}
