package org.example.finmanager.adapter.inbound.cli.menu;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.finmanager.adapter.inbound.cli.action.AddExpenseAction;
import org.example.finmanager.adapter.inbound.cli.action.AddIncomeAction;
import org.example.finmanager.adapter.inbound.cli.action.ListOperationsAction;
import org.example.finmanager.adapter.inbound.cli.menu.model.Menu;
import org.example.finmanager.adapter.inbound.cli.menu.model.MenuItem;
import org.example.finmanager.adapter.inbound.cli.message.MessageCatalog;
import org.example.finmanager.adapter.inbound.cli.message.MessageKeys;
import org.springframework.stereotype.Component;

/** Меню операций: добавить доход, добавить расход, список операций. */
@Component
@RequiredArgsConstructor
public class OperationsMenu {

  private static final int ADD_INCOME_NUMBER = 1;
  private static final int ADD_EXPENSE_NUMBER = 2;
  private static final int LIST_NUMBER = 3;

  private final MessageCatalog messages;
  private final AddIncomeAction addIncomeAction;
  private final AddExpenseAction addExpenseAction;
  private final ListOperationsAction listOperationsAction;

  /** Строит меню. */
  public Menu build() {
    String title = messages.get(MessageKeys.MENU_OPERATIONS_TITLE);
    String incomeLabel = messages.get(MessageKeys.MENU_OPERATIONS_ADD_INCOME);
    MenuItem income = new MenuItem(ADD_INCOME_NUMBER, incomeLabel, addIncomeAction);
    String expenseLabel = messages.get(MessageKeys.MENU_OPERATIONS_ADD_EXPENSE);
    MenuItem expense = new MenuItem(ADD_EXPENSE_NUMBER, expenseLabel, addExpenseAction);
    String listLabel = messages.get(MessageKeys.MENU_OPERATIONS_LIST);
    MenuItem list = new MenuItem(LIST_NUMBER, listLabel, listOperationsAction);
    MenuItem back = MenuItem.leave(messages.get(MessageKeys.MENU_BACK));
    return new Menu(title, List.of(income, expense, list, back));
  }
}
