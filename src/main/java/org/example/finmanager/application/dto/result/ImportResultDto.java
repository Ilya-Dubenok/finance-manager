package org.example.finmanager.application.dto.result;

import java.util.List;

/**
 * Результат импорта снимка.
 *
 * @param categoryCount число категорий в снимке
 * @param budgetCount число бюджетов в снимке
 * @param operationCount число операций в снимке
 * @param originalOwner владелец, указанный в снимке, или {@code null}
 * @param ownerMismatch {@code true}, если владелец снимка не совпадает с текущим пользователем
 * @param alerts оповещения по состоянию кошелька после импорта
 */
public record ImportResultDto(
    int categoryCount,
    int budgetCount,
    int operationCount,
    String originalOwner,
    boolean ownerMismatch,
    List<AlertDto> alerts) {

  /** Копирует список, чтобы результат нельзя было изменить снаружи. */
  public ImportResultDto {
    alerts = List.copyOf(alerts);
  }
}
