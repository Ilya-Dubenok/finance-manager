package org.example.finmanager.adapter.inbound.cli.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.result.CategoryTotalRowDto;
import org.example.finmanager.application.dto.result.CategoryTotalsDto;
import org.junit.jupiter.api.Test;

public class TotalsRendererTest {

  private static final PeriodDto ALL_TIME = new PeriodDto(null, null);

  private final String newLine = System.lineSeparator();
  private final TotalsRenderer renderer = new TotalsRenderer(new MessageCatalog());

  @Test
  public void drawsTableTotalsAndWarningAboutMissingCategories() {
    CategoryTotalsDto totals =
        new CategoryTotalsDto(
            ALL_TIME,
            true,
            List.of(
                row("Food", OperationType.EXPENSE, "800"),
                row("Salary", OperationType.INCOME, "60000")),
            new BigDecimal("60000"),
            new BigDecimal("800"),
            List.of("Cinema"));

    String expected =
        String.join(
            newLine,
            "Period: all time",
            "+----------+---------+-----------+",
            "| Category | Type    |    Amount |",
            "+----------+---------+-----------+",
            "| Food     | expense |    800.00 |",
            "| Salary   | income  | 60,000.00 |",
            "+----------+---------+-----------+",
            "Total income for selected categories: 60,000.00",
            "Total expenses for selected categories: 800.00",
            "[!] Categories not found: Cinema. Totals calculated for the rest.");
    assertThat(renderer.render(totals)).isEqualTo(expected);
  }

  @Test
  public void showsTheBoundsOfTheGivenPeriod() {
    PeriodDto period = new PeriodDto(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    CategoryTotalsDto totals =
        new CategoryTotalsDto(
            period,
            true,
            List.of(row("Food", OperationType.EXPENSE, "5")),
            BigDecimal.ZERO,
            new BigDecimal("5"),
            List.of());

    assertThat(renderer.render(totals)).startsWith("Period: 01.11.2026 - 30.11.2026");
  }

  @Test
  public void listsSeveralMissingCategoriesSeparatedByCommas() {
    CategoryTotalsDto totals =
        new CategoryTotalsDto(
            ALL_TIME,
            true,
            List.of(row("Food", OperationType.EXPENSE, "5")),
            BigDecimal.ZERO,
            new BigDecimal("5"),
            List.of("Cinema", "Gym"));

    assertThat(renderer.render(totals))
        .endsWith("[!] Categories not found: Cinema, Gym. Totals calculated for the rest.");
  }

  @Test
  public void saysSoWhenNoneOfTheCategoriesWasFound() {
    CategoryTotalsDto totals =
        new CategoryTotalsDto(
            ALL_TIME, false, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, List.of("Cinema"));

    assertThat(renderer.render(totals))
        .isEqualTo("[!] None of the specified categories was found.");
  }

  @Test
  public void saysSoWhenFoundCategoriesHaveNoOperationsInThePeriod() {
    CategoryTotalsDto totals =
        new CategoryTotalsDto(
            ALL_TIME, true, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, List.of());

    assertThat(renderer.render(totals))
        .isEqualTo("No operations found for the selected period.");
  }

  @Test
  public void keepsTheWarningAfterTheMessageAboutMissingOperations() {
    CategoryTotalsDto totals =
        new CategoryTotalsDto(
            ALL_TIME, true, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, List.of("Cinema"));

    assertThat(renderer.render(totals))
        .isEqualTo(
            String.join(
                newLine,
                "No operations found for the selected period.",
                "[!] Categories not found: Cinema. Totals calculated for the rest."));
  }

  private CategoryTotalRowDto row(String category, OperationType type, String amount) {
    return new CategoryTotalRowDto(category, type, new BigDecimal(amount));
  }
}
