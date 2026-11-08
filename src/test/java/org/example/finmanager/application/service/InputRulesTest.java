package org.example.finmanager.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.finmanager.RefusalAssert;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class InputRulesTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "   ", "\t"})
  public void requireNotBlankRejectsMissingAndBlankValues(String value) {
    RefusalAssert.assertRefusal(() -> InputRules.requireNotBlank(value), ErrorCode.EMPTY_VALUE);
  }

  @Test
  public void requireNotBlankAcceptsValueWithText() {
    assertThatCode(() -> InputRules.requireNotBlank(" a ")).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "0.00", "-1", "-0.01"})
  public void requireAmountRejectsZeroAndNegative(String amount) {
    RefusalAssert.assertRefusal(
        () -> InputRules.requireAmount(new BigDecimal(amount)), ErrorCode.AMOUNT_NOT_POSITIVE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"10.001", "0.005", "1.234"})
  public void requireAmountRejectsMoreThanTwoDecimals(String amount) {
    RefusalAssert.assertRefusal(
        () -> InputRules.requireAmount(new BigDecimal(amount)),
        ErrorCode.AMOUNT_TOO_MANY_DECIMALS);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1000000000", "999999999.99"})
  public void requireAmountChecksUpperLimit(String amount) {
    BigDecimal value = new BigDecimal(amount);
    if (value.compareTo(new BigDecimal("999999999.99")) > 0) {
      RefusalAssert.assertRefusal(
          () -> InputRules.requireAmount(value), ErrorCode.AMOUNT_TOO_LARGE);
    } else {
      assertThatCode(() -> InputRules.requireAmount(value)).doesNotThrowAnyException();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.01", "1", "1500.50", "10.000", "1E+2"})
  public void requireAmountAcceptsValidAmounts(String amount) {
    assertThatCode(() -> InputRules.requireAmount(new BigDecimal(amount)))
        .doesNotThrowAnyException();
  }

  @Test
  public void requireAmountRejectsMissingAmount() {
    RefusalAssert.assertRefusal(() -> InputRules.requireAmount(null), ErrorCode.EMPTY_VALUE);
  }

  @Test
  public void requireNotInFutureRejectsOnlyDatesAfterToday() {
    RefusalAssert.assertRefusal(
        () -> InputRules.requireNotInFuture(TODAY.plusDays(1), TODAY), ErrorCode.DATE_IN_FUTURE);
    assertThatCode(() -> InputRules.requireNotInFuture(TODAY, TODAY)).doesNotThrowAnyException();
    assertThatCode(() -> InputRules.requireNotInFuture(TODAY.minusYears(5), TODAY))
        .doesNotThrowAnyException();
  }

  @Test
  public void requirePeriodRejectsStartAfterEnd() {
    PeriodDto reversed = new PeriodDto(TODAY, TODAY.minusDays(1));

    RefusalAssert.assertRefusal(() -> InputRules.requirePeriod(reversed), ErrorCode.PERIOD_INVALID);
  }

  @Test
  public void requirePeriodAcceptsOpenAndSingleDayPeriods() {
    assertThatCode(() -> InputRules.requirePeriod(new PeriodDto(null, null)))
        .doesNotThrowAnyException();
    assertThatCode(() -> InputRules.requirePeriod(new PeriodDto(TODAY, null)))
        .doesNotThrowAnyException();
    assertThatCode(() -> InputRules.requirePeriod(new PeriodDto(null, TODAY)))
        .doesNotThrowAnyException();
    assertThatCode(() -> InputRules.requirePeriod(new PeriodDto(TODAY, TODAY)))
        .doesNotThrowAnyException();
  }

  @Test
  public void requireCommentAcceptsMissingAndMaximumLengthComment() {
    assertThatCode(() -> InputRules.requireComment(null)).doesNotThrowAnyException();
    assertThatCode(() -> InputRules.requireComment("x".repeat(100))).doesNotThrowAnyException();
  }

  @Test
  public void requireCommentRejectsTooLongComment() {
    RefusalAssert.assertRefusalWithArgs(
        () -> InputRules.requireComment("x".repeat(101)), ErrorCode.VALUE_TOO_LONG, 100);
  }

  @Test
  public void requireCommentRejectsControlCharacters() {
    RefusalAssert.assertRefusal(
        () -> InputRules.requireComment("a\tb"), ErrorCode.COMMENT_INVALID_CHARS);
  }

  @Test
  public void requireCategoryNameAcceptsMaximumLengthName() {
    assertThatCode(() -> InputRules.requireCategoryName("n".repeat(50)))
        .doesNotThrowAnyException();
  }

  @Test
  public void requireCategoryNameRejectsBlankName() {
    RefusalAssert.assertRefusal(() -> InputRules.requireCategoryName(" "), ErrorCode.EMPTY_VALUE);
  }

  @Test
  public void requireCategoryNameRejectsTooLongName() {
    RefusalAssert.assertRefusalWithArgs(
        () -> InputRules.requireCategoryName("n".repeat(51)), ErrorCode.VALUE_TOO_LONG, 50);
  }

  @ParameterizedTest
  @ValueSource(strings = {"a,b", ",", "a\tb"})
  public void requireCategoryNameRejectsCommaAndControlCharacters(String name) {
    RefusalAssert.assertRefusal(
        () -> InputRules.requireCategoryName(name), ErrorCode.NAME_INVALID_CHARS);
  }
}
