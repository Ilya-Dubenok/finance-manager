package org.example.finmanager.adapter.inbound.cli.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.result.BudgetExceededAlert;
import org.example.finmanager.application.dto.result.BudgetStatusDto;
import org.example.finmanager.application.dto.result.CategoryAmountDto;
import org.example.finmanager.application.dto.result.SummaryDto;
import org.junit.jupiter.api.Test;

public class SummaryRendererTest {

  private final String newLine = System.lineSeparator();
  private final MessageCatalog messages = new MessageCatalog();
  private final SummaryRenderer renderer =
      new SummaryRenderer(messages, new AlertRenderer(messages));

  @Test
  public void drawsTheExampleFromTheRequirements() {
    SummaryDto summary =
        SummaryDto.builder()
            .period(new PeriodDto(null, null))
            .hasOperations(true)
            .totalIncome(money("63000"))
            .totalExpense(money("8300"))
            .balance(money("54700"))
            .incomeByCategory(
                List.of(amount("Bonus", "3000"), amount("Salary", "60000")))
            .expenseByCategory(
                List.of(
                    amount("Entertainment", "3000"),
                    amount("Food", "800"),
                    amount("Taxi", "1500"),
                    amount("Utilities", "3000")))
            .alerts(
                List.of(
                    new BudgetExceededAlert(
                        "Utilities", new BigDecimal("2500"), new BigDecimal("3000"))))
            .budgets(
                List.of(
                    budget("Entertainment", "3000", "3000"),
                    budget("Food", "4000", "800"),
                    budget("Utilities", "2500", "3000")))
            .build();

    String expected =
        lines(
            "Total income: 63,000.00",
            "Total expenses: 8,300.00",
            "Balance: 54,700.00",
            "",
            "Income by category",
            "+----------+-----------+",
            "| Category |    Amount |",
            "+----------+-----------+",
            "| Bonus    |  3,000.00 |",
            "| Salary   | 60,000.00 |",
            "+----------+-----------+",
            "",
            "Expenses by category",
            "+---------------+----------+",
            "| Category      |   Amount |",
            "+---------------+----------+",
            "| Entertainment | 3,000.00 |",
            "| Food          |   800.00 |",
            "| Taxi          | 1,500.00 |",
            "| Utilities     | 3,000.00 |",
            "+---------------+----------+",
            "",
            "Budgets by category",
            "+---------------+----------+----------+------------------+",
            "| Category      |    Limit |    Spent | Remaining budget |",
            "+---------------+----------+----------+------------------+",
            "| Entertainment | 3,000.00 | 3,000.00 |             0.00 |",
            "| Food          | 4,000.00 |   800.00 |         3,200.00 |",
            "| Utilities     | 2,500.00 | 3,000.00 |          -500.00 |",
            "+---------------+----------+----------+------------------+",
            "",
            "[!] Budget limit exceeded for category \"Utilities\": spent 3,000.00 "
                + "against a limit of 2,500.00 (over by 500.00).");
    assertThat(renderer.render(summary)).isEqualTo(expected);
  }

  @Test
  public void showsThePeriodLineWhenAnyBoundIsSet() {
    SummaryDto summary =
        emptySummary(new PeriodDto(LocalDate.of(2026, 11, 1), null)).hasOperations(true).build();

    assertThat(renderer.render(summary))
        .startsWith(lines("Period: 01.11.2026 - ...", "Total income: 0.00"));
  }

  @Test
  public void showsBothBoundsOfThePeriod() {
    PeriodDto period = new PeriodDto(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    SummaryDto summary = emptySummary(period).hasOperations(true).build();

    assertThat(renderer.render(summary)).startsWith("Period: 01.11.2026 - 30.11.2026");
  }

  @Test
  public void withoutPeriodThereIsNoPeriodLine() {
    SummaryDto summary = emptySummary(new PeriodDto(null, null)).hasOperations(true).build();

    assertThat(renderer.render(summary)).startsWith("Total income: 0.00");
  }

  @Test
  public void replacesCategoryTablesWithMessageWhenThereAreNoOperationsButKeepsBudgets() {
    SummaryDto summary =
        emptySummary(new PeriodDto(null, null))
            .hasOperations(false)
            .budgets(List.of(budget("Food", "4000", "800")))
            .build();

    String text = renderer.render(summary);

    assertThat(text)
        .contains("No operations found for the selected period.")
        .doesNotContain("Income by category")
        .doesNotContain("Expenses by category")
        .contains("Budgets by category")
        .contains("| Food");
  }

  @Test
  public void skipsEmptyBlocksAndTheBudgetBlockWithoutBudgets() {
    SummaryDto summary =
        emptySummary(new PeriodDto(null, null))
            .hasOperations(true)
            .expenseByCategory(List.of(amount("Food", "5")))
            .build();

    assertThat(renderer.render(summary))
        .contains("Expenses by category")
        .doesNotContain("Income by category")
        .doesNotContain("Budgets by category");
  }

  private SummaryDto.SummaryDtoBuilder emptySummary(PeriodDto period) {
    return SummaryDto.builder()
        .period(period)
        .totalIncome(BigDecimal.ZERO)
        .totalExpense(BigDecimal.ZERO)
        .balance(BigDecimal.ZERO)
        .incomeByCategory(List.of())
        .expenseByCategory(List.of())
        .budgets(List.of())
        .alerts(List.of());
  }

  private String lines(String... lines) {
    return String.join(newLine, lines);
  }

  private BigDecimal money(String value) {
    return new BigDecimal(value);
  }

  private CategoryAmountDto amount(String category, String amount) {
    return new CategoryAmountDto(category, money(amount));
  }

  private BudgetStatusDto budget(String category, String limit, String spent) {
    BigDecimal remaining = money(limit).subtract(money(spent));
    return new BudgetStatusDto(category, money(limit), money(spent), remaining);
  }
}
