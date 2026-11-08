package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.springframework.stereotype.Component;

/** Справка: выводит описание меню, форматов ввода и правил. */
@Component
@RequiredArgsConstructor
public class HelpAction implements MenuAction {

  private final ConsoleIo io;
  private final MessageCatalog messages;

  @Override
  public MenuOutcome execute() {
    io.println(messages.get(MessageKeys.HELP_TEXT));
    return MenuOutcome.STAY;
  }
}
