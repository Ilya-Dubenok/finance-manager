package org.example.finmanager.application.dto.result;

import java.util.List;
import org.example.finmanager.application.dto.model.OperationDto;

/**
 * Результат добавления операции.
 *
 * @param operation добавленная операция
 * @param alerts оповещения по состоянию кошелька после операции
 */
public record OperationResultDto(OperationDto operation, List<AlertDto> alerts) {

  /** Копирует список, чтобы результат нельзя было изменить снаружи. */
  public OperationResultDto {
    alerts = List.copyOf(alerts);
  }
}
