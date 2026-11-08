package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.RemoveBudgetForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.command.RemoveBudgetCommand;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.port.inbound.BudgetPort;
import org.springframework.stereotype.Component;

/** Пункт «Удалить бюджет»: спрашивает категорию и подтверждение. */
@Component
@RequiredArgsConstructor
public class RemoveBudgetAction implements MenuAction {

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final PromptReader prompts;
  private final BudgetPort budgets;
  private final CliSession session;
  private final CommandMapper commandMapper;
  private final CategoryHint categoryHint;

  @Override
  public MenuOutcome execute() {
    categoryHint.show(OperationType.EXPENSE);
    String category = prompts.readRequired(MessageKeys.PROMPT_CATEGORY);
    boolean confirmed =
        prompts.readConfirmation(MessageKeys.PROMPT_CONFIRM_REMOVE_BUDGET, category);
    if (!confirmed) {
      io.println(messages.get(MessageKeys.DIALOG_CANCELLED));
      return MenuOutcome.STAY;
    }

    RemoveBudgetCommand command =
        commandMapper.toCommand(session.login(), new RemoveBudgetForm(category));
    budgets.removeBudget(command);

    io.println(messages.get(MessageKeys.BUDGET_REMOVED, category));
    return MenuOutcome.STAY;
  }
}
