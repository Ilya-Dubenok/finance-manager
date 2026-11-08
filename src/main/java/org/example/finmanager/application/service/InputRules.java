package org.example.finmanager.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.experimental.UtilityClass;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.exception.ApplicationException;
import org.example.finmanager.application.exception.ErrorCode;

/** Проверки входных значений, общие для сервисов. */
@UtilityClass
public class InputRules {

  /** Наибольшая длина названия категории. */
  public static final int MAX_CATEGORY_NAME_LENGTH = 50;

  /** Наибольшая длина комментария. */
  public static final int MAX_COMMENT_LENGTH = 100;

  private static final int MAX_DECIMAL_PLACES = 2;
  private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");
  private static final char CATEGORY_NAME_SEPARATOR = ',';

  /**
   * Проверяет, что значение задано.
   *
   * @param value проверяемое значение
   * @throws ApplicationException {@code EMPTY_VALUE}, если значение {@code null} или состоит из
   *     одних пробелов
   */
  public void requireNotBlank(String value) {
    if (value == null || value.isBlank()) {
      throw new ApplicationException(ErrorCode.EMPTY_VALUE);
    }
  }

  /**
   * Проверяет сумму: больше нуля, не больше двух знаков после запятой, не больше предела.
   *
   * @param amount сумма
   * @throws ApplicationException {@code EMPTY_VALUE}, {@code AMOUNT_NOT_POSITIVE}, {@code
   *     AMOUNT_TOO_MANY_DECIMALS} или {@code AMOUNT_TOO_LARGE}
   */
  public void requireAmount(BigDecimal amount) {
    if (amount == null) {
      throw new ApplicationException(ErrorCode.EMPTY_VALUE);
    }
    if (amount.signum() <= 0) {
      throw new ApplicationException(ErrorCode.AMOUNT_NOT_POSITIVE);
    }
    if (amount.stripTrailingZeros().scale() > MAX_DECIMAL_PLACES) {
      throw new ApplicationException(ErrorCode.AMOUNT_TOO_MANY_DECIMALS);
    }
    if (amount.compareTo(MAX_AMOUNT) > 0) {
      throw new ApplicationException(ErrorCode.AMOUNT_TOO_LARGE);
    }
  }

  /**
   * Проверяет, что дата не позже сегодняшней.
   *
   * @param date дата операции
   * @param today сегодняшняя дата
   * @throws ApplicationException {@code DATE_IN_FUTURE}
   */
  public void requireNotInFuture(LocalDate date, LocalDate today) {
    if (date.isAfter(today)) {
      throw new ApplicationException(ErrorCode.DATE_IN_FUTURE);
    }
  }

  /**
   * Проверяет период: начало не позже конца, если заданы обе границы.
   *
   * @param period период
   * @throws ApplicationException {@code PERIOD_INVALID}
   */
  public void requirePeriod(PeriodDto period) {
    boolean bothBounds = period.from() != null && period.to() != null;
    if (bothBounds && period.from().isAfter(period.to())) {
      throw new ApplicationException(ErrorCode.PERIOD_INVALID);
    }
  }

  /**
   * Проверяет комментарий: до 100 символов, без управляющих символов.
   *
   * @param comment комментарий или {@code null}
   * @throws ApplicationException {@code VALUE_TOO_LONG} или {@code COMMENT_INVALID_CHARS}
   */
  public void requireComment(String comment) {
    if (comment == null) {
      return;
    }
    if (comment.length() > MAX_COMMENT_LENGTH) {
      throw new ApplicationException(ErrorCode.VALUE_TOO_LONG, MAX_COMMENT_LENGTH);
    }
    if (comment.chars().anyMatch(Character::isISOControl)) {
      throw new ApplicationException(ErrorCode.COMMENT_INVALID_CHARS);
    }
  }

  /**
   * Проверяет название категории: не пустое, до 50 символов, без запятой и управляющих символов.
   *
   * @param name название без пробелов по краям
   * @throws ApplicationException {@code EMPTY_VALUE}, {@code VALUE_TOO_LONG} или {@code
   *     NAME_INVALID_CHARS}
   */
  public void requireCategoryName(String name) {
    requireNotBlank(name);
    if (name.length() > MAX_CATEGORY_NAME_LENGTH) {
      throw new ApplicationException(ErrorCode.VALUE_TOO_LONG, MAX_CATEGORY_NAME_LENGTH);
    }
    boolean hasSeparator = name.indexOf(CATEGORY_NAME_SEPARATOR) >= 0;
    if (hasSeparator || name.chars().anyMatch(Character::isISOControl)) {
      throw new ApplicationException(ErrorCode.NAME_INVALID_CHARS);
    }
  }
}
