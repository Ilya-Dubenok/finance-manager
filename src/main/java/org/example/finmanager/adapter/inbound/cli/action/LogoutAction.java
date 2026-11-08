package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.CliSession;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.springframework.stereotype.Component;

/** Выход из аккаунта: сохраняет данные, завершает сессию и возвращает в меню до входа. */
@Component
@RequiredArgsConstructor
public class LogoutAction implements MenuAction {

  private final CliSession session;
  private final SaveChangesDialog saveChanges;

  @Override
  public MenuOutcome execute() {
    saveChanges.run();
    session.logOut();
    return MenuOutcome.LEAVE;
  }
}
