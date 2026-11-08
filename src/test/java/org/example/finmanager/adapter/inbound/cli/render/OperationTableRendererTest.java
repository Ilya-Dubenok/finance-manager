package org.example.finmanager.adapter.inbound.cli.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.finmanager.adapter.inbound.cli.dto.OperationRow;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.junit.jupiter.api.Test;

public class OperationTableRendererTest {

  private final OperationTableRenderer renderer = new OperationTableRenderer(new MessageCatalog());

  @Test
  public void drawsHeadersNumbersAndRightAlignedAmounts() {
    OperationRow transfer =
        new OperationRow("08.11.2026", "expense", "Transfers", "400.00", "bob", "gift");
    OperationRow salary =
        new OperationRow("09.11.2026", "income", "Salary", "60,000.00", null, "");

    String table = renderer.render(List.of(transfer, salary));

    String newLine = System.lineSeparator();
    String expected =
        String.join(
            newLine,
            "+---+------------+---------+-----------+-----------+--------------+---------+",
            "| # | Date       | Type    | Category  |    Amount | Counterparty | Comment |",
            "+---+------------+---------+-----------+-----------+--------------+---------+",
            "| 1 | 08.11.2026 | expense | Transfers |    400.00 | bob          | gift    |",
            "| 2 | 09.11.2026 | income  | Salary    | 60,000.00 |              |         |",
            "+---+------------+---------+-----------+-----------+--------------+---------+");
    assertThat(table).isEqualTo(expected);
  }
}
