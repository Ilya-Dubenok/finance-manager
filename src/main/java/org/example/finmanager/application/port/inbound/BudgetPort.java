package org.example.finmanager.application.port.inbound;

import org.example.finmanager.application.dto.command.RemoveBudgetCommand;
import org.example.finmanager.application.dto.command.SetBudgetCommand;
import org.example.finmanager.application.dto.result.BudgetResultDto;

/** Бюджеты на расходные категории. */
public interface BudgetPort {

  /**
   * Устанавливает бюджет категории или заменяет существующий. Лимит может быть меньше уже
   * потраченного.
   *
   * @param command категория и лимит
   * @return состояние бюджета после установки
   * @throws org.example.finmanager.application.exception.ApplicationException если категория не
   *     найдена среди расходных, она доходная или лимит не проходит проверку
   */
  BudgetResultDto setBudget(SetBudgetCommand command);

  /**
   * Удаляет бюджет категории.
   *
   * @param command категория
   * @throws org.example.finmanager.application.exception.ApplicationException если категории нет
   *     среди расходных или у неё нет бюджета
   */
  void removeBudget(RemoveBudgetCommand command);
}
