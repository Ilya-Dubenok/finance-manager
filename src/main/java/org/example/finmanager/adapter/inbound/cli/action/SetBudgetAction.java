package org.example.finmanager.adapter.inbound.cli.action;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.SetBudgetForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.adapter.inbound.cli.render.AlertRenderer;
import org.example.finmanager.application.dto.command.SetBudgetCommand;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.dto.result.BudgetResultDto;
import org.example.finmanager.application.dto.result.BudgetStatusDto;
import org.example.finmanager.application.port.inbound.BudgetPort;
import org.springframework.stereotype.Component;

/** Пункт «Установить или изменить бюджет»: спрашивает категорию и лимит. */
@Component
@RequiredArgsConstructor
public class SetBudgetAction implements MenuAction {

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final PromptReader prompts;
  private final BudgetPort budgets;
  private final CliSession session;
  private final CommandMapper commandMapper;
  private final AlertRenderer alertRenderer;
  private final CategoryHint categoryHint;

  @Override
  public MenuOutcome execute() {
    categoryHint.show(OperationType.EXPENSE);
    String category = prompts.readRequired(MessageKeys.PROMPT_CATEGORY);
    BigDecimal limit = prompts.readAmount(MessageKeys.PROMPT_LIMIT);

    SetBudgetForm form = new SetBudgetForm(category, limit);
    SetBudgetCommand command = commandMapper.toCommand(session.login(), form);
    BudgetResultDto result = budgets.setBudget(command);

    BudgetStatusDto status = result.status();
    io.println(
        messages.get(
            MessageKeys.BUDGET_SET,
            status.category(),
            status.limit(),
            status.spent(),
            status.remaining()));
    alertRenderer.render(result.alerts()).forEach(io::println);
    return MenuOutcome.STAY;
  }
}
