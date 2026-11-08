package org.example.finmanager.adapter.inbound.cli.input;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

/**
 * Разбирает сумму из текста: {@code 1500}, {@code 1500.50} или {@code 1500,50}.
 *
 * <p>Проверяется только формат. Допустимость значения (больше нуля, два знака, предел) проверяет
 * сервис.
 */
@UtilityClass
public class AmountParser {

  private static final Pattern AMOUNT_PATTERN = Pattern.compile("[+-]?\\d+(?:[.,]\\d+)?");
  private static final char DECIMAL_COMMA = ',';
  private static final char DECIMAL_POINT = '.';

  /**
   * Разбирает сумму.
   *
   * @param text введённый текст
   * @return сумма или пустой результат, если формат не подходит
   */
  public Optional<BigDecimal> parse(String text) {
    String trimmed = text.trim();
    if (!AMOUNT_PATTERN.matcher(trimmed).matches()) {
      return Optional.empty();
    }
    return Optional.of(new BigDecimal(trimmed.replace(DECIMAL_COMMA, DECIMAL_POINT)));
  }
}
