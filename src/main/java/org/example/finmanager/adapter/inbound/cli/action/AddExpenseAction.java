package org.example.finmanager.adapter.inbound.cli.action;

import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuOutcome;
import org.example.finmanager.application.dto.model.OperationType;
import org.springframework.stereotype.Component;

/** Пункт «Добавить расход». */
@Component
@RequiredArgsConstructor
public class AddExpenseAction implements MenuAction {

  private final AddOperationDialog dialog;

  @Override
  public MenuOutcome execute() {
    dialog.run(OperationType.EXPENSE);
    return MenuOutcome.STAY;
  }
}
