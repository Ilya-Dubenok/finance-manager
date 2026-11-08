package org.example.finmanager.application.port.inbound;

import org.example.finmanager.application.dto.command.TransferCommand;
import org.example.finmanager.application.dto.result.TransferResultDto;

/** Переводы между пользователями. */
public interface TransferPort {

  /**
   * Переводит деньги: отправителю добавляется расход, получателю доход, оба в системной категории
   * «Transfers», датой сегодня. Операция атомарна: при любом отказе не меняется ни один кошелёк.
   *
   * @param command отправитель, получатель, сумма и комментарий
   * @return добавленный расход отправителя и оповещения ему
   * @throws org.example.finmanager.application.exception.ApplicationException если получателя нет,
   *     он совпадает с отправителем, сумма неверна, больше баланса отправителя или файл кошелька
   *     получателя повреждён
   */
  TransferResultDto transfer(TransferCommand command);
}
