package org.example.finmanager.adapter.inbound.cli.action;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.ImportForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.adapter.inbound.cli.render.AlertRenderer;
import org.example.finmanager.application.dto.command.ImportCommand;
import org.example.finmanager.application.dto.result.ImportResultDto;
import org.example.finmanager.application.port.inbound.ReportPort;
import org.springframework.stereotype.Component;

/** Пункт «Импорт JSON-снимка»: путь к файлу и подтверждение замены кошелька. */
@Component
@RequiredArgsConstructor
public class ImportSnapshotAction implements MenuAction {

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final PromptReader prompts;
  private final ReportPort reports;
  private final CliSession session;
  private final CommandMapper commandMapper;
  private final AlertRenderer alertRenderer;

  @Override
  public MenuOutcome execute() {
    Path source = prompts.readPath(MessageKeys.PROMPT_IMPORT_PATH);
    if (!prompts.readConfirmation(MessageKeys.PROMPT_CONFIRM_IMPORT)) {
      io.println(messages.get(MessageKeys.DIALOG_CANCELLED));
      return MenuOutcome.STAY;
    }

    ImportCommand command = commandMapper.toCommand(session.login(), new ImportForm(source));
    ImportResultDto result = reports.importSnapshot(command);

    io.println(
        messages.get(
            MessageKeys.IMPORT_DONE,
            result.categoryCount(),
            result.budgetCount(),
            result.operationCount()));
    if (result.ownerMismatch()) {
      io.println(
          messages.get(MessageKeys.IMPORT_OWNER_MISMATCH, result.originalOwner(), session.login()));
    }
    alertRenderer.render(result.alerts()).forEach(io::println);
    return MenuOutcome.STAY;
  }
}
