package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.dto.DeleteCategoryForm;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.mapper.CommandMapper;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.command.DeleteCategoryCommand;
import org.example.finmanager.application.dto.model.OperationType;
import org.example.finmanager.application.port.inbound.CategoryPort;
import org.springframework.stereotype.Component;

/** Пункт «Удалить категорию»: тип, название и подтверждение. */
@Component
@RequiredArgsConstructor
public class DeleteCategoryAction implements MenuAction {

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
    boolean confirmed =
        prompts.readConfirmation(MessageKeys.PROMPT_CONFIRM_DELETE_CATEGORY, name, type);
    if (!confirmed) {
      io.println(messages.get(MessageKeys.DIALOG_CANCELLED));
      return MenuOutcome.STAY;
    }

    DeleteCategoryCommand command =
        commandMapper.toCommand(session.login(), new DeleteCategoryForm(type, name));
    categories.deleteCategory(command);

    io.println(messages.get(MessageKeys.CATEGORY_DELETED, name, type));
    return MenuOutcome.STAY;
  }
}
