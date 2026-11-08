package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.CreateCategoryForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.command.CreateCategoryCommand;
import org.example.finmanager.application.dto.model.CategoryDto;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.port.inbound.CategoryPort;
import org.springframework.stereotype.Component;

/** Пункт «Создать категорию»: спрашивает тип и название. */
@Component
@RequiredArgsConstructor
public class CreateCategoryAction implements MenuAction {

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final PromptReader prompts;
  private final CategoryPort categories;
  private final CliSession session;
  private final CommandMapper commandMapper;

  @Override
  public MenuOutcome execute() {
    OperationType type = prompts.readType(MessageKeys.PROMPT_TYPE);
    String name = prompts.readRequired(MessageKeys.PROMPT_CATEGORY_NAME);

    CreateCategoryForm form = new CreateCategoryForm(type, name);
    CreateCategoryCommand command = commandMapper.toCommand(session.login(), form);
    CategoryDto created = categories.createCategory(command);

    io.println(messages.get(MessageKeys.CATEGORY_CREATED, created.name(), created.type()));
    return MenuOutcome.STAY;
  }
}
