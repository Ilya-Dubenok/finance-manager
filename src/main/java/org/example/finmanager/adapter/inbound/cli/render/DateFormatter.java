package org.example.finmanager.adapter.inbound.cli.render;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.experimental.UtilityClass;

/** Формат даты в интерфейсе: {@code dd.mm.yyyy}. */
@UtilityClass
public class DateFormatter {

  private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  /**
   * Форматирует дату.
   *
   * @param date дата
   * @return текст вида {@code 08.11.2026}
   */
  public String format(LocalDate date) {
    return FORMAT.format(date);
  }
}
