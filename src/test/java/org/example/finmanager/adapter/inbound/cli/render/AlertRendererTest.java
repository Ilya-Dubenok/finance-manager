package org.example.finmanager.adapter.inbound.cli.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.application.dto.result.BudgetExceededAlert;
import org.example.finmanager.application.dto.result.ExpensesExceedIncomeAlert;
import org.junit.jupiter.api.Test;

public class AlertRendererTest {

  private final AlertRenderer renderer = new AlertRenderer(new MessageCatalog());

  @Test
  public void rendersBudgetExcessWithAmountOverTheLimit() {
    BudgetExceededAlert alert =
        new BudgetExceededAlert("Utilities", new BigDecimal("2500"), new BigDecimal("3000"));

    assertThat(renderer.render(List.of(alert)))
        .containsExactly(
            "[!] Budget limit exceeded for category \"Utilities\": spent 3,000.00 against a limit "
                + "of 2,500.00 (over by 500.00).");
  }

  @Test
  public void rendersImbalanceWithExpensesFirst() {
    ExpensesExceedIncomeAlert alert =
        new ExpensesExceedIncomeAlert(new BigDecimal("5000"), new BigDecimal("8300"));

    assertThat(renderer.render(List.of(alert)))
        .containsExactly("[!] Expenses (8,300.00) exceed income (5,000.00).");
  }

  @Test
  public void keepsTheOrderOfAlerts() {
    List<String> lines =
        renderer.render(
            List.of(
                new BudgetExceededAlert("A", BigDecimal.ONE, BigDecimal.TEN),
                new ExpensesExceedIncomeAlert(BigDecimal.ZERO, BigDecimal.TEN)));

    assertThat(lines).hasSize(2);
    assertThat(lines.get(0)).contains("category \"A\"");
    assertThat(lines.get(1)).contains("Expenses (10.00)");
  }

  @Test
  public void noAlertsGiveNoLines() {
    assertThat(renderer.render(List.of())).isEmpty();
  }
}
