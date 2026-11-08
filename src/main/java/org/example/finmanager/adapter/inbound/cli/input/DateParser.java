package org.example.finmanager.adapter.inbound.cli.input;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Разбирает дату в формате {@code dd.mm.yyyy} (день и месяц можно писать без нуля).
 *
 * <p>Проверяется только формат и существование даты. Запрет на будущее проверяет сервис.
 */
@UtilityClass
public class DateParser {

  private static final DateTimeFormatter FORMAT =
      DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT);

  /**
   * Разбирает дату.
   *
   * @param text введённый текст
   * @return дата или пустой результат, если формат не подходит или такой даты нет
   */
  public Optional<LocalDate> parse(String text) {
    try {
      return Optional.of(LocalDate.parse(text.trim(), FORMAT));
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }
}
