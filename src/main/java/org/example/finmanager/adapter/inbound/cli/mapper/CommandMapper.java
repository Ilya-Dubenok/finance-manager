package org.example.finmanager.adapter.inbound.cli.mapper;

import org.example.finmanager.adapter.inbound.cli.dto.AddOperationForm;
import org.example.finmanager.adapter.inbound.cli.dto.CreateCategoryForm;
import org.example.finmanager.adapter.inbound.cli.dto.DeleteCategoryForm;
import org.example.finmanager.adapter.inbound.cli.dto.ExportForm;
import org.example.finmanager.adapter.inbound.cli.dto.ImportForm;
import org.example.finmanager.adapter.inbound.cli.dto.PeriodForm;
import org.example.finmanager.adapter.inbound.cli.dto.RemoveBudgetForm;
import org.example.finmanager.adapter.inbound.cli.dto.RenameCategoryForm;
import org.example.finmanager.adapter.inbound.cli.dto.SetBudgetForm;
import org.example.finmanager.adapter.inbound.cli.dto.TransferForm;
import org.example.finmanager.application.dto.command.AddOperationCommand;
import org.example.finmanager.application.dto.command.CreateCategoryCommand;
import org.example.finmanager.application.dto.command.DeleteCategoryCommand;
import org.example.finmanager.application.dto.command.ExportCommand;
import org.example.finmanager.application.dto.command.ImportCommand;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.command.RemoveBudgetCommand;
import org.example.finmanager.application.dto.command.RenameCategoryCommand;
import org.example.finmanager.application.dto.command.SetBudgetCommand;
import org.example.finmanager.application.dto.command.TransferCommand;
import org.mapstruct.Mapper;

/** Превращает данные диалогов CLI в команды application. */
@Mapper(componentModel = "spring")
public interface CommandMapper {

  /** Собирает команду добавления операции. */
  AddOperationCommand toCommand(String login, AddOperationForm form);

  /** Собирает команду создания категории. */
  CreateCategoryCommand toCommand(String login, CreateCategoryForm form);

  /** Собирает команду переименования категории. */
  RenameCategoryCommand toCommand(String login, RenameCategoryForm form);

  /** Собирает команду удаления категории. */
  DeleteCategoryCommand toCommand(String login, DeleteCategoryForm form);

  /** Собирает команду установки бюджета. */
  SetBudgetCommand toCommand(String login, SetBudgetForm form);

  /** Собирает команду удаления бюджета. */
  RemoveBudgetCommand toCommand(String login, RemoveBudgetForm form);

  /** Собирает команду перевода; отправитель — логин сессии. */
  TransferCommand toCommand(String fromLogin, TransferForm form);

  /** Собирает команду экспорта. */
  ExportCommand toCommand(String login, ExportForm form);

  /** Собирает команду импорта. */
  ImportCommand toCommand(String login, ImportForm form);

  /** Собирает период. */
  PeriodDto toPeriod(PeriodForm form);
}
