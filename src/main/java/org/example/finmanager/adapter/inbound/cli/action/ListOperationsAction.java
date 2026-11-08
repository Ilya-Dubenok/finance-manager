package org.example.finmanager.adapter.inbound.cli.action;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.OperationRow;
import org.example.finmanager.adapter.inbound.cli.dto.PeriodForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.mapper.OperationRowMapper;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.adapter.inbound.cli.render.OperationTableRenderer;
import org.example.finmanager.application.dto.command.PeriodDto;
import org.example.finmanager.application.dto.model.OperationDto;
import org.example.finmanager.application.port.inbound.OperationPort;
import org.springframework.stereotype.Component;

/** Пункт «Список операций»: спрашивает необязательный период и показывает таблицу. */
@Component
@RequiredArgsConstructor
public class ListOperationsAction implements MenuAction {

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final PromptReader prompts;
  private final OperationPort operations;
  private final CliSession session;
  private final CommandMapper commandMapper;
  private final OperationRowMapper rowMapper;
  private final OperationTableRenderer tableRenderer;

  @Override
  public MenuOutcome execute() {
    PeriodForm form = prompts.readPeriod();
    PeriodDto period = commandMapper.toPeriod(form);
    List<OperationDto> found = operations.listOperations(session.login(), period);
    if (found.isEmpty()) {
      io.println(messages.get(MessageKeys.INFO_NO_OPERATIONS));
      return MenuOutcome.STAY;
    }
    List<OperationRow> rows = found.stream().map(rowMapper::toRow).toList();
    io.println(tableRenderer.render(rows));
    return MenuOutcome.STAY;
  }
}
