package org.example.finmanager.application.dto.result;

import java.util.List;
import org.example.finmanager.application.dto.model.OperationDto;

/**
 * Результат перевода.
 *
 * @param fromOperation расход, добавленный отправителю
 * @param toLogin логин получателя
 * @param alerts оповещения отправителю по состоянию его кошелька
 */
public record TransferResultDto(
    OperationDto fromOperation, String toLogin, List<AlertDto> alerts) {

  /** Копирует список, чтобы результат нельзя было изменить снаружи. */
  public TransferResultDto {
    alerts = List.copyOf(alerts);
  }
}
