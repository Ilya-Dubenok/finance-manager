package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.springframework.stereotype.Component;

/** Выход из приложения из главного меню: сохраняет данные и закрывает все меню. */
@Component
@RequiredArgsConstructor
public class ExitAction implements MenuAction {

  private final SaveChangesDialog saveChanges;

  @Override
  public MenuOutcome execute() {
    saveChanges.run();
    return MenuOutcome.EXIT;
  }
}
