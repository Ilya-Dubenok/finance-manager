package org.example.finmanager.adapter.inbound.cli.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TableRendererTest {

  private final String newLine = System.lineSeparator();

  @Test
  public void drawsBorderedTableWithTextOnTheLeftAndNumbersOnTheRight() {
    String table =
        TableRenderer.render(
            List.of("Category", "Amount"),
            List.of(List.of("Bonus", "3,000.00"), List.of("Salary", "60,000.00")),
            Set.of(1));

    String expected =
        String.join(
            newLine,
            "+----------+-----------+",
            "| Category |    Amount |",
            "+----------+-----------+",
            "| Bonus    |  3,000.00 |",
            "| Salary   | 60,000.00 |",
            "+----------+-----------+");
    assertThat(table).isEqualTo(expected);
  }

  @Test
  public void columnIsAsWideAsItsHeaderWhenCellsAreShorter() {
    String table =
        TableRenderer.render(List.of("Category"), List.of(List.of("Tea")), Set.of());

    assertThat(table).startsWith("+----------+" + newLine + "| Category |");
    assertThat(table).contains("| Tea      |");
  }

  @Test
  public void tableWithoutRowsHasOnlyHeader() {
    String table = TableRenderer.render(List.of("A", "B"), List.of(), Set.of());

    String expected =
        String.join(newLine, "+---+---+", "| A | B |", "+---+---+", "+---+---+");
    assertThat(table).isEqualTo(expected);
  }

  @Test
  public void missingCellIsShownAsEmpty() {
    List<String> row = Arrays.asList("x", null);

    String table = TableRenderer.render(List.of("One", "Two"), List.of(row), Set.of());

    assertThat(table).contains("| x   |     |");
  }

  @Test
  public void noTrailingLineBreak() {
    String table = TableRenderer.render(List.of("A"), List.of(), Set.of());

    assertThat(table).doesNotEndWith(newLine);
  }
}
