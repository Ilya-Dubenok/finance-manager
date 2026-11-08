package org.example.finmanager.application.dto.result;

import java.util.List;

/**
 * Результат установки бюджета.
 *
 * @param status состояние бюджета после установки
 * @param alerts оповещения по бюджету этой категории
 */
public record BudgetResultDto(BudgetStatusDto status, List<AlertDto> alerts) {

  /** Копирует список, чтобы результат нельзя было изменить снаружи. */
  public BudgetResultDto {
    alerts = List.copyOf(alerts);
  }
}
