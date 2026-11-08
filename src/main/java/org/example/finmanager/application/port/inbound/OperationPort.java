package org.example.finmanager.application.port.inbound;

import java.util.List;
import org.example.finmanager.application.dto.command.AddOperationCommand;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.result.OperationResultDto;

/** Операции: доходы и расходы. */
public interface OperationPort {

  /**
   * Добавляет доход или расход.
   *
   * @param command данные операции
   * @return добавленная операция
   * @throws org.example.finmanager.application.exception.ApplicationException если категории нет
   *     или значения не проходят проверку
   */
  OperationResultDto addOperation(AddOperationCommand command);

  /**
   * Возвращает операции за период по возрастанию даты, при равных датах в порядке добавления.
   *
   * @param login логин владельца
   * @param period период или {@code null}, тогда все операции
   * @return операции; пустой список, если подходящих нет
   * @throws org.example.finmanager.application.exception.ApplicationException {@code
   *     PERIOD_INVALID}, если начало позже конца
   */
  List<OperationDto> listOperations(String login, PeriodDto period);
}
