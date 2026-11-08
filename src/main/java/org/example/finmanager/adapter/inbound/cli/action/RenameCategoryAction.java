package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.RenameCategoryForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.command.RenameCategoryCommand;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.port.inbound.CategoryPort;
import org.springframework.stereotype.Component;

/** Пункт «Переименовать категорию»: тип, текущее и новое название. */
@Component
@RequiredArgsConstructor
public class RenameCategoryAction implements MenuAction {

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final PromptReader prompts;
  private final CategoryPort categories;
  private final CliSession session;
  private final CommandMapper commandMapper;
  private final CategoryHint categoryHint;

  @Override
  public MenuOutcome execute() {
    OperationType type = prompts.readType(MessageKeys.PROMPT_TYPE);
    categoryHint.show(type);
    String name = prompts.readRequired(MessageKeys.PROMPT_CATEGORY);
    String newName = prompts.readRequired(MessageKeys.PROMPT_NEW_CATEGORY_NAME);

    RenameCategoryForm form = new RenameCategoryForm(type, name, newName);
    RenameCategoryCommand command = commandMapper.toCommand(session.login(), form);
    CategoryDto renamed = categories.renameCategory(command);

    io.println(messages.get(MessageKeys.CATEGORY_RENAMED, name, type, renamed.name()));
    return MenuOutcome.STAY;
  }
}
