package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.input.PromptReader;
import org.example.finmanager.adapter.inbound.cli.io.ConsoleIo;
import org.example.finmanager.adapter.inbound.cli.menu.MainMenu;
import org.example.finmanager.adapter.inbound.cli.menu.MenuRunner;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.example.finmanager.application.dto.result.AuthenticatedUserDto;
import org.example.finmanager.application.port.inbound.UserAccessPort;
import org.springframework.stereotype.Component;

/** Вход в аккаунт: спрашивает логин и пароль и открывает главное меню. */
@Component
@RequiredArgsConstructor
public class LoginAction implements MenuAction {

  private final ConsoleIo io;
  private final MessageCatalog messages;
  private final PromptReader prompts;
  private final UserAccessPort userAccess;
  private final CliSession session;
  private final MenuRunner menuRunner;
  private final MainMenu mainMenu;

  @Override
  public MenuOutcome execute() {
    io.println(messages.get(MessageKeys.LOGIN_HINT));
    String login = prompts.readRequired(MessageKeys.PROMPT_LOGIN);
    String password = prompts.readSecret(MessageKeys.PROMPT_PASSWORD);
    AuthenticatedUserDto user = userAccess.login(login, password);
    session.logIn(user.login());
    return menuRunner.open(mainMenu::build);
  }
}
