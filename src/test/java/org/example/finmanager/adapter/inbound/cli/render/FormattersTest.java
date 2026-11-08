package org.example.finmanager.adapter.inbound.cli.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class FormattersTest {

  @ParameterizedTest
  @CsvSource({
    "54700, '54,700.00'",
    "0, '0.00'",
    "-500, '-500.00'",
    "1234567.891, '1,234,567.89'",
    "0.5, '0.50'",
    "999999999.99, '999,999,999.99'",
    "800.00, '800.00'"
  })
  public void moneyHasThousandsSeparatorAndTwoDecimals(String amount, String expected) {
    assertThat(MoneyFormatter.format(new BigDecimal(amount))).isEqualTo(expected);
  }

  @Test
  public void datesUseTwoDigitDayAndMonth() {
    assertThat(DateFormatter.format(LocalDate.of(2026, 11, 8))).isEqualTo("08.11.2026");
    assertThat(DateFormatter.format(LocalDate.of(2026, 1, 5))).isEqualTo("05.01.2026");
  }

  @Test
  public void periodBoundIsDateOrEllipsis() {
    assertThat(PeriodFormatter.bound(LocalDate.of(2026, 11, 8))).isEqualTo("08.11.2026");
    assertThat(PeriodFormatter.bound(null)).isEqualTo("...");
  }

  @Test
  public void periodIsUnboundedOnlyWithoutAnyBound() {
    LocalDate date = LocalDate.of(2026, 11, 8);

    assertThat(PeriodFormatter.isUnbounded(null)).isTrue();
    assertThat(PeriodFormatter.isUnbounded(new PeriodDto(null, null))).isTrue();
    assertThat(PeriodFormatter.isUnbounded(new PeriodDto(date, null))).isFalse();
    assertThat(PeriodFormatter.isUnbounded(new PeriodDto(null, date))).isFalse();
  }

  @Test
  public void warningIsMarkedWithAnAsciiMarker() {
    assertThat(WarningFormatter.format("text")).isEqualTo("[!] text");
  }

  @Test
  public void typesAreLowerCaseWords() {
    assertThat(TypeFormatter.format(OperationType.INCOME)).isEqualTo("income");
    assertThat(TypeFormatter.format(OperationType.EXPENSE)).isEqualTo("expense");
  }

  @Test
  public void missingCommentBecomesEmptyText() {
    assertThat(CommentFormatter.truncate(null)).isEmpty();
  }

  @Test
  public void commentUpToThirtyCharactersIsKept() {
    String thirty = "x".repeat(30);

    assertThat(CommentFormatter.truncate("lunch")).isEqualTo("lunch");
    assertThat(CommentFormatter.truncate(thirty)).isEqualTo(thirty);
  }

  @Test
  public void longerCommentIsCutToThirtyCharactersWithEllipsis() {
    String truncated = CommentFormatter.truncate("x".repeat(31));

    assertThat(truncated).hasSize(30).isEqualTo("x".repeat(27) + "...");
  }
}
