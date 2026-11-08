package org.example.finmanager.adapter.inbound.cli.render;

import java.time.LocalDate;
import lombok.experimental.UtilityClass;
import org.example.finmanager.application.dto.command.PeriodDto;

/** Показ границ периода: дата или многоточие для отсутствующей границы. */
@UtilityClass
public class PeriodFormatter {

  private static final String OPEN_BOUND = "...";

  /**
   * Форматирует границу периода.
   *
   * @param bound граница или {@code null}
   * @return дата в формате вывода или многоточие
   */
  public String bound(LocalDate bound) {
    return bound == null ? OPEN_BOUND : DateFormatter.format(bound);
  }

  /**
   * Проверяет, что у периода нет ни одной границы.
   *
   * @param period период или {@code null}
   * @return {@code true}, если период не ограничен
   */
  public boolean isUnbounded(PeriodDto period) {
    return period == null || (period.from() == null && period.to() == null);
  }
}
