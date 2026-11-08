package org.example.finmanager.adapter.inbound.cli.action;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.AddOperationForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.adapter.inbound.cli.render.AlertRenderer;
import org.example.finmanager.application.dto.command.AddOperationCommand;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.result.OperationResultDto;
import org.example.finmanager.application.port.inbound.OperationPort;
import org.springframework.stereotype.Component;

/** Диалог добавления дохода или расхода: категория, сумма, дата, комментарий. */
@Component
@RequiredArgsConstructor
public class AddOperationDialog {

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final PromptReader prompts;
  private final OperationPort operations;
  private final CliSession session;
  private final CommandMapper commandMapper;
  private final AlertRenderer alertRenderer;
  private final CategoryHint categoryHint;

  /**
   * Проводит диалог и добавляет операцию.
   *
   * @param type тип добавляемой операции
   */
  public void run(OperationType type) {
    categoryHint.show(type);
    String category = prompts.readRequired(MessageKeys.PROMPT_CATEGORY);
    BigDecimal amount = prompts.readAmount(MessageKeys.PROMPT_AMOUNT);
    LocalDate date = prompts.readOptionalDate(MessageKeys.PROMPT_DATE);
    String comment = prompts.readOptional(MessageKeys.PROMPT_COMMENT);

    AddOperationForm form = new AddOperationForm(type, category, amount, date, comment);
    AddOperationCommand command = commandMapper.toCommand(session.login(), form);
    OperationResultDto result = operations.addOperation(command);

    OperationDto added = result.operation();
    String messageKey = successKey(type);
    io.println(messages.get(messageKey, added.category(), added.amount(), added.date()));
    alertRenderer.render(result.alerts()).forEach(io::println);
  }

  private String successKey(OperationType type) {
    if (type == OperationType.INCOME) {
      return MessageKeys.OPERATION_INCOME_ADDED;
    }
    return MessageKeys.OPERATION_EXPENSE_ADDED;
  }
}
